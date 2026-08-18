package fansirsqi.xposed.sesame.hook.captcha

import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.VersionHook
import android.content.Context
import android.content.Intent
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.hook.view.PageMonitor
import fansirsqi.xposed.sesame.hook.view.PageMonitor.addHandler
import fansirsqi.xposed.sesame.hook.view.PageMonitor.enableWindowMonitoring

/**
 * 验证码子系统门面：注册页面处理器、版本门槛判断、手动触发验证码。
 *
 * 不拦截/阻止验证码显示，验证码的实际处理由 Captcha1Handler / Captcha2Handler 完成。
 *
 * @author ghostxx
 * @since 2025-10-23
 */
object CaptchaHook {
    private const val TAG = "CaptchaHook"

    /** 验证码对话框类名 */
    private const val CLASS_CAPTCHA_DIALOG = "com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog"

    /** 核身验证码 Activity */
    private const val CLASS_CAPTCHA_SWIPE_ACTIVITY = "com.alipay.mobile.verifyidentity.module.captchaswipe.ui.CaptchaSwipeActivity"

    /** 保存 ClassLoader 供后续反射使用 */
    private var savedClassLoader: ClassLoader? = null

    /** 全部验证码处理器实例（单一事实来源：PageMonitor 注册与锚点监测共用同一批实例） */
    private val captcha1Handler = Captcha1Handler()
    private val captcha2Handler = Captcha2Handler()
    internal val captchaHandlers: List<BaseCaptchaHandler> = listOf(captcha1Handler, captcha2Handler)

    /**
     * 初始化（仅保存 ClassLoader，不注册任何拦截 Hook）
     *
     * @param classLoader 目标应用的 ClassLoader
     */
    fun setupHook(classLoader: ClassLoader) {
        savedClassLoader = classLoader
        Log.record(TAG, "CaptchaHook初始化完成（不拦截验证码）")
    }

    /**
     * 注册验证码页面处理器到 PageMonitor（应在宿主 Application attach 后调用，
     * 此时支付宝版本号已捕获，可做版本门槛判断）
     */
    fun registerHandlers() {
        Log.record(TAG, "准备注册验证码页面处理器，当前版本: ${ApplicationHook.alipayVersion}")
        if (!shouldEnableCaptchaHandling(ApplicationHook.alipayVersion)) {
            Log.record(TAG, "版本不支持自动滑块验证，跳过验证码页面处理器注册")
            return
        }
        Log.record(TAG, "版本检查通过，开始注册验证码页面处理器")
        enableWindowMonitoring(savedClassLoader)
        addHandler("com.alipay.mobile.nebulax.xriver.activity.XRiverActivity", captcha1Handler)
        addHandler("com.alipay.mobile.nebulax.xriver.activity.XRiverTransActivity\$Main", captcha2Handler)
        addHandler("com.alipay.mobile.nebulax.integration.mpaas.activity.NebulaTransActivity\$Main", captcha2Handler)
        Log.record(TAG, "验证码页面处理器注册完成")
    }

    /**
     * 检查目标应用版本是否支持自动滑块验证处理
     * @return true表示版本低于等于12.99.99.99999，支持启用；false表示不支持
     */
    fun shouldEnableCaptchaHandling(version: AlipayVersion): Boolean {
        if (!VersionHook.hasVersion() || version.toString().isEmpty()) {
            Log.record(TAG, "验证码处理版本判断失败：未捕获到目标应用版本")
            return false
        }

        val maxSupported = AlipayVersion("12.99.99.99999")
        if (version > maxSupported) {
            // 只有在不支持时才打印警告
            Log.record(TAG, "目标应用版本 $version 高于 $maxSupported，不支持自动过滑块验证")
            return false
        }

        Log.record(TAG, "验证码处理版本判断通过: $version <= $maxSupported")
        return true
    }

    /**
     * 获取已保存的 ClassLoader
     */
    fun getClassLoader(): ClassLoader? = savedClassLoader

    /**
     * 手动触发验证码显示
     *
     * 依次尝试两种方式，任一成功即返回：
     * 1. 反射实例化 CaptchaDialog 并调用 show()
     * 2. 启动 CaptchaSwipeActivity
     *
     * @return 是否成功触发
     */
    fun triggerCaptcha(): Boolean {
        val classLoader = savedClassLoader
        if (classLoader == null) {
            Log.error(TAG, "❌ ClassLoader未初始化")
            return false
        }

        Log.record(TAG, "🚀 尝试触发验证码...")

        if (triggerCaptchaDialog(classLoader)) {
            return true
        }
        if (triggerCaptchaSwipeActivity(classLoader)) {
            return true
        }

        Log.error(TAG, "❌ 所有验证码触发方式均失败")
        return false
    }

    /**
     * 方式1：反射实例化 CaptchaDialog 并调用 show()。
     * 遍历所有声明构造函数，按常见签名补 null 参数尝试实例化。
     */
    private fun triggerCaptchaDialog(classLoader: ClassLoader): Boolean {
        return try {
            val captchaDialogClass = Class.forName(CLASS_CAPTCHA_DIALOG, false, classLoader)
            Log.record(TAG, "找到CaptchaDialog类: ${captchaDialogClass.name}")

            val context = resolveHostContext() ?: run {
                Log.error(TAG, "⚠️ 无法获取支付宝Context")
                return false
            }

            val constructors = captchaDialogClass.declaredConstructors
            Log.record(TAG, "CaptchaDialog构造函数数量: ${constructors.size}")

            for (constructor in constructors) {
                try {
                    constructor.isAccessible = true
                    val params = constructor.parameterTypes
                    Log.record(TAG, "尝试构造函数: (${params.joinToString { it.simpleName }})")

                    val instance = when (params.size) {
                        1 -> constructor.newInstance(context)
                        2 -> constructor.newInstance(context, null)
                        3 -> constructor.newInstance(context, null, null)
                        else -> continue
                    }

                    // 调用 show() 显示对话框
                    instance.javaClass.methods.find { it.name == "show" }?.let { showMethod ->
                        showMethod.invoke(instance)
                        Log.record(TAG, "✅ CaptchaDialog.show() 调用成功")
                        return true
                    }
                } catch (e: Throwable) {
                    Log.record(TAG, "构造函数失败: ${e.message}")
                }
            }

            Log.record(TAG, "⚠️ CaptchaDialog 所有构造函数均失败")
            false
        } catch (e: Throwable) {
            Log.error(TAG, "⚠️ CaptchaDialog 触发失败")
            Log.printStackTrace(TAG, "CaptchaDialog详情", e)
            false
        }
    }

    /**
     * 方式2：通过 Intent 启动 CaptchaSwipeActivity。
     */
    private fun triggerCaptchaSwipeActivity(classLoader: ClassLoader): Boolean {
        return try {
            Class.forName(CLASS_CAPTCHA_SWIPE_ACTIVITY, false, classLoader)
            Log.record(TAG, "找到CaptchaSwipeActivity类: $CLASS_CAPTCHA_SWIPE_ACTIVITY")

            val context = resolveHostContext() ?: run {
                Log.error(TAG, "⚠️ 无法获取支付宝Context")
                return false
            }

            val intent = Intent().apply {
                setClassName(context, CLASS_CAPTCHA_SWIPE_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Log.record(TAG, "✅ CaptchaSwipeActivity 启动成功")
            true
        } catch (e: Throwable) {
            Log.error(TAG, "⚠️ CaptchaSwipeActivity 启动失败")
            Log.printStackTrace(TAG, "CaptchaSwipe详情", e)
            false
        }
    }

    /**
     * 获取宿主（支付宝）应用 Context
     *
     * 优先取 PageMonitor 缓存的 Context，失败则反射 ActivityThread.currentApplication 兜底。
     */
    private fun resolveHostContext(): Context? {
        return try {
            PageMonitor.getContext()?.let { return it }

            val activityThread = Class.forName(
                "android.app.ActivityThread",
                false,
                CaptchaHook::class.java.classLoader
            )
            ReflectUtil.callStaticMethod(activityThread, "currentApplication") as? Context
        } catch (e: Throwable) {
            Log.record(TAG, "获取Context失败: ${e.message}")
            null
        }
    }
}

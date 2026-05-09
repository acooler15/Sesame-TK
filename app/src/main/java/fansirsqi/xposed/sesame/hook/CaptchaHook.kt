package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.util.Log

/**
 * 验证码触发工具类
 * 
 * 保留ClassLoader，提供手动触发验证码的能力。
 * 不再拦截/阻止验证码显示。
 * 
 * @author ghostxx
 * @since 2025-10-23
 */
object CaptchaHook {
    private const val TAG = "CaptchaHook"

    /** 验证码对话框类名 */
    private const val CLASS_CAPTCHA_DIALOG = "com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog"

    /** 核身验证码Activity */
    private const val CLASS_CAPTCHA_SWIPE_ACTIVITY = "com.alipay.mobile.verifyidentity.module.captchaswipe.ui.CaptchaSwipeActivity"

    /** RPC层验证码调度器 */
    private const val CLASS_RPC_RDS_UTIL_IMPL = "com.alipay.edge.observer.rpc.RpcRdsUtilImpl"

    /** 保存ClassLoader供后续使用 */
    private var savedClassLoader: ClassLoader? = null

    /**
     * 初始化（仅保存ClassLoader，不注册任何拦截Hook）
     *
     * @param classLoader 目标应用的ClassLoader
     */
    fun setupHook(classLoader: ClassLoader) {
        savedClassLoader = classLoader
        Log.record(TAG, "CaptchaHook初始化完成（不拦截验证码）")
    }

    /**
     * 获取已保存的ClassLoader
     */
    fun getClassLoader(): ClassLoader? = savedClassLoader

    /**
     * 手动触发验证码显示
     * 
     * 尝试多种方式触发验证码：
     * 1. 直接实例化 CaptchaDialog 并显示
     * 2. 如果失败，尝试启动 CaptchaSwipeActivity
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

        // 方式1：直接实例化并显示 CaptchaDialog
        if (triggerCaptchaDialog(classLoader)) {
            return true
        }

        // 方式2：尝试启动 CaptchaSwipeActivity
        if (triggerCaptchaSwipeActivity(classLoader)) {
            return true
        }

        Log.error(TAG, "❌ 所有验证码触发方式均失败")
        return false
    }

    /**
     * 通过反射实例化 CaptchaDialog 并调用 show()
     */
    private fun triggerCaptchaDialog(classLoader: ClassLoader): Boolean {
        return try {
            val captchaDialogClass = XposedHelpers.findClass(CLASS_CAPTCHA_DIALOG, classLoader)
            Log.record(TAG, "找到CaptchaDialog类: ${captchaDialogClass.name}")

            // 尝试获取Activity Context
            val context = getAlipayContext()
            if (context == null) {
                Log.error(TAG, "⚠️ 无法获取支付宝Context")
                return false
            }

            // 尝试多种构造函数
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

                    if (instance != null) {
                        // 调用 show() 方法
                        val showMethod = instance.javaClass.methods.find { it.name == "show" }
                        if (showMethod != null) {
                            showMethod.invoke(instance)
                            Log.record(TAG, "✅ CaptchaDialog.show() 调用成功")
                            return true
                        }
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
     * 尝试启动 CaptchaSwipeActivity
     */
    private fun triggerCaptchaSwipeActivity(classLoader: ClassLoader): Boolean {
        return try {
            val activityClass = XposedHelpers.findClass(CLASS_CAPTCHA_SWIPE_ACTIVITY, classLoader)
            Log.record(TAG, "找到CaptchaSwipeActivity类: ${activityClass.name}")

            val context = getAlipayContext()
            if (context == null) {
                Log.error(TAG, "⚠️ 无法获取支付宝Context")
                return false
            }

            // 通过Intent启动Activity
            val intent = android.content.Intent()
            intent.setClassName(context, CLASS_CAPTCHA_SWIPE_ACTIVITY)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
     * 获取支付宝应用Context
     */
    private fun getAlipayContext(): android.content.Context? {
        return try {
            // 方式1: 通过SimplePageManager获取
            val simplePageManagerClass = XposedHelpers.findClass(
                "com.alipay.mobile.nebula.webview.SesameTKWrapper", 
                savedClassLoader!!
            )
            // 如果SimplePageManager有getContext方法
            val ctx = fansirsqi.xposed.sesame.hook.simple.SimplePageManager.getContext()
            if (ctx != null) return ctx

            // 方式2: 通过ActivityThread获取当前Application
            val activityThread = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentApp = XposedHelpers.callStaticMethod(activityThread, "currentApplication")
            currentApp as? android.content.Context
        } catch (e: Throwable) {
            Log.record(TAG, "获取Context失败: ${e.message}")
            null
        }
    }
}

package fansirsqi.xposed.sesame.ui.legacy

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.core.app.Files
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.notify.ToastUtil
import fansirsqi.xposed.sesame.core.util.LanguageUtil
import fansirsqi.xposed.sesame.core.util.PortUtil
import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.compose.CommonAlertDialog
import fansirsqi.xposed.sesame.ui.dto.ModelDto
import fansirsqi.xposed.sesame.ui.dto.ModelFieldInfoDto
import fansirsqi.xposed.sesame.ui.dto.ModelFieldShowDto
import fansirsqi.xposed.sesame.ui.dto.ModelGroupDto
import fansirsqi.xposed.sesame.ui.extension.WatermarkLayer
import fansirsqi.xposed.sesame.ui.model.UiMode
import fansirsqi.xposed.sesame.ui.repository.ConfigRepository
import fansirsqi.xposed.sesame.ui.theme.AppTheme
import fansirsqi.xposed.sesame.ui.theme.ThemeManager
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.ReserveMap
import fansirsqi.xposed.sesame.util.maps.SesameGiftMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap
import org.json.JSONException
import java.nio.charset.StandardCharsets

/**
 * Web 配置页（Compose 外壳 + AndroidView WebView）
 * 由 BaseActivity + activity_web_settings 布局迁移而来：Activity 类型、布局、菜单已 Compose 化，业务逻辑全部保留
 */
class WebSettingsActivity : ComponentActivity() {
    private lateinit var exportLauncher: ActivityResultLauncher<Intent>
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var webView: WebView
    private lateinit var context: Context
    private var userId: String? = null
    private var userName: String? = null
    private val tabList = ArrayList<ModelDto>()
    private val groupList = ArrayList<ModelGroupDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        userId = null
        userName = null
        val intent = intent
        if (intent != null) {
            userId = intent.getStringExtra("userId")
            userName = intent.getStringExtra("userName")
            intent.getBooleanExtra("debug", BuildConfig.DEBUG)
        }
        Model.initAllModel()
        UserMap.setCurrentUserId(userId)
        UserMap.load(userId)
        IdMapManager.getInstance(CooperateMap::class.java).load(userId)
        IdMapManager.getInstance(VitalityRewardsMap::class.java).load(userId)
        IdMapManager.getInstance(MemberBenefitsMap::class.java).load(userId)
        IdMapManager.getInstance(SesameGiftMap::class.java).load(userId)
        IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).load(userId)
        IdMapManager.getInstance(ReserveMap::class.java).load()
        IdMapManager.getInstance(BeachMap::class.java).load()
        Config.load(userId)
        LanguageUtil.setLocale(this)
        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    Log.record(TAG, "WebSettingsActivity.handleOnBackPressed: go back")
                    webView.goBack()
                } else {
                    Log.record(TAG, "WebSettingsActivity.handleOnBackPressed: save")
                    save()
                    finish()
                }
            }
        })

        // 初始化导出逻辑
        exportLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                PortUtil.handleExport(this, result.data!!.data, userId)
            }
        }
        // 初始化导入逻辑
        importLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                PortUtil.handleImport(this, result.data!!.data, userId)
            }
        }
        // tabList/groupList 构建
        val modelConfigMap = Model.getModelConfigMap()
        for ((key, modelConfig) in modelConfigMap) {
            tabList.add(ModelDto(key, modelConfig.name, modelConfig.icon, modelConfig.group?.code, null))
        }
        for (modelGroup in ModelGroup.entries) {
            groupList.add(ModelGroupDto(modelGroup.code, modelGroup.displayName, modelGroup.icon))
        }
        // 渲染 Compose 配置页（原 setContentView(R.layout.activity_web_settings)）
        setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                WatermarkLayer(uidList = listOfNotNull(userId)) {
                    WebSettingsScreen(
                        title = if (userName != null) {
                            "设置: " + userName
                        } else {
                            "设置"
                        },
                        webViewFactory = ::createWebView,
                        onWebViewCreated = { webView = it },
                        onMenuAction = { itemId -> handleMenuAction(itemId) }
                    )
                }
            }
        }
    }

    /**
     * 创建并配置 WebView（原 onCreate 中 findViewById 后的全部配置逻辑）
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(ctx: Context): WebView = WebView(ctx).apply {
        val settings = this.settings
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.loadsImagesAutomatically = true
        settings.defaultTextEncodingName = StandardCharsets.UTF_8.name()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // 强制在当前 WebView 中加载 url
                val requestUrl: Uri = request.url
                val scheme = requestUrl.scheme
                assert(scheme != null)
                return if (
                    scheme.equals("http", ignoreCase = true) ||
                    scheme.equals("https", ignoreCase = true) ||
                    scheme.equals("ws", ignoreCase = true) ||
                    scheme.equals("wss", ignoreCase = true)
                ) {
                    view.loadUrl(requestUrl.toString())
                    true
                } else {
                    view.stopLoading()
                    Toast.makeText(context, "Forbidden Scheme:\"" + scheme + "\"", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
//            loadUrl("http://192.168.31.69:5500/app/src/main/assets/web/index.html")
            loadUrl("file:///android_asset/web/semi_index.html")
        } else {
            loadUrl("file:///android_asset/web/semi_index.html")
        }
        addJavascriptInterface(WebViewCallback(), "HOOK")

        requestFocus()
    }

    private inner class WebViewCallback {
        @JavascriptInterface
        fun getTabs(): String {
            val result = JsonUtil.formatJson(tabList, false)
            if (BuildConfig.DEBUG) {
//                Log.record(TAG, "WebSettingsActivity.getTabs: " + result)
            }
            return result
        }

        /**
         * 新增：检查当前系统是否为深色模式
         */
        @JavascriptInterface
        fun isNightMode(): Boolean {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }

        @JavascriptInterface
        fun getBuildInfo(): String {
            return BuildConfig.APPLICATION_ID + ":" + BuildConfig.VERSION_NAME
        }

        @JavascriptInterface
        fun getGroup(): String {
            val result = JsonUtil.formatJson(groupList, false)
            if (BuildConfig.DEBUG) {
//                Log.record(TAG, "WebSettingsActivity.getGroup: " + result)
            }
            return result
        }

        @JavascriptInterface
        fun getModelByGroup(groupCode: String): String {
            val modelConfigCollection = Model.getGroupModelConfig(ModelGroup.getByCode(groupCode)).values
            val modelDtoList = ArrayList<ModelDto>()
            for (modelConfig in modelConfigCollection) {
                val modelFields = ArrayList<ModelFieldShowDto>()
                for (modelField in modelConfig.fields.values) {
                    modelFields.add(ModelFieldShowDto.toShowDto(modelField))
                }
                modelDtoList.add(ModelDto(modelConfig.code, modelConfig.name, modelConfig.icon, groupCode, modelFields))
            }
            val result = JsonUtil.formatJson(modelDtoList, false)
            if (BuildConfig.DEBUG) {
//                Log.record(TAG, "WebSettingsActivity.getModelByGroup: " + result)
            }
            return result
        }

        @JavascriptInterface
        fun setModelByGroup(groupCode: String, modelsValue: String): String {
            val modelDtoList: List<ModelDto> = JsonUtil.parseObject(modelsValue, object : TypeReference<List<ModelDto>>() {})
            val modelConfigSet = Model.getGroupModelConfig(ModelGroup.getByCode(groupCode))
            for (modelDto in modelDtoList) {
                val modelConfig = modelConfigSet[modelDto.modelCode]
                if (modelConfig != null) {
                    val modelFields = modelDto.modelFields
                    if (modelFields != null) {
                        for (newModelField in modelFields) {
                            val modelField = modelConfig.getModelField(newModelField.code)
                            if (modelField != null) {
                                modelField.setConfigValue(newModelField.configValue)
                            }
                        }
                    }
                }
            }
            return "SUCCESS"
        }

        @JavascriptInterface
        fun getModel(modelCode: String): String? {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                val modelFields = modelConfig.fields
                val list = ArrayList<ModelFieldShowDto>()
                for (modelField in modelFields.values) {
                    list.add(ModelFieldShowDto.toShowDto(modelField))
                }
                val result = JsonUtil.formatJson(list, false)
                if (BuildConfig.DEBUG) {
//                    Log.record(TAG, "WebSettingsActivity.getModel: " + result)
                }
                return result
            }
            return null
        }

        @JavascriptInterface
        fun setModel(modelCode: String, fieldsValue: String): String {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                try {
                    val modelFields = modelConfig.fields
                    val map: Map<String, ModelFieldShowDto>? = JsonUtil.parseObject(
                        fieldsValue,
                        object : TypeReference<Map<String, ModelFieldShowDto>>() {}
                    )
                    if (map != null) {
                        for ((key, newModelField) in map) {
                            if (newModelField != null) {
                                val modelField = modelFields[key]
                                if (modelField != null) {
                                    val configValue = newModelField.configValue
                                    if (configValue == null || configValue.trim { it <= ' ' }.isEmpty()) {
                                        continue
                                    }
                                    modelField.setConfigValue(configValue)
                                }
                            }
                        }
                        return "SUCCESS"
                    }
                } catch (e: Exception) {
                    Log.printStackTrace("WebSettingsActivity", e)
                }
            }
            return "FAILED"
        }

        @JavascriptInterface
        @Throws(JSONException::class)
        fun getField(modelCode: String, fieldCode: String): String? {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                val modelField = modelConfig.getModelField(fieldCode)
                if (modelField != null) {
                    val result = JsonUtil.formatJson(ModelFieldInfoDto.toInfoDto(modelField), false)
                    if (BuildConfig.DEBUG) {
//                        Log.record(TAG, "WebSettingsActivity.getField: " + result)
                    }
                    return result
                }
            }
            return null
        }

        @JavascriptInterface
        fun setField(modelCode: String, fieldCode: String, fieldValue: String?): String {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                try {
                    val modelField = modelConfig.getModelField(fieldCode)
                    if (modelField != null) {
                        modelField.setConfigValue(fieldValue)
                        return "SUCCESS"
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
            return "FAILED"
        }

        /**
         * 新增方法：保存并退出
         * 前端调用 window.HOOK.saveOnExit() 时触发
         */
        @JavascriptInterface
        fun saveOnExit(): Boolean {
            // 切换到主线程执行 UI 操作和保存逻辑
            runOnUiThread {
                Log.record(TAG, "WebViewCallback: saveOnExit called")
                // 1. 调用外部类 WebSettingsActivity 的 save() 方法进行持久化保存
                save()
                // 2. 关闭当前 Activity
                this@WebSettingsActivity.finish()
            }
            return true
        }

        @JavascriptInterface
        fun Log(log: String) {
            Log.record(TAG, "设置：" + log)
        }
    }

    /**
     * 处理菜单项点击，逻辑与原 onOptionsItemSelected 保持一致
     */
    private fun handleMenuAction(itemId: Int) {
        when (itemId) {
            1 -> { // 导出配置
                val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
                exportIntent.type = "*/*"
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + userName + "]-config_v2.json")
                exportLauncher.launch(exportIntent)
            }
            2 -> { // 导入配置
                val importIntent = Intent(Intent.ACTION_GET_CONTENT)
                importIntent.addCategory(Intent.CATEGORY_OPENABLE)
                importIntent.type = "*/*"
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json")
                importLauncher.launch(importIntent)
            }
            3 -> { // 删除配置（经确认对话框后执行）
                val userConfigDirectoryFile = if (StringUtil.isEmpty(userId)) {
                    Files.getDefaultConfigV2File()
                } else {
                    Files.getUserConfigDir(userId!!)
                }
                if (Files.delFile(userConfigDirectoryFile)) {
                    Toast.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            4 -> ListDialog.show( // 查看单向好友列表
                this, "单向好友列表", AlipayUser.getList { user -> user.friendStatus != 1 },
                SelectModelFieldFunc.newMapInstance(), false, ListDialog.ListType.SHOW
            )
            5 -> { // 切换 UI 到新配置页
                ConfigRepository.setUiMode(UiMode.New)
                val intent = Intent(this, SettingActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", userName)
                finish()
                startActivity(intent)
            }
            6 -> { // 保存
                // 在调用 save() 之前，先调用 JS 函数同步 WebView 中的数据到 Java 端
                Log.record(TAG, "WebSettingsActivity.onOptionsItemSelected: Calling handleData() in WebView")
                webView.evaluateJavascript("if(typeof handleData === 'function'){ handleData(); } else { console.error('handleData function not found'); }", null)
                // 使用 Handler 延迟执行 save()，给 JS 一点时间完成异步操作
                // 200 毫秒是一个经验值，如果仍然有问题可以适当增加
                Handler(Looper.getMainLooper()).postDelayed({ save() }, 200) // 延迟 200 毫秒
            }
            7 -> { // 复制 userId 到剪切板
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("userId", this.userId)
                cm.setPrimaryClip(clipData)
                ToastUtil.showToastWithDelay(this, "复制成功！", 100)
            }
        }
    }

    private fun save() {
        if (Config.isModify(userId)) {
            if (Config.save(userId, false)) {
                Toast.makeText(context, "保存成功！", Toast.LENGTH_SHORT).show()
                if (!StringUtil.isEmpty(userId)) {
                    try {
                        val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                        intent.putExtra("userId", userId)
                        sendBroadcast(intent)
                    } catch (th: Throwable) {
                        Log.printStackTrace(th)
                    }
                }
            } else {
                Toast.makeText(context, "保存失败！", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "配置未修改，无需保存！", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "WebSettingsActivity"
    }
}

/**
 * Web 配置页 Compose 外壳：Scaffold + TopAppBar + DropdownMenu 菜单 + AndroidView WebView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebSettingsScreen(
    title: String,
    webViewFactory: (Context) -> WebView,
    onWebViewCreated: (WebView) -> Unit,
    onMenuAction: (Int) -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "菜单")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            // 菜单项顺序与原 onCreateOptionsMenu 一致
                            val menuItems = listOf(
                                "导出配置", "导入配置", "删除配置", "单向好友", "切换UI", "保存", "复制ID"
                            )
                            menuItems.forEachIndexed { index, menuText ->
                                DropdownMenuItem(
                                    text = { Text(menuText) },
                                    onClick = {
                                        menuExpanded = false
                                        if (index + 1 == 3) {
                                            // 删除配置需先确认
                                            showDeleteDialog = true
                                        } else {
                                            onMenuAction(index + 1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        AndroidView(
            factory = { ctx ->
                webViewFactory(ctx).also { createdWebView -> onWebViewCreated(createdWebView) }
            },
            // 保留原布局 activity_web_settings 中 WebView 的 48dp 底部留白
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = 48.dp)
                .fillMaxSize()
        )
    }

    // 删除配置确认对话框，行为与原菜单项 3 的 AlertDialog.Builder 一致
    if (showDeleteDialog) {
        CommonAlertDialog(
            showDialog = true,
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = { onMenuAction(3) },
            title = "警告",
            text = "确认删除该配置？",
            confirmText = "确认",
            dismissText = "取消"
        )
    }
}

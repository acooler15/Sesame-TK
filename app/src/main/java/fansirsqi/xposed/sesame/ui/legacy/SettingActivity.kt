package fansirsqi.xposed.sesame.ui.legacy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.extension.WatermarkLayer
import fansirsqi.xposed.sesame.ui.model.UiMode
import fansirsqi.xposed.sesame.ui.repository.ConfigRepository
import fansirsqi.xposed.sesame.ui.theme.AppTheme
import fansirsqi.xposed.sesame.ui.theme.ThemeManager
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.core.app.Files
import fansirsqi.xposed.sesame.core.util.LanguageUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.PortUtil
import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.core.notify.ToastUtil
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.ReserveMap
import fansirsqi.xposed.sesame.util.maps.SesameGiftMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap

class SettingActivity : ComponentActivity() {
    private lateinit var exportLauncher: ActivityResultLauncher<Intent>
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private var userId: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化用户信息
        val intent = intent
        if (intent != null) {
            this.userId = intent.getStringExtra("userId")
            this.userName = intent.getStringExtra("userName")
        }

        // 初始化各种配置数据
        Model.initAllModel()
        UserMap.setCurrentUserId(this.userId)
        UserMap.load(this.userId)
        IdMapManager.getInstance(CooperateMap::class.java).load(this.userId)
        IdMapManager.getInstance(VitalityRewardsMap::class.java).load()
        IdMapManager.getInstance(MemberBenefitsMap::class.java).load()
        IdMapManager.getInstance(SesameGiftMap::class.java).load()
        IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).load()
        IdMapManager.getInstance(ReserveMap::class.java).load()
        IdMapManager.getInstance(BeachMap::class.java).load()
        Config.load(this.userId)
        // 设置语言
        LanguageUtil.setLocale(this)
        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                save()
                finish()
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
        // 渲染 Compose 配置页
        setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                WatermarkLayer(uidList = listOfNotNull(userId)) {
                    SettingScreen(
                        title = if (this.userName != null) {
                            "设置: " + this.userName
                        } else {
                            "设置"
                        },
                        onMenuAction = { itemId -> handleMenuAction(itemId) }
                    )
                }
            }
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
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + this.userName + "]-config_v2.json")
                exportLauncher.launch(exportIntent)
            }
            2 -> { // 导入配置
                val importIntent = Intent(Intent.ACTION_GET_CONTENT)
                importIntent.addCategory(Intent.CATEGORY_OPENABLE)
                importIntent.type = "*/*"
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json")
                importLauncher.launch(importIntent)
            }
            3 -> deleteConfig() // 删除配置
            4 -> ListDialog.show( // 查看单向好友列表
                this, "单向好友列表", AlipayUser.getList { user -> user.friendStatus != 1 },
                SelectModelFieldFunc.newMapInstance(), false, ListDialog.ListType.SHOW
            )
            5 -> { // 切换到 WEBUI
                ConfigRepository.setUiMode(UiMode.Web)
                val intent = Intent(this, WebSettingsActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", userName)
                finish()
                startActivity(intent)
            }
            6 -> save()
            7 -> {
                // 复制userId到剪切板
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("userId", this.userId)
                cm.setPrimaryClip(clipData)
                ToastUtil.showToastWithDelay(this, "复制成功！", 100)
            }
        }
    }

    /**
     * 删除当前用户配置，逻辑与原菜单项 3 确认后一致
     */
    private fun deleteConfig() {
        val userConfigDirectoryFile = if (StringUtil.isEmpty(this.userId)) {
            Files.getDefaultConfigV2File()
        } else {
            Files.getUserConfigDir(this.userId!!)
        }
        if (Files.delFile(userConfigDirectoryFile)) {
            ToastUtil.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show()
        } else {
            ToastUtil.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun save() {
        try {
            if (Config.isModify(this.userId) && Config.save(this.userId, false)) {
                ToastUtil.showToastWithDelay(this, "保存成功！", 100)
                if (!StringUtil.isEmpty(this.userId)) {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                    intent.putExtra("userId", this.userId)
                    sendBroadcast(intent)
                }
            }
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}

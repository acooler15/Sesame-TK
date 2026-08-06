package fansirsqi.xposed.sesame.ui.legacy

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.BaseActivity
import fansirsqi.xposed.sesame.ui.adapter.ContentPagerAdapter
import fansirsqi.xposed.sesame.ui.adapter.TabAdapter
import fansirsqi.xposed.sesame.ui.extension.WatermarkInjector
import fansirsqi.xposed.sesame.ui.model.UiMode
import fansirsqi.xposed.sesame.ui.repository.ConfigRepository
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.Files
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

class SettingActivity : BaseActivity() {
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
        IdMapManager.getInstance(VitalityRewardsMap::class.java).load(this.userId)
        IdMapManager.getInstance(MemberBenefitsMap::class.java).load(this.userId)
        IdMapManager.getInstance(SesameGiftMap::class.java).load(this.userId)
        IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).load(this.userId)
        IdMapManager.getInstance(ReserveMap::class.java).load()
        IdMapManager.getInstance(BeachMap::class.java).load()
        Config.load(this.userId)
        // 设置语言和布局
        LanguageUtil.setLocale(this)
        setContentView(R.layout.activity_settings)
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
        // 设置副标题
        if (this.userName != null) {
            baseSubtitle = getString(R.string.settings) + ": " + this.userName
        }
        initializeTabs()
        WatermarkInjector.inject(this)
    }

    private fun initializeTabs() {
        try {
            val recyclerTabList = findViewById<RecyclerView>(R.id.recycler_tab_list)
            recyclerTabList.layoutManager = LinearLayoutManager(this)
            val modelConfigMap = Model.getModelConfigMap()
            val tabTitles = ArrayList<String>()
            for (config in modelConfigMap.values) {
                tabTitles.add(config.name!!)
            }
            val viewPager = findViewById<ViewPager2>(R.id.view_pager_content)
            val tabAdapter = TabAdapter(this, tabTitles, object : TabAdapter.OnTabClickListener {
                override fun onTabClick(position: Int) {
                    viewPager.setCurrentItem(position, true)
                }
            })
            recyclerTabList.adapter = tabAdapter
            val contentAdapter = ContentPagerAdapter(supportFragmentManager, lifecycle, modelConfigMap)
            viewPager.adapter = contentAdapter
            viewPager.isUserInputEnabled = false // 禁止用户手动滑动
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    recyclerTabList.smoothScrollToPosition(position)
                    tabAdapter.setSelectedPosition(position)
                }
            })
        } catch (t: Throwable) {
            Log.error(TAG, "初始化Tabs失败: " + t.message)
            Log.printStackTrace(TAG, t)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 创建菜单选项
        menu.add(0, 1, 1, "导出配置")
        menu.add(0, 2, 2, "导入配置")
        menu.add(0, 3, 3, "删除配置")
        menu.add(0, 4, 4, "单向好友")
        menu.add(0, 5, 5, "切换WEBUI")
        menu.add(0, 6, 6, "保存")
        menu.add(0, 7, 7, "复制ID")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 处理菜单项点击事件
        when (item.itemId) {
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
            3 -> AlertDialog.Builder(this) // 删除配置
                .setTitle("警告")
                .setMessage("确认删除该配置？")
                .setPositiveButton(R.string.ok) { _, _ ->
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
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
            4 -> ListDialog.show( // 查看单向好友列表
                this, "单向好友列表", AlipayUser.getList { user -> user.friendStatus != 1 },
                SelectModelFieldFunc.newMapInstance(), false, ListDialog.ListType.SHOW
            )
            5 -> { // 切换到新 UI
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
        return super.onOptionsItemSelected(item)
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

    companion object {
        private val TAG: String = SettingActivity::class.java.simpleName
    }
}

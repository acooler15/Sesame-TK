package fansirsqi.xposed.sesame.core.app
import fansirsqi.xposed.sesame.core.notify.ToastUtil

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import fansirsqi.xposed.sesame.data.General
import java.util.Calendar

object IconManager {
    const val COMPONENT_DEFAULT = General.MODULE_PACKAGE_UI_ICON

    /**
     * 核心方法：根据"用户是否想隐藏"来决定最终状态
     * @param context 上下文
     * @param userWantsHide 用户是否勾选了"隐藏图标"
     */
    fun syncIconState(context: Context, userWantsHide: Boolean) {
        val pm = context.packageManager

        if (userWantsHide) {
            disableComponent(context, pm, COMPONENT_DEFAULT)
        } else {
            enableComponent(context, pm, COMPONENT_DEFAULT)
        }

        if (inDateRange(1, 1, 1)) {
            ToastUtil.showToast(context, "Happy New Year!")
        }
    }

    private fun inDateRange(mon: Int, start: Int, end: Int): Boolean {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return mon == month && (day in start..end)
    }

    private fun enableComponent(context: Context, pm: PackageManager, className: String) {
        val componentName = ComponentName(context, className)
        if (pm.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun disableComponent(context: Context, pm: PackageManager, className: String) {
        val componentName = ComponentName(context, className)
        if (pm.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

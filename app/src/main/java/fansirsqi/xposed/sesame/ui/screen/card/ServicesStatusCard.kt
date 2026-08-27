package fansirsqi.xposed.sesame.ui.screen.card

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fansirsqi.xposed.sesame.core.app.CommandUtil.ServiceStatus
import fansirsqi.xposed.sesame.service.unlock.UnlockAccessibilityService

/**
 * 无障碍服务是否已在系统设置中开启（Compose 侧与主进程同进程，可直接查询系统设置）。
 */
fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, UnlockAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesStatusCard(
    status: ServiceStatus, // 使用新定义的状态
    expanded: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val accessibilityEnabled = isAccessibilityEnabled(context)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp), // 稍微调整间距
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (status) {
                is ServiceStatus.Active -> MaterialTheme.colorScheme.primaryContainer
                is ServiceStatus.Inactive -> MaterialTheme.colorScheme.errorContainer
                is ServiceStatus.Loading -> MaterialTheme.colorScheme.surfaceVariant
                else -> {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            }
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    is ServiceStatus.Active -> {
                        Icon(Icons.Outlined.CheckCircle, "已授权")
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(text = "滑块验证服务正常", style = MaterialTheme.typography.titleMedium)
                            Text(text = "授权方式: ${status.type.displayName}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(text = "", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    is ServiceStatus.Inactive -> {
                        Icon(Icons.Outlined.Warning, "未授权")
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(text = "滑块验证服务不可用", style = MaterialTheme.typography.titleMedium)
                            Text(text = "点击查看解决方案", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is ServiceStatus.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(text = "正在检查服务权限...", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    else -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(text = "正在检查服务权限...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // 内置解锁无障碍状态行
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (accessibilityEnabled) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    if (accessibilityEnabled) "无障碍已开启" else "无障碍未开启"
                )
                Column(Modifier.padding(start = 20.dp)) {
                    Text(
                        text = if (accessibilityEnabled) "内置解锁无障碍服务: 已运行" else "内置解锁无障碍服务: 未开启",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!accessibilityEnabled) {
                        Text(text = "点击跳转系统设置", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 展开内容：故障排查
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = "授权指南", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "本模块需要后台执行 Shell 命令来处理滑块验证。\n\n" +
                                "可选方案：\n" +
                                "1. Shizuku (推荐)：免 Root，需安装 Shizuku APP 并激活。\n" +
                                "2. Root：如果你已 Root，请授予本应用 Root 权限。",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "内置解锁前置条件", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "内置亮屏与锁屏解锁需要：\n" +
                                "1. 开启无障碍服务（上方状态行可跳转开启）。\n" +
                                "2. Root 或 Shizuku 至少其一（Shizuku 用 ADB 启动后重启不自启，需手动拉起）。\n" +
                                "3. 在基础设置中配置锁屏密码与解锁开关。",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

package fansirsqi.xposed.sesame.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import fansirsqi.xposed.sesame.data.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Utility class for handling import and export operations. */
object PortUtil {
    @JvmStatic
    fun handleExport(context: Context, uri: Uri?, userId: String?) {
        if (uri == null) {
            ToastUtil.makeText("未选择目标位置", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val configV2File: File = if (StringUtil.isEmpty(userId)) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId!!)
            }
            val inputStream = FileInputStream(configV2File)
            if (Files.streamTo(inputStream, context.contentResolver.openOutputStream(uri)!!)) {
                ToastUtil.makeText("导出成功！", Toast.LENGTH_SHORT).show()
            } else {
                ToastUtil.makeText("导出失败！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.printStackTrace(e)
            ToastUtil.makeText("导出失败：发生异常", Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun handleImport(context: Context, uri: Uri?, userId: String?) {
        if (uri == null) {
            ToastUtil.makeText("导入失败：未选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val configV2File: File = if (StringUtil.isEmpty(userId)) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId!!)
            }
            val outputStream = FileOutputStream(configV2File)
            if (Files.streamTo(context.contentResolver.openInputStream(uri)!!, outputStream)) {
                ToastUtil.makeText("导入成功！", Toast.LENGTH_SHORT).show()
                if (!StringUtil.isEmpty(userId)) {
                    try {
                        val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                        intent.putExtra("userId", userId)
                        context.sendBroadcast(intent)
                    } catch (th: Throwable) {
                        Log.printStackTrace(th)
                    }
                }
                val intent = (context as Activity).intent
                (context as Activity).finish()
                context.startActivity(intent)
            } else {
                ToastUtil.makeText("导入失败！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.printStackTrace(e)
            ToastUtil.makeText("导入失败：发生异常", Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun save(context: Context, userId: String?) {
        try {
            if (Config.isModify(userId) && Config.save(userId, false)) {
                ToastUtil.showToastWithDelay("保存成功！", 100)
                if (!StringUtil.isEmpty(userId)) {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                    intent.putExtra("userId", userId)
                    context.sendBroadcast(intent)
                }
            }
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}

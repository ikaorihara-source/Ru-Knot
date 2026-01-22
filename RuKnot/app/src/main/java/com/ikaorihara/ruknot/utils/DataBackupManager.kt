package com.ikaorihara.ruknot.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.edit
import com.google.gson.Gson
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.data.repository.AlarmRepository
import com.ikaorihara.ruknot.streamer.StreamerRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 定义要备份的 App 设置 (排除背景路径)
data class AppSettings(
    val languageTag: String,    // 语言
    val themeModeName: String,  // 夜间模式 (存 Enum 名字)
    val roomUpdateInterval: Long,
    val userUpdateInterval: Long,
    val globalVolume: Int,
    val proxyUrl: String
)

// 总备份结构
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val streamers: List<StreamerRoom>,
    val alarms: List<AlarmRule>,
    val unlockedRingtones: Map<String, Boolean>,
    val appSettings: AppSettings? = null // 可空，兼容旧版本
)

object DataBackupManager {
    private val gson = Gson()

    private const val PREF_NAME = "hidden_ringtones_unlock"

    // 导出数据
    suspend fun exportData(
        context: Context,
        repository: AlarmRepository,
        currentSettings: AppSettings
    ) = withContext(Dispatchers.IO) {
        try {
            // 获取数据库数据
            val streamers = repository.getAllStreamersSync()
            val alarms = repository.getAllRulesSync()

            // 获取隐藏铃声解锁状态
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val unlockedMap = mutableMapOf<String, Boolean>()
            prefs.all.forEach { (key, value) ->
                if (value is Boolean) unlockedMap[key] = value
            }

            // 打包所有数据
            val backupData = BackupData(
                streamers = streamers,
                alarms = alarms,
                unlockedRingtones = unlockedMap,
                appSettings = currentSettings
            )

            // JSON 序列化 + 加密
            val jsonString = gson.toJson(backupData)
            val encryptedString = SecurityUtils.encrypt(jsonString)

            // 生成文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "RuKnot_Backup_$timeStamp.rkn"

            // 写入文件 (兼容 Android 10+ Scoped Storage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(encryptedString.toByteArray())
                    }
                }
            } else {
                // 兼容 Android 8.0 - 9.0 (API 26-28): 使用传统文件系统
                // 注意：这需要在 Manifest 声明 WRITE_EXTERNAL_STORAGE，并在运行时申请权限！

                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { output ->
                    output.write(encryptedString.toByteArray())
                }

                // 关键步骤：通知媒体扫描器，否则用户连接电脑或在文件管理器里可能看不到这个新文件
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/octet-stream"),
                    null
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_backup_successful),
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_backup_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 导入数据 (解析 Uri)
    suspend fun importData(
        context: Context,
        uri: Uri,
        repository: AlarmRepository,
        onRestoreSettings: suspend (AppSettings) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // 读取文件
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    stringBuilder.append(reader.readText())
                }
            }
            val encryptedContent = stringBuilder.toString()

            // 解密
            val jsonString = try {
                SecurityUtils.decrypt(encryptedContent)
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_unrecognized_file),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@withContext
            }

            // 解析 JSON
            val data = gson.fromJson(jsonString, BackupData::class.java)

            // 恢复数据
            // 恢复主播
            data.streamers.forEach { repository.insertStreamer(it) } // 假设 insert 是 OnConflictStrategy.REPLACE
            // 恢复闹钟
            data.alarms.forEach { repository.insertRule(it) }

            // 恢复铃声解锁状态
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                data.unlockedRingtones.forEach { (id, isUnlocked) ->
                    putBoolean(id, isUnlocked)
                }
                apply()
            }

            // 恢复设置 (如果有)
            data.appSettings?.let {
                onRestoreSettings(it)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_recovery_successful),
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
package com.ikaorihara.ruknot.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.ikaorihara.ruknot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

// 定义 JSON 结构
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changeLog: String
)

object UpdateManager {
    private val client = OkHttpClient()
    private val gson = Gson()

    // 检查更新
    suspend fun checkUpdate(jsonUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(jsonUrl).build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext null

            val info = gson.fromJson(json, UpdateInfo::class.java)

            // 如果云端版本号 > 当前版本号，则返回更新信息
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                return@withContext info
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    // 下载 APK
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext null

            // 保存到外部缓存目录 (不需要申请存储权限)
            val contentLength = body.contentLength()
            val file = File(context.externalCacheDir, "update.apk")

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)

            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8 * 1024) // 8KB 缓存
                    var bytesCopied: Long = 0
                    var bytesRead: Int

                    // 记录上一次的进度，防止更新太频繁导致 UI 卡顿
                    var lastProgress = 0f

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // 每次循环检查协程是否被取消
                        if (!coroutineContext.isActive) {
                            throw kotlinx.coroutines.CancellationException("User cancelled download")
                        }

                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        if (contentLength > 0) {
                            val currentProgress = bytesCopied.toFloat() / contentLength.toFloat()
                            // 只有进度变化超过 1% 时才通知 UI
                            if (currentProgress - lastProgress >= 0.01f || currentProgress >= 1.0f) {
                                lastProgress = currentProgress
                                onProgress(currentProgress) // 回调进度
                            }
                        }
                    }
                }
            }
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    // 安装 APK (核心难点)
    fun installApk(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // 兼容 Android 7.0+ FileProvider
        val uri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }
}
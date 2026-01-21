package com.ikaorihara.ruknot.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局异常捕获处理类
 * 当程序发生 UncaughtException 时，由该类来接管程序，并记录发送错误报告。
 */
@SuppressLint("StaticFieldLeak")
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var context: Context

    // 初始化
    fun init(context: Context) {
        this.context = context.applicationContext
        // 获取系统默认的 UncaughtException 处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该 CrashHandler 为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handleException(e) && defaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            defaultHandler?.uncaughtException(t, e)
        } else {
            try {
                // 给 3 秒时间让日志写入完成
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "error : ", e)
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(1)
        }
    }

    /**
     * 自定义错误处理，收集错误信息、发送错误报告等操作均在此完成.
     * @return true: 如果处理了该异常信息; otherwise false.
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }

        // 收集设备参数信息
        val infos = collectDeviceInfo(context)

        // 保存日志文件
        saveCrashInfo2File(ex, infos)

        return true
    }

    /**
     * 收集设备参数信息
     */
    private fun collectDeviceInfo(ctx: Context): Map<String, String> {
        val infos = HashMap<String, String>()
        try {
            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            if (pi != null) {
                val versionName = pi.versionName ?: "null"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toString()
                }
                infos["versionName"] = versionName
                infos["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "an error occured when collect package info", e)
        }

        infos["MODEL"] = Build.MODEL
        infos["DEVICE"] = Build.DEVICE
        infos["MANUFACTURER"] = Build.MANUFACTURER
        infos["PRODUCT"] = Build.PRODUCT
        infos["SDK_INT"] = Build.VERSION.SDK_INT.toString()
        infos["RELEASE"] = Build.VERSION.RELEASE

        return infos
    }

    /**
     * 保存错误信息到文件中
     */
    private fun saveCrashInfo2File(ex: Throwable, infos: Map<String, String>) {
        val sb = StringBuffer()

        // 写入设备信息
        sb.append("---------------- Device Info ----------------\n")
        for ((key, value) in infos) {
            sb.append("$key=$value\n")
        }

        // 写入时间
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = System.currentTimeMillis()
        val time = formatter.format(Date())
        sb.append("Time=$time\n")

        // 写入异常堆栈
        sb.append("---------------- Crash Stack ----------------\n")
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)

        try {
            // 文件名: crash-2026-01-20-12-00-00.txt
            val fileName = "crash-$time-$timestamp.txt".replace(":", "-").replace(" ", "-")

            // 路径: /Android/data/com.ikaorihara.ruknot/files/crash_logs/
            val logDir = File(context.getExternalFilesDir(null), "crash_logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val file = File(logDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }

            val fw = FileWriter(file, true)
            val bw = BufferedWriter(fw)
            bw.write(sb.toString())
            bw.flush()
            bw.close()

            Log.e(TAG, "Crash log saved to: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "an error occured while writing file...", e)
        }
    }

    /**
     * 导出最新的日志到系统 Downloads 文件夹
     */
    fun exportLatestLog(context: Context) {
        // 找到私有目录里的日志文件
        val logDir = File(context.getExternalFilesDir(null), "crash_logs")
        if (!logDir.exists() || logDir.listFiles().isNullOrEmpty()) {
            Toast.makeText(context, "暂无崩溃日志", Toast.LENGTH_SHORT).show()
            return
        }

        // 找到最新的那一个文件 (按时间排序)
        val latestFile = logDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash-") }
            ?.maxByOrNull { it.lastModified() }

        if (latestFile == null) {
            Toast.makeText(context, "未找到日志文件", Toast.LENGTH_SHORT).show()
            return
        }

        // 开始复制到公共目录 (Downloads)
        try {
            val fileName = "RuKnot_Log_${latestFile.name}" // 加个前缀，方便用户找

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore (无需权限)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(latestFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    Toast.makeText(context, "已导出到下载目录：$fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                // Android 9 及以下 使用传统文件复制 (需要 WRITE_EXTERNAL_STORAGE 权限，通常你的App应该有了)
                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadDir, fileName)
                latestFile.copyTo(destFile, overwrite = true)
                Toast.makeText(context, "已导出到：${destFile.absolutePath}", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
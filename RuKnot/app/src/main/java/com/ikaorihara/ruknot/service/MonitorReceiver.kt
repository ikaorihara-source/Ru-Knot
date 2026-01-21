package com.ikaorihara.ruknot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class MonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action
        Log.d("MonitorReceiver", "收到广播: $action")

        // 无论是开机广播，还是我们自己设定的 AlarmManager 定时广播
        // 都尝试启动监控服务
        if (action == Intent.ACTION_BOOT_COMPLETED || action == null) { // 假设你定义的重启 || action == "${packageName}.RESTART_SERVICE"
            startMonitorServiceSafely(context)
        }
    }

    private fun startMonitorServiceSafely(context: Context) {
        try {
            val serviceIntent = Intent(context, MonitorService::class.java)

            // Android 8.0 (O) 以上必须用 startForegroundService
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            // 捕获 Android 12+ 的后台启动限制异常
            // 如果 App 在后台被限制启动服务，这里会捕获异常，防止 App 崩溃弹窗
            Log.e("MonitorReceiver", "无法在后台启动服务 (可能是 Android 12+ 限制): ${e.message}")
            e.printStackTrace()

            // 【可选保底策略】
            // 如果真的启动失败了，可以尝试用 WorkManager 或者发送一个普通 Notification 引导用户点进来
            triggerWorkManagerFallback(context)
        }
    }

    private fun triggerWorkManagerFallback(context: Context) {
        // 创建一个加急任务 (Expedited)
        // 加急任务允许在后台立即运行，并赋予临时白名单权限
        val request = OneTimeWorkRequestBuilder<StartServiceWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}

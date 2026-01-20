package com.ikaorihara.ruknot.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ikaorihara.ruknot.MainActivity
import com.ikaorihara.ruknot.R

class StartServiceWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("StartServiceWorker", "保底策略生效：WorkManager 正在尝试启动服务...")

        return try {
            val intent = Intent(context, MonitorService::class.java)

            // 在 Worker 里，我们有短暂的特权可以调用这个
            context.startForegroundService(intent)

            Log.d("StartServiceWorker", "保底成功：服务已拉起")
            Result.success()
        } catch (e: Exception) {
            Log.e("StartServiceWorker", "保底失败：彻底无法启动", e)

            // 终极保底：如果连 WorkManager 都拉不起来，就弹个通知求用户点
            sendCrashNotification(context)

            Result.failure()
        }
    }

    // --- 终极保底通知 ---
    private fun sendCrashNotification(context: Context) {
        val channelId = "service_revive_channel"
        val notificationId = 9999 // 一个特殊的 ID

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道 (必须)
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notify_service_resumption), // 用户可见的渠道名
            NotificationManager.IMPORTANCE_HIGH // 设为 HIGH，尽可能引起注意
        ).apply {
            description = context.getString(R.string.notify_service_resumption_message)
        }
        notificationManager.createNotificationChannel(channel)

        // 准备点击跳转 Intent (跳回 MainActivity)
        // 只要用户点开 App，MainActivity 的 onCreate 里的 startForegroundService 就会自动把服务拉起来
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 确保你有这个图标
            .setContentTitle(context.getString(R.string.notify_service_suspended))
            .setContentText(context.getString(R.string.notify_service_suspended_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 悬浮提示
            .setContentIntent(pendingIntent) // 点击跳转
            .setAutoCancel(true) // 点击后自动消失
            .build()

        // 发送
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
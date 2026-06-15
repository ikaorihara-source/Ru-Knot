package com.ikaorihara.ruknot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 专门处理通知栏按钮点击的接收器
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == "ACTION_DISMISS_ALARM") {
            // 发送广播给 MonitorService 或 Activity 让它们停止播放
            val stopIntent = Intent(context, MonitorService::class.java)
            stopIntent.action = "ACTION_STOP_ALARM_SOUND"

            context.startForegroundService(stopIntent)
        }
    }
}
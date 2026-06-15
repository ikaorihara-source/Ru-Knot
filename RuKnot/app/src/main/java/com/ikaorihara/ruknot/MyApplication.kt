package com.ikaorihara.ruknot

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ikaorihara.ruknot.service.StartServiceWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 在这里注册 WorkManager 心跳
        setupKeepAliveWorker()
    }

    private fun setupKeepAliveWorker() {
        // 定义任务：每 15 分钟检查一次
        val keepAliveRequest = PeriodicWorkRequestBuilder<StartServiceWorker>(
            15, TimeUnit.MINUTES
        ).build()

        // 提交任务
        // 使用 UniquePeriodicWork 配合 KEEP 策略：
        // 如果任务已经存在，就保持原样（不覆盖，不打断）；如果不存在，才新建。
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RuKnotKeepAlive", // 任务唯一 ID
            ExistingPeriodicWorkPolicy.KEEP,
            keepAliveRequest
        )
    }
}
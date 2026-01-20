package com.ikaorihara.ruknot.util // 记得包名要对应

import android.content.Context

/**
 * 获取当前 App 的版本名称 (例如 1.0.0)
 * 读取自 build.gradle 中的 versionName
 */
fun getAppVersionName(context: Context): String {
    return try {
        // 获取包管理器
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        // 返回版本名
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown"
    }
}
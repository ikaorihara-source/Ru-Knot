package com.ikaorihara.ruknot.utils

import android.annotation.SuppressLint
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    // 1. 定义标准的时间格式 "HH:mm" (24小时制)
    @SuppressLint("ConstantLocale")
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    /**
     * 将 "22:00" 这样的字符串转为 LocalTime 对象
     * 如果格式不对，返回 null
     */
    fun parseTime(timeString: String): LocalTime? {
        return try {
            if (timeString.isBlank()) return null
            LocalTime.parse(timeString, TIME_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 将 LocalTime 对象转为 "HH:mm" 字符串
     */
    fun formatTime(time: LocalTime): String {
        return time.format(TIME_FORMATTER)
    }

    /**
     * 检查结束时间是否跨天
     * @return true 表示跨天 (例如 23:00 -> 06:00)
     */
    fun isNextDay(start: String, end: String): Boolean {
        val s = parseTime(start) ?: return false
        val e = parseTime(end) ?: return false
        return e.isBefore(s) // 如果结束时间早于开始时间，说明跨天了
    }

    // 生成人类可读的描述
    // 例如：offset=1 -> "次日"
    //      offset=0 -> "当日"
    fun getDayOffsetLabel(offset: Int): String {
        return when (offset) {
            0 -> "当日" // 或者留空 ""
            1 -> "次日"
            2 -> "后天"
            else -> "+$offset 天"
        }
    }
}
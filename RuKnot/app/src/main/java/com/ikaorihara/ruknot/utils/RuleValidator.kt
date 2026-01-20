package com.ikaorihara.ruknot.utils

import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.data.AlarmType
import java.time.LocalDateTime

object RuleValidator {

    /**
     * 判断当前时间 (now) 是否命中了某条规则 (rule)
     */
    fun isRuleMatch(rule: AlarmRule, now: LocalDateTime): Boolean {
        // 规则必须是启用的
        if (!rule.isEnabled) return false

        // 解析时间为“分钟数” (00:00 = 0, 23:59 = 1439)
        val startMins = parseTimeToMinutes(rule.startTime)
        // 结束时间加上偏移天数 (例如 02:00 + 1天 = 120 + 1440 = 1560)
        val endMins = parseTimeToMinutes(rule.endTime) + (rule.endDayOffset * 24 * 60)

        // 核心检查逻辑：
        // 既要检查“今天”的规则是否覆盖到了现在
        // 也要检查“昨天”的规则是否跨天延续到了现在
        return checkTimeMatch(rule, now, 0, startMins, endMins) ||
                checkTimeMatch(rule, now, -1, startMins, endMins)
    }

    private fun checkTimeMatch(
        rule: AlarmRule,
        now: LocalDateTime,
        dayOffset: Long, // 0 = 查今天, -1 = 查昨天
        ruleStartMins: Int,
        ruleEndMins: Int
    ): Boolean {
        // 计算我们要检查的那一天的日期
        val targetDate = now.plusDays(dayOffset)
        val targetDayOfWeek = targetDate.dayOfWeek.value // 1(周一) - 7(周日)

        // 检查那天是不是用户设定的重复日期
        if (!isDayInPayload(rule, targetDayOfWeek)) {
            return false
        }

        // 计算“当前时间”相对于“目标日期 00:00”过去了多少分钟
        // 例子：现在是周二 01:00 (now)。
        // 如果查周一(昨天, offset=-1)，那现在相对于周一就是 25小时 (1500分钟)。
        val currentMins = (now.hour * 60 + now.minute) + (-dayOffset * 24 * 60).toInt()

        // 判断是否在区间内
        return currentMins in ruleStartMins..<ruleEndMins
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        // 假设格式是 "HH:mm"
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (_: Exception) {
            0 // 出错时返回 0，避免崩溃
        }
    }

    private fun isDayInPayload(rule: AlarmRule, day: Int): Boolean {// 修正逻辑：根据类型严格检查
        return when (rule.type) {
            AlarmType.REPEAT -> {
                // 如果是重复类型，检查具体是周几
                rule.repeatPayload.split(",").contains(day.toString())
            }

            AlarmType.ONCE -> true // 单次闹钟不限日期（由 isTriggered 控制）
        }
    }
}
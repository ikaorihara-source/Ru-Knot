package com.ikaorihara.ruknot.data

import java.io.Serializable

// 必须实现 Serializable 才能通过 Intent 传递 List
data class AlarmItem(
    val streamerName: String,
    val ruleName: String, // 对应原来的 alarmLabel
    val coverUrl: String,
    val isPinned: Boolean, // 用于排序
    val ringtone: String?, // 用于决定铃声
    val roomId: Long,
    val volume: Int,
    val customOrder: Int,
    val isVibrationOnly: Boolean = false
) : Serializable
package com.ikaorihara.ruknot.notification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "type")
    val type: String,     // 区分类型：比如 "DYNAMIC" (动态) 或 "LIVE" (开播)

    @ColumnInfo(name = "room_id")
    val roomId: Long,     // 直播间 ID (仅 type="LIVE" 时必填)

    @ColumnInfo(name = "user_id")
    val userId: Long,     // 主播ID

    @ColumnInfo(name = "dynamic_id")
    val dynamicId: String? = null, // 动态 ID (仅 type="DYNAMIC" 时必填)

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String,

    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false
)
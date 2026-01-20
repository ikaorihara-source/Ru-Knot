package com.ikaorihara.ruknot.streamer

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 这是一个“数据实体”，告诉数据库我们要存哪些信息
@Entity(tableName = "streamer_room_table")
data class StreamerRoom(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "room_id")
    val roomId: Long,       // 房间号 (B站的唯一ID，用作主键)

    @ColumnInfo(name = "user_id")
    val userId: Long,   // 主播ID

    @ColumnInfo(name = "user_name")
    val userName: String,   // 主播名字

    @ColumnInfo(name = "title")
    val title: String,      // 直播间标题

    @ColumnInfo(name = "cover_url")
    val coverUrl: String,   // 封面图片地址

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String,   // 封面图片地址

    @ColumnInfo(name = "is_live")
    val isLive: Boolean = false, // 当前是否在直播

    @ColumnInfo(name = "follower")
    val follower: String, // 当前是否在直播

    @ColumnInfo(name = "like_num")
    val likeNum: String, // 当前是否在直播

    // 如果是 null，就代表使用默认铃声 (default_alarm.wav)
    @ColumnInfo(name = "ringtone_uri")
    val ringtoneUri: String? = null,

    // 是否仅震动
    @ColumnInfo(name = "is_vibration_only")
    val isVibrationOnly: Boolean = false,

    // 总开关 (默认为 true)
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    // 置顶字段
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false, // 默认为 false (不置顶)

    // 锁定状态
    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false,

    // 自定义排序权重
    // 数字越小排越前面。默认给个大数字，或者用 ID 做初始值
    @ColumnInfo(name = "custom_order")
    val customOrder: Int = 0
)
package com.ikaorihara.ruknot.alarm

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ikaorihara.ruknot.data.AlarmType
import com.ikaorihara.ruknot.streamer.StreamerRoom // 需要导入 StreamerRoom

@Entity(
    tableName = "alarm_rules_table",
    // 级联删除：如果删了主播 (StreamerRoom)，它的闹钟规则也会自动消失
    foreignKeys = [
        ForeignKey(
            entity = StreamerRoom::class,
            parentColumns = ["room_id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AlarmRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "room_id", index = true)
    val roomId: Long,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "alarm_name")
    val alarmName: String = "",

    // --- 时间定义 ---
    @ColumnInfo(name = "start_time")
    val startTime: String, // "10:00"

    @ColumnInfo(name = "end_time")
    val endTime: String,   // "10:00"

    // ★★★ 新增：结束日期的偏移量 ★★★
    // 0 = 当天 (同日)
    // 1 = 次日 (+1天)
    // 2 = 后天 (+2天)
    // 以此类推...
    // 用于处理跨夜的时间段，例如: 23:00 到 次日 02:00
    @ColumnInfo(name = "end_day_offset")
    val endDayOffset: Int = 0,

    // --- 重复规则 ---
    @ColumnInfo(name = "type")
    val type: AlarmType, // 需要在 Converters 里处理这个枚举

    // 存储重复的数据，例如 "1,2,3,4,5" 代表周一到周五
    @ColumnInfo(name = "repeat_payload")
    val repeatPayload: String = "",

    // --- 过滤规则 ---
    // (保留这个字段，以便配合刚才写的 UI 进行弹幕关键字过滤)
    @ColumnInfo(name = "keywords")
    val keywords: String = "",

    // ★★★ 防止重复触发 ★★★
    // 记录是否已经触发
    @ColumnInfo(name = "is_triggered")
    val isTriggered: Boolean = false,

    // ★★★ 音量 (0 - 100) ★★★
    @ColumnInfo(name = "volume")
    val volume: Int = 0
)
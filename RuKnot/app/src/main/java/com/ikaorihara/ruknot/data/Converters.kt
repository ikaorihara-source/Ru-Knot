package com.ikaorihara.ruknot.data

import androidx.room.TypeConverter

class Converters {
    // 把枚举转成字符串存进数据库
    @TypeConverter
    fun fromAlarmType(value: AlarmType): String {
        return value.name
    }

    // 把数据库里的字符串转回枚举
    @TypeConverter
    fun toAlarmType(value: String): AlarmType {
        return try {
            AlarmType.valueOf(value) // 从数据库读出时变回枚举
        } catch (_: Exception) {
            AlarmType.REPEAT // 如果出错，给个默认值
        }
    }
}
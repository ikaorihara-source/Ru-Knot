package com.ikaorihara.ruknot.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ikaorihara.ruknot.alarm.AlarmRule // 引用 alarm 包的实体
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDAO {
    // 1. 获取某房间的所有规则 (用于显示列表)
    // Flow 意味着只要数据库变了，界面会自动刷新
    @Query("SELECT * FROM alarm_rules_table ORDER BY id DESC")
    fun getAllRules(): Flow<List<AlarmRule>>

    // 2. 插入或更新规则
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlarmRule)

    // 3. 删除规则
    @Delete
    suspend fun deleteRule(rule: AlarmRule)

    // 4. 获取所有开启的规则 (以后做后台提醒服务要用)
    @Query("SELECT * FROM alarm_rules_table WHERE is_enabled = 1")
    suspend fun getAllEnabledRules(): List<AlarmRule>

    @Query("SELECT * FROM alarm_rules_table WHERE room_id = :roomId")
    suspend fun getAllRulesByStreamerId(roomId: Long): List<AlarmRule>

    // ★★★ 根据主播 RoomID 查找已开启的规则 ★★★
    @Query("SELECT * FROM alarm_rules_table WHERE room_id = :roomId AND is_enabled = 1")
    suspend fun getRulesByStreamerId(roomId: Long): List<AlarmRule>

    // ★★★ 更新规则 (主要用于更新 lastTriggerDate) ★★★
    @Update
    suspend fun updateRule(rule: AlarmRule)

    // 专门用于重置状态 (比如第二天重置，或者下播重置)
    @Query("UPDATE alarm_rules_table SET is_triggered = :isTriggered WHERE id = :id")
    suspend fun updateRuleTriggered(id: Int, isTriggered: Boolean)

    // 专门用于 ONCE 类型的闹钟 (触发后：既要标记已触发，又要关闭开关)
    @Query("UPDATE alarm_rules_table SET is_triggered = :isTriggered, is_enabled = :isEnabled WHERE id = :id")
    suspend fun updateRuleState(id: Int, isTriggered: Boolean, isEnabled: Boolean)

    // 单独控制开关 (UI列表页点击开关时用这个，比全量更新安全)
    @Query("UPDATE alarm_rules_table SET is_enabled = :isEnabled WHERE id = :id")
    suspend fun updateRuleEnabled(id: Int, isEnabled: Boolean)
}
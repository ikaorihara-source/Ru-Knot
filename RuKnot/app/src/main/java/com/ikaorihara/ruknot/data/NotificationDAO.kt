package com.ikaorihara.ruknot.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ikaorihara.ruknot.notification.NotificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: NotificationRecord)

    // 按照时间倒序获取所有通知 (最新的在最上面)
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<NotificationRecord>>

    // 清空历史记录
    @Query("DELETE FROM notification_history")
    suspend fun clearAll()

    @Query("DELETE FROM notification_history WHERE is_locked = 0")
    suspend fun clearAllUnlocked()

    @Delete
    suspend fun deleteRecord(record: NotificationRecord)

    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllRecordsList(): List<NotificationRecord>

    @Query("UPDATE notification_history SET is_locked = :isLocked WHERE id = :id")
    suspend fun updateLockStatus(id: Int, isLocked: Boolean)
}
package com.ikaorihara.ruknot.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ikaorihara.ruknot.streamer.StreamerRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamerDAO {
    // 如果房间号已经存在，就直接忽略
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStreamer(room: StreamerRoom)

    // 删除房间
    @Delete
    suspend fun deleteStreamer(room: StreamerRoom)

    // 3. 获取所有房间
    // 返回 Flow 的意思是：只要数据库里的数据变了，界面会自动收到通知并刷新
    @Query("SELECT * FROM streamer_room_table ORDER BY is_pinned DESC, is_live DESC, custom_order ASC")
    suspend fun getAllStreamersList(): List<StreamerRoom>

    // 获取所有房间
    // 返回 Flow 的意思是：只要数据库里的数据变了，界面会自动收到通知并刷新
    @Query("SELECT * FROM streamer_room_table ORDER BY is_pinned DESC, is_live DESC, custom_order ASC")
    fun getAllStreamersFlow(): Flow<List<StreamerRoom>>

    // 根据房间号查询单个房间 (后面可能会用到)
    @Query("SELECT * FROM streamer_room_table WHERE room_id = :id")
    suspend fun getStreamerById(id: Long): StreamerRoom?

    // 更新房间
    @Update
    suspend fun updateStreamer(room: StreamerRoom)

    // 专门给 checkAllRooms 用：只更新直播状态、标题、封面
    @Query("UPDATE streamer_room_table SET is_live = :isLive, title = :title, cover_url = :coverUrl WHERE room_id = :roomId")
    suspend fun updateRoomStatus(roomId: Long, isLive: Boolean, title: String, coverUrl: String)

    // 专门给 checkAllStreamers 用：只更新名字、头像、粉丝数、点赞
    @Query("UPDATE streamer_room_table SET user_name = :userName, avatar_url = :avatarUrl, follower = :follower, like_num = :likeNum WHERE user_id = :userId")
    suspend fun updateUserInfo(
        userId: Long,
        userName: String,
        avatarUrl: String,
        follower: String,
        likeNum: String
    )

    // 更新置顶状态
    @Query("UPDATE streamer_room_table SET is_pinned = :isPinned WHERE room_id = :roomId")
    suspend fun updatePinStatus(roomId: Long, isPinned: Boolean)

    // 更新锁定状态
    @Query("UPDATE streamer_room_table SET is_locked = :isLocked WHERE room_id = :roomId")
    suspend fun updateLockStatus(roomId: Long, isLocked: Boolean)

    @Update
    suspend fun updateStreamers(rooms: List<StreamerRoom>)

    // 只更新排序字段
    @Query("UPDATE streamer_room_table SET custom_order = :order WHERE room_id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    // 查找未置顶房间中最小的 custom_order
    // 如果没有未置顶房间，会返回 null
    @Query("SELECT MIN(custom_order) FROM streamer_room_table WHERE is_pinned = 0")
    suspend fun getMinUnpinnedCustomOrder(): Int?
}
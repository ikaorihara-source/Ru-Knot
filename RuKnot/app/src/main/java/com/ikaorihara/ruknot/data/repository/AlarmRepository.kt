package com.ikaorihara.ruknot.data.repository // 注意这里多了 .repository

import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.data.AlarmDAO
import com.ikaorihara.ruknot.data.StreamerDAO
import com.ikaorihara.ruknot.streamer.StreamerRoom
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val streamerDAO: StreamerDAO,
    private val alarmDAO: AlarmDAO
) {

    // ==========================================
    // Streamer 相关 (供 ViewModel 使用)
    // ==========================================

    // 提供 Flow 数据流给 UI 观察
    fun getAllStreamersFlow(): Flow<List<StreamerRoom>> {
        return streamerDAO.getAllStreamersFlow()
    }

    suspend fun getAllRulesSync(): List<AlarmRule> {
        return alarmDAO.getAllRulesList()
    }

    // 插入新主播 (AddStreamerDialog 用)
    suspend fun insertStreamer(room: StreamerRoom) {
        streamerDAO.insertStreamer(room)
    }

    // 更新主播 (全量更新，供 UI 开关、修改铃声用)
    suspend fun updateStreamer(room: StreamerRoom) {
        streamerDAO.updateStreamer(room)
    }

    // 删除主播
    suspend fun deleteStreamer(room: StreamerRoom) {
        streamerDAO.deleteStreamer(room)
    }

    // 置顶主播
    suspend fun pinStreamer(roomId: Long, isPinned: Boolean) {
        streamerDAO.updatePinStatus(roomId, isPinned)
    }

    // 锁定/解锁主播
    suspend fun lockStreamer(roomId: Long, isLocked: Boolean) {
        streamerDAO.updateLockStatus(roomId, isLocked)
    }

    // 锁定/解锁主播
    suspend fun updateStreamers(rooms: List<StreamerRoom>) {
        streamerDAO.updateStreamers(rooms)
    }

    // 实现 updateStreamerOrder
    suspend fun updateStreamerOrder(roomId: Long, order: Int) {
        // 调用 DAO 里刚才写的方法
        streamerDAO.updateOrder(roomId, order)
    }

    // 获取未置顶区的最小序号
    suspend fun getMinUnpinnedOrder(): Int {
        // 如果数据库里一个未置顶的都没有，默认返回 0
        return streamerDAO.getMinUnpinnedCustomOrder() ?: 0
    }

    // ==========================================
    // Service 专用 (局部更新，防止数据跳变)
    // ==========================================
    suspend fun getAllStreamersSync(): List<StreamerRoom> {
        return streamerDAO.getAllStreamersList()
    }

    // 专门给 checkAllRooms (Task 1) 用：只更新直播状态、标题、封面
    suspend fun updateRoomStatus(roomId: Long, isLive: Boolean, title: String, coverUrl: String) {
        streamerDAO.updateRoomStatus(roomId, isLive, title, coverUrl)
    }

    // 专门给 checkAllStreamers (Task 2) 用：只更新名字、头像、粉丝数、点赞
    suspend fun updateUserInfo(
        userId: Long,
        userName: String,
        avatarUrl: String,
        follower: String,
        likeNum: String
    ) {
        streamerDAO.updateUserInfo(userId, userName, avatarUrl, follower, likeNum)
    }

    // ==========================================
    // AlarmRule 相关 (供 ViewModel 使用)
    // ==========================================
    fun getAllRulesFlow(): Flow<List<AlarmRule>> {
        return alarmDAO.getAllRules()
    }

    suspend fun insertRule(rule: AlarmRule) {
        alarmDAO.insertRule(rule)
    }

    suspend fun updateRule(rule: AlarmRule) {
        alarmDAO.updateRule(rule)
    }

    suspend fun deleteRule(rule: AlarmRule) {
        alarmDAO.deleteRule(rule)
    }

    suspend fun getRulesByStreamerId(roomId: Long): List<AlarmRule> {
        return alarmDAO.getRulesByStreamerId(roomId)
    }

    suspend fun getAllRulesByStreamerId(roomId: Long): List<AlarmRule> {
        return alarmDAO.getAllRulesByStreamerId(roomId)
    }

    // 专门给 Service 用：只更新触发状态
    suspend fun updateRuleTriggered(id: Int, isTriggered: Boolean) {
        alarmDAO.updateRuleTriggered(id, isTriggered)
    }

    // 专门给 Service 用：用于 ONCE 闹钟触发
    suspend fun updateRuleState(id: Int, isTriggered: Boolean, isEnabled: Boolean) {
        alarmDAO.updateRuleState(id, isTriggered, isEnabled)
    }

    // 给 UI 用：点击列表开关
    suspend fun updateRuleEnabled(id: Int, isEnabled: Boolean) {
        alarmDAO.updateRuleEnabled(id, isEnabled)
    }
}
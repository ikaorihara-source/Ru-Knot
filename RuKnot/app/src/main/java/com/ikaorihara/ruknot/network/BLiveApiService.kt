package com.ikaorihara.ruknot.network

import retrofit2.http.GET
import retrofit2.http.Query

interface BLiveApiService {

    // 查房间 /room/v1/Room/get_info?room_id=xxxx
    @GET("/room/v1/Room/get_info")
    suspend fun getRoomInfo(
        @Query("room_id") roomId: Long
    ): BilibiliApiResponse

    // 查用户 https://api.bilibili.com/x/web-interface/card B站通用的名片接口，不需要 WBI 加密，非常稳定
    @GET("https://api.bilibili.com/x/web-interface/card")
    suspend fun getUserInfo(
        @Query("mid") mid: Long // 注意：这里参数叫 mid (Member ID)，其实就是 uid
    ): BilibiliUserResponse
}
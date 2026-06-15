package com.ikaorihara.ruknot.network

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface BLiveApiService {

    // 查房间 https://api.live.bilibili.com/room/v1/Room/get_info?room_id=xxxx
    @GET("https://api.live.bilibili.com/room/v1/Room/get_info")
    suspend fun getRoomInfo(
        @Query("room_id") roomId: Long
    ): BilibiliApiResponse

    // 查用户 https://api.bilibili.com/x/web-interface/card B站通用的名片接口，不需要 WBI 加密，非常稳定
    @GET("https://api.bilibili.com/x/web-interface/card")
    suspend fun getUserInfo(
        @Query("mid") mid: Long // 注意：这里参数叫 mid (Member ID)，其实就是 uid
    ): BilibiliUserResponse

    // 查动态：使用官方移动端网关 (专门避开 Web 端的底层防火墙 TLS 拦截)
//    @Headers("User-Agent: Mozilla/5.0 BiliDroid/7.84.0 (bbcallen@gmail.com) os/android model/SM-G981B mobi_app/android build/78400100 channel/master innerVer/78400100 osVer/13 network/2")
//    @GET("https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history?mobi_app=android")
    @Headers(
//        "User-Agent: Mozilla/5.0 BiliDroid/7.84.0 (bbcallen@gmail.com) os/android",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Origin: https://space.bilibili.com",
        "Referer: https://space.bilibili.com/",
        "Accept: application/json, text/plain, */*",
        "Sec-Fetch-Site: same-site",
        "Sec-Fetch-Mode: cors",
        "Sec-Fetch-Dest: empty"
    )
    @GET("https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space")
    suspend fun getUserDynamics(
        @Query("host_mid") hostUid: Long
    ): BiliAppDynamicResponse

    // 校验登录状态
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Origin: https://www.bilibili.com",
        "Referer: https://www.bilibili.com/"
    )
    @GET("https://api.bilibili.com/x/web-interface/nav")
    suspend fun checkLoginStatus(): BiliAppDynamicResponse
}
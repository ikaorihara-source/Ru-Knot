package com.ikaorihara.ruknot.network

import com.google.gson.annotations.SerializedName

// 最外层的壳：B站 API 都会返回 code, msg, data
data class BilibiliApiResponse(
    val code: Int,
    val msg: String,
    val data: RoomInfoData? // data 可能会是空的，所以加个问号
)

// 直播间信息
data class RoomInfoData(
    @SerializedName("room_id")
    val roomId: Long,       // 房间号

    @SerializedName("title")
    val title: String,      // 直播标题

    @SerializedName("live_status")
    val liveStatus: Int,    // 核心状态：0=未开播, 1=直播中, 2=轮播

    @SerializedName("user_cover")
    val userCover: String,  // 封面图

    @SerializedName("uid")
    val uId: Long
)
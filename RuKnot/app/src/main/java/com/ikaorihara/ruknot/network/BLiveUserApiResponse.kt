package com.ikaorihara.ruknot.network

import com.google.gson.annotations.SerializedName

// 外层壳
data class BilibiliUserResponse(
    val code: Int,
    val data: UserCardData?
)

// 中层：Card 数据包
data class UserCardData(
    @SerializedName("card")
    val card: UserInfoDetails?,

    @SerializedName("follower")
    val follower: Long,

    @SerializedName("like_num")
    val likeNum: Long
)

// 内层
data class UserInfoDetails(
    @SerializedName("name")
    val name: String,

    @SerializedName("face")
    val avatarUrl: String // 这是高清头像
)
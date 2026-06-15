package com.ikaorihara.ruknot.network

import com.google.gson.annotations.SerializedName

data class BiliAppDynamicResponse(
    val code: Int,
    val data: BiliAppDynamicData?
)

//data class BiliAppDynamicData(
//    val cards: List<BiliAppDynamicItem>?
//)
//
//data class BiliAppDynamicItem(
//    val desc: BiliAppDynamicDesc?,
//    @SerializedName("card")
//    val cardStr: String? // 注意：移动端接口的卡片内容是一个被转义的 JSON 字符串
//)
//
//data class BiliAppDynamicDesc(
//    @SerializedName("dynamic_id_str")
//    val idStr: String,
//
//    @SerializedName("timestamp")
//    val timestamp: Long
//)

data class BiliAppDynamicData(
    @SerializedName("items")
    val items: List<BiliAppDynamicItem>?
)

// 动态项目列表
data class BiliAppDynamicItem(
    @SerializedName("id_str")
    val idStr: String,

    @SerializedName("modules")
    val modules: BiliAppDynamicModules?
)

data class BiliAppDynamicModules(
    @SerializedName("module_author")
    val moduleAuthor: BiliAppModuleAuthor?,

    @SerializedName("module_dynamic")
    val moduleDynamic: BiliAppModuleDynamic?
)

// 时间戳存放位置
data class BiliAppModuleAuthor(
    @SerializedName("pub_ts")
    val pubTs: Long?
)

// 动态正文/标题存放位置
data class BiliAppModuleDynamic(
    @SerializedName("desc")
    val desc: BiliAppDynamicDesc?,

    @SerializedName("major")
    val major: BiliAppDynamicMajor?
)

// 纯图文动态的内容
data class BiliAppDynamicDesc(
    @SerializedName("text")
    val text: String?
)

// 视频/专栏等动态的内容
data class BiliAppDynamicMajor(
    @SerializedName("archive")
    val archive: BiliAppArchive?
)

data class BiliAppArchive(
    @SerializedName("title")
    val title: String?,

    @SerializedName("desc")
    val desc: String?
)
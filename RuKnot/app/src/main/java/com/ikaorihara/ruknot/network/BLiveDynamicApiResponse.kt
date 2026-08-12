package com.ikaorihara.ruknot.network

import com.google.gson.annotations.SerializedName

data class BiliAppDynamicResponse(
    val code: Int,
    val message: String?,
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
    @SerializedName("update_num")
    val updateNum: Int?,

    @SerializedName("update_baseline")
    val updateBaseline: String?,

    @SerializedName("offset")
    val offset: String?,

    @SerializedName("has_more")
    val hasMore: Boolean?,

    @SerializedName("items")
    val items: List<BiliAppDynamicItem>?
)

// 动态项目列表
data class BiliAppDynamicItem(
    @SerializedName("id_str")
    val idStr: String,

    @SerializedName("type")
    val type: String?,

    @SerializedName("modules")
    val modules: BiliAppDynamicModules?,

    @SerializedName("orig")
    val orig: BiliAppDynamicItem?
)

data class BiliAppDynamicModules(
    @SerializedName("module_author")
    val moduleAuthor: BiliAppModuleAuthor?,

    @SerializedName("module_dynamic")
    val moduleDynamic: BiliAppModuleDynamic?
)

// 时间戳存放位置
data class BiliAppModuleAuthor(
    @SerializedName("mid")
    val mid: Long?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("pub_ts")
    val pubTs: Long?,

    @SerializedName("pub_time")
    val pubTime: String?,

    @SerializedName("pub_action")
    val pubAction: String?
)

// 动态正文/标题存放位置
data class BiliAppModuleDynamic(
    @SerializedName("desc")
    val desc: BiliAppDynamicText?,

    @SerializedName("major")
    val major: BiliAppDynamicMajor?,

    @SerializedName("additional")
    val additional: BiliAppDynamicAdditional?,

    @SerializedName("topic")
    val topic: BiliAppTopic?
)

//// 纯图文动态的内容
//data class BiliAppDynamicDesc(
//    @SerializedName("text")
//    val text: String?
//)

// 很多字段内部都有 text 属性（如 desc, summary, desc1 等）
data class BiliAppDynamicText(
    @SerializedName("text")
    val text: String?
)

// 话题信息
data class BiliAppTopic(
    @SerializedName("name")
    val name: String?
)

// 多种媒体类型的载体
data class BiliAppDynamicMajor(
    @SerializedName("type")
    val type: String?,

    @SerializedName("archive")
    val archive: BiliAppArchive?,

    @SerializedName("article")
    val article: BiliAppArticle?,

    @SerializedName("common")
    val common: BiliAppCommon?,

    @SerializedName("opus")
    val opus: BiliAppOpus?,

    @SerializedName("live_rcmd")
    val liveRcmd: BiliAppLiveRcmd?,

    @SerializedName("draw")
    val draw: BiliAppDraw?
)

// 视频投稿
data class BiliAppArchive(
    @SerializedName("bvid")
    val bvid: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("desc")
    val desc: String?
)

// 专栏文章
data class BiliAppArticle(
    @SerializedName("title")
    val title: String?,

    @SerializedName("desc")
    val desc: String?,

    @SerializedName("label")
    val label: String?
)

// 通用卡片(如装扮、活动分享)
data class BiliAppCommon(
    @SerializedName("title")
    val title: String?,

    @SerializedName("desc")
    val desc: String?
)

// 新版图文
data class BiliAppOpus(
    @SerializedName("title")
    val title: String?,

    @SerializedName("summary")
    val summary: BiliAppDynamicText?
)

// 包含图片的动态
data class BiliAppDraw(
    @SerializedName("id")
    val id: Long?
)

// 直播推荐 (B站把直播状态以 JSON 字符串形式塞在 content 里)
data class BiliAppLiveRcmd(
    @SerializedName("content")
    val content: String?
)

// 附加内容(如预约、投票、商品)
data class BiliAppDynamicAdditional(
    @SerializedName("type")
    val type: String?,

    @SerializedName("reserve")
    val reserve: BiliAppReserve?
)

// 直播预约卡片
data class BiliAppReserve(
    @SerializedName("title")
    val title: String?,

    @SerializedName("desc1")
    val desc1: BiliAppDynamicText?,

    @SerializedName("desc2")
    val desc2: BiliAppDynamicText?
)
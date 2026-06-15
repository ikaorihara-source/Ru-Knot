package com.ikaorihara.ruknot.data

import kotlinx.serialization.Serializable

@Serializable
data class RemoteBackground(
    val id: String,
    val name: String,
    val url: String,
    val thumb: String,
    val isVideo: Boolean,
    val size: String
) {
    // 下载和预览都用这个加速后的链接
    val downloadUrl: String get() = url

    // 智能获取缩略图链接
    val thumbUrl: String get() = thumb
}
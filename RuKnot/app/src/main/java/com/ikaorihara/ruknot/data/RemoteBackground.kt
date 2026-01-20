package com.ikaorihara.ruknot.data

import kotlinx.serialization.Serializable

@Serializable
data class RemoteBackground(
    val id: String,
    val name: String,
    val url: String,      // GitHub 原始链接
    val thumb: String,   // 新增这个字段，允许为空(兼容旧数据)
    val isVideo: Boolean,
    val size: String
) {
    // 下载和预览都用这个加速后的链接
    val downloadUrl: String get() = url

    // 智能获取缩略图链接
    val thumbUrl: String get() = thumb
}
package com.ikaorihara.ruknot.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class) // 忽略 API 不稳定警告，Media3 标准写法
@Composable
fun VideoBackground(videoPath: String) {
    val context = LocalContext.current

    // 初始化 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // ★★★ 核心配置 ★★★
            repeatMode = Player.REPEAT_MODE_ALL // 无限循环
            volume = 0f                         // 静音 (Mute)
            playWhenReady = true                // 准备好自动播
        }
    }

    LaunchedEffect(videoPath) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoPath))
        exoPlayer.prepare() // 重新准备播放新资源
    }

    // 渲染 PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // 隐藏进度条、暂停按钮等

                // ★★★ 关键：让视频像 Image(ContentScale.Crop) 一样充满屏幕 ★★★
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // 页面销毁时释放资源 (防止内存泄漏)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
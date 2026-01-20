package com.ikaorihara.ruknot.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * 全局背景容器
 * @param bgResId 背景图资源 ID (-1 表示不使用图片)
 * @param content App 的内容
 */
@Composable
fun AppBackground(
//    bgResId: Int,
    bgPath: String?, // 接受路径
    content: @Composable () -> Unit
) {
    // 判断是否是夜间模式
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 检查文件是否存在
        if (bgPath != null && File(bgPath).exists()) {
            // ★★★ 核心逻辑：判断资源类型 ★★★
            val isVideo = bgPath.endsWith(".mp4") || bgPath.endsWith(".mov")

            if (isVideo) {
                // === 播放视频 ===
                VideoBackground(videoPath = bgPath)
            } else {
                // === 显示图片 ===
                Image(
                    painter = rememberAsyncImagePainter(File(bgPath)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 中层：智能遮罩 (关键逻辑)
            val overlayColor = if (isDark) Color.Black else Color.White

            // 调整这里的 alpha 值来控制深浅
            // 日间：遮 85% 的白色，让图片只显出淡淡的纹理，保证文字看清
            // 夜间：遮 60% 的黑色，让图片变暗，护眼
            val alpha = if (isDark) 0.6f else 0.85f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = alpha))
            )
        } else {
            // 如果没选图片，就用默认的主题背景色
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // 3. 上层：App 内容
        // 这里不需要再包 Surface，或者 Surface 设为透明
        content()
    }
}
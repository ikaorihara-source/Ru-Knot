package com.ikaorihara.ruknot.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.ikaorihara.ruknot.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // 动画状态
    val alphaAnim = remember { Animatable(0f) }
    val context = LocalContext.current

    // 加载你的艺术字体 (防止找不到字体导致崩溃)
    val artTypeface = remember {
        try {
            // 如果你有 art_font.ttf，就加载它
            // 如果没有，就用系统默认的 Serif 字体代替
            val tf = ResourcesCompat.getFont(context, R.font.art_font)
            FontFamily(tf!!)
        } catch (_: Exception) {
            FontFamily.Serif // 备用字体
        }
    }

    LaunchedEffect(Unit) {
        // 淡入 (1秒)
        alphaAnim.animateTo(1f, animationSpec = tween(1000))
        // 停留展示 (1.5秒)
        delay(1500)
        // 结束
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // 整体容器，应用淡入动画
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp) // 给整个页面加边距，防止字贴着屏幕边缘
                .alpha(alphaAnim.value)
        ) {
            // === Logo 区域 ===
            Image(
                // ★★★ 记得替换成你的 Logo ★★★
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(60.dp)) // Logo 和文字的间距

            // === 文字排版区域 ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp) // 两句诗之间的垂直间距
            ) {
                // 靠左
                Text(
                    text = stringResource(R.string.splash_quote_1),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 20.sp,
                    fontFamily = artTypeface,
                    textAlign = TextAlign.Start, // 左对齐
                    lineHeight = 30.sp, // 增加行高，防止字太挤
                    textDecoration = TextDecoration.Underline, // ★ 下划线
                    modifier = Modifier.align(Alignment.Start) // 布局靠左
                )

                // 居中
                Text(
                    text = stringResource(R.string.splash_quote_2),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 20.sp,
                    fontFamily = artTypeface,
                    textAlign = TextAlign.Center, // 右对齐
                    lineHeight = 30.sp,
                    textDecoration = TextDecoration.Underline, // ★ 下划线
                    modifier = Modifier.align(Alignment.CenterHorizontally) // 布局居中
                )

                // 靠右
                Text(
                    text = stringResource(R.string.splash_quote_3),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 20.sp,
                    fontFamily = artTypeface,
                    textAlign = TextAlign.End, // 右对齐
                    lineHeight = 30.sp,
                    textDecoration = TextDecoration.Underline, // ★ 下划线
                    modifier = Modifier.align(Alignment.End) // 布局靠右
                )
            }
        }
    }
}
package com.ikaorihara.ruknot.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// === 夜间模式配色 (Dark Theme) ===
private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80

    // 背景用褐色，字用米白，按钮用黄色
    primary = RuruYellow,           // 主色：黄色
    onPrimary = RuruBrown,          // 主色上的字：褐色 (黄色背景写深色字才看得清)
    primaryContainer = RuruBrownLight,
    onPrimaryContainer = RuruYellow,

    secondary = RuruYellow,
    onSecondary = RuruBrown,

    background = RuruBrown,         // ★★★ 背景：色卡里的深褐色
    onBackground = RuruCream,       // 背景上的字：米白色

    surface = RuruBrownLight,       // 卡片/弹窗背景：稍微浅一点的褐色
    onSurface = RuruCream,          // 卡片上的字：米白色

    surfaceVariant = Color(0xFF4E342E), // 设置页卡片的颜色
    onSurfaceVariant = RuruCream,

    outline = RuruYellow.copy(alpha = 0.5f) // 分割线颜色
)

// === 日间模式配色 (Light Theme) ===
private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */

    // 背景用米白，字用褐色，按钮用黄色
    primary = RuruYellow,           // 主色：黄色 (App 图标色)
    onPrimary = RuruBrown,          // 按钮上的字：褐色
    primaryContainer = RuruCreamDark,
    onPrimaryContainer = RuruBrown,

    secondary = RuruBrown,          // 次要操作：褐色
    onSecondary = Color.White,

    background = RuruCream,         // ★★★ 背景：色卡里的米白色
    onBackground = RuruBrownText,   // 背景上的字：深褐色 (代替纯黑)

    surface = Color.White,          // 卡片背景：纯白 (在米白背景上更突出)
    onSurface = RuruBrownText,      // 卡片上的字

    surfaceVariant = RuruCreamDark, // 设置页卡片颜色
    onSurfaceVariant = RuruBrownText,

    outline = RuruBrown.copy(alpha = 0.5f)
)

@Composable
fun RuKnotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // 关掉动态取色，强制使用我们的露露配色！
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            // 如果是日间模式，状态栏图标变黑；夜间模式则变白
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
package com.ikaorihara.ruknot.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// === 1. 从色卡提取的颜色 ===
val RuruYellow = Color(0xFFFFCC80) // 色卡左边：温暖的黄色 (主色/高亮)
val RuruBrown = Color(0xFF5E4032) // 色卡中间：深褐色 (夜间背景/日间文字)
val RuruCream = Color(0xFFFFF8E7) // 色卡右边：米白色 (日间背景/夜间文字)

// === 2. 衍生颜色 (为了更有层次感) ===

// 日间模式辅助
val RuruCreamDark = Color(0xFFF5E6D0) // 稍微深一点的米色，用于卡片背景
val RuruBrownText = Color(0xFF4E342E) //以此作为正文黑色，比纯黑柔和

// 夜间模式辅助
val RuruBrownLight = Color(0xFF795548) // 稍微浅一点的褐色，用于卡片背景
val RuruYellowDark = Color(0xFFFFA726) // 深一点的黄色，用于夜间模式的高亮
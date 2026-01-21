package com.ikaorihara.ruknot.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ikaorihara.ruknot.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    @DrawableRes val icon: Int
) {
    object Home : BottomNavItem("rooms", R.string.nav_room, R.drawable.ic_room)
    object Alarms : BottomNavItem("alarms", R.string.nav_alarm, R.drawable.ic_alarm)
    object Links : BottomNavItem("links", R.string.nav_links, R.drawable.ic_link)
    object Settings : BottomNavItem("settings", R.string.nav_setting, R.drawable.ic_setting)
}
package com.ikaorihara.ruknot.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. 定义三种模式
enum class AppThemeMode {
    FOLLOW_SYSTEM, // 跟随系统 (默认)
    LIGHT,         // 强制浅色
    DARK           // 强制深色
}

// 扩展属性：创建 DataStore
private val Context.dataStore by preferencesDataStore(name = "settings_pref")

class ThemeStorage(private val context: Context) {
    // 定义 Key
    private val THEME_KEY = intPreferencesKey("theme_mode")

    // 读取设置 (返回 Flow，一改动界面自动刷新)
    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val index = preferences[THEME_KEY] ?: AppThemeMode.FOLLOW_SYSTEM.ordinal
        AppThemeMode.entries[index]
    }

    // 保存设置
    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.ordinal
        }
    }
}
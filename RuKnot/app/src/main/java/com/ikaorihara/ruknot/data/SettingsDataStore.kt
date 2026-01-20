package com.ikaorihara.ruknot.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 扩展属性，单例 DataStore
private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_ROOM_UPDATE_INTERVAL = longPreferencesKey("room_update_interval")
        private val KEY_USER_UPDATE_INTERVAL = longPreferencesKey("user_update_interval")
        private val KEY_GLOBAL_VOLUME = intPreferencesKey("global_volume")
        private val KEY_BACKGROUND_PATH = stringPreferencesKey("app_background_bg_path")
        private val KEY_RANDOM_BACKGROUND = booleanPreferencesKey("is_random_background")
        private val KEY_GITHUB_PROXY_URL = stringPreferencesKey("github_proxy_url")
    }

    // 房间数据更新频率 (默认 30秒)
    val roomUpdateInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ROOM_UPDATE_INTERVAL] ?: 30L
        }

    // 主播信息更新频率 (默认 60秒)
    val userUpdateInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_USER_UPDATE_INTERVAL] ?: 60L
        }

    // 保存设置的方法
    suspend fun saveRoomUpdateInterval(seconds: Long) {
        context.dataStore.edit { it[KEY_ROOM_UPDATE_INTERVAL] = seconds }
    }

    suspend fun saveUserUpdateInterval(seconds: Long) {
        context.dataStore.edit { it[KEY_USER_UPDATE_INTERVAL] = seconds }
    }

    // 读取全局音量 (默认值设为 100)
    val globalVolume: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_GLOBAL_VOLUME] ?: 80
        }

    // 保存全局音量
    suspend fun setGlobalVolume(volume: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GLOBAL_VOLUME] = volume
        }
    }

    // 改读取逻辑
    val backgroundPath: Flow<String?> = context.dataStore.data
        .map { preferences ->
            // 默认为 null 或者 "default"
            preferences[KEY_BACKGROUND_PATH]
        }

    // 改保存逻辑
    suspend fun saveBackgroundPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_PATH] = path
        }
    }

    // 读取随机开关状态
    val isRandomBackground: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_RANDOM_BACKGROUND] ?: false // 默认关闭
        }

    // 保存随机开关状态
    suspend fun setRandomBackground(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RANDOM_BACKGROUND] = enabled
        }
    }

    val githubProxyUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            // 默认值使用
            preferences[KEY_GITHUB_PROXY_URL] ?: "https://v6.gh-proxy.org/"
        }

    suspend fun saveGithubProxyUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GITHUB_PROXY_URL] = url
        }
    }
}
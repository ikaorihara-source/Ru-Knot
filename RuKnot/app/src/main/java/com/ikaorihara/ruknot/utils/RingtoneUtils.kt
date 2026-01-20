package com.ikaorihara.ruknot.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.ikaorihara.ruknot.R
import java.time.LocalTime
import kotlin.random.Random

object RingtoneUtils {

    // 特殊标记：表示用户选择了“默认/随机”
    const val DEFAULT_RINGTONE_URI = "app_default_random"
    private const val PREF_NAME = "hidden_ringtones_unlock"

    // 定义内置铃声列表 (名字 + 资源ID)
    data class BuiltInRingtone(
        val id: String, // 给每个铃声一个唯一ID，方便存状态
        val name: String,
        val resId: Int,
        val isHidden: Boolean = false // 标记是否是隐藏款
    )

    val builtInRingtones = listOf(
        BuiltInRingtone(
            "moonlight_distance",
            "握不住的月光-现实的距离",
            R.raw.ruru_moonlight_mirage_01
        ),
        BuiltInRingtone(
            "moonlight_sink",
            "握不住的月光-清醒的沉沦",
            R.raw.ruru_moonlight_mirage_02
        ),
        BuiltInRingtone(
            "moonlight_wish",
            "握不住的月光-唯一的愿望",
            R.raw.ruru_moonlight_mirage_03
        ),
        BuiltInRingtone("ika_ask", "伊卡让我录个闹钟……", R.raw.ruru_ika_ask_record_alarm),
        BuiltInRingtone("its_ringing", "喂——，响了哦。", R.raw.ruru_its_ringing),
        BuiltInRingtone("its_wakeup", "现在是起床时间。", R.raw.ruru_its_wakeup_time),
        BuiltInRingtone("suona", "噢老弟，该起床了。", R.raw.ruru_suona),
        BuiltInRingtone("watch_phone", "喂！看手机！看手机！", R.raw.ruru_watch_phone),

        BuiltInRingtone("hidden_ssr", "那个……我开播了…", R.raw.ruru_special_01, true),
        BuiltInRingtone("hidden_ur", "该起床了~宝宝~", R.raw.ruru_special_02, true)
    )

    /**
     * ★★★ 核心逻辑：获取最终要播放的 URI ★★★
     * 输入：数据库里存的 uriString
     * 输出：真正能给 MediaPlayer 播放的 Uri
     */
    fun getEffectiveRingtone(
        context: Context,
        savedUriString: String,
        toggleHidden: Boolean = false
    ): Uri {
        // 如果是自定义文件 (content://...)，直接解析返回
        if (savedUriString != DEFAULT_RINGTONE_URI && savedUriString.contains("://")) {
            return savedUriString.toUri()
        }

        // 如果是内置铃声的具体某一个 (android.resource://...)，也直接返回
        if (savedUriString != DEFAULT_RINGTONE_URI) {
            // 简单的检查，或者你可以遍历 builtInRingtones 对比 ID
            return savedUriString.toUri()
        }

        // 如果是“默认”，则根据时间自动选择
        val targetResId = pickRingtoneByTime(context, toggleHidden)

        return getUriFromRaw(context, targetResId)
    }

    // 根据当前时间选择一个资源ID
    private fun pickRingtoneByTime(context: Context, toggleHidden: Boolean): Int {

        if (toggleHidden) {
            val chance = Random.nextDouble()

            // 1.5% 概率 (0.0 <= chance < 0.015) 触发 [9]
            if (chance < 0.015) {
                unlockRingtone(context, 9) // 触发了解锁！
                return builtInRingtones[9].resId
            }

            // 3.5% 概率 (0.015 <= chance < 0.05) 触发 [8]
            // 这里的 0.05 是因为 1.5% + 3.5% = 5%
            if (chance in 0.015..<0.05) {
                unlockRingtone(context, 8) // 触发了解锁！
                return builtInRingtones[8].resId
            }
        }

        val hour = LocalTime.now().hour
        // 简单的根据时间段映射 (你可以根据需求改得更复杂)
        return when (hour) {
            in 6..10 -> builtInRingtones[5].resId   // 06:00 - 10:59 早晨
            in 11..14 -> builtInRingtones[4].resId // 11:00 - 14:59 上午
            in 15..18 -> builtInRingtones[3].resId // 15:00 - 18:59 下午
            in 19..23 -> builtInRingtones[7].resId // 19:00 - 23:59 晚上
            else -> builtInRingtones[6].resId      // 深夜
        }
    }

    // 定义广播的 Action 常量，方便两边统一引用
    const val ACTION_RINGTONE_UNLOCKED = "com.ikaorihara.ruknot.ACTION_RINGTONE_UNLOCKED"

    // --- 解锁逻辑 ---
    private fun unlockRingtone(context: Context, index: Int) {
        val target = builtInRingtones[index]
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 如果还没解锁过，就保存一下，并且可以弹个 Notification 或者 Toast 告诉用户
        // 但因为这里通常是在 Service 后台运行，弹 Toast 可能看不到，
        // 建议单纯保存状态，下次用户打开 App 列表看到新铃声会很惊喜
        if (!prefs.getBoolean(target.id, false)) {
            prefs.edit {
                putBoolean(target.id, true)
            }

            // 发送广播通知 UI 刷新
            // 指定包名，防止发送给其他 App，提高安全性
            val intent = android.content.Intent(ACTION_RINGTONE_UNLOCKED).apply {
                setPackage(context.packageName)
                putExtra("unlocked_name", target.name) // 顺便把名字传出去，万一 UI 想弹窗恭喜
            }
            context.sendBroadcast(intent)
        }
    }

    /**
     * ★★★ 给 UI 层用的：获取当前“可见”的铃声列表 ★★★
     * 自动过滤掉还没解锁的隐藏款
     */
    fun getVisibleRingtones(context: Context): List<BuiltInRingtone> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        return builtInRingtones.filter { ringtone ->
            if (ringtone.isHidden) {
                // 如果是隐藏款，必须查表看是否已解锁
                prefs.getBoolean(ringtone.id, false)
            } else {
                // 普通款永远可见
                true
            }
        }
    }

    // 辅助方法：把 raw id 转成 uri
    fun getUriFromRaw(context: Context, resId: Int): Uri {
        return "android.resource://${context.packageName}/$resId".toUri()
    }
}
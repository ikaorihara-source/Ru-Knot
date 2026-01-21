import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.utils.RingtoneUtils

// RingtoneSelectDialog.kt

@Composable
fun RingtoneSelectionList(
    currentUri: String?, // 允许为 null
    enabled: Boolean = true,
    onRingtoneSelected: (String) -> Unit,
    onPickCustom: () -> Unit
) {
    val context = LocalContext.current

    // ★★★ 修改 1: 获取可见列表 (过滤掉未解锁的 SSR/UR) ★★★
    // 使用 remember 避免每次重绘都读 SharedPreferences
    var visibleRingtones = remember {
        RingtoneUtils.getVisibleRingtones(context)
    }

    // 处理 null 情况，如果为 null 视为默认
    val safeCurrentUri = if (enabled) (currentUri ?: RingtoneUtils.DEFAULT_RINGTONE_URI) else ""

    // 注册广播接收器
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == RingtoneUtils.ACTION_RINGTONE_UNLOCKED) {
                    // 收到解锁通知，重新获取列表
                    visibleRingtones = RingtoneUtils.getVisibleRingtones(context)
                    // 可选：在这里弹个 Toast 恭喜用户
                    Toast.makeText(context, R.string.toast_hidden_ringtone, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Android 13+ 需要指定 RECEIVER_NOT_EXPORTED，旧版本不需要
        val filter = android.content.IntentFilter(RingtoneUtils.ACTION_RINGTONE_UNLOCKED)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        // 如果被禁用，降低整体透明度，视觉上变灰
        modifier = Modifier.alpha(if (enabled) 1f else 0.38f)
    ) {
        // 默认/智能选项
        RingtoneItem(
            name = stringResource(R.string.ringtone_default),
            isSelected = safeCurrentUri == RingtoneUtils.DEFAULT_RINGTONE_URI,
            enabled = enabled,
            onClick = { if (enabled) onRingtoneSelected(RingtoneUtils.DEFAULT_RINGTONE_URI) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 遍历 visibleRingtones 而不是 builtInRingtones
        visibleRingtones.forEach { ringtone ->
            val thisUriStr = RingtoneUtils.getUriFromRaw(context, ringtone.resId).toString()

            // 如果是隐藏款，可以在名字前加个标记
            val displayName = if (ringtone.isHidden) "✨ ${ringtone.name}" else ringtone.name

            RingtoneItem(
                name = displayName,
                isSelected = safeCurrentUri == thisUriStr,
                enabled = enabled,
                onClick = { onRingtoneSelected(thisUriStr) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 自定义选项
        val isCustomSelected =
            safeCurrentUri != RingtoneUtils.DEFAULT_RINGTONE_URI && safeCurrentUri.isNotEmpty() &&
                    visibleRingtones.none {
                        RingtoneUtils.getUriFromRaw(context, it.resId).toString() == safeCurrentUri
                    }

        RingtoneItem(
            name = if (isCustomSelected) stringResource(R.string.ringtone_customize_selected) else stringResource(
                R.string.ringtone_customize
            ),
            isSelected = isCustomSelected,
            enabled = enabled,
            onClick = { onPickCustom() }
        )
    }
}

@Composable
fun RingtoneItem(
    name: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = if (enabled) onClick else null, // RadioButton 也要能点
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = name)
    }
}
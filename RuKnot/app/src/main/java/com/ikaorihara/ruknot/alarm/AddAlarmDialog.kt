package com.ikaorihara.ruknot.alarm

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.data.AlarmType
import com.ikaorihara.ruknot.streamer.StreamerRoom

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    streamers: List<StreamerRoom>,
    existingRule: AlarmRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (AlarmRule) -> Unit
) {
    val context = LocalContext.current

    // === 解析旧数据 (如果存在) ===
    // 你的时间存的是 "10:30" 这种字符串，需要拆开给 TimePicker 用
    fun parseTime(timeStr: String?): Pair<Int, Int> {
        if (timeStr.isNullOrEmpty()) return 0 to 0 // 默认 12:00
        val parts = timeStr.split(":")
        return if (parts.size == 2) parts[0].toInt() to parts[1].toInt() else 0 to 0
    }

    val (initStartH, initStartM) = parseTime(existingRule?.startTime)
    val (initEndH, initEndM) = parseTime(existingRule?.endTime)

    // === 状态定义 ===
    var selectedStreamer by remember {
        mutableStateOf(
            if (existingRule != null) streamers.find { it.roomId == existingRule.roomId }
            else if (streamers.isNotEmpty()) streamers[0] else null
        )
    }
    var expandStreamerDropdown by remember { mutableStateOf(false) }

    // 开始时间状态
    val startTimeState = rememberTimePickerState(
        initialHour = initStartH,
        initialMinute = initStartM,
        is24Hour = true
    )
    // 结束时间状态
    val endTimeState = rememberTimePickerState(
        initialHour = initEndH,
        initialMinute = initEndM,
        is24Hour = true
    )

    var keywords by remember { mutableStateOf(existingRule?.keywords ?: "") }

    // 解析 RepeatPayload (字符串 "1,2,3" -> Set<Int>)
    var selectedDays by remember {
        mutableStateOf(
            if (!existingRule?.repeatPayload.isNullOrEmpty()) {
                existingRule.repeatPayload.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            } else {
                emptySet()
            }
        )
    }

    // 名字状态 (回显旧名字)
    var alarmName by remember { mutableStateOf(existingRule?.alarmName ?: "") }

    // ★★★ 音量状态 (0f - 100f) ★★★
    var volume by remember { mutableFloatStateOf(existingRule?.volume?.toFloat() ?: 0f) }

    // ★★★ 预览播放器逻辑 ★★★
    val previewPlayer = remember { MediaPlayer() }

    // 当对话框关闭时，释放播放器
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (previewPlayer.isPlaying) previewPlayer.stop()
                previewPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 播放预览函数
    fun playPreview() {
        try {
            previewPlayer.reset()
            // 获取当前选中主播的铃声，如果没有则用默认
            val uri = selectedStreamer?.ringtoneUri?.toUri()
                ?: "android.resource://${context.packageName}/${R.raw.default_alarm}".toUri()

            previewPlayer.setDataSource(context, uri)
            previewPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA) // 预览时走媒体声道即可，不用惊动闹钟声道
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            // 设置音量 (0.0 - 1.0)
            val vol = volume / 100f
            previewPlayer.setVolume(vol, vol)

            previewPlayer.prepare()
            previewPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 当音量改变时，实时调整播放器音量（如果正在播放）
    LaunchedEffect(volume) {
        if (previewPlayer.isPlaying) {
            val vol = volume / 100f
            previewPlayer.setVolume(vol, vol)
        }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        title = {
            val titleId =
                if (existingRule == null) R.string.add_alarm_title else R.string.edit_alarm_title
            Text(stringResource(titleId))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = alarmName,
                    onValueChange = { alarmName = it },
                    label = { Text(stringResource(R.string.label_alarm_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // --- 主播选择 ---
                Box {
                    val displayText = selectedStreamer?.userName
                        ?: if (streamers.isEmpty()) stringResource(R.string.no_streamer_available)
                        else stringResource(R.string.select_streamer_placeholder)

                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.link_streamer)) },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (streamers.isNotEmpty()) expandStreamerDropdown = true
                            },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                if (streamers.isNotEmpty()) expandStreamerDropdown = true
                            })
                    DropdownMenu(
                        expanded = expandStreamerDropdown,
                        onDismissRequest = { expandStreamerDropdown = false }
                    ) {
                        streamers.forEach { streamer ->
                            DropdownMenuItem(
                                text = { Text(streamer.userName) },
                                onClick = {
                                    selectedStreamer = streamer
                                    expandStreamerDropdown = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // ★★★ 音量设置区域 ★★★
                val volumeLabel = if (volume.toInt() == 0) {
                    stringResource(
                        R.string.label_volume,
                        stringResource(R.string.label_global_volume)
                    )
                } else {
                    stringResource(R.string.label_volume, "${volume.toInt()}%")
                }

                Text(
                    text = volumeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (volume.toInt() == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (volume.toInt() == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        // 拖动结束时播放预览，以免一直拖一直重置
                        onValueChangeFinished = { playPreview() },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }

                HorizontalDivider()

                // --- 时间范围选择 ---
                Text(
                    stringResource(R.string.label_active_time_range),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 开始时间
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally // 居中显示
                ) {
                    Text(
                        stringResource(R.string.label_start_time),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInput(state = startTimeState)
                }

//                Spacer(modifier = Modifier.height(2.dp)) // 中间加点空隙

                // 结束时间
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally // 居中显示
                ) {
                    Text(
                        stringResource(R.string.label_end_time),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInput(state = endTimeState)
                }

                HorizontalDivider()

                // --- 星期选择 ---
                Text(
                    stringResource(R.string.label_repeat_days),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysLabels = listOf("1", "2", "3", "4", "5", "6", "7")
                    daysLabels.forEachIndexed { index, label ->
                        val dayValue = index + 1
                        val isSelected = selectedDays.contains(dayValue)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedDays =
                                        if (isSelected) selectedDays - dayValue else selectedDays + dayValue
                                }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // --- 关键字 ---
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text(stringResource(R.string.label_keywords)) },
                    leadingIcon = { Icon(Icons.Default.Keyboard, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedStreamer != null,
                onClick = {
                    selectedStreamer?.let { streamer ->
                        // === 核心：数据转换逻辑 ===

                        // 格式化时间为 "HH:mm"
                        val startStr =
                            String.format("%02d:%02d", startTimeState.hour, startTimeState.minute)
                        val endStr =
                            String.format("%02d:%02d", endTimeState.hour, endTimeState.minute)

                        // 计算是否跨天 (如果结束时间比开始时间早，说明是第二天)
                        // 例如 23:00 到 02:00
                        val startMinutes = startTimeState.hour * 60 + startTimeState.minute
                        val endMinutes = endTimeState.hour * 60 + endTimeState.minute
                        val offset = if (endMinutes <= startMinutes) 1 else 0

                        // 把选中的天数 Set 变成字符串 "1,3,5"
                        val payloadStr = selectedDays.sorted().joinToString(",")

                        val ruleToSave = AlarmRule(
                            id = existingRule?.id ?: 0,
                            roomId = streamer.roomId,
                            isEnabled = existingRule?.isEnabled ?: true,

                            alarmName = alarmName,
                            volume = volume.toInt(),

                            // 填入转换后的数据
                            startTime = startStr,
                            endTime = endStr,
                            endDayOffset = offset,
                            repeatPayload = payloadStr,

                            // 如果选了天数就是 REPEAT，没选就是 ONE_TIME (或者你可以自己定逻辑)
                            type = if (selectedDays.isNotEmpty()) AlarmType.REPEAT else AlarmType.ONCE,

                            keywords = keywords
                        )
                        onConfirm(ruleToSave)
                    }
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
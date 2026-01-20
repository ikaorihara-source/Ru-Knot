package com.ikaorihara.ruknot.streamer

import RingtoneSelectionList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.utils.RingtoneUtils

@Composable
fun AddStreamerDialog(
    existingStreamer: StreamerRoom? = null,
    initialGlobalVolume: Int = 80,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = existingStreamer != null
    val mediaPlayer = remember { android.media.MediaPlayer() }
    val audioManager =
        remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }

    // 用来记录用户原来的音量，以便恢复
    var originalStreamVolume by remember { mutableStateOf(-1) }

    // 初始化房间号 (编辑模式回显)
    var inputRoomId by remember {
        // 如果是新增，默认用智能铃声
        mutableStateOf(existingStreamer?.roomId?.toString() ?: "")
    }

    // 初始化铃声状态
    // selectedRingtoneUri: null 代表默认铃声，有值代表自定义
    var selectedRingtoneUri by remember {
        mutableStateOf(existingStreamer?.ringtoneUri)
    }

    var isVibrationOnly by remember {
        mutableStateOf(existingStreamer?.isVibrationOnly ?: false)
    }

    // 试听函数
    fun playPreview(uriString: String?) {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()

            // 获取当前系统最大音量
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)

            // 记录当前音量 (如果还没记录过)
            if (originalStreamVolume == -1) {
                originalStreamVolume =
                    audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            }

            // 计算目标音量 (把 0-100 的百分比 转换成 系统音量刻度)
            // 例如：系统最大刻度是 15，用户设了 80%，那就是 12
            val targetVol = (maxVol * (initialGlobalVolume / 100f)).toInt()

            // 强制设置系统媒体音量
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)

            val targetUri = RingtoneUtils.getEffectiveRingtone(
                context,
                uriString ?: RingtoneUtils.DEFAULT_RINGTONE_URI
            )

            // 使用 MUSIC 通道试听 (ALARM 通道可能会被静音模式屏蔽，预览通常用 MUSIC)
            @Suppress("DEPRECATION")
            mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
            mediaPlayer.setDataSource(context, targetUri)
            mediaPlayer.prepare()
            mediaPlayer.start()

            // 监听播放结束，自动恢复音量
            mediaPlayer.setOnCompletionListener {
                if (originalStreamVolume != -1) {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        originalStreamVolume,
                        0
                    )
                    originalStreamVolume = -1 // 重置标记
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 记得释放资源
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()

                // 如果 Dialog 关闭时音量还没恢复 (比如正在试听时用户点了取消)，强制恢复
                if (originalStreamVolume != -1) {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        originalStreamVolume,
                        0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 文件选择器
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            //以此确保获得了持久化的读取权限(防止重启APP后铃声失效)
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 更新选中状态
            val uriString = it.toString()
            selectedRingtoneUri = uriString

            // 选中自定义文件后，也自动试听一下
            playPreview(uriString)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        // 使用新词 "Add Streamer"
        title = {
            Text(stringResource(if (isEditMode) R.string.edit_streamer_title else R.string.add_streamer_title))
        },
        text = {
            Column {
                // === 房间号输入框 ===
                OutlinedTextField(
                    value = inputRoomId,
                    onValueChange = { newValue ->
                        // 只能输入数字
                        if (newValue.all { it.isDigit() }) {
                            inputRoomId = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.label_room_id)) },
                    singleLine = true,
                    // ⚠️ 编辑模式下禁止修改房间号
                    enabled = !isEditMode,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = if (isEditMode) OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ) else OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // === 铃声选择区域 ===
                Text(
                    text = stringResource(R.string.label_ringtone),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 仅震动开关
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isVibrationOnly = !isVibrationOnly } // 点击整行都能切换
                        .padding(vertical = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isVibrationOnly,
                        onCheckedChange = { isVibrationOnly = it }
                    )
                    Text(
                        text = stringResource(R.string.label_vibrate_only),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 在弹窗内容里显示选择列表
                LazyColumn(modifier = Modifier.height(300.dp)) { // 给个高度让它能滚动
                    item {
                        RingtoneSelectionList(
                            currentUri = selectedRingtoneUri,
                            enabled = !isVibrationOnly,
                            onRingtoneSelected = { uri ->
                                if (!isVibrationOnly) {
                                    selectedRingtoneUri = uri
                                    playPreview(uri)
                                }
                            },
                            onPickCustom = {
                                if (!isVibrationOnly) {
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // 只有输入了房间号才允许点击
                enabled = inputRoomId.isNotBlank(),
                onClick = {
                    inputRoomId.toLongOrNull()?.let { id ->
                        onConfirm(id, selectedRingtoneUri, isVibrationOnly)
                    }
                }
            ) {
                // 动态按钮文字: "Save" 或 "Add"
                Text(stringResource(if (isEditMode) R.string.btn_save else R.string.btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                // 复用已有的 "dialog_cancel" (Cancel)
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
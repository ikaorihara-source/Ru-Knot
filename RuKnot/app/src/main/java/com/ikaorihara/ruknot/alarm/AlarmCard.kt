package com.ikaorihara.ruknot.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.streamer.StreamerRoom

@Composable
fun AlarmCard(
    rule: AlarmRule,
    streamer: StreamerRoom?, // 可能找不到主播 (比如被删了)，所以是 nullable
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // 点击对话框外面关闭
            title = {
                Text(stringResource(R.string.dialog_delete_title))
            },
            text = {
                // 这里用了 stringResource 的带参数功能，自动填入主播名字
                Text(stringResource(R.string.dialog_delete_message, rule.alarmName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete() // 真正执行删除
                        showDialog = false // 关闭对话框
                    }
                ) {
                    Text(stringResource(R.string.btn_confirm), color = Color.Red) // 确认按钮设为红色，警示
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // 点击整个卡片 -> 编辑
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            // 如果闹钟是关闭状态，卡片颜色变暗一点
            containerColor = if (rule.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：主播头像
            if (streamer != null) {
                AsyncImage(
                    model = streamer.coverUrl, // 优先用头像，没有就用封面
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 占位符 (灰色方块)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中间：信息区域
            Column(modifier = Modifier.weight(1f)) {
                // 如果有名字，显示在最上面，加粗
                if (rule.alarmName.isNotEmpty()) {
                    Text(
                        text = rule.alarmName,
                        style = MaterialTheme.typography.titleMedium, // 用稍微大一点的字
                        color = MaterialTheme.colorScheme.primary,    // 用主题色高亮
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 第一行：时间范围 (例如 10:00 - 12:00 +1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${rule.startTime} - ${rule.endTime}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 跨天标记
                    if (rule.endDayOffset > 0) {
                        Text(
                            text = "+1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // 第二行：主播名字
                Text(
                    text = streamer?.userName ?: stringResource(R.string.unknown_user),
                    style = MaterialTheme.typography.bodyMedium
                )

                // 第三行：重复周期 & 关键字
                val repeatText = formatRepeatDays(rule.repeatPayload)
                val infoText =
                    if (rule.keywords.isNotEmpty()) "$repeatText | Key: ${rule.keywords}" else repeatText

                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 右侧操作区
            Column(horizontalAlignment = Alignment.End) {
                // 开关组件
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )

                // 删除按钮
                IconButton(onClick = {
                    showDialog = true // 点击后，只打开对话框，不直接删除！
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_desc),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

// 辅助函数：格式化星期显示 (例如 "1,2" -> "Mon, Tue")
// 你可以把这个函数放在文件末尾，或者专门的一个 Utils 文件里
@Composable
private fun formatRepeatDays(payload: String): String {
    if (payload.isEmpty()) return stringResource(R.string.alarm_once)

    val daysMap = mapOf(
        "1" to stringResource(R.string.alarm_monday),
        "2" to stringResource(R.string.alarm_tuesday),
        "3" to stringResource(R.string.alarm_wednesday),
        "4" to stringResource(R.string.alarm_thursday),
        "5" to stringResource(R.string.alarm_friday),
        "6" to stringResource(R.string.alarm_saturday),
        "7" to stringResource(R.string.alarm_sunday)
    )

    return payload.split(",")
        .mapNotNull { daysMap[it] }
        .joinToString(", ")
}
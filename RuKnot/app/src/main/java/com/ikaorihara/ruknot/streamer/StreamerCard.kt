package com.ikaorihara.ruknot.streamer

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ikaorihara.ruknot.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamerCard(
    room: StreamerRoom,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPin: () -> Unit,
    onToggleLock: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, // 点击对话框外面关闭
            title = {
                Text(stringResource(R.string.dialog_delete_title))
            },
            text = {
                // 这里用了 stringResource 的带参数功能，自动填入主播名字
                Text(stringResource(R.string.dialog_delete_message, room.userName))
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

    // 卡片 UI
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (room.isLive) Color(0xEEFFBB00) else if (room.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
            // 如果开播了，背景变成淡淡的粉色，否则是灰色
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 (用 Coil 加载网络图片)
            AsyncImage(
                model = room.avatarUrl,
                contentDescription = stringResource(R.string.cover_desc),
                contentScale = ContentScale.Crop, // 保证图片填满圆形不变形
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (room.userName == "未知主播") stringResource(R.string.unknown_user) else room.userName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (room.isLive) Color(0xFFE91E63) else Color.Unspecified, // 名字变粉色
                    maxLines = 1
                )
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (room.isLive) Icons.Default.LiveTv else Icons.Default.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (room.isLive) Color.Green else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (room.isLive) stringResource(R.string.status_live) else stringResource(
                            R.string.status_offline
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (room.isLive) Color.Green else Color.Gray,
                        maxLines = 1
                    )
                }
            }

            // 文字信息
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = room.roomId.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (room.isLive) Color(0xFFE91E63) else Color.Unspecified, // 名字变粉色
                    maxLines = 1
                )

                // 加个小图标会让数据更好看
                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = room.follower,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        Icons.Default.ThumbUp,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = room.likeNum,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            // 右侧操作区
            Column(horizontalAlignment = Alignment.End) {
                // 开关组件
                Switch(
                    checked = room.isEnabled, // 读取当前状态
                    onCheckedChange = { isChecked ->
                        onToggle(isChecked) // 当用户点击时，把新状态传出去
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 置顶按钮
                    IconButton(
                        onClick = {
                            // 如果是露露，拦截点击事件
                            if (room.roomId == 22389206L) {
                                // 如果是露露，点击不仅不取消置顶，还弹出“表白”
                                Toast.makeText(
                                    context,
                                    R.string.ruru_always_pinned,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            } else {
                                //如果是其他主播，才允许切换状态
                                onPin()
                            }
                        }
                    ) {
                        Icon(
                            // 如果置顶了用实心图标，没置顶用空心图标
                            imageVector = if (room.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(R.string.pin_desc),
                            // 没置顶的时候颜色淡一点，置顶了变亮
                            tint = if (room.isPinned) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // 动态锁/删除按钮
                    // 我们不能用 IconButton，因为要支持长按，所以用 Box + combinedClickable 自己造一个
                    Box(
                        modifier = Modifier
                            .size(40.dp) // 按钮大小
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    if (room.roomId != 22389206L) {
                                        if (room.isLocked) {
                                            // 如果锁住了，点击只提示
                                            Toast.makeText(
                                                context,
                                                R.string.do_not_delete_desc,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // 没锁，点击弹出删除框
                                            showDialog = true
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.can_not_delete_ruru,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onLongClick = {
                                    if (room.roomId != 22389206L) {
                                        // 长按：震动一下，然后切换状态
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleLock()

                                        // 可以在这里弹个Toast提示状态变了
                                        val msg =
                                            if (room.isLocked) R.string.room_unlock else R.string.room_locked
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.can_not_delete_ruru,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                indication = ripple(bounded = false, radius = 20.dp),
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 根据状态显示不同图标
                        if (room.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.lock_desc),
                                tint = MaterialTheme.colorScheme.primary // 锁住时显示高亮色
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_desc),
                                tint = Color.Gray // 普通删除是灰色
                            )
                        }
                    }
                }
            }
        }
    }
}
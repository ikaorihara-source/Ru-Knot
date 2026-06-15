package com.ikaorihara.ruknot.notification

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val historyList by viewModel.notificationHistory.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_notifications),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    // 如果有记录，显示清空按钮
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearNotificationHistory() }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.btn_clear_history)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            // 空状态占位图
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.label_no_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 消息列表
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList, key = { it.id }) { record ->
                    // 强行获取并记住当前最新的 record 状态，打破闭包缓存陷阱
                    val currentRecord by rememberUpdatedState(record)

                    // 滑动删除状态管理器
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            when (dismissValue) {
                                // 右滑 (StartToEnd)：切换锁定状态
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    viewModel.toggleNotificationLock(currentRecord.id, !currentRecord.isLocked)
                                    false // 返回 false 会让卡片像弹簧一样缩回去（不消失）
                                }
                                // 左滑 (EndToStart)：如果是未锁定状态，则删除
                                SwipeToDismissBoxValue.EndToStart -> {
                                    if (!currentRecord.isLocked) {
                                        viewModel.deleteNotification(currentRecord)
                                        true // 返回 true，卡片滑出屏幕并消失
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        }
                    )

                    // 滑动删除包裹容器
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = !currentRecord.isLocked, // 向左滑动
                        enableDismissFromStartToEnd = true,  // 向右滑动
                        backgroundContent = {
                            val direction = dismissState.targetValue

                            // 区分当前是往左滑还是往右滑
                            val isSwipingRight = direction == SwipeToDismissBoxValue.StartToEnd
                            val isSwipingLeft = direction == SwipeToDismissBoxValue.EndToStart

                            // 根据滑动方向和当前锁定状态决定背景颜色
                            val color by animateColorAsState(
                                targetValue = when {
                                    isSwipingRight && currentRecord.isLocked -> MaterialTheme.colorScheme.secondaryContainer // 要解锁了
                                    isSwipingRight && !currentRecord.isLocked -> MaterialTheme.colorScheme.primaryContainer // 要锁定了
                                    isSwipingLeft -> MaterialTheme.colorScheme.errorContainer // 要删除了
                                    else -> Color.Transparent
                                },
                                label = "dismissColor"
                            )

                            // 红色删除底板
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                // 根据方向把图标放在左边还是右边
                                contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                if (isSwipingRight) {
                                    // 右滑时显示的图标 (根据即将变成的状态来显示)
                                    Icon(
                                        imageVector = if (currentRecord.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = stringResource(R.string.btn_lock_unlock),
                                        tint = if (currentRecord.isLocked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else if (isSwipingLeft) {
                                    // 左滑时显示的垃圾桶
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.btn_remove),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    ) {
                        // 这就是你原来的那个卡片，被包裹在里面
                        NotificationItemCard(currentRecord)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(record: NotificationRecord) {
    val context = LocalContext.current

    // 格式化时间，比如 "05-16 19:30"
    val timeString = DateFormat.format("MM-dd HH:mm", record.timestamp).toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // ★ 终极交互：点击卡片，根据 type 跳转 B 站不同的协议链接
                val intent = when (record.type) {
                    "LIVE" -> Intent(Intent.ACTION_VIEW, "bilibili://live/${record.roomId}".toUri())
                    "DYNAMIC" -> {
                        if (!record.dynamicId.isNullOrEmpty()) {
                            Intent(
                                Intent.ACTION_VIEW,
                                "bilibili://following/detail/${record.dynamicId}".toUri()
                            )
                        } else null
                    }

                    else -> null
                }

                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(it)
                    } catch (e: Exception) {
                        // 如果没装 B站 App，可能会报错，你可以在这里加个 Toast 提示
                        e.printStackTrace()
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // 卡片背景稍微带点透明度，搭配你的动态壁纸更好看
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：主播头像
            AsyncImage(
                model = record.avatarUrl.ifEmpty { R.drawable.ic_notification_info }, // 默认占位图
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface), // 防透明图
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：标题、时间和内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 标题 (开播和动态用不同颜色区分一下)
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 时间戳
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 消息正文 (直播间标题 或 动态内容)
                record.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
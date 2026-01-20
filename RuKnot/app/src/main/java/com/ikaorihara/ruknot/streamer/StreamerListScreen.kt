package com.ikaorihara.ruknot.streamer

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.viewmodel.MainViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamerListScreen(viewModel: MainViewModel) {
    // 初始化 Reorderable 状态
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val lazyListState = rememberLazyListState()

    // 监听数据库里的房间列表 (一旦数据库变了，这里自动刷新)
    val roomList by viewModel.rooms.collectAsState()
    // 监听全局音量
    val globalVolume by viewModel.globalVolume.collectAsState()

    // 核心判断：是否处于“战时状态”（有人直播）
    // 如果有人直播，或者列表为空，就彻底禁止排序功能
    val isAnyLive = roomList.any { it.isEnabled && it.isLive }
    val isSortEnabled = !isAnyLive && roomList.isNotEmpty()

    // 创建一个本地 UI 列表 (用于瞬时动画)
    var uiList by remember { mutableStateOf(roomList) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // === 拖拽过程中的回调 (实时交换) ===
        // 这里只是 UI 层的视觉交换，手指松开后才会写入数据库

        // --- 露露 (ID 22389206) 是不动的神 ---
        // 如果拖拽的是露露，或者试图把别人拖到露露头上(index 0)，直接拒绝
        val fromRoom = uiList.getOrNull(from.index) ?: return@rememberReorderableLazyListState
        val toRoom = uiList.getOrNull(to.index) ?: return@rememberReorderableLazyListState

        if (fromRoom.roomId == 22389206L || toRoom.roomId == 22389206L) {
            // 震动提示用户“这里不能动”
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            return@rememberReorderableLazyListState
        }

        // --- 阶级隔离 (置顶 vs 非置顶) ---
        // 如果跨越了阶级，拒绝交换
        if (fromRoom.isPinned != toRoom.isPinned) {
            return@rememberReorderableLazyListState
        }

        // 如果规则都通过，通知 ViewModel 交换数据 (内存交换)
        // 注意：这里需要 ViewModel 提供一个“纯内存交换不写库”的方法，或者直接更新 customOrder
        // 为了流畅性，通常这里调用 ViewModel 更新 StateFlow
        // 核心：只更新本地 UI List，瞬间完成交换，不卡顿
        uiList = uiList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }

        // 震动反馈，提升手感
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }

    // 同步数据：平时保持 UI 和数据库一致
    LaunchedEffect(roomList) {
        if (!reorderableState.isAnyItemDragging) {
            uiList = roomList
        }
    }

    // 保存数据：拖拽结束(松手)时，才写入数据库
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            // 如果顺序变了，且列表内容没变（防止刚松手时数据刷新了），则保存
            // 简单的判断是：如果 uiList 和 databaseList 顺序不同，就保存
            if (uiList != roomList) {
                // 注意：这里需要去 ViewModel 加这个新方法，或者沿用旧方法
                viewModel.updateListOrder(uiList)
            }
        }
    }

    // 控制“添加弹窗”显示的状态
    var showDialog by remember { mutableStateOf(false) }
    var editingStreamer by remember { mutableStateOf<StreamerRoom?>(null) } // null = 新增模式

    Scaffold(
        // 把脚手架设为透明，否则会挡住背景图
        containerColor = Color.Transparent,

        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingStreamer = null // 点击 + 号，意味着是新增模式
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add_room))
            }
        }
    ) { padding ->
        if (roomList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_streamers_hint))
            }
        } else {
            LazyColumn(
                // 使用标准 LazyColumn，但绑定 reorderableState
                state = lazyListState,
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp), //稍微紧凑一点，用 8.dp 比较好看
                modifier = Modifier.padding(horizontal = 16.dp) // 给两边留点缝隙
            ) {
                // 注意：必须使用 itemsIndexed 并且提供 key
                itemsIndexed(uiList, key = { _, room -> room.roomId }) { index, room ->

                    // 判断当前卡片是否允许被拖拽
                    // 1. 不是露露
                    // 2. 全局允许排序 (没人直播)
                    val isDraggable = isSortEnabled && room.roomId != 22389206L

                    ReorderableItem(reorderableState, key = room.roomId) { isDragging ->

                        // 拖拽时的视觉效果：浮起 + 阴影
                        val elevation by animateDpAsState(
                            if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )

                        // 用 Box 包裹卡片，并应用 longPressDraggable 修改器
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // ★★★ 只有满足条件才应用拖拽手势 ★★★
                                .then(
                                    if (isDraggable) {
                                        Modifier.longPressDraggableHandle(
                                            onDragStarted = {
                                                // 开始拖拽时震动一下
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            StreamerCard(
                                room = room,
                                // 视觉小技巧，如果正在拖拽，把卡片稍微放大一点或者加个阴影效果 (通过 Card 的 elevation)
                                modifier = Modifier.padding(vertical = if (isDragging) 4.dp else 0.dp),
                                // 开关逻辑
                                onToggle = { isEnabled ->
                                    // 只修改开关状态，保留其他所有信息
                                    viewModel.updateStreamer(room.copy(isEnabled = isEnabled))
                                },
                                // 置顶逻辑
                                onPin = { viewModel.pinStreamer(room) },
                                // 切换锁定
                                onToggleLock = {
                                    // 新状态
                                    val newLockedState = !room.isLocked

                                    // 立即更新本地UI
                                    uiList =
                                        uiList.map { if (it.roomId == room.roomId) it.copy(isLocked = newLockedState) else it }

                                    // 异步更新数据库
                                    viewModel.lockStreamer(room)

                                    // 震动反馈
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                // 编辑逻辑 (点击卡片)
                                onClick = {
                                    editingStreamer = room // 记录当前点的是谁
                                    showDialog = true      // 打开弹窗
                                },
                                onDelete = {
                                    viewModel.deleteStreamer(room)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 显示添加弹窗
        if (showDialog) {
            AddStreamerDialog(
                existingStreamer = editingStreamer, // 把当前编辑的对象传进去 (用于回显)
                initialGlobalVolume = globalVolume,
                onDismiss = { showDialog = false },
                onConfirm = { roomId, ringtoneUri, isVibrationOnly ->
                    if (editingStreamer == null) {
                        // === 新增模式 (editingStreamer is null) ===
                        viewModel.addStreamer(roomId, ringtoneUri, isVibrationOnly)
                    } else {
                        // === 编辑模式 (editingStreamer is set) ===
                        // ⚠️ 关键：使用 .copy() 只更新铃声，保留原有的 title, coverUrl, isLive 等数据
                        val updatedRoom = editingStreamer!!.copy(
                            // roomId 不允许改，isEnabled 保持原样
                            ringtoneUri = ringtoneUri,
                            isVibrationOnly = isVibrationOnly
                        )
                        viewModel.updateStreamer(updatedRoom)
                    }
                    showDialog = false
                }
            )
        }
    }
}
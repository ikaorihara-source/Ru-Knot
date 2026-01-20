package com.ikaorihara.ruknot.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.viewmodel.MainViewModel

@Composable
fun AlarmListScreen(viewModel: MainViewModel) {
    // 观察数据
    val alarms by viewModel.alarms.collectAsState()
    val streamers by viewModel.rooms.collectAsState() // 需要主播列表来显示名字

    // 弹窗状态
    var showDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AlarmRule?>(null) } // 记录当前正在编辑哪一个，null表示新建

    Scaffold(
        // ★★★ 核心修改：把脚手架设为透明，否则会挡住背景图 ★★★
        containerColor = Color.Transparent,

        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRule = null // 清空编辑状态，表示新建
                showDialog = true
            }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_add_schedule)
                )
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_alarms_hint)) // 建议放入 strings.xml
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(alarms) { rule ->
                    // 核心逻辑：根据 rule.roomId 去 streamers 列表里找对应的主播对象
                    val linkedStreamer = streamers.find { it.roomId == rule.roomId }

                    AlarmCard(
                        rule = rule,
                        streamer = linkedStreamer,
                        onToggle = { isEnabled ->
                            // 更新开关状态
                            viewModel.updateAlarm(
                                rule.copy(
                                    isEnabled = isEnabled,
                                    isTriggered = if (isEnabled) false else rule.isTriggered
                                )
                            )
                        },
                        onClick = {
                            // 点击卡片 -> 进入编辑模式
                            editingRule = rule
                            showDialog = true
                        },
                        onDelete = {
                            viewModel.deleteAlarm(rule)
                        }
                    )
                }
            }
        }

        // 显示弹窗 (添加 或 编辑)
        if (showDialog) {
            AddAlarmDialog(
                streamers = streamers,
                existingRule = editingRule, // 把当前编辑的对象传进去 (如果是新建则是 null)
                onDismiss = { showDialog = false },
                onConfirm = { updatedRule ->
                    viewModel.updateAlarm(updatedRule) // 无论是新增还是修改，都调用 insert (Replace策略)
                    showDialog = false
                }
            )
        }
    }
}

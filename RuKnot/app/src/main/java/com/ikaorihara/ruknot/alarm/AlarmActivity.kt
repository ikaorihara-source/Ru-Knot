package com.ikaorihara.ruknot.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ikaorihara.ruknot.MainActivity
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.data.AlarmItem
import com.ikaorihara.ruknot.service.MonitorService
import com.ikaorihara.ruknot.ui.theme.RuKnotTheme

class AlarmActivity : ComponentActivity() {

    // 定义广播接收器
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "${packageName}.STOP_AUDIO") {
                finish() // 收到停止广播，直接关闭页面，onDestroy 会自动停止声音
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 关键：允许在锁屏上显示，并点亮屏幕
        configureLockScreen()

        // --- 立即取消通知，防止通知的铃声和 Activity 的铃声重叠 ---
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2001) // 这里的 2001 必须对应 MonitorService 里 notify() 的 ID

        // 尝试获取列表
        val alarmList = if (Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("ALARM_LIST", ArrayList::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("ALARM_LIST") as? ArrayList<*>
        }

        // 强转一下类型，方便 Compose 使用。如果为空，就给个空列表
        val safeList = alarmList ?: arrayListOf()

        setContent {
            RuKnotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    // 显示多人列表
                    MultiStreamerAlarmScreen(
                        alarms = safeList,
                        onDismiss = { jumpToMain() }, // 一个按钮解散全部
                        onCardClick = { roomId ->
                            jumpToLive(roomId)
                        }
                    )
                }
            }
        }

        // 注册广播 (放在 onCreate 最后)
        val filter = IntentFilter("${packageName}.STOP_AUDIO")

        // RECEIVER_NOT_EXPORTED 在旧手机上会被自动忽略，在新手机上生效
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun configureLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    // 打开主界面 (作为保底，或者底部按钮的动作)
    private fun jumpToMain() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                // CLEAR_TOP 保证不会在现有栈上堆叠，而是回到主页
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // 无论如何，都要停止闹钟并销毁当前页面
            stopServiceAlarm()
        }
    }

    // 跳转到 B 站直播间并停止闹钟
    private fun jumpToLive(roomId: Long) {
        try {
            // 尝试构建 B 站协议的 Intent
            val uri = "bilibili://live/$roomId".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            stopServiceAlarm()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果跳转失败（比如没装B站），打开 MainActivity 作为保底
            jumpToMain()
        }
    }

    // 发送命令给 Service 让它闭嘴
    private fun stopServiceAlarm() {
        val stopIntent = Intent(this, MonitorService::class.java)
        stopIntent.action = "ACTION_STOP_ALARM_SOUND"
        startService(stopIntent)

        // 也可以直接关闭自己
        finish()
    }

    @Composable
    fun MultiStreamerAlarmScreen(
        alarms: java.util.ArrayList<out Any>,
        onDismiss: () -> Unit,
        onCardClick: (Long) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 根据人数动态改变标题
            val title = if (alarms.size > 1) {
                stringResource(R.string.notify_great_now_live, alarms.size)
            } else {
                stringResource(R.string.label_alarm_rang)
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // 列表区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // 占据中间剩余空间
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(alarms) { item ->
                    val alarmItem = item as AlarmItem

                    StreamerCard(
                        item = alarmItem,
                        onClick = { onCardClick(alarmItem.roomId) }
                    )
                }
            }

            // 底部按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_acknowledged),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    @Composable
    fun StreamerCard(
        item: AlarmItem,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.streamerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = getString(R.string.pin_desc),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.ruleName, // 这里显示具体的闹钟规则名（如“突击歌回”）
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServiceAlarm()

        // 注销广播 (防止内存泄漏)
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
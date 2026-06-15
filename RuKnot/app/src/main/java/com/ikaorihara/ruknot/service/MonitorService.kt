package com.ikaorihara.ruknot.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import com.ikaorihara.ruknot.MainActivity
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.alarm.AlarmActivity
import com.ikaorihara.ruknot.data.AlarmItem
import com.ikaorihara.ruknot.data.AlarmType
import com.ikaorihara.ruknot.data.AppDatabase
import com.ikaorihara.ruknot.data.SettingsDataStore
import com.ikaorihara.ruknot.data.repository.AlarmRepository
import com.ikaorihara.ruknot.network.BiliAppDynamicItem
import com.ikaorihara.ruknot.network.RetrofitClient
import com.ikaorihara.ruknot.notification.NotificationRecord
import com.ikaorihara.ruknot.streamer.StreamerRoom
import com.ikaorihara.ruknot.utils.RingtoneUtils
import com.ikaorihara.ruknot.utils.RuleValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

class MonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var repository: AlarmRepository // 假设你有 Repository
    private lateinit var settingsStore: SettingsDataStore

//    private var pollingJob1: Job? = null
//    private var pollingJob2: Job? = null

    // 全局播放器
    private var currentJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1 // 用于记录用户原来的音量，闹钟结束后恢复

    // 新增：全局静态变量，用于记录当前响铃状态
    companion object {
        var isRinging: Boolean = false
        var currentAlarmList: ArrayList<AlarmItem> = arrayListOf()

        private const val PREF_NAME = "monitor_service_state"
        private const val KEY_LAST_USER_CHECK = "last_user_check_time"
        private const val KEY_LAST_INFO_CHECK = "last_info_check_time"
    }

    override fun onCreate() {
        super.onCreate()
        // 这里获取你的 Repository 实例 (根据你用的 Koin/Hilt 或者手动单例调整)
        val db = AppDatabase.getDatabase(applicationContext)
        repository = AlarmRepository(db.StreamerDAO(), db.AlarmDAO(), db.NotificationDao())

        // 初始化 SettingsStore
        settingsStore = SettingsDataStore(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 处理停止命令
        if (intent?.action == "ACTION_STOP_ALARM_SOUND") {
            Log.d("MonitorService", "收到停止命令，执行静音操作")
            stopAlarmSound() // 调用停止方法

            // 取消掉一直在响的通知
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2001)

            // 停止后，安排 15秒后重启检查（兜底）
            scheduleNextCheck(15_000L)
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            // 尝试启动前台服务
            // 只要这里报错，后面所有逻辑都不会执行，直接跳到 catch
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 如果是 Android 14+，且你在 Manifest 改成了 shortService
                // 这里其实可以直接传 0 或者对应的 Type 常量，但在 XML 定义好后，通常传 0 或不传参系统会自动匹配
                startForeground(1001, createNotification())
            } else {
                startForeground(1001, createNotification())
            }
        } catch (e: Exception) {
            // 这里的捕获至关重要
            Log.e("MonitorService", "系统拒绝启动前台服务 (配额耗尽): ${e.message}")

            // 如果被系统拒绝了，说明我们“太吵了”。
            // 策略：认怂，安排一个“超长冷却时间”后的闹钟 (比如 5分钟后)，然后自杀。
            // 这样可以让系统重置对我们的配额限制。
            if (!isRinging) {
                scheduleNextCheck(30_000L) // 5分钟 (300秒) 后再试
                stopSelf()
            }

            return START_NOT_STICKY
        }

        // 如果正在响铃，保持服务存活，不执行检查逻辑，防止自己把闹钟杀掉了
        if (isRinging) {
            return START_STICKY
        }

        currentJob?.cancel()

        // 核心检查逻辑：使用协程执行一次检查，然后自杀
        currentJob = serviceScope.launch {
            try {
                // --- 读取用户设置 ---
                // Job1 (直播) 基础间隔
                val roomIntervalSec = settingsStore.roomUpdateInterval.first().coerceAtLeast(15L)
                val roomIntervalMs = roomIntervalSec * 1000L

                // Job2 (用户) 基础间隔
                val userIntervalSec = settingsStore.userUpdateInterval.first().coerceAtLeast(30L)
                val userIntervalMs = userIntervalSec * 1000L

                val dynamicIntervalSec =
                    settingsStore.dynamicUpdateInterval.first().coerceAtLeast(60L)
                val dynamicIntervalMs = dynamicIntervalSec * 1000L

                // 随机范围 -1秒 ~ +4秒
                val jitterMs = Random.nextLong(-1000, 4001)
                // 最终间隔：基础 + 抖动，并保底 10秒防止过快
                val finalDelayMs = (roomIntervalMs + jitterMs).coerceAtLeast(5_000L)

                // --- 总是执行高频任务 (查直播) ---
                val job1 = launch {
                    checkAllRooms()
                }

                // --- 检查是否需要执行低频任务 (查头像) ---
                val job2 = launch {
                    val sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    val lastUserCheckTime = sp.getLong(KEY_LAST_USER_CHECK, 0L)
                    val now = System.currentTimeMillis()

                    // 添加宽容度，允许提前执行
                    // 或者你也可以用百分比：val tolerance = 5000L
                    val tolerancePercent = 0.1 // 10%
                    val tolerance = (userIntervalMs * tolerancePercent).toLong()

                    // 如果 (当前时间 - 上次检查时间) > 用户设置的 Job2 间隔，才执行
                    if (now - lastUserCheckTime >= (userIntervalMs - tolerance)) {
                        Log.d("MonitorService", "执行低频任务: 刷新主播头像信息")
                        checkAllStreamers()

                        // 记录这次执行的时间
                        sp.edit { putLong(KEY_LAST_USER_CHECK, now) }
                    }
                }

                // --- 检查是否需要执行低频任务 (提取动态) ---
                val job3 = launch {
                    val sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    val lastInfoCheckTime = sp.getLong(KEY_LAST_INFO_CHECK, 0L)
                    val now = System.currentTimeMillis()

                    // 添加宽容度，允许提前执行
                    // 或者你也可以用百分比：val tolerance = 5000L
                    val tolerancePercent = 0.1 // 10%
                    val tolerance = (dynamicIntervalMs * tolerancePercent).toLong()

                    // 先读取 COOKIE，如果没有，直接跳过不查动态
                    val cookie = settingsStore.biliCookie.first()
                    if (cookie.isNotEmpty()) {
                        // 如果 (当前时间 - 上次检查时间) > 用户设置的 Job3 间隔，才执行
                        if (now - lastInfoCheckTime >= (dynamicIntervalMs - tolerance)) {
                            Log.d("MonitorService", "执行低频任务: 检测主播动态")
                            checkAllStreamersDynamics(cookie)

                            // 记录这次执行的时间
                            sp.edit { putLong(KEY_LAST_INFO_CHECK, now) }
                        }
                    } else {
                        Log.d("MonitorService", "未配置 COOKIE，跳过动态检查")
                    }
                }

                // 等待两个任务都完成 (耗时取决于最慢的那个，而不是两者的和)
                joinAll(job1, job2, job3)

//                checkAllStreamersDynamics()

                // --- 安排下一次唤醒 ---
                if (!isRinging) {
                    scheduleNextCheck(finalDelayMs)
                    stopSelf() // 任务做完，停止服务，重置系统计时器
                }

            } catch (_: java.util.concurrent.CancellationException) {
                // 如果是 JobCancellationException，说明是 stopSelf() 导致的正常退出，不要报错
                Log.d("MonitorService", "服务正常结束 (Job cancelled)")
            } catch (e: Exception) {
                Log.e("MonitorService", "检查过程出错", e)
                // 出错兜底
                if (!isRinging) {
                    scheduleNextCheck(5_000L)
//                    stopSelf()
                }
            } finally {
                // 使用安全的 stopSelf(startId)
                if (!isRinging) {
                    stopSelf(startId)
                }
            }
        }

        return START_STICKY
    }

    // 当用户在多任务界面划掉 App 时触发
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MonitorService", "任务被移除 (App被划掉)")

        try {
            // 避免旧进程还没死透，新进程就冲上来导致冲突
            scheduleNextCheck(5_000L)

        } catch (e: Exception) {
            // 既然 App 都要死了，这里如果报错绝对不能崩，静静地走就好
            Log.e("MonitorService", "onTaskRemoved 出错", e)
        }

        super.onTaskRemoved(rootIntent)
    }

//    private fun startPolling1() {
//        if (pollingJob1?.isActive == true) return
//
//        pollingJob1 = serviceScope.launch {
//            while (isActive) {
//                try {
//                    checkAllRooms()
//                } catch (e: Exception) {
//                    Log.e("MonitorService", "Error checking streamers", e)
//                }
//
//                // 修改这里：动态获取延迟时间
//                val intervalSeconds = settingsStore.roomUpdateInterval.first()
//                // 限制最小 15秒 (防止用户设太小导致封IP)
//                val safeInterval = intervalSeconds.coerceAtLeast(15L)
//
//                val jitterMs = Random.nextLong(-1000, 4001)
//
//                // 间隔几秒
//                delay(safeInterval * 1000L + jitterMs) // 转成毫秒
//            }
//        }
//    }
//
//    private fun startPolling2() {
//        if (pollingJob2?.isActive == true) return
//
//        pollingJob2 = serviceScope.launch {
//            while (isActive) {
//                try {
//                    checkAllStreamers()
//                } catch (e: Exception) {
//                    Log.e("MonitorService", "Error checking streamers", e)
//                }
//
//                // 修改这里：动态获取延迟时间
//                val intervalSeconds = settingsStore.roomUpdateInterval.first()
//                // 限制最小 30秒 (防止用户设太小导致封IP)
//                val safeInterval = intervalSeconds.coerceAtLeast(30L)
//
//                val jitterMs = Random.nextLong(-1000, 4001)
//
//                // 间隔几秒
//                delay(safeInterval * 1000L + jitterMs) // 转成毫秒
//            }
//        }
//    }

    private suspend fun checkAllRooms() {
        // 准备一个空篮子，用来装这次轮询抓到的所有主播
        val currentBatch = mutableListOf<AlarmItem>()

        // 获取所有已保存的主播
        val streamers = repository.getAllStreamersSync()

        streamers.forEach { localStreamer ->
            // 如果【主播开关开启】，去检查房间
            if (localStreamer.isEnabled) {
                // 请求 B站 API
                try {
                    val roomResp = RetrofitClient.service.getRoomInfo(localStreamer.roomId)

                    if (roomResp.code == 0 && roomResp.data != null) {
                        val roomData = roomResp.data

                        val isLiveNow = roomData.liveStatus == 1

                        if (!localStreamer.isLive && isLiveNow) {
                            // 【刚开播】发送通知或执行特定逻辑
                            val title = getString(R.string.notify_go_live, localStreamer.userName)

                            sendNotification(
                                title = title,
                                coverUrl = roomData.userCover,
                                message = roomData.title,
                                roomId = localStreamer.roomId
                            )

                            serviceScope.launch {
                                repository.insertNotification(
                                    NotificationRecord(
                                        title = title,
                                        message = roomData.title,
                                        timestamp = System.currentTimeMillis(),
                                        type = "LIVE",
                                        roomId = localStreamer.roomId,
                                        userId = localStreamer.userId,
                                        dynamicId = null,
                                        avatarUrl = roomData.userCover.ifEmpty { localStreamer.avatarUrl }
                                    )
                                )
                            }
                        } else if (localStreamer.isLive && !isLiveNow) {
                            // 【刚下播】通常不需要跳转，roomId 传 null
                            val title = getString(R.string.notify_offline, localStreamer.userName)
                            val message = getString(R.string.notify_offline_message)

                            sendNotification(
                                title = getString(R.string.notify_offline, localStreamer.userName),
                                coverUrl = roomData.userCover,
                                message = getString(R.string.notify_offline_message),
                                roomId = null
                            )

                            serviceScope.launch {
                                repository.insertNotification(
                                    NotificationRecord(
                                        title = title,
                                        message = message,
                                        timestamp = System.currentTimeMillis(),
                                        type = "LIVE",
                                        roomId = localStreamer.roomId,
                                        userId = localStreamer.userId,
                                        dynamicId = null,
                                        avatarUrl = roomData.userCover.ifEmpty { localStreamer.avatarUrl }
                                    )
                                )
                            }
                        }
                        checkRulesForStreamer(localStreamer, isLiveNow, currentBatch)

                        if (!isLiveNow) {
                            // 重置该主播名下的所有闹钟规则，为下次开播做准备
                            resetRulesForStreamer(localStreamer.roomId)
                        }

                        // 更新主播信息 (这会让 UI 自动刷新)
                        if (localStreamer.isLive != isLiveNow ||
                            localStreamer.title != roomData.title ||
                            localStreamer.coverUrl != roomData.userCover
                        ) {
                            repository.updateRoomStatus(
                                roomId = localStreamer.roomId,
                                isLive = isLiveNow,
                                title = roomData.title,
                                coverUrl = roomData.userCover
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MonitorService", "网络请求失败", e)
                }

                val jitterMs = Random.nextLong(100, 301)

                delay(jitterMs)
            } else {
                repository.updateRoomStatus(
                    roomId = localStreamer.roomId,
                    isLive = false,
                    title = localStreamer.title,
                    coverUrl = localStreamer.coverUrl
                )

                // 重置该主播名下的所有闹钟规则
                resetRulesForStreamer(localStreamer.roomId)
            }
        }

        // 循环结束，如果篮子里有鱼，统一触发
        if (currentBatch.isNotEmpty()) {
            triggerBatchAlarm(currentBatch)
        }
    }

    private suspend fun checkRulesForStreamer(
        room: StreamerRoom,
        isStreamerLive: Boolean,
        batchList: MutableList<AlarmItem>
    ) {
        val rules = repository.getRulesByStreamerId(room.roomId)
            .filter { it.isEnabled }

        val now = LocalDateTime.now()

        rules.forEach { rule ->
            // 使用工具类判断时间
            val isTimeMatch = RuleValidator.isRuleMatch(rule, now)

            // 触发条件：时间符合
            if (isTimeMatch) {
                // 只有在【时间命中】且【主播确实在直播】时，才尝试触发
                if (isStreamerLive && !rule.isTriggered) {

                    batchList.add(
                        AlarmItem(
                            streamerName = room.userName,
                            ruleName = rule.alarmName.ifEmpty { room.title },
                            coverUrl = room.coverUrl,
                            isPinned = room.isPinned, // 关键：带上置顶状态
                            ringtone = room.ringtoneUri,
                            isVibrationOnly = room.isVibrationOnly,
                            roomId = room.roomId,
                            volume = rule.volume,
                            customOrder = room.customOrder
                        )
                    )

                    if (rule.type == AlarmType.ONCE) {
                        // 如果是单次闹钟：标记触发 + 关闭开关
                        repository.updateRuleState(
                            id = rule.id,
                            isTriggered = true,
                            isEnabled = false
                        )
                    } else {
                        // 如果是重复闹钟：只标记触发
                        repository.updateRuleTriggered(
                            id = rule.id,
                            isTriggered = true
                        )
                    }
                    Log.d("MonitorService", "闹钟触发: ${rule.alarmName}")
                }
            } else {
                // 时间窗口过期，重置
                if (rule.isTriggered) {
                    // 只重置触发状态
                    repository.updateRuleTriggered(
                        id = rule.id,
                        isTriggered = false
                    )
                    Log.d("MonitorService", "闹钟重置: ${rule.alarmName}")
                }
            }
        }
    }

    private suspend fun resetRulesForStreamer(roomId: Long) {
        // 获取该主播的所有规则（不分是否开启，全部重置比较保险）
        val allRulesOfStreamer = repository.getAllRulesByStreamerId(roomId)

        // 先过滤出那些【真正需要重置】的规则 (也就是 isTriggered 为 true 的)
        val triggeredRules = allRulesOfStreamer.filter { it.isTriggered }

        // 如果列表为空，说明大家都很干净，直接返回，不要操作数据库
        if (triggeredRules.isEmpty()) {
            return
        }

        Log.d(
            "MonitorService",
            "检测到主播下播(或状态修复)，重置 ${triggeredRules.size} 个已触发的闹钟"
        )

        // 只更新需要更新的
        triggeredRules.forEach { rule ->
            // 将已触发状态回滚为 false
            repository.updateRule(rule.copy(isTriggered = false))
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private suspend fun triggerBatchAlarm(items: List<AlarmItem>) {
        val context = this
        val channelId = "alarm_high_priority_channel"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 排序逻辑：置顶的(true)排前面，如果都置顶，按自定义排序
        val sortedItems = items.sortedWith(
            compareByDescending<AlarmItem> { it.isPinned }.thenBy { it.customOrder }
        )

        // 更新全局状态
        isRinging = true
        currentAlarmList = ArrayList(sortedItems)

        // 取出排在第一位的，用它的铃声和音量
        val topItem = sortedItems.firstOrNull() ?: return

        // 使用工具类计算最终 URI
        // 获取数据库存的字符串 (可能是 null, 可能是 "app_default...", 可能是 "content://...")
        val savedUriString = topItem.ringtone ?: RingtoneUtils.DEFAULT_RINGTONE_URI

        // 让工具类决定播放什么 (这里面包含了 SSR/UR 的概率判定和解锁逻辑)
        // 传入 'this' 作为 Context
        val finalUriToPlay = RingtoneUtils.getEffectiveRingtone(this, savedUriString, true)

        // 决定音量
        val finalVolume = if (topItem.volume == 0) {
            settingsStore.globalVolume.first()
        } else {
            topItem.volume
        }

        // 启动播放
        startAlarmSound(finalUriToPlay, finalVolume, topItem.isVibrationOnly)

        val existingChannel = notificationManager.getNotificationChannel(channelId)
        if (existingChannel == null) {
            // 核心：必须在创建渠道时就绑定声音和震动
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notify_channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)

                setSound(null, null)

                // 开启并设置震动
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 构建 Intent：把整个列表传过去
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            // 传整个列表
            putExtra("ALARM_LIST", ArrayList(sortedItems))
        }

        val requestCode = 2000 // 固定 ID，保证更新同一个 PendingIntent

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // [查看/接听] 按钮 -> 点击也是跳转 Activity
        val acceptAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground, // 换成你的 ic_call_answer 图标
            getString(R.string.notify_watch_stream),
            fullScreenPendingIntent
        ).build()

        // [挂断/关闭] 按钮 -> 发送广播给 NotificationActionReceiver
        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DISMISS_ALARM"
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground, // 换成你的 ic_call_end 图标
            getString(R.string.notify_close_alarm),
            dismissPendingIntent
        ).build()

        // 显示通知
        val titleText = if (sortedItems.size > 1) {
            getString(
                R.string.notify_many_go_live,
                topItem.streamerName,
                sortedItems.size.toString()
            )
        } else {
            getString(R.string.notify_go_live, topItem.streamerName)
        }

        val imageLoader = ImageLoader(context)

        // 加载头像作为 LargeIcon
        val coverRequest = ImageRequest.Builder(context)
            .data(topItem.coverUrl.ifEmpty { R.drawable.ic_alarm })
            .error(R.drawable.ic_notification)
            .size(256, 256) // 强制限制图片大小，防止 OOM
            .precision(coil.size.Precision.EXACT)
            .allowHardware(false) // 必须设为 false 才能转为 Bitmap
            .build()
        val coverBitmap = (imageLoader.execute(coverRequest).drawable as? BitmapDrawable)?.bitmap

        // 构建通知
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 确保 logo 已放入 res/drawable
            .setLargeIcon(coverBitmap)
            .setContentTitle(getString(R.string.label_now_live))
            .setContentText(titleText)

            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            .setFullScreenIntent(fullScreenPendingIntent, true)

            .setOngoing(true)
            .setAutoCancel(false)

            .addAction(acceptAction)
            .addAction(dismissAction)

        // 先生成 Notification 对象
        val notification = builder.build()

        // 手动给 Notification 对象加上 "死缠烂打" 的 Flag
        notification.flags = notification.flags or
                Notification.FLAG_ONGOING_EVENT or // 声音一直循环
                Notification.FLAG_NO_CLEAR     // 彻底禁止左滑清除

        // 发送通知
        notificationManager.notify(2001, notification)

        // 直接启动 Activity
        try {
            // 让 App 在前台时，或者在允许后台弹窗的旧手机上，直接跳出闹钟页面
            startActivity(fullScreenIntent)
            Log.d("MonitorService", "已尝试强制启动 AlarmActivity 霸屏")
        } catch (e: Exception) {
            // 这里报错是正常的 (Android 10+ 限制后台启动)，不用担心，
            // 只要上面的 notificationManager.notify 执行了，Full Screen Intent 就会生效。
            Log.w("MonitorService", "后台启动 Activity 被拦截，将依赖全屏通知展示: ${e.message}")
        }
    }

    // 强制播放音频（无视静音）
    private fun startAlarmSound(
        uri: android.net.Uri,
        volumePercent: Int,
        isVibrationOnly: Boolean
    ) {
        try {
            // 防止重复播放
            if (mediaPlayer?.isPlaying == true) return

            // --- 震动逻辑 (无论是否静音，都要震动) ---
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            // 强震动模式：[等待0ms, 震1000ms, 停500ms] 循环
            // 普通震动节奏：[等待 0ms, 震动 1000ms, 等待 1000ms] 循环
            val pattern =
                if (isVibrationOnly) longArrayOf(0, 1000, 500) else longArrayOf(0, 1000, 1000)

            // 核心判断：如果是【仅震动】，直接跳过播放音乐的部分
            if (isVibrationOnly) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
                Log.d("MonitorService", "仅震动模式：跳过音乐播放")
                return
            }

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

            // --- 抢占音频焦点 (告诉系统我要出声了) ---
            val result =
                audioManager?.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener { } // 简单处理，不做复杂逻辑
                        .build()
                )

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w("MonitorService", "未能获取音频焦点，可能声音会被其他App压住")
            }

            // 核心：强制把闹钟声道的音量拉到最大！
            // 这行代码是穿透静音的关键。
            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 0
            originalVolume =
                audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0 // 记住原来的音量

            if (originalVolume < maxVolume) { //(maxVolume * 0.5)
                // 如果音量太小，强制设为最大
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                Log.d("AlarmActivity", "已强制将闹钟音量调至最大: $maxVolume")
            }

            // 初始化播放器
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // 必须是 ALARM 通道
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // ★★★ 应用用户设定的音量 ★★★
                // MediaPlayer 的音量是 0.0f 到 1.0f
                val vol = volumePercent / 100f
                setVolume(vol, vol)

                isLooping = true // 让声音无限循环，直到用户点击关闭
                prepare()
                start()
            }

//            // 开启震动
//            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                val vibratorManager =
//                    getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
//                vibratorManager.defaultVibrator
//            } else {
//                @Suppress("DEPRECATION")
//                getSystemService(VIBRATOR_SERVICE) as Vibrator
//            }

            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))

        } catch (e: Exception) {
            Log.e("MonitorService", "播放失败", e)
        }
    }

    // 停止播放的方法
    private fun stopAlarmSound() {
        // 重置全局状态
        isRinging = false
        currentAlarmList.clear()

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()

            if (originalVolume != -1) {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "MonitorServiceChannel"
        val channelName = "Bilibili Monitor Service"

        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notify_monitoring, getString(R.string.app_name)))
            .setContentText(getString(R.string.notify_monitoring_message))
            .setSmallIcon(R.drawable.ic_notification) // 记得换成你的图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun sendNotification(
        title: String,
        coverUrl: String,
        message: String,
        roomId: Long? = null
    ) {
        val context = this
        val channelId = "streamer_status_channel"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道 (Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notify_room_update),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // 构建点击通知后的意图
        val intent = if (roomId != null) {
            // 如果有房间号，尝试唤起 B 站 App 直播间
            Intent(Intent.ACTION_VIEW, "bilibili://live/$roomId".toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // 如果没有房间号，默认打开本 App 主界面
            Intent(this, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            roomId?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val imageLoader = ImageLoader(context)

        // 加载头像作为 LargeIcon
        val coverRequest = ImageRequest.Builder(context)
            .data(coverUrl.ifEmpty { R.drawable.ic_notification })
            .error(R.drawable.ic_notification)
            .size(256, 256) // 强制限制图片大小，防止 OOM
            .precision(coil.size.Precision.EXACT)
            .allowHardware(false) // 必须设为 false 才能转为 Bitmap
            .build()
        val coverBitmap = (imageLoader.execute(coverRequest).drawable as? BitmapDrawable)?.bitmap

        // 构建并发送通知
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 确保你有这个图标资源
            .setLargeIcon(coverBitmap)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 点击跳转
            .setAutoCancel(true)            // 点击后自动消失
            .build()

        // 使用 roomId 作为 ID，这样同一个主播的通知会互相覆盖，不会弹出满屏通知
        notificationManager.notify(roomId?.toInt() ?: 100, notification)
    }

    private suspend fun checkAllStreamers() {
        // 获取所有已保存的主播
        val streamers = repository.getAllStreamersSync()

        streamers.forEach { localStreamer ->
            // 请求 B站 API
            try {
                val userResp = RetrofitClient.service.getUserInfo(localStreamer.userId)

                if (userResp.code == 0 && userResp.data != null && userResp.data.card != null) {
                    val userData = userResp.data

                    // 更新主播信息 (这会让 UI 自动刷新)
                    if (localStreamer.userName != userData.card.name ||
                        localStreamer.avatarUrl != userData.card.avatarUrl.ifEmpty { localStreamer.avatarUrl } ||
                        localStreamer.follower != userData.follower.toString() ||
                        localStreamer.likeNum != userData.likeNum.toString()
                    ) {
                        repository.updateUserInfo(
                            userId = localStreamer.userId,
                            userName = userData.card.name,
                            avatarUrl = userData.card.avatarUrl.ifEmpty { localStreamer.avatarUrl },
                            follower = userData.follower.toString(),
                            likeNum = userData.likeNum.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MonitorService", "网络请求失败", e)
            }
        }
    }

    // 核心函数
//    private fun scheduleNextCheck() {
    private fun scheduleNextCheck(delayMs: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // 注意：这里的 MonitorReceiver 必须是你之前创建并注册在 Manifest 里的那个
        val intent = Intent(this, MonitorReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

//        val triggerAt = System.currentTimeMillis() + 60_000 // 1 分钟后触发
        val triggerAt = System.currentTimeMillis() + delayMs // 1 分钟后触发

        // 适配 Android 12+ (API 31+) 的精确闹钟检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // 有权限，使用精确闹钟（即使用户在睡觉也能准时叫醒 Service）
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                // 没权限，降级使用普通闹钟，避免崩溃
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else
        // Android 6.0 - 11
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
    }

    // 1.3.0 动态捕捉
    private suspend fun checkAllStreamersDynamics(cookie: String) {

        // 注入全局变量供 Retrofit 拦截器使用
        RetrofitClient.userCookie = cookie

        val sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // 获取所有已保存的主播
        val streamers = repository.getAllStreamersSync()

        Log.d("MonitorService", "开始执行低频动态检查...")

        streamers.forEach { streamer ->
            // 如果【主播开关开启】，去检查动态
            if (streamer.isEnabled) {
                try {
                    // 先调用一次导航接口，模拟真实浏览器访问（心跳机制/预热）
                    try {
                        RetrofitClient.service.checkLoginStatus()
                        delay(Random.nextLong(500, 1500)) // 随机延迟0.5到1.5秒，模拟人类操作
                    } catch (e: Exception) {
                        Log.w("MonitorService", "心跳预热请求失败", e)
                    }

                    // 调用 B 站官方 Android 端接口
                    val response = RetrofitClient.service.getUserDynamics(streamer.userId)

                    // 移动端的数据在 cards 里
                    if (response.code == 0 && !response.data?.items.isNullOrEmpty()) {
                        val items = response.data.items

                        // 找出当前列表中【真正】最新的动态（通过时间戳，防止被置顶的老动态骗了）
                        var newestItemInList = items[0]
                        for (item in items) {
                            val t1 = item.modules?.moduleAuthor?.pubTs ?: 0L
                            val t2 = newestItemInList.modules?.moduleAuthor?.pubTs ?: 0L
                            if (t1 > t2) {
                                newestItemInList = item
                            }
                        }

                        val latestId = newestItemInList.idStr
                        val latestTime = newestItemInList.modules?.moduleAuthor?.pubTs ?: 0L

                        val savedIdKey = "last_dynamic_id_${streamer.userId}"
                        val savedTimeKey = "last_dynamic_time_${streamer.userId}" // ★ 新增时间记录

                        val savedId = sp.getString(savedIdKey, "")
                        val savedTime = sp.getLong(savedTimeKey, 0L)

                        if (savedId.isNullOrEmpty() || savedTime == 0L) {
                            sp.edit {
                                putString(savedIdKey, latestId)
                                putLong(savedTimeKey, latestTime)
                            }
                        } else if (savedId != latestId) {
                            val newDynamics =
                                mutableListOf<BiliAppDynamicItem>()

                            // 核心防洪逻辑：遍历所有动态，只提取时间戳严格大于已存记录的
                            for (item in items) {
                                val itemTime = item.modules?.moduleAuthor?.pubTs ?: continue

                                // 跳过老动态和由于主播删动态导致的旧列表
                                if (itemTime <= savedTime) continue
                                // 极端双重保险：跳过 ID 相同的
                                if (item.idStr == savedId) continue

                                newDynamics.add(item)
                            }

                            if (newDynamics.isNotEmpty()) {
                                newDynamics.sortedBy { it.modules?.moduleAuthor?.pubTs ?: 0L }.forEach { item ->
                                    val dynamicId = item.idStr
                                    var text: String? = getString(R.string.notify_new_info_message)

                                    // 解析 App 接口特有的 JSON 字符串 (cardStr) 来提取正文
                                    try {
                                        text = item.modules?.moduleDynamic?.desc?.text
                                        if (text.isNullOrEmpty()) {
                                            text = item.modules?.moduleDynamic?.major?.archive?.title
                                        }
                                        if (text.isNullOrEmpty()) {
                                            text = item.modules?.moduleDynamic?.major?.archive?.desc
                                        }
                                        if (text.isNullOrEmpty()) {
                                            text = getString(R.string.notify_new_info_message)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MonitorService", "解析动态 JSON 失败", e)
                                    }

                                    var shortText = text?.take(100)
                                    text?.length?.let { if (it > 100 ) shortText += "..." }

//                                    Log.d("MonitorService", "推送新动态: $dynamicId")

                                    val title =
                                        getString(R.string.notify_new_info, streamer.userName)

                                    sendDynamicNotification(
                                        title = title,
                                        message = shortText,
                                        avatarUrl = streamer.avatarUrl,
                                        dynamicId = dynamicId
                                    )

                                    repository.insertNotification(
                                        NotificationRecord(
                                            title = title,
                                            message = text, // 这里的 text 就是你解析出来的正文
                                            timestamp = System.currentTimeMillis(),
                                            type = "DYNAMIC",
                                            roomId = streamer.roomId,
                                            userId = streamer.userId,
                                            dynamicId = dynamicId, // 确保这里传入你提取的动态 ID
                                            avatarUrl = streamer.avatarUrl
                                        )
                                    )

                                    delay(500)
                                }

                                // 无论有没有新通知（哪怕是因为删动态触发的空过），都要将指针刷新到当前最新
                                sp.edit {
                                    putString(savedIdKey, latestId)
                                    putLong(savedTimeKey, latestTime)
                                }
                            }
                        }
                    } else if (response.code == -412) {
                        Log.w("MonitorService", "触发风控，临时跳过动态检查: ${streamer.userName}")
                        // 如果触发风控，建议直接 return 停止本次所有检查
                        return
                    } else if (response.code != 0) {
                        Log.w("MonitorService", "动态接口返回异常代码: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("MonitorService", "获取动态失败: ${streamer.userName}", e)
                }

                val jitterMs = Random.nextLong(1, 301)

                // 稍微停顿 1 秒，防止短时间内并发请求太多被封 IP
                delay(15000L + jitterMs)
            }
        }
    }

    // 发送动态专用通知 (点击跳转B站动态页)
    private suspend fun sendDynamicNotification(
        title: String,
        message: String?,
        avatarUrl: String,
        dynamicId: String
    ) {
        val context = this
        val channelId = "dynamic_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // 创建独立渠道
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notify_info_update),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // 点击跳转到 B 站动态
        val intent = Intent(Intent.ACTION_VIEW, "bilibili://following/detail/$dynamicId".toUri())
        val pendingIntent = PendingIntent.getActivity(
            this,
            dynamicId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val imageLoader = ImageLoader(context)

        // 加载头像作为 LargeIcon
        val avatarRequest = ImageRequest.Builder(context)
            .data(avatarUrl.ifEmpty { R.drawable.ic_notification })
            .error(R.drawable.ic_notification)
            .size(256, 256) // 强制限制图片大小，防止 OOM
            .precision(coil.size.Precision.EXACT)
            .allowHardware(false) // 必须设为 false 才能转为 Bitmap
            .build()
        val avatarBitmap = (imageLoader.execute(avatarRequest).drawable as? BitmapDrawable)?.bitmap

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 换成你自己的小图标
            .setLargeIcon(avatarBitmap)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(dynamicId.hashCode(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        stopAlarmSound()
        super.onDestroy()
    }
}

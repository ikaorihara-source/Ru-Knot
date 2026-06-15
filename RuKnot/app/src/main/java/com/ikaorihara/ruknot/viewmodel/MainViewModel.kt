package com.ikaorihara.ruknot.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.data.AppThemeMode
import com.ikaorihara.ruknot.data.RemoteBackground
import com.ikaorihara.ruknot.data.SettingsDataStore
import com.ikaorihara.ruknot.data.ThemeStorage
import com.ikaorihara.ruknot.data.repository.AlarmRepository
import com.ikaorihara.ruknot.network.RetrofitClient
import com.ikaorihara.ruknot.network.UserCardData
import com.ikaorihara.ruknot.notification.NotificationRecord
import com.ikaorihara.ruknot.streamer.StreamerRoom
import com.ikaorihara.ruknot.utils.AppSettings
import com.ikaorihara.ruknot.utils.CrashHandler
import com.ikaorihara.ruknot.utils.DataBackupManager
import com.ikaorihara.ruknot.utils.UpdateInfo
import com.ikaorihara.ruknot.utils.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException

// 继承 AndroidViewModel 是为了能拿到 application context 来创建数据库
class MainViewModel(
    application: Application,
    private val repository: AlarmRepository
) :
    AndroidViewModel(application) {

    // 初始化 SettingsDataStore
    private val settingsDataStore = SettingsDataStore(application)

    // 初始化存储工具
    private val themeStorage = ThemeStorage(application)

    // 供界面观察的主题流
    val themeMode: StateFlow<AppThemeMode> = themeStorage.themeMode
        .stateIn(viewModelScope, SharingStarted.Lazily, AppThemeMode.FOLLOW_SYSTEM)

    // 修改主题的方法
    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            themeStorage.setThemeMode(mode)
        }
    }

    val currentCookie: StateFlow<String> = settingsDataStore.biliCookie
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    // 暴露全局音量状态
    val globalVolume: StateFlow<Int> = settingsDataStore.globalVolume
        .stateIn(viewModelScope, SharingStarted.Lazily, 80)

    // 修改全局音量的方法
    fun setGlobalVolume(volume: Int) {
        viewModelScope.launch {
            settingsDataStore.setGlobalVolume(volume)
        }
    }

    // 改成观察 String 类型的路径
    val currentBackgroundPath = settingsDataStore.backgroundPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 在线列表数据源
    private val _onlineBackgrounds = MutableStateFlow<List<RemoteBackground>>(emptyList())
    val onlineBackgrounds = _onlineBackgrounds.asStateFlow()

    // 下载状态 (Key=ID, Value=是否正在下载)
    private val _downloadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val downloadingStates = _downloadingStates.asStateFlow()

    // 刷新触发器 (用来通知 UI 文件变动了)
    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    val isRandomBg: StateFlow<Boolean> = settingsDataStore.isRandomBackground
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

//    // 暴露当前的代理地址
//    val currentProxyUrl: StateFlow<String> = settingsDataStore.githubProxyUrl
//        .stateIn(viewModelScope, SharingStarted.Lazily, "https://gh-proxy.com/")

    private val baseUrl =
        "https://orihararurubutton.blob.core.windows.net/orihararurubuttoncontainer/"

    private val updateUrl =
        "${baseUrl}app-release/"
    private val backgroundsUrl =
        "${baseUrl}backgrounds/"

    // 定义 Update JSON 地址
    private val updateJsonUrl = "${updateUrl}update.json"

    // 添加状态
    var updateInfoState by mutableStateOf<UpdateInfo?>(null)
    var isDownloading by mutableStateOf(false)

    // 下载进度 (0.0f ~ 1.0f)
    var downloadProgress by mutableFloatStateOf(0f)

    private var downloadJob: Job? = null

    // 标记是否已经自动检查过
    private var hasAutoChecked = false

    // 检查更新
    fun checkAppUpdate(context: Context? = null, isManual: Boolean = false) {
        // 逻辑：如果是自动检查，且之前已经查过了，直接跳过
        if (!isManual && hasAutoChecked) {
            return
        }

        // 如果是自动检查，标记为“已检查”
        if (!isManual) {
            hasAutoChecked = true
        }

        viewModelScope.launch {

            val info = UpdateManager.checkUpdate(updateJsonUrl)

            if (info != null) {
                // 触发 UI 弹窗
                updateInfoState = info
            } else {
                // 无更新
                if (isManual && context != null) {
                    // 只有手动检查时，才提示“已是最新”
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_latest_version),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // 执行更新
    fun performUpdate(context: Context) {
        val info = updateInfoState ?: return

        downloadJob = viewModelScope.launch {
            if (!context.packageManager.canRequestPackageInstalls()) {
                // 提示用户
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_allow_unknown_source),
                        Toast.LENGTH_LONG
                    ).show()
                }

                // 引导去设置页
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = "package:${context.packageName}".toUri()
                // 添加这个 flag 确保从设置页返回时能回到你的 App
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                // 暂停更新，直接返回
                // 用户去开启权限后，需要按返回键回到 App，再次点击“更新”按钮
                return@launch
            }

            isDownloading = true
            downloadProgress = 0f

            try {
                val file = UpdateManager.downloadApk(context, info.downloadUrl) { progress ->
                    downloadProgress = progress // 实时更新进度条
                }

                isDownloading = false

                if (file != null) {
                    UpdateManager.installApk(context, file)
                } else {
                    // 下载失败提示
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_download_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: CancellationException) {
                // 捕获取消异常
                Log.d("Update", "下载已取消")
                isDownloading = false
                downloadProgress = 0f
                // 这里不需要弹错误提示，因为是用户主动取消的
            }
        }
    }

    // 取消下载的方法
    fun cancelUpdate() {
        downloadJob?.cancel() // 停止协程
        isDownloading = false // 重置状态
        downloadProgress = 0f
    }

    // --- 拉取 GitHub 列表 ---
    // 下载并解析 JSON
    private fun fetchBackgroundList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
//                // 换用 ghproxy.net
//                var proxy = currentProxyUrl.value
//                // 健壮性修复：如果用户没填斜杠，自动补上
//                if (proxy.isNotEmpty() && !proxy.endsWith("/")) {
//                    proxy += "/"
//                }
                val jsonUrl =
//                    "${proxy}https://raw.githubusercontent.com/ikaorihara-source/RuKnot-Assets/main/backgrounds/backgrounds.json"
                    "${backgroundsUrl}backgrounds.json"

                // 创建一个“不检查证书”的 OkHttpClient (核弹级解决方案)
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    @SuppressLint("CustomX509TrustManager")
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                val unsafeClient = OkHttpClient.Builder()
                    .sslSocketFactory(
                        sslContext.socketFactory,
                        trustAllCerts[0] as X509TrustManager
                    )
                    .hostnameVerifier { _, _ -> true }
                    .retryOnConnectionFailure(true) // ★★★ 自动重试，解决 EOF 报错
                    .build()

                // 构建请求
                val request = Request.Builder()
                    .url(jsonUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .build()

                // 执行请求
                val response = unsafeClient.newCall(request).execute()
                val jsonStr = response.body?.string()

                if (response.isSuccessful && !jsonStr.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonStr)
                    val list = mutableListOf<RemoteBackground>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            RemoteBackground(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                url = obj.getString("url"),
                                thumb = obj.optString("thumb", ""),
                                isVideo = obj.getBoolean("isVideo"),
                                size = obj.getString("size")
                            )
                        )
                    }
                    _onlineBackgrounds.value = list
                    Log.d("MainViewModel", "✅ 成功加载 ${list.size} 个背景")
                } else {
                    Log.e("MainViewModel", "❌ 服务器返回错误: ${response.code}")
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ 获取列表惨败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // --- 下载并设为背景 (核心逻辑) ---
    fun downloadBackground(
        context: Context,
        item: RemoteBackground?,
        setBackground: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            // ★★★ 核心修复：如果是 null，说明用户点了“默认” ★★★
            if (item == null) {
                // 保存 "default" 字符串，代表恢复默认背景
                settingsDataStore.saveBackgroundPath("default")
                return@launch
            }

            // --- 下面的逻辑只有 item 不为 null 时才会执行 ---
            // 检查文件是否已存在
            val dir = File(context.filesDir, "backgrounds")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, item.id)

            if (file.exists() && file.length() > 0) {
                // 已存在，直接设置路径
                settingsDataStore.saveBackgroundPath(file.absolutePath)
                return@launch
            }

            // 不存在，开始下载
            _downloadingStates.value += (item.id to true) // 标记下载中

            try {
                // 创建一个“不检查证书”的 OkHttpClient (核弹级解决方案)
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    @SuppressLint("CustomX509TrustManager")
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                val unsafeClient = OkHttpClient.Builder()
                    .sslSocketFactory(
                        sslContext.socketFactory,
                        trustAllCerts[0] as X509TrustManager
                    )
                    .hostnameVerifier { _, _ -> true }
                    .retryOnConnectionFailure(true) // ★★★ 自动重试，解决 EOF 报错
                    .build()

//                var proxy = currentProxyUrl.value
//                // ★★★ 健壮性修复：如果用户没填斜杠，自动补上 ★★★
//                if (proxy.isNotEmpty() && !proxy.endsWith("/")) {
//                    proxy += "/"
//                }
//                val downloadUrl = "${proxy}${item.downloadUrl}"
                val downloadUrl = "${backgroundsUrl}${item.downloadUrl}"

                // 构建请求
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .build()

                // 执行下载
                val response = unsafeClient.newCall(request).execute()

                if (response.isSuccessful) {
                    // 把下载流写入文件
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 下载成功，保存路径
                    if (setBackground) {
                        settingsDataStore.saveBackgroundPath(file.absolutePath)
                    }

                    _refreshTrigger.value += 1
                    Log.d("MainViewModel", "✅ 图片下载并设置成功: ${file.absolutePath}")
                } else {
                    Log.e("MainViewModel", "❌ 图片下载失败: ${response.code}")
                    if (file.exists()) file.delete() // 删掉空文件
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (file.exists()) file.delete() // 删掉坏文件
            } finally {
                _downloadingStates.value -= item.id // 移除标记
            }
        }
    }

    // 简单的设置路径方法 (给已下载的文件用)
    fun setBackgroundPath(path: String) {
        viewModelScope.launch {
            // 用户手动选了，把“随机模式”关掉
            settingsDataStore.setRandomBackground(false)
            settingsDataStore.saveBackgroundPath(path)
        }
    }

    // 删除背景
    fun deleteBackground(context: Context, item: RemoteBackground) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, "backgrounds/${item.id}")
            if (file.exists()) {
                file.delete()

                // 如果当前正在用这张图，重置为默认
                if (currentBackgroundPath.value == file.absolutePath) {
                    settingsDataStore.saveBackgroundPath("default")
                }

                // ★★★ 删除成功，通知 UI 刷新 (隐藏删除按钮) ★★★
                _refreshTrigger.value += 1
            }
        }
    }

    // 随机挑选一张已下载的背景
    private suspend fun pickRandomDownloadedBackground() {
        val dir = File(getApplication<Application>().filesDir, "backgrounds")
        if (dir.exists()) {
            // 找出所有已下载的文件 (排除隐藏文件)
            val files = dir.listFiles { file -> !file.isHidden && file.isFile && file.length() > 0 }

            if (!files.isNullOrEmpty()) {
                // 随机挑一个
                val randomFile = files.random()
                settingsDataStore.saveBackgroundPath(randomFile.absolutePath)
                Log.d("MainViewModel", "🎲 随机切换背景: ${randomFile.name}")
            } else {
                // 如果没有下载任何图，就恢复默认
                settingsDataStore.saveBackgroundPath("default")
            }
        }
    }

    // 切换随机开关
    fun toggleRandomBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setRandomBackground(enabled)
            if (enabled) {
                // 如果打开了开关，立刻随机一次给用户看
                pickRandomDownloadedBackground()
            }
        }
    }

//    // 增加修改方法供 UI 调用
//    fun updateGithubProxyUrl(newUrl: String) {
//        viewModelScope.launch {
//            settingsDataStore.saveGithubProxyUrl(newUrl)
//        }
//    }

    // 增加修改方法供 UI 调用
    fun updateBiliCookie(cookie: String) {
        val cleanCookie = cookie.replace("\n", "").replace("\r", "").trim()

        viewModelScope.launch {
            settingsDataStore.saveBiliCookie(cleanCookie)
        }
    }

    // 测试 COOKIE 是否有效
    fun testCookie(testToken: String, context: Context) {
        if (testToken.isBlank()) {
            Toast.makeText(context, context.getString(R.string.toast_empty_cookie), Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 临时覆盖 RetrofitClient 中的 Token 用于测试
                val oldToken = RetrofitClient.userCookie
                RetrofitClient.userCookie = testToken

                // 发起验证请求 (复用了 BiliAppDynamicResponse)
                val response = RetrofitClient.service.checkLoginStatus()

                // 恢复成原来的 Token (真实的保存由用户点击"保存"按钮决定)
                RetrofitClient.userCookie = oldToken

                // 判断 code 是否为 0
                withContext(Dispatchers.Main) {
                    if (response.code == 0) {
                        Toast.makeText(context, context.getString(R.string.toast_test_success), Toast.LENGTH_LONG).show()
                    } else {
                        // 通常 -101 代表未登录
                        Toast.makeText(context, context.getString(R.string.toast_test_failed, response.code.toString()), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 备份数据到本地文件 (.rkn)
     */
    fun backupData(context: Context) {
        viewModelScope.launch {
            // 收集当前的设置

            // 语言
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val languageTag = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags()

            // 从 DataStore 获取当前值 (使用 first() 获取快照)
            val roomInterval = settingsDataStore.roomUpdateInterval.first()
            val userInterval = settingsDataStore.userUpdateInterval.first()
            val dynamicInterval = settingsDataStore.dynamicUpdateInterval.first() // ★
            val cookie = settingsDataStore.biliCookie.first()
            val volume = settingsDataStore.globalVolume.first()
//            val proxy = settingsDataStore.githubProxyUrl.first()

            // 从 ThemeStorage 获取当前主题
            val themeName = themeMode.value.name

            val settings = AppSettings(
                languageTag = languageTag,
                themeModeName = themeName,
                roomUpdateInterval = roomInterval,
                userUpdateInterval = userInterval,
                dynamicUpdateInterval = dynamicInterval,
                biliCookie = cookie,
                globalVolume = volume
            )

            // 调用 Manager 进行导出
            DataBackupManager.exportData(context, repository, settings)
        }
    }

    /**
     * 从本地文件恢复数据
     */
    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch {
            DataBackupManager.importData(context, uri, repository) { restored ->
                // 回调：这里执行设置的恢复逻辑

                // 恢复语言
                val locales = if (restored.languageTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(restored.languageTag)
                }
                // 切回主线程设置语言
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setApplicationLocales(locales)
                }

                // 恢复主题
                try {
                    val mode = AppThemeMode.valueOf(restored.themeModeName)
                    setTheme(mode) // 调用 MainViewModel 已有的 setTheme
                } catch (_: Exception) {
                    // 忽略枚举不匹配的情况
                }

                // 恢复 DataStore 中的数据
                settingsDataStore.saveRoomUpdateInterval(restored.roomUpdateInterval)
                settingsDataStore.saveUserUpdateInterval(restored.userUpdateInterval)
                settingsDataStore.saveDynamicUpdateInterval(restored.dynamicUpdateInterval)
                settingsDataStore.saveBiliCookie(restored.biliCookie)
                settingsDataStore.setGlobalVolume(restored.globalVolume)
            }
        }
    }

    // 原始数据流
    private val _rawRooms = repository.getAllStreamersFlow()

    // 使用 Flow 来观察数据库变化 (Real-time updates)
    // 只要数据库变了，这个 list 就会自动更新
    // 计算后的 UI 数据流
    val rooms: StateFlow<List<StreamerRoom>> = _rawRooms
        .map { list ->
            // 1. 检查是否有任意一个已启用的主播正在直播
            val isAnyLive = list.any { it.isEnabled && it.isLive }

            if (isAnyLive) {
                // === 战时模式 (有人开播) ===
                // 规则：置顶已播 -> 置顶未播 -> 未置顶已播 -> 未置顶未播
                list.sortedWith(
                    compareByDescending<StreamerRoom> { it.isPinned } // 先看置顶
                        .thenByDescending { it.isLive }           // 再看直播状态
                        .thenBy { it.customOrder }                // 最后按自定义顺序列出
                )
            } else {
                // === 平时模式 (无人开播) ===
                // 规则：完全按 customOrder 排，但要区分置顶区和非置顶区
                // (虽然 customOrder 理论上已经包含了这个顺序，但为了保险我们强制分组)
                list.sortedWith(
                    compareByDescending<StreamerRoom> { it.isPinned }
                        .thenBy { it.customOrder }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // 监听房间列表，如果发现露露不见了，立刻复活她
        viewModelScope.launch {
            rooms.collect { list ->
                // 如果列表不为空（说明DB已加载），但没有露露
                if (list.none { it.roomId == 22389206L }) {
                    addStreamer(22389206L, null, false)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // 如果开关是开的，就随机换一张
            if (settingsDataStore.isRandomBackground.first()) {
                pickRandomDownloadedBackground()
            }
        }

        viewModelScope.launch {
//            currentProxyUrl.collect { proxy ->
//                Log.d("MainViewModel", "代理地址已加载/更新: $proxy，正在刷新列表...")
//            }
            Log.d("MainViewModel", "正在刷新列表...")
            fetchBackgroundList()
        }
    }

    fun updateListOrder(newList: List<StreamerRoom>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 遍历 newList，根据 index 更新每个 item 的 customOrder 字段
            newList.forEachIndexed { index, room ->
                if (room.customOrder != index) {
                    repository.updateStreamerOrder(room.roomId, index)
                }
            }
        }
    }

    // 核心功能：根据房间号添加/更新主播
    fun addStreamer(roomId: Long, ringtoneUriString: String?, isVibrationOnly: Boolean) {
        // 启动一个协程（在后台线程干活，不卡住界面）
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // --- 处理铃声权限 (关键！) ---
                ensureRingtonePermission(ringtoneUriString)

                // 查房间信息
                val roomResp = RetrofitClient.service.getRoomInfo(roomId)

                if (roomResp.code == 0 && roomResp.data != null) {
                    val roomData = roomResp.data
                    val uid = roomData.uId

                    // 查用户信息
                    var userData: UserCardData? = null

                    try {
                        // 发起二次请求
                        val userResp = RetrofitClient.service.getUserInfo(uid)
                        if (userResp.code == 0 && userResp.data != null && userResp.data.card != null) {
                            userData = userResp.data
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "查用户信息失败，使用默认封面", e)
                    }

                    // 获取当前未置顶区“最靠前”的号码
                    val minOrder = repository.getMinUnpinnedOrder()

                    // 让新来的排在它前面 (减 1)
                    val newOrder = minOrder - 1

                    // 把查到的数据转换成我们数据库的格式
                    val newRoom = StreamerRoom(
                        roomId = roomData.roomId,
                        userId = uid,
                        userName = userData?.card?.name ?: "未知主播", // 有时候API不返回名字
                        title = roomData.title,
                        coverUrl = roomData.userCover,
                        avatarUrl = userData?.card?.avatarUrl ?: roomData.userCover,
                        isLive = roomData.liveStatus == 1, // 1代表直播中
                        follower = userData?.follower?.toString() ?: "-",
                        likeNum = userData?.likeNum?.toString() ?: "-",
                        ringtoneUri = ringtoneUriString, // 把选好的铃声存进去
                        isVibrationOnly = isVibrationOnly,
                        isPinned = roomData.roomId == 22389206L,
                        isLocked = false,
                        customOrder = newOrder
                    )

                    // 存入数据库 (Dao 会自动通知 rooms 更新)
                    repository.insertStreamer(newRoom)
                    Log.d("MainViewModel", "添加成功: ${userData?.card?.name ?: "未知主播"}")
                } else {
                    Log.e("MainViewModel", "B站API报错: ${roomResp.msg}")
                }
            } catch (e: Exception) {
                // 比如没网了，或者B站服务器炸了
                Log.e("MainViewModel", "网络请求失败", e)
            }
        }
    }

    private fun ensureRingtonePermission(uriString: String?) {
        if (uriString.isNullOrEmpty()) return // 如果是空的或者是默认铃声，直接跳过

        try {
            val uri = uriString.toUri()
            // 关键：告诉安卓系统“我要永久持有这个文件的读取权限”
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Log.d("MainViewModel", "铃声权限获取成功: $uriString")
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "无法获取铃声权限 (可能是系统铃声或不需要权限): ${e.message}")
        } catch (e: Exception) {
            Log.e("MainViewModel", "处理铃声URI时出错: ${e.message}")
        }
    }

    fun updateStreamer(room: StreamerRoom) {
        viewModelScope.launch {
            // 如果铃声没变，重复调用这个方法也是安全的（系统会忽略）
            ensureRingtonePermission(room.ringtoneUri)

            repository.updateStreamer(room)
        }
    }

    // 删除主播
    fun deleteStreamer(room: StreamerRoom) {
        // ★★★ 特化逻辑：保护折原露露 ★★★
        if (room.roomId == 22389206L) {
            // 可以选择在这里打印 Log，或者什么都不做直接返回
            Log.d("MainViewModel", "尝试删除折原露露被拦截！")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStreamer(room)
        }
    }

    // 切换置顶状态
    fun pinStreamer(room: StreamerRoom) {
        if (room.roomId == 22389206L) {
            // 可以选择在这里打印 Log，或者什么都不做直接返回
            Log.d("MainViewModel", "尝试取消置顶折原露露被拦截！")
            return
        }

        viewModelScope.launch {
            // 取反：如果是 true 变 false，反之亦然
            val newStatus = !room.isPinned
            repository.pinStreamer(room.roomId, newStatus)
        }
    }

    // 切换上锁状态
    fun lockStreamer(room: StreamerRoom) {
        if (room.roomId == 22389206L) {
            // 可以选择在这里打印 Log，或者什么都不做直接返回
            Log.d("MainViewModel", "尝试删除折原露露被拦截！")
            return
        }

        viewModelScope.launch {
            // 取反当前状态：如果是锁的就解锁，没锁就加锁
            repository.lockStreamer(room.roomId, !room.isLocked)
        }
    }

    val alarms: StateFlow<List<AlarmRule>> = repository.getAllRulesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 更新或添加闹钟 (因为是 Replace 策略，ID 一样就是更新，ID 不一样就是添加)
    fun updateAlarm(rule: AlarmRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    // 删除闹钟
    fun deleteAlarm(rule: AlarmRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    val notificationHistory: StateFlow<List<NotificationRecord>> =
        repository.getAllNotificationsFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearNotificationHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllNotifications()
        }
    }

    fun deleteNotification(record: NotificationRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotification(record)
        }
    }

    fun toggleNotificationLock(id: Int, isLocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleNotificationLock(id, isLocked)
        }
    }

    fun exportCrashLog(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // 切回主线程显示 Toast
            withContext(Dispatchers.Main) {
                CrashHandler.exportLatestLog(context)
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: AlarmRepository
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("未知视图模型类")
    }
}
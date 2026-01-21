package com.ikaorihara.ruknot

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ikaorihara.ruknot.alarm.AlarmActivity
import com.ikaorihara.ruknot.alarm.AlarmListScreen
import com.ikaorihara.ruknot.data.AppDatabase
import com.ikaorihara.ruknot.data.AppThemeMode
import com.ikaorihara.ruknot.data.getEulaText
import com.ikaorihara.ruknot.data.getPrivacyText
import com.ikaorihara.ruknot.data.repository.AlarmRepository
import com.ikaorihara.ruknot.screens.LegalMenuScreen
import com.ikaorihara.ruknot.screens.SettingsScreen
import com.ikaorihara.ruknot.screens.WebLinksScreen
import com.ikaorihara.ruknot.screens.WebViewScreen
import com.ikaorihara.ruknot.service.MonitorService
import com.ikaorihara.ruknot.streamer.StreamerListScreen
import com.ikaorihara.ruknot.ui.AppBackground
import com.ikaorihara.ruknot.ui.BottomNavItem
import com.ikaorihara.ruknot.ui.theme.RuKnotTheme
import com.ikaorihara.ruknot.utils.CrashHandler
import com.ikaorihara.ruknot.utils.ignoreSSLCheck
import com.ikaorihara.ruknot.viewmodel.MainViewModel
import com.ikaorihara.ruknot.viewmodel.MainViewModelFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    // 获取我们刚才写的 ViewModel (大脑)
    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AlarmRepository(database.StreamerDAO(), database.AlarmDAO())

        // 把 application 也传进去
        MainViewModelFactory(application, repository)
    }

    // 定义所需权限列表
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // 权限申请回调
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            // 权限全部通过，检查电池优化并启动服务
            checkBatteryOptimization()
        } else {
            // 权限被拒绝，强制退出
            Toast.makeText(this, getString(R.string.toast_missing_permissions), Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 核心：初始化崩溃日志捕获
        // 这样一旦有未捕获的异常，就会自动写入文件
        CrashHandler.init(this)

        // 防篡改核心逻辑
        // 只在 Release 模式下检查 (Debug 模式放行)
        // 只有当 local.properties 配置了 SHA1 时才检查 (方便他人学习代码)
        if (!BuildConfig.DEBUG && BuildConfig.APP_SHA1.isNotEmpty()) {
            if (!validateAppSignature(BuildConfig.APP_SHA1)) {
                // 校验失败！说明被篡改了
                // 可以在这里加个 Toast 骂一句，或者直接闪退
                finish()
                Process.killProcess(Process.myPid()) // 强制杀进程
                return
            }
        }

        ignoreSSLCheck()

        setContent {
            // 获取 SharedPreferences
            val context = LocalContext.current
            val prefs =
                remember { context.getSharedPreferences("app_settings", MODE_PRIVATE) }

            // 控制 Splash
            var showSplash by remember { mutableStateOf(true) }

            // 检查是否已同意 EULA (默认为 false)
            var isEulaAccepted by remember {
                mutableStateOf(prefs.getBoolean("is_eula_accepted", false))
            }

            // 监听当前的主题模式 (Flow)
            // 只要这里的值一变，整个 App 就会重绘
            val currentMode by viewModel.themeMode.collectAsState()

            // 监听背景图设置
            // 你需要在 ViewModel 里把 repository/datastore 的 backgroundId 暴露出来
            // 假设 viewModel.currentBackgroundId 是一个 StateFlow<Int>
            val bgPath by viewModel.currentBackgroundPath.collectAsState()

            // 智能判断：到底要不要开深色？
            val useDarkTheme = when (currentMode) {
                AppThemeMode.LIGHT -> false // 强制浅色
                AppThemeMode.DARK -> true  // 强制深色
                AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme() // 听系统的
            }

            RuKnotTheme(darkTheme = useDarkTheme) {
                if (showSplash) {
                    // 显示启动页 (Splash)
                    // 传入背景图路径，保持启动页背景和主页一致（如果需要的话，或者用纯色）
                    // 这里为了简单，我们让 Splash 也是透明背景叠在 AppBackground 上
                    AppBackground(bgPath = bgPath) {
                        // 注意：这里调用我们在 SplashScreen.kt 写好的组件
                        // 记得导包：import com.ikaorihara.ruknot.screens.SplashScreen
                        com.ikaorihara.ruknot.screens.SplashScreen(
                            onSplashFinished = {
                                // 动画结束，切换状态
                                showSplash = false
                            }
                        )
                    }
                } else {
                    // 如果【已同意】：显示正常的主界面
                    if (isEulaAccepted) {
                        // 核心：进入主界面时，才启动服务和请求权限
                        LaunchedEffect(Unit) {
                            startPermissionChain() // 请求权限

                            val intent = Intent(context, MonitorService::class.java)
                            startForegroundService(intent) // 启动服务
                        }

                        // 使用背景容器包裹整个 App
                        AppBackground(bgPath = bgPath) {
                            // 注意：Surface 的颜色必须设为 Transparent，否则会盖住背景图！
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Transparent // 关键：设为透明！
                            ) {
                                MainScreen(viewModel)
                            }
                        }
                    } else {
                        // 这里用 AppBackground 垫底，为了让弹窗后面不至于全黑，好看一点
                        AppBackground(bgPath = bgPath) {
                            // 显示 EULA 确认对话框
                            EulaConsentDialog(
                                onAccept = {
                                    // 用户点击同意：写入 SP，更新状态
                                    prefs.edit { putBoolean("is_eula_accepted", true) }
                                    isEulaAccepted = true
                                },
                                onDecline = {
                                    // 用户点击拒绝：直接关闭 App
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 校验当前 App 的签名是否与预设的 trustedSHA1 一致
     */
    private fun validateAppSignature(trustedSHA1: String): Boolean {
        try {
            val context = applicationContext
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            // 遍历所有签名（通常只有一个）
            if (signatures != null) {
                for (signature in signatures) {
                    val currentSHA1 = getSHA1(signature.toByteArray())
                    // 对比指纹 (忽略大小写)
                    if (currentSHA1.equals(trustedSHA1, ignoreCase = true)) {
                        return true // 验证通过
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false // 验证失败
    }

    // 辅助函数：计算字节数组的 SHA1
    private fun getSHA1(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA1")
        val bytes = md.digest(data)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * 权限检查链条入口
     */
    private fun startPermissionChain() {
        checkStandardPermissions()
    }

    // 检查基础权限 (通知、音频)
    private fun checkStandardPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            // 弹窗解释为什么需要这些权限
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.need_permissions_title))
                .setMessage(getString(R.string.need_permissions_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.btn_acknowledged)) { _, _ ->
                    permissionLauncher.launch(requiredPermissions)
                }
                .setNegativeButton(getString(R.string.btn_exit)) { _, _ -> finish() }
                .show()
        } else {
            // 已经有权限了，直接走后续逻辑
            checkBatteryOptimization()
        }
    }

    // 检查电池优化白名单 (保活关键)
    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        // 检查电池优化白名单 (保活关键)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.prevent_bg_terminated_title))
                .setMessage(getString(R.string.prevent_bg_terminated_message))
                .setPositiveButton(getString(R.string.btn_setting)) { _, _ ->
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:$packageName".toUri()
                        }
                    startActivity(intent)
                    checkFullScreenIntentPermission()
                }
                .setNegativeButton(getString(R.string.btn_exit)) { _, _ -> finish() }
                .show()
        } else {
            // 如果电池优化已过，直接检查全屏权限
            checkFullScreenIntentPermission()
        }
    }

    private fun checkFullScreenIntentPermission() {
        // 全屏意图检查仅针对 Android 14 (API 34) 及以上版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // 检查是否已经拥有全屏意图权限
            if (!notificationManager.canUseFullScreenIntent()) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.need_full_screen_title))
                    .setMessage(getString(R.string.need_full_screen_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.btn_setting)) { _, _ ->
                        try {
                            // 跳转到系统的“全屏意图”设置页面
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = "package:$packageName".toUri()
                                }
                            startActivity(intent)
                        } catch (_: Exception) {
                            // 如果某些 ROM 不支持直接跳转到该页面，跳转到应用详情页
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:$packageName".toUri()
                                }
                            startActivity(intent)
                        }
                        checkExactAlarmPermission()
                    }
                    .setNegativeButton(getString(R.string.btn_exit)) { _, _ -> finish() }
                    .show()
            } else {
                checkExactAlarmPermission()
            }
        } else {
            checkExactAlarmPermission()
        }
    }

    // 检查精确闹钟权限 (针对 Android 13+ 准时关键)
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.allow_precise_alarm_title))
                    .setMessage(getString(R.string.allow_precise_alarm_message))
                    .setPositiveButton(getString(R.string.btn_setting)) { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(intent)
                        showAutoStartGuide()
                    }
                    .setNegativeButton(getString(R.string.btn_exit)) { _, _ -> finish() }
                    .show()
                return
            } else {
                showAutoStartGuide()
            }
        } else {
            showAutoStartGuide()
        }
    }

    // 自启动引导
    private fun showAutoStartGuide() {
        // 使用 SharedPreferences 记录，避免每次打开 App 都弹窗烦用户
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isGuided = prefs.getBoolean("autostart_guided", false)

        if (!isGuided) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.allow_ensure_no_miss_title))
                .setMessage(getString(R.string.allow_ensure_no_miss_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.btn_setting)) { _, _ ->
                    // 记录已引导过
                    prefs.edit { putBoolean("autostart_guided", true) }

                    openAutoStartSettings(this)
                }
                .setNegativeButton(getString(R.string.btn_exit)) { _, _ -> finish() }
                .show()
        } else {
            finalStartService()
        }
    }

    // 新增这个方法：把它放在 MainActivity 类里面
    private fun openAutoStartSettings(context: Context) {
        val intent = Intent()

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val manufacturer = Build.MANUFACTURER.lowercase()

        val componentName = when (manufacturer) {
            // === 三星 (Samsung) ===
            "samsung" -> ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )

            // === ROG / 华硕 (Asus) ===
            "asus" -> ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity"
            )

            // === 红魔 (Nubia) ===
            "nubia" -> ComponentName(
                "cn.nubia.security2",
                "cn.nubia.security.appmanage.selfstart.ui.SelfStartActivity"
            )

            // === 小米 / Redmi ===
            "xiaomi", "redmi" -> ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )

            // === OPPO / Realme ===
            "oppo", "realme" -> ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )

            // === VIVO / iQOO ===
            "vivo" -> ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )

            // === 荣耀 ===
            "honor" -> ComponentName(
                "com.hihonor.systemmanager",
                "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )

            // === 一加 (OnePlus) ===
            "oneplus" -> ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )

            // === 联想 / 摩托罗拉 ===
            "lenovo", "motorola" -> ComponentName(
                "com.lenovo.security",
                "com.lenovo.security.purebackground.PureBackgroundActivity"
            )

            // === HTC ===
            "htc" -> ComponentName(
                "com.htc.pitroad",
                "com.htc.pitroad.landingpage.activity.LandingPageActivity"
            )

            // === 魅族 (Meizu) ===
            "meizu" -> ComponentName(
                "com.meizu.safe",
                "com.meizu.safe.permission.SmartBGActivity"
            )

            // === 乐视 (EUI) ===
            "letv" -> ComponentName(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"
            )

            // === 华为 ===
            "huawei" -> ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )

            "google", "sony" -> null

            else -> null
        }

        try {
            if (componentName != null) {
                intent.component = componentName
                context.startActivity(intent)
            } else {
                // 跳转到应用详情页作为保底
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                settingsIntent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(settingsIntent)
            }
        } catch (_: Exception) {
            // 跳转失败处理：如果连详情页都跳不过去，可以尝试手动打开设置首页，或者忽略
            try {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                settingsIntent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(settingsIntent)
            } catch (_: Exception) {
                // 实在不行就跳到设置首页，总比崩溃好
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    // 彻底放弃，什么都不做，防止闪退
                }
            }
        }
    }

    // 最终启动服务
    private fun finalStartService() {
        val intent = Intent(this, MonitorService::class.java)
        startForegroundService(intent)
    }

    // 生命周期检查
    override fun onResume() {
        super.onResume()

        // 核心逻辑：如果正在响铃，用户点图标进来不应该看主页，而是应该看闹钟页
        if (MonitorService.isRinging && MonitorService.currentAlarmList.isNotEmpty()) {

            // 准备跳转
            val intent = Intent(this, AlarmActivity::class.java).apply {
                // 加上 Flag 避免重复创建
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

                // 把数据传过去，否则 AlarmActivity 打开是空的
                putExtra("ALARM_LIST", MonitorService.currentAlarmList)
            }
            startActivity(intent)

            // 可选：如果希望用户按“返回”键不要回到主页，而是直接退出，可以把 MainActivity 关掉
            // 但通常保留在后台也没事。这里建议不 finish，体验更流畅。
            // finish()
            return
        }
    }
}

// ==========================================
// 强制 EULA 确认弹窗
// ==========================================
@Composable
fun EulaConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // 使用 Material 3 的 AlertDialog
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {
            // 禁止点击外部关闭！
            // 这里什么都不做，强制用户必须选
        },
        title = {
            Text(
                text = stringResource(R.string.welcome_title, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.agree_eula_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 一个可滚动的区域显示主要条款
                Box(
                    modifier = Modifier
                        .height(300.dp) // 固定高度，内容太多就滚动
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        // 显示 EULA
                        Text(
                            text = getEulaText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // 显示 隐私政策
                        Text(
                            text = getPrivacyText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(
                    text = stringResource(R.string.btn_agree_continue),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(
                    text = stringResource(R.string.btn_exit),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()

    // 定义底部按钮列表
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Alarms,
        BottomNavItem.Links,
        BottomNavItem.Settings
    )

    Scaffold(
        // 把脚手架设为透明，否则会挡住背景图
        containerColor = Color.Transparent,

        bottomBar = {
            NavigationBar {
                // 获取当前路由，为了让选中的按钮高亮
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    // 自定义选中逻辑
                    val isSelected = when (item) {
                        BottomNavItem.Links -> {
                            // 如果是 Links 按钮：当前是 links 页 OR 当前是 webview 页，都算选中
                            currentRoute == item.route || currentRoute?.startsWith("webview") == true
                        }

                        BottomNavItem.Settings -> {
                            currentRoute == item.route || currentRoute?.equals("legal_menu") == true
                        }

                        else -> {
                            // 其他按钮：必须路由完全匹配才算选中
                            currentRoute == item.route
                        }
                    }

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = stringResource(item.titleResId),

                                // 图片保持原色（不被选中态染色
                                tint = Color.Unspecified,

                                // 图片太大或太小，可以调整大小
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = isSelected,
                        onClick = {
                            // 如果当前在看网页，点了相关链接，回到列表
                            if (item == BottomNavItem.Links && currentRoute?.startsWith("webview") == true) {
                                navController.popBackStack(BottomNavItem.Links.route, false)
                            } else if (item == BottomNavItem.Settings && currentRoute?.equals("legal_menu") == true) {
                                navController.popBackStack(BottomNavItem.Settings.route, false)
                            } else {
                                navController.navigate(item.route) {
                                    // 避免点击多次造成页面堆叠
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 这里的 NavHost 负责切换中间的内容区域
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = BottomNavItem.Home.route) {

                // Tab 1: 主播列表
                composable(BottomNavItem.Home.route) {
                    StreamerListScreen(viewModel)
                }

                // Tab 2: 闹钟列表
                composable(BottomNavItem.Alarms.route) {
                    AlarmListScreen(viewModel)
                }

                // Tab 3: 链接列表
                composable(BottomNavItem.Links.route) {
                    WebLinksScreen(
                        onNavigateToWeb = { url ->
                            // 编码 URL 并跳转到 webview 页面
                            val encodedUrl =
                                URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            navController.navigate("webview/$encodedUrl")
                        },
                        onBack = {
                            // 因为是底部 Tab，点返回通常什么都不做，或者跳回首页
                            navController.navigate(BottomNavItem.Home.route)
                        }
                    )
                }

                // Tab 4: 设置
                composable(BottomNavItem.Settings.route) {
                    SettingsScreen(
                        viewModel,
                        onNavigateToLegal = { navController.navigate("legal_menu") }
                    )
                }

                // 法律菜单页
                composable("legal_menu") {
                    LegalMenuScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // 辅助页面：内置浏览器 WebView
                // (这个页面没有底部 Tab，是点链接跳过来的)
                composable(
                    route = "webview/{url}",
                    arguments = listOf(navArgument("url") { type = NavType.StringType })
                ) { backStackEntry ->
                    val urlArg = backStackEntry.arguments?.getString("url") ?: ""
                    val decodedUrl = URLDecoder.decode(urlArg, StandardCharsets.UTF_8.toString())

                    WebViewScreen(
                        url = decodedUrl,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
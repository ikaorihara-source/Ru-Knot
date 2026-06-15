package com.ikaorihara.ruknot.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.data.AppThemeMode
import com.ikaorihara.ruknot.data.SettingsDataStore
import com.ikaorihara.ruknot.util.getAppVersionName
import com.ikaorihara.ruknot.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToLegal: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 观察 ViewModel 中的状态
    val themeMode by viewModel.themeMode.collectAsState()
    val globalVolume by viewModel.globalVolume.collectAsState()

    val appVersion = remember { getAppVersionName(context) }

    // 实例化 DataStore (用于频率设置)
    val dataStore = remember { SettingsDataStore(context) }

    // 观察频率设置
    val roomInterval by dataStore.roomUpdateInterval.collectAsState(initial = 30L)
    val userInterval by dataStore.userUpdateInterval.collectAsState(initial = 60L)
    val dynamicInterval by dataStore.dynamicUpdateInterval.collectAsState(initial = 300L)
    val scope = rememberCoroutineScope()

    // 应用内更新弹窗
    val updateInfo = viewModel.updateInfoState
    val isDownloading = viewModel.isDownloading
    val downloadProgress = viewModel.downloadProgress

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                // 点击外部关闭 (下载中禁止关闭)
                if (!isDownloading) viewModel.updateInfoState = null
            },
            title = {
                Text(
                    stringResource(
                        R.string.update_dialog_title,
                        updateInfo.versionName
                    )
                )
            }, // 建议添加字符串资源: "发现新版本: %s"
            text = {
                Column {
                    Text(
                        stringResource(R.string.update_dialog_content_header),
                        fontWeight = FontWeight.Bold
                    ) // "更新内容："
                    Spacer(Modifier.height(4.dp))
                    Text(updateInfo.changeLog, style = MaterialTheme.typography.bodySmall)

                    if (isDownloading) {
                        Spacer(Modifier.height(16.dp))

                        // 显示进度条 (传入 progress 参数)
                        // progress 必须是 0.0 到 1.0 之间
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // 显示百分比文字
                        // 格式化为整数百分比，例如 "正在下载中... 45%"
                        val percentage = (downloadProgress * 100).toInt()
                        Text(
                            text = stringResource(
                                R.string.update_dialog_downloading,
                                percentage
                            ), // 建议放入 strings.xml
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(onClick = {
                        // 核心调用：执行更新
                        viewModel.performUpdate(context)
                    }) {
                        Text(stringResource(R.string.btn_update_in_app)) // "立即更新"
                    }
                }
            },
            dismissButton = {
                if (isDownloading) {
                    TextButton(
                        onClick = {
                            viewModel.cancelUpdate() // 调用取消
                        }
                    ) {
                        Text(
                            stringResource(R.string.btn_cancel),
                            color = MaterialTheme.colorScheme.error
                        ) // 建议用红色显示
                    }
                } else {
                    // 到夸克更新按钮
                    TextButton(
                        onClick = {
                            try {
                                val uri = "https://pan.quark.cn/s/712f0d7dbae6?pwd=fQRW".toUri()
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    R.string.toast_cant_open_browser,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.btn_update_in_quark))
                    }

                    // 到百度更新按钮
                    TextButton(
                        onClick = {
                            try {
                                val uri =
                                    "https://pan.baidu.com/s/1N3yA9bkiCtYueVZEamvpEw?pwd=msj8".toUri()
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    R.string.toast_cant_open_browser,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.btn_update_in_baidu))
                    }

                    TextButton(onClick = { viewModel.updateInfoState = null }) {
                        Text(stringResource(R.string.btn_update_later)) // "暂不更新"
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // APP 语言设置
        ExpandableSection(
            title = stringResource(R.string.label_language),
            icon = R.drawable.ic_language,
            initiallyExpanded = false
        ) {
            LanguageSettingCard()
        }

        // 夜间模式设置
        ExpandableSection(
            title = stringResource(R.string.label_night_mode),
            icon = R.drawable.ic_night,
            initiallyExpanded = false
        ) {
            ThemeSettingCard(
                currentMode = themeMode,
                onModeSelected = { viewModel.setTheme(it) }
            )
        }

        // 数据更新频率设置
        ExpandableSection(
            title = stringResource(R.string.label_data_sync_frequency),
            icon = R.drawable.ic_sync,
            initiallyExpanded = false
        ) {
            PollingIntervalCard(
                roomInterval = roomInterval,
                userInterval = userInterval,
                dynamicInterval = dynamicInterval,
                onRoomIntervalChange = { value ->
                    scope.launch {
                        dataStore.saveRoomUpdateInterval(
                            value
                        )
                    }
                },
                onUserIntervalChange = { value ->
                    scope.launch {
                        dataStore.saveUserUpdateInterval(
                            value
                        )
                    }
                },
                onDynamicIntervalChange = { value ->
                    scope.launch {
                        dataStore.saveDynamicUpdateInterval(
                            value
                        )
                    }
                }
            )
        }

        // B站登录设置
        ExpandableSection(
            title = stringResource(R.string.label_login_token),
            icon = R.drawable.ic_sync,
            initiallyExpanded = false
        ) {
            LoginTokenCard(
                viewModel = viewModel
            )
        }

        // 全局音量设置
        ExpandableSection(
            title = stringResource(R.string.label_global_volume),
            icon = R.drawable.ic_music,
            initiallyExpanded = false
        ) {
            GlobalVolumeCard(
                volume = globalVolume,
                onVolumeChange = { viewModel.setGlobalVolume(it) }
            )
        }

        // 背景选择卡片
        ExpandableSection(
            title = stringResource(R.string.label_personalized_background),
            icon = R.drawable.ic_background,
            initiallyExpanded = false
        ) {
            PersonalizedBackgroundCard(
                viewModel = viewModel
            )
        }

//        // 网络设置 (修改代理)
//        ExpandableSection(
//            title = stringResource(R.string.label_network_proxy),
//            icon = R.drawable.ic_network,
//            initiallyExpanded = false
//        ) {
//            NetworkProxyCard(
//                viewModel = viewModel
//            )
//        }

        // 后台保活设置
        ExpandableSection(
            title = stringResource(R.string.label_battery_optimization),
            icon = R.drawable.ic_battery_vertical, // 建议找一个电池相关的图标
            initiallyExpanded = false
        ) {
            BatteryOptimizationCard()
        }

        // 添加权限请求的 Launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.backupData(context)
            } else {
                Toast.makeText(
                    context,
                    R.string.toast_backup_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 数据备份与恢复
        // 定义文件选择器
        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { selectedUri ->
                // 文件名后缀校验逻辑
                var isValidFile = false
                val contentResolver = context.contentResolver

                // 查询文件名
                contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val fileName = cursor.getString(nameIndex)
                            // 检查是否以 .rkn 结尾 (忽略大小写)
                            if (fileName.endsWith(".rkn", ignoreCase = true)) {
                                isValidFile = true
                            }
                        }
                    }
                }

                if (isValidFile) {
                    // 用户选了文件后，调用 ViewModel 恢复
                    viewModel.restoreData(context, selectedUri)
                } else {
                    Toast.makeText(context, R.string.toast_unrecognized_file, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        ExpandableSection(
            title = stringResource(R.string.label_data_management),
            icon = R.drawable.ic_network,
            initiallyExpanded = false
        ) {
            DataBackupCard(
                onBackup = {
                    // 判断版本：如果是 Android 10 (Q) 以上，直接备份
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        viewModel.backupData(context)
                    } else {
                        // 如果是 Android 9 以下，先检查权限
                        // 需要 ContextCompact 或 ContextCompat
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.backupData(context)
                        } else {
                            // 没权限，弹窗申请
                            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                },
                onRestore = {
                    // 打开文件选择器，尝试只显示二进制文件和未知类型文件
                    importLauncher.launch(arrayOf("application/octet-stream", "application/*"))
                }
            )
        }

        // 关于与免责声明 (放在最下面)
        ExpandableSection(
            title = stringResource(R.string.label_about_and_disclaimer),
            icon = R.drawable.ic_info,
            initiallyExpanded = false
        ) {
            AboutCard(
                appVersion = appVersion,
                onCheckAppUpdate = { viewModel.checkAppUpdate(context = context, isManual = true) },
                onExportLog = { viewModel.exportCrashLog(context) },
                onNavigateToLegal = onNavigateToLegal
            )
        }

        // 底部留白，防止被导航栏遮挡
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==========================================
// 通用组件：可展开的卡片
// ==========================================
@Composable
fun ExpandableSection(
    title: String,
    @DrawableRes icon: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    // 箭头旋转动画
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = stringResource(R.string.label_rotation)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 标题栏 (可点击)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,

                    // 图片保持原色（不被选中态染色
                    tint = Color.Unspecified,

                    // 图片太大或太小，可以调整大小
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // 旋转的小箭头
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.label_expand),
                    modifier = Modifier.rotate(rotationState)
                )
            }

            // 内容区域 (带折叠动画)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    // 加一条分割线，区分标题和内容
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    content()
                }
            }
        }
    }
}

// ==========================================
// 语言设置卡片 (核心难点)
// ==========================================
@Composable
fun LanguageSettingCard() {
    var expanded by remember { mutableStateOf(false) }

    // 定义支持的语言列表 (Key: 显示名称, Value: 语言代码)
    // 虽然无法直接读取 res 文件夹结构，但标准做法是在这里定义你支持的语言
    val languageOptions = mapOf(
        stringResource(R.string.follow_system) to "", // 空字符串代表跟随系统
        "English" to "en",
        "简体中文" to "zh-CN"
    )

    // 获取当前语言标签
    val currentLocale = remember {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }

    // 计算当前显示什么文字
    val displayLabel = if (currentLocale.isEmpty()) {
        stringResource(R.string.follow_system)
    } else {
        // 简单的匹配逻辑
        if (currentLocale.contains("zh")) "简体中文"
        else if (currentLocale.contains("en")) "English"
        else stringResource(R.string.follow_system)
    }

    // 下拉菜单区域
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false, // 禁用输入，但让外层 Box 响应点击
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        // 覆盖一个透明的可点击区域
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languageOptions.forEach { (label, tag) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        // ★★★ 核心：切换语言 ★★★
                        val localeList = if (tag.isEmpty()) {
                            LocaleListCompat.getEmptyLocaleList() // 跟随系统
                        } else {
                            LocaleListCompat.forLanguageTags(tag)
                        }
                        AppCompatDelegate.setApplicationLocales(localeList)
                    }
                )
            }
        }
    }
}

// ==========================================
// 夜间模式卡片
// ==========================================
@Composable
fun ThemeSettingCard(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {

        // 选项列表
        val options = listOf(
            AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.follow_system),
            AppThemeMode.DARK to stringResource(R.string.enable_night_mode),
            AppThemeMode.LIGHT to stringResource(R.string.disable_night_mode)
        )

        options.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (currentMode == mode),
                    onClick = { onModeSelected(mode) }
                )
                Text(text = label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

// ==========================================
// 频率设置卡片
// ==========================================
@Composable
fun PollingIntervalCard(
    roomInterval: Long,
    userInterval: Long,
    dynamicInterval: Long,
    onRoomIntervalChange: (Long) -> Unit,
    onUserIntervalChange: (Long) -> Unit,
    onDynamicIntervalChange: (Long) -> Unit
) {
    Column {
        // --- 房间更新 ---
        Text(
            stringResource(R.string.label_live_detection_interval, roomInterval),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            stringResource(R.string.live_detection_interval_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Slider(
            value = roomInterval.toFloat(),
            onValueChange = { newValue ->
                val stepped = (newValue / 15).roundToLong() * 15
                onRoomIntervalChange(stepped.coerceIn(15, 600))
            },
            valueRange = 15f..600f,
            steps = 38 // (600-15)/15 - 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 主播更新 ---
        Text(
            stringResource(R.string.label_streamer_info_sync_interval, userInterval),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            stringResource(R.string.streamer_info_sync_interval_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Slider(
            value = userInterval.toFloat(),
            onValueChange = { newValue ->
                val stepped = (newValue / 30).roundToLong() * 30
                onUserIntervalChange(stepped.coerceIn(30, 600))
            },
            valueRange = 30f..600f,
            steps = 18 // (600-30)/30 - 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 动态更新 ---
        Text(
            stringResource(R.string.label_dynamic_info_sync_interval, dynamicInterval),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            stringResource(R.string.dynamic_info_sync_interval_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Slider(
            value = dynamicInterval.toFloat(),
            onValueChange = { newValue ->
                val stepped = (newValue / 60).roundToLong() * 60
                onDynamicIntervalChange(stepped.coerceIn(60, 1800))
            },
            valueRange = 60f..1800f,
            steps = 28, // (1800-60)/60 - 1
            enabled = false
        )
    }
}

// ==========================================
// 登入设置内容
// ==========================================
@Composable
fun LoginTokenCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val cookie by viewModel.currentCookie.collectAsState()
    var tempSess by remember(cookie) { mutableStateOf(cookie) }

    Column {
        Text(
            text = stringResource(R.string.login_token_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tempSess,
            onValueChange = { tempSess = it },
            label = { Text("Cookie") },
            placeholder = { Text(stringResource(R.string.holder_paste_here)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 将单按钮改为 Row 布局包容两个按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End, // 靠右对齐
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 测试按钮
            OutlinedButton(
                onClick = {
                    viewModel.testCookie(tempSess, context)
                }
            ) {
                Text(stringResource(R.string.btn_test))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 保存按钮
            OutlinedButton(
                onClick = {
                    viewModel.updateBiliCookie(tempSess)
                    Toast.makeText(
                        context,
                        R.string.toast_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }
}

// ==========================================
// 全局音量卡片
// ==========================================
@Composable
fun GlobalVolumeCard(
    volume: Int,
    onVolumeChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = null,

                // 图片保持原色（不被选中态染色
                tint = if (volume == 0) Color.Gray else Color.Unspecified,

                // 图片太大或太小，可以调整大小
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.label_global_volume),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$volume%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 0
        )

        Text(
            text = stringResource(R.string.global_volume_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ==========================================
// 背景选择卡片
// ==========================================
@Composable
fun PersonalizedBackgroundCard(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    val onlineList by viewModel.onlineBackgrounds.collectAsState()
    val downloadingMap by viewModel.downloadingStates.collectAsState()
    val currentPath by viewModel.currentBackgroundPath.collectAsState()
    val isRandomBg by viewModel.isRandomBg.collectAsState()

    val baseUrl =
        "https://orihararurubutton.blob.core.windows.net/orihararurubuttoncontainer/"
    val backgroundsUrl =
        "${baseUrl}backgrounds/"

    Column {
        // ★★★ 随机背景开关行 ★★★
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { viewModel.toggleRandomBackground(!isRandomBg) }, // 点击整行也能切换
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_random_background),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.random_background_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            androidx.compose.material3.Switch(
                checked = isRandomBg,
                onCheckedChange = { viewModel.toggleRandomBackground(it) }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Text(
            stringResource(
                R.string.personalized_background_description,
                onlineList.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 横向滚动列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // === 默认选项 (固定在第一个) ===
            item {
                // 判断默认是否被选中 (currentPath 为 null 或 "default" 时)
                val isDefaultSelected = currentPath == null || currentPath == "default"

                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isDefaultSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            // 设为 null 就是恢复默认
                            viewModel.downloadBackground(context, null)
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFFFF8E7))
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF5E4032))
                        )
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.default_background),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

//            var proxy = viewModel.currentProxyUrl.value
//            // 健壮性修复：如果用户没填斜杠，自动补上
//            if (proxy.isNotEmpty() && !proxy.endsWith("/")) {
//                proxy += "/"
//            }

            // === 在线列表 (从 GitHub JSON 获取) ===
            items(onlineList) { item ->
                // 监听刷新信号 (只要 refreshTrigger 变了，这里就会重新计算)
                val trigger by viewModel.refreshTrigger.collectAsState()

                // 检查文件是否存在 (isDownloaded)
                val isDownloaded = remember(item.id, trigger) {
                    val file = File(context.filesDir, "backgrounds/${item.id}")
                    file.exists() && file.length() > 0
                }

                // 状态判断
                val isDownloading = downloadingMap[item.id] == true
                // 选中判断：如果当前路径包含这个文件的 ID，就认为是选中了
                val isSelected = currentPath?.contains(item.id) == true

                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = when {
                                isSelected -> 3.dp      // 选中：3.dp 粗边框
                                isDownloaded -> 1.8.dp  // 已下载：1.8.dp 细边框 (提示用户已拥有)
                                else -> 0.dp            // 未下载：无边框
                            },
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary       // 选中：主色调
                                isDownloaded -> MaterialTheme.colorScheme.outline // 已下载：变体色 (看起来像个相框)
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (isDownloading) return@clickable

                            // 只有没在下载的时候，点击才有效
                            if (isDownloaded) {
                                // 如果已下载：直接设为背景 (不需要下载)
                                val file =
                                    File(context.filesDir, "backgrounds/${item.id}")
                                viewModel.setBackgroundPath(file.absolutePath) // 需在 ViewModel 加个简单的方法，或者直接调 saveBackgroundPath
                            } else {
                                // 如果未下载：开始下载并设置
                                viewModel.downloadBackground(context, item)
                            }
                        }
                ) {
                    // 显示封面 (自动用镜像链接)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("${backgroundsUrl}${item.thumbUrl}") // 核心：直接用 RemoteBackground 里的链接
                            .size(200, 300) // 限制尺寸，列表不卡顿
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )

                    // 下载中的 Loading (转圈圈)
                    if (isDownloading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                    } else if (isDownloaded) {
                        // 已下载：显示右上角删除按钮 (X)
                        Box(Modifier.fillMaxSize()) {
                            // 右上角的红叉
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(15.dp)
                                    .clickable {
                                        // 点击 X 删除文件
                                        viewModel.deleteBackground(context, item)
                                    }
                            )
                        }
                    } else {
                        // 未下载：可以在右上角显示一个小下载图标，提示用户
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(15.dp)
                        )
                    }

                    // 视频小图标 (如果是视频)
                    if (item.isVideo) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

//// ==========================================
//// 网络代理卡片
//// ==========================================
//@Composable
//fun NetworkProxyCard(
//    viewModel: MainViewModel
//) {
//    val proxyUrl by viewModel.currentProxyUrl.collectAsState()
//    var tempUrl by remember(proxyUrl) { mutableStateOf(proxyUrl) }
//
//    Column(modifier = Modifier.padding(16.dp)) {
//        Text(
//            text = stringResource(R.string.label_modify_proxy),
//            style = MaterialTheme.typography.titleSmall,
//            fontWeight = FontWeight.Bold
//        )
//        Text(
//            text = stringResource(R.string.modify_proxy_description),
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.outline
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = tempUrl,
//            onValueChange = { tempUrl = it },
//            label = { Text(stringResource(R.string.example_proxy, "https://v6.gh-proxy.org/")) },
//            modifier = Modifier.fillMaxWidth(),
//            singleLine = true
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // 推荐按钮流式布局 (或者简单的 Row)
//        Text(stringResource(R.string.recommend_nodes), style = MaterialTheme.typography.bodySmall)
//        LazyRow(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.padding(top = 4.dp)
//        ) {
//            val nodes = listOf(
//                "https://v6.gh-proxy.org/",
//                "https://gh-proxy.com/",
//                "https://gh-proxy.org/",
//                "https://cdn.ghproxy.net/",
//                "https://edgeone.ghproxy.net/",
//                "https://hk.ghproxy.net/",
//                "https://ghproxy.net/"
//            )
//            items(nodes) { node ->
//                androidx.compose.material3.AssistChip(
//                    onClick = {
//                        tempUrl = node
//                        viewModel.updateGithubProxyUrl(node)
//                    },
//                    label = { Text(stringResource(R.string.label_node, nodes.indexOf(node) + 1)) }
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // 保存按钮
//        OutlinedButton(
//            onClick = { viewModel.updateGithubProxyUrl(tempUrl) },
//            modifier = Modifier.align(Alignment.End)
//        ) {
//            Text(stringResource(R.string.btn_save))
//        }
//    }
//}

// ==========================================
// 电池优化设置卡片 (保活关键)
// ==========================================
@SuppressLint("BatteryLife")
@Composable
fun BatteryOptimizationCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val packageName = context.packageName

    // 获取 PowerManager
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    // 状态：是否已经在白名单中
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(packageName))
    }

    // 监听生命周期：当用户从系统设置页返回 App (ON_RESUME) 时，自动刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations =
                    powerManager.isIgnoringBatteryOptimizations(packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.label_prevent_kill),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.battery_optimization_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isIgnoringBatteryOptimizations) {
            // 状态：已开启 (显示为不可点击的绿色或灰色按钮，表明状态正常)
            OutlinedButton(
                onClick = {
                    // 已经是白名单了，通常不需要操作，但可以允许用户去查看
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // 也可以设为 false 禁用，这里设为 true 允许用户进去检查
                enabled = true
            ) {
                Icon(
                    Icons.Default.Check, //painterResource(R.drawable.ic_check_circle), // 确保你有 ic_check_circle 或者用 Icons.Default.Check
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_whitelist_enabled),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // 状态：未开启 (申请按钮)
            OutlinedButton(
                onClick = {
                    try {
                        // 尝试 1: 直接弹窗请求白名单 (需要权限)
                        val intent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:$packageName".toUri()
                            }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 尝试 2: 如果崩溃（部分厂商阉割），则跳转到列表页让用户自己找
                        try {
                            val intent =
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                R.string.toast_battery_optimization_not_supported,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_battery_horizental),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_request_whitelist))
            }
        }
    }
}

// ==========================================
// 数据备份卡片
// ==========================================
@Composable
fun DataBackupCard(
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.label_local_backup),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.local_backup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮：备份
        OutlinedButton(
            onClick = onBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_backup_data))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮：恢复
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_restore_data))
        }
    }
}

// ==========================================
// 背景选择卡片
// ==========================================
@Composable
fun AboutCard(
    appVersion: String,
    onCheckAppUpdate: () -> Unit,
    onExportLog: () -> Unit,
    onNavigateToLegal: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.label_developer),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.label_version, appVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮：检查更新按钮
        OutlinedButton(
            onClick = onCheckAppUpdate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_check_update))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮：导出崩溃日志按钮
        OutlinedButton(
            onClick = onExportLog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_export_logs))
        }

        Spacer(modifier = Modifier.height(8.dp)) // 两个按钮之间的间距

        // 按钮：阅读完整协议
        OutlinedButton(
            onClick = onNavigateToLegal, // 点击打开弹窗
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_info_title))
        }
    }
}
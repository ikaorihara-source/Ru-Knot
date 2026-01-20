package com.ikaorihara.ruknot.screens

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
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
    val scope = rememberCoroutineScope()

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
            icon = Icons.Default.Language,
            initiallyExpanded = false
        ) {
            LanguageSettingCard()
        }

        // 夜间模式设置
        ExpandableSection(
            title = stringResource(R.string.label_night_mode),
            icon = Icons.Default.BrightnessMedium,
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
            icon = Icons.Default.Sync,
            initiallyExpanded = false
        ) {
            PollingIntervalCard(
                roomInterval = roomInterval,
                userInterval = userInterval,
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
                }
            )
        }

        // 全局音量设置
        ExpandableSection(
            title = stringResource(R.string.label_global_volume),
            icon = Icons.AutoMirrored.Filled.VolumeUp,
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
            icon = Icons.Default.Image,
            initiallyExpanded = false
        ) {
            PersonalizedBackgroundCard(
                viewModel = viewModel
            )
        }

        // ==========================================
        // 网络设置 (修改代理)
        // ==========================================
        ExpandableSection(
            title = stringResource(R.string.label_network_proxy),
            icon = Icons.Default.Public // 需要导入 Icons.Default.Public
        ) {
            NetworkProxyCard(
                viewModel = viewModel
            )
        }

        // 关于与免责声明 (放在最下面)
        ExpandableSection(
            title = stringResource(R.string.label_about_and_disclaimer),
            icon = Icons.Default.Info,
            initiallyExpanded = false
        ) {
            AboutCard(
                appVersion = appVersion,
                onNavigateToLegal = onNavigateToLegal
            )
        }

        // 底部留白，防止被导航栏遮挡
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==========================================
// ★★★ 通用组件：可展开的卡片 ★★★
// ==========================================
@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
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
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
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
// 频率设置内容
// ==========================================
@Composable
fun PollingIntervalCard(
    roomInterval: Long,
    userInterval: Long,
    onRoomIntervalChange: (Long) -> Unit,
    onUserIntervalChange: (Long) -> Unit
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
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
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

            var proxy = viewModel.currentProxyUrl.value
            // ★★★ 健壮性修复：如果用户没填斜杠，自动补上 ★★★
            if (proxy.isNotEmpty() && !proxy.endsWith("/")) {
                proxy += "/"
            }

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
                            .data("${proxy}${item.thumbUrl}") // 核心：直接用 RemoteBackground 里的链接
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

// ==========================================
// 网络代理卡片
// ==========================================
@Composable
fun NetworkProxyCard(
    viewModel: MainViewModel
) {
    val proxyUrl by viewModel.currentProxyUrl.collectAsState()
    var tempUrl by remember(proxyUrl) { mutableStateOf(proxyUrl) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.label_modify_proxy),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.modify_proxy_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tempUrl,
            onValueChange = { tempUrl = it },
            label = { Text(stringResource(R.string.example_proxy, "https://v6.gh-proxy.org/")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 推荐按钮流式布局 (或者简单的 Row)
        Text(stringResource(R.string.recommend_nodes), style = MaterialTheme.typography.bodySmall)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            val nodes = listOf(
                "https://v6.gh-proxy.org/",
                "https://gh-proxy.com/",
                "https://gh-proxy.org/",
                "https://cdn.ghproxy.net/",
                "https://edgeone.ghproxy.net/",
                "https://hk.ghproxy.net/",
                "https://ghproxy.net/"
            )
            items(nodes) { node ->
                androidx.compose.material3.AssistChip(
                    onClick = {
                        tempUrl = node
                        viewModel.updateGithubProxyUrl(node)
                    },
                    label = { Text(stringResource(R.string.label_node, nodes.indexOf(node) + 1)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 保存按钮
        OutlinedButton(
            onClick = { viewModel.updateGithubProxyUrl(tempUrl) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.btn_save))
        }
    }
}

// ==========================================
// 背景选择卡片
// ==========================================
@Composable
fun AboutCard(
    appVersion: String,
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
            Text(stringResource(R.string.legal_info_title))
        }
    }
}
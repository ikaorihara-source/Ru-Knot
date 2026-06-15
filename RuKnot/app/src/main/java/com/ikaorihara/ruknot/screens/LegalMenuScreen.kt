package com.ikaorihara.ruknot.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R
import com.ikaorihara.ruknot.data.getEulaText
import com.ikaorihara.ruknot.data.getOpenSourceText
import com.ikaorihara.ruknot.data.getPrivacyText
import com.ikaorihara.ruknot.data.getSpecialThanksText

// 定义菜单项数据结构
data class LegalMenuItem(
    val title: String,
    val content: String, // 点击后要显示的具体文本
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalMenuScreen(
    onBack: () -> Unit
) {
    // 准备列表数据
    val menuItems = listOf(
        LegalMenuItem(
            stringResource(R.string.label_eula),
            getEulaText(),
            Icons.Default.Description // 用户协议图标
        ),
        LegalMenuItem(
            stringResource(R.string.label_privacy_policy),
            getPrivacyText(),
            Icons.Default.PrivacyTip // 隐私政策图标
        ),
        LegalMenuItem(
            stringResource(R.string.label_open_source_licences),
            getOpenSourceText(),
            Icons.Default.Code // 代码图标
        ),
        LegalMenuItem(
            stringResource(R.string.label_special_thanks), // 直接这里判断，省去改 XML
            content = getSpecialThanksText(),
            Icons.Default.Favorite // 爱心图标
        ),
        LegalMenuItem(
            stringResource(R.string.label_about_developer),
            stringResource(R.string.developer_for_love),
            Icons.Default.Person // 开发者图标
        )
    )

    // 控制弹窗显示
    var selectedItem by remember { mutableStateOf<LegalMenuItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,

        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.legal_info_title)) }, // 模仿截图标题
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp), // 两侧留白，和设置页一致
            verticalArrangement = Arrangement.spacedBy(16.dp), // 卡片之间的间距
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            items(menuItems) { item ->
                // 列表项样式
                Card(
                    colors = CardDefaults.cardColors(
                        // 使用和设置页一样的 surfaceVariant 颜色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { selectedItem = item }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp) // 内部间距
                    ) {
                        // 左侧图标
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // 标题文字
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 通用弹窗：显示选中的内容
    if (selectedItem != null) {
        AlertDialog(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            onDismissRequest = { selectedItem = null },
            title = { Text(selectedItem!!.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = selectedItem!!.content)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}
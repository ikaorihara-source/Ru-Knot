package com.ikaorihara.ruknot.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikaorihara.ruknot.R

// 定义链接数据结构
data class LinkItem(
    val title: String,
    val url: String,
//    val icon: ImageVector,
    val iconRes: Int,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLinksScreen(
    onNavigateToWeb: (String) -> Unit, // 回调：把 URL 传出去
    onBack: () -> Unit
) {
    // ★★★ 露露专属链接列表 ★★★
    val links = listOf(
        LinkItem(
            stringResource(R.string.about_ruru_title),
            "https://m.bilibili.com/opus/653069078941925401?spm_id_from=333.1387.0.0",
            R.drawable.ic_bilibili_aboutme,
            stringResource(R.string.about_ruru_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_space_title),
            "https://space.bilibili.com/631070414/dynamic?spm_id_from=333.1365.list.card_avatar.click",
            R.drawable.ic_bilibili_space,
            stringResource(R.string.ruru_space_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_workbench_title),
            "https://www.kdocs.cn/office/d/306131805254?share_id=G1KR8abFtEr64K",
            R.drawable.ic_workbench,
            stringResource(R.string.ruru_workbench_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_live_title),
            "https://live.bilibili.com/22389206",
            R.drawable.ic_bilibili_stream,
            stringResource(R.string.ruru_live_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_taobao_title),
            "https://m.tb.cn/h.7Nj1PIqgP2jKQib",
            R.drawable.ic_taobao,
            stringResource(R.string.ruru_taobao_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_qq_title),
            "https://qm.qq.com/q/kwq5dldWec",
            R.drawable.ic_qq,
            stringResource(R.string.ruru_qq_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_qq_yinyue_title),
            "https://y.qq.com/n/ryqq_v2/singer/000SldX50VMPRG",
            R.drawable.ic_qq_yinyue,
            stringResource(R.string.ruru_qq_yinyue_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_netease_cloud_title),
            "https://music.163.com/#/artist?id=30281779",
            R.drawable.ic_netease_cloud,
            stringResource(R.string.ruru_netease_cloud_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_weibo_title),
            "https://weibo.com/yamatsumiitsuki",
            R.drawable.ic_weibo,
            stringResource(R.string.ruru_weibo_description)
        ),
        LinkItem(
            stringResource(R.string.ruru_tiktok_title),
            "https://v.douyin.com/u32TZvgqJF4",
            R.drawable.ic_tiktok,
            stringResource(R.string.ruru_tiktok_description)
        ),
    )

    Scaffold(
        // ★★★ 核心修改：把脚手架设为透明，否则会挡住背景图 ★★★
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(links) { item ->
                LinkCard(item = item, onClick = { onNavigateToWeb(item.url) })
            }
        }
    }
}

@Composable
fun LinkCard(item: LinkItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
//                imageVector = item.icon,
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 文本
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 箭头
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
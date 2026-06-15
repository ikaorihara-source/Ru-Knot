package com.ikaorihara.ruknot.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.ikaorihara.ruknot.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 状态管理
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("加载中...") }
    var currentUrl by remember { mutableStateOf(url) } // 记录当前正在浏览的 URL

    // WebView 实例
    var webView: WebView? by remember { mutableStateOf(null) }

    // 拦截返回键
    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            // 如果能后退就后退，不能后退就关闭页面
                            if (webView?.canGoBack() == true) webView?.goBack() else onBack()
                        }) {
                            // 如果能后退显示返回箭头，否则显示关闭叉叉 (可选优化，这里统一用箭头)
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    },
                    actions = {
                        // 刷新按钮
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_refresh)
                            )
                        }

                        // 强制关闭按钮 (X)
                        // 无论网页浏览到哪里，点这个直接退出浏览器
                        IconButton(onClick = { onBack() }) {
                            // 需要引入 Icons.Default.Close
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_close)
                            )
                        }

                        // 外部浏览器打开按钮
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, currentUrl.toUri())
                                context.startActivity(intent)
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.btn_open_browser)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // 进度条放在 TopBar 下面，看起来更像浏览器
                if (progress < 1.0f && isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    ) { paddingValues ->
        // 浏览器主体
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false

                                // 正常网页：http 或 https，交给 WebView 自己加载
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return false
                                }

                                // 特殊协议：bilibili://, mqq://, alipay://, tel:// 等
                                try {
                                    // 创建一个 Intent，试图打开能处理这个协议的 APP
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    view?.context?.startActivity(intent)
                                    return true // ★★★ 返回 true，表示我们拦截处理了，防止 WebView 报错
                                } catch (e: Exception) {
                                    // 异常处理：用户手机可能没安装 B站/QQ，或者协议不对
                                    e.printStackTrace()
                                    // 这里你可以选择弹个 Toast 提示用户“未安装该应用”，或者什么都不做
                                    // return true 依然是为了防止显示那个红色的错误页面
                                    return true
                                }
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                isLoading = true
                                // 更新当前 URL，方便外部浏览器打开
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }

                            // ★★★ 获取网页标题 ★★★
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrEmpty()) {
                                    pageTitle = title
                                }
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                update = { view ->
                    webView = view
                    // 只有 URL 确实改变且和当前不一致时才加载 (防止重组导致刷新)
                    if (view.url != url && view.originalUrl != url) {
                        view.loadUrl(url)
                    }
                }
            )
        }
    }
}
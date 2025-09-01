package com.example.myapplicationw.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationw.R
import com.example.myapplicationw.interfaces.JsInterface

@ExperimentalGetImage class MainActivity : AppCompatActivity() {
    private var jsInterfaces=JsInterface(this)
    private lateinit var webView:WebView

    private val specialPages = listOf(
        "bills.html",
        "profile.html",
        "index.html"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        webView=findViewById(R.id.loginWebView)
        // 配置 WebView
        webView.settings.apply {
            javaScriptEnabled = true               // 启用 JavaScript（如果需要）
            allowFileAccess = true                 // 允许访问文件
            allowFileAccessFromFileURLs = true     // 允许通过 file:// URL 访问其他文件
            allowUniversalAccessFromFileURLs = true // 允许通过 file:// URL 访问任何来源
            domStorageEnabled = true               // 启用 DOM 存储（如 localStorage）
        }

        webView.addJavascriptInterface(jsInterfaces, "AndroidInterface")

        webView.loadUrl("file:///android_asset/login.html");
        // 设置 WebViewClient 处理页面导航
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // 在当前 WebView 中加载 URL，而非打开系统浏览器
                view.loadUrl(url)
                return true
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                    Log.e("WebView", "资源加载错误: ${error?.description}")

            }
            // 拦截请求并添加跨域响应头（关键部分）
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // 1. 先获取原始响应
                val originalResponse = super.shouldInterceptRequest(view, request)

                // 2. 如果是目标接口域名（替换为你的后端域名），添加跨域头
                if (request.url.host == "graywolf.top") {
                    // 复制原始响应头并添加CORS相关头
                    val modifiedHeaders = mutableMapOf<String, String>().apply {
                        // 保留原始响应头
                        originalResponse?.responseHeaders?.let { putAll(it) }

                        // 添加跨域许可头
                        put("Access-Control-Allow-Origin", "*") // 开发环境可用*，生产环境建议指定具体前端域名
                        put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD")
                        put("Access-Control-Allow-Headers", "accept, Content-Type, Authorization")
                        put("Access-Control-Max-Age", "86400") // 预检请求缓存时间（24小时）
                    }

                    // 3. 返回添加了跨域头的响应
                    return originalResponse?.let {
                        WebResourceResponse(
                            it.mimeType,
                            it.encoding,
                            it.statusCode,
                            it.reasonPhrase,
                            modifiedHeaders,
                            it.data
                        )
                    }
                }

                // 非目标域名的请求，返回原始响应
                return originalResponse
            }
        }


    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            JsInterface.REQUEST_CODE_IMAGE_PICK -> {
                jsInterfaces.handleImageResult(resultCode, data)
            }
            JsInterface.REQUEST_CODE_QR_SCAN -> {
                jsInterfaces.handleScanResult(resultCode, data)
            }
        }
    }
    // 判断是否为特殊页面
    private fun isSpecialPage(): Boolean {
        val currentUrl = webView.url ?: return false
        return specialPages.any { currentUrl.contains(it) }
    }

    // 判断是否为特殊页面的主状态（非子状态）
    private fun isSpecialPageMainState(): Boolean {
        val currentUrl = webView.url ?: return false
        // 检查是否包含特殊页面且不包含任何子状态哈希
        return specialPages.any { page ->
            currentUrl.contains(page) &&
                    !currentUrl.contains("#") // 没有哈希参数视为主状态
        }
    }

    // 获取特殊页面的子状态哈希（如#annual）
    private fun getSpecialPageSubState(): String? {
        val currentUrl = webView.url ?: return null
        val hashIndex = currentUrl.indexOf("#")
        return if (hashIndex != -1 && hashIndex < currentUrl.length - 1) {
            currentUrl.substring(hashIndex)
        } else {
            null
        }
    }

    // 重写返回键逻辑
    override fun onBackPressed() {
        when {
            // 情况1：特殊页面的主状态，直接退出
            isSpecialPageMainState() -> {
                finish()
            }
            // 情况2：特殊页面的子状态（有哈希），先回退到主状态
            isSpecialPage() && webView.canGoBack() -> {
                webView.goBack()
            }
            // 情况3：非特殊页面但可以回退，正常回退
            webView.canGoBack() -> {
                webView.goBack()
            }
            // 情况4：其他无法回退的情况，执行默认退出
            else -> {
                super.onBackPressed()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        jsInterfaces.onRequestPermissionsResult(requestCode)
    }
    override fun onDestroy() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.loadUrl("about:blank")
        webView.pauseTimers()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
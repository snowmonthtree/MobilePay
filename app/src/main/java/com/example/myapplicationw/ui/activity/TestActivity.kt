package com.example.myapplicationw.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationw.interfaces.JsInterface
import com.example.myapplicationw.interfaces.OnNavItemClickListener
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.navigation.MyBottomNavView

/**
 * 测试页面Activity
 * 负责WebView加载、底部导航交互、相机权限请求、JS桥接等核心逻辑
 */
@ExperimentalGetImage
class TestActivity : AppCompatActivity(), OnNavItemClickListener {

    // 常量定义（遵循常量命名规范：全大写+下划线分隔）
    companion object {
        private const val TAG = "TestActivity" // 日志标签，与类名一致
        private const val CORS_TARGET_DOMAIN = "graywolf.top" // CORS目标域名
        private const val NAV_INDEX_LOGIN = -1 // 登录页特殊索引（非导航项）

        // 特殊页面列表（控制底部导航栏显示/隐藏）
        private val SPECIAL_PAGES = listOf("index.html", "bills.html", "profile.html")

        // 导航项与URL映射（索引需与MyBottomNavView的常量严格对应）
        private val NAV_URL_MAP = listOf(
            "file:///android_asset/index.html",  // 0: 首页
            "",                                   // 1: 扫一扫（无URL，跳转原生页面）
            "",                                   // 2: 智能助手（无URL，跳转原生页面）
            "file:///android_asset/bills.html",   // 3: 账单页
            "file:///android_asset/profile.html"  // 4: 我的页面
        )
    }

    // 成员变量（遵循驼峰命名，添加lateinit标识延迟初始化）
    private lateinit var webView: WebView // WebView实例（移除m前缀，更简洁）
    private lateinit var jsInterface: JsInterface // JS交互接口
    private lateinit var bottomNavView: MyBottomNavView // 底部导航组件
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String> // 相机权限请求器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        // 初始化顺序：先初始化基础组件，再初始化业务逻辑
        initBottomNav()
        initWebView()
        initJsInterface()
        initCameraPermissionLauncher()
        adaptSystemInsets() // 适配系统Insets（替代原adaptSystemNavBar）

        // 初始加载登录页
        Log.d(TAG, "onCreate: 开始加载登录页")
        loadWebUrlByIndex(NAV_INDEX_LOGIN)
    }

    /**
     * 初始化底部导航组件
     */
    private fun initBottomNav() {
        bottomNavView = findViewById(R.id.bottom_nav)
        bottomNavView.setOnNavItemClickListener(this) // 设置导航点击监听器
        // 初始选中首页（后续可根据需求调整）
        bottomNavView.setSelectedIndex(MyBottomNavView.INDEX_HOME)
    }

    /**
     * 初始化WebView（配置WebSettings、WebViewClient）
     */
    private fun initWebView() {
        webView = findViewById(R.id.main_webview)
        val webSettings = webView.settings

        // WebSettings配置（按功能分组，添加注释说明）
        webSettings.apply {
            javaScriptEnabled = true // 启用JS（必须，用于与H5交互）
            allowFileAccess = true // 允许访问本地文件（加载asset资源）
            domStorageEnabled = true // 启用DOM存储（H5本地存储）
            displayZoomControls = false // 隐藏缩放按钮（优化UI）
            loadsImagesAutomatically = true // 自动加载图片

            allowFileAccessFromFileURLs = true     // 允许通过 file:// URL 访问其他文件
            allowUniversalAccessFromFileURLs = true // 允许通过 file:// URL 访问任何来源
        }

        // 设置WebViewClient（处理页面加载、错误、URL拦截等）
        webView.webViewClient = object : WebViewClient() {
            /**
             * 页面开始加载时回调
             */
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "WebView开始加载: $url") // 日志打印加载URL
            }

            /**
             * 页面加载完成时回调
             * 控制底部导航栏显示/隐藏
             */
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    val isSpecialPage = SPECIAL_PAGES.any { page -> it.contains(page) }
                    bottomNavView.visibility = if (isSpecialPage) View.VISIBLE else View.GONE
                    Log.d(TAG, "WebView加载完成: $url, 导航栏状态: ${if (isSpecialPage) "显示" else "隐藏"}")
                }
            }

            /**
             * 拦截URL加载（统一由WebView处理，避免跳转外部浏览器）
             */
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                Log.d(TAG, "拦截URL加载: $url")
                view.loadUrl(url)
                return true // 返回true表示已处理，不交给系统
            }

            /**
             * 接收Web资源加载错误
             */
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val errorMsg = error?.description ?: "未知错误"
                val failedUrl = request?.url.toString()
                Log.e(TAG, "WebView资源加载错误: $errorMsg, URL: $failedUrl")
                Toast.makeText(this@TestActivity, "页面加载失败: $errorMsg", Toast.LENGTH_SHORT).show()
            }

            /**
             * 拦截Web资源请求（处理CORS跨域问题）
             */
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // 1. 先获取原始响应
                val originalResponse = super.shouldInterceptRequest(view, request)
                val requestUrl = request.url.toString()
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

    /**
     * 初始化JS交互接口（添加@JavascriptInterface注解的桥梁）
     */
    private fun initJsInterface() {
        jsInterface = JsInterface(this)
        // 添加JS接口，命名空间为"AndroidInterface"（H5端需对应）
        webView.addJavascriptInterface(jsInterface, "AndroidInterface")
        Log.d(TAG, "initJsInterface: JS交互接口初始化完成")
    }

    /**
     * 初始化相机权限请求器（使用Activity Result API，替代旧requestPermissions）
     */
    private fun initCameraPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // 权限授予：跳转到扫码页面
                Log.d(TAG, "相机权限已授予，跳转到扫码页面")
                startActivity(Intent(this, QRScannerActivity::class.java))
            } else {
                // 权限拒绝：提示用户开启
                Log.w(TAG, "相机权限被拒绝")
                Toast.makeText(this, "请在设置中开启相机权限，否则无法使用扫码功能", Toast.LENGTH_LONG).show()
            }
        }
        Log.d(TAG, "initCameraPermissionLauncher: 相机权限请求器初始化完成")
    }

    /**
     * 适配系统Insets（状态栏、导航栏），避免布局被遮挡
     * 替代原adaptSystemNavBar，支持Edge-to-Edge模式
     */
    private fun adaptSystemInsets() {
        // 底部导航栏：添加底部内边距（适配系统导航栏）
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                0 // 保留原有内边距
            )
            insets
        }

        // 主容器：添加顶部内边距（适配状态栏），底部不添加（由导航栏占位）
        val mainContainer = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                0
            )
            insets
        }
        Log.d(TAG, "adaptSystemInsets: 系统Insets适配完成")
    }

    /**
     * 根据索引加载Web页面
     * @param index 导航项索引（NAV_INDEX_LOGIN表示加载登录页）
     */
    private fun loadWebUrlByIndex(index: Int) {
        when (index) {
            NAV_INDEX_LOGIN -> {
                // 加载登录页
                val loginUrl = "file:///android_asset/login.html"
                webView.loadUrl(loginUrl)
                Log.d(TAG, "loadWebUrlByIndex: 加载登录页，URL: $loginUrl")
            }
            in NAV_URL_MAP.indices -> {
                // 加载导航项对应的URL（过滤空URL）
                val targetUrl = NAV_URL_MAP[index]
                if (targetUrl.isNotEmpty() && webView.url != targetUrl) {
                    webView.loadUrl(targetUrl)
                    Log.d(TAG, "loadWebUrlByIndex: 加载导航页，索引: $index, URL: $targetUrl")
                }
            }
            else -> {
                // 无效索引：默认加载首页
                Log.w(TAG, "loadWebUrlByIndex: 无效索引: $index，默认加载首页")
                loadWebUrlByIndex(MyBottomNavView.INDEX_HOME)
            }
        }
    }

    /**
     * 检查相机权限（内部调用，用于扫一扫功能）
     */
    private fun checkCameraPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            // 已有权限：直接跳转扫码页
            startActivity(Intent(this, QRScannerActivity::class.java))
        } else {
            // 无权限：请求权限
            Log.d(TAG, "checkCameraPermission: 无相机权限，发起权限请求")
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ------------------------------ 接口实现 ------------------------------
    /**
     * 底部导航项点击事件（实现OnNavItemClickListener）
     */
    override fun onNavItemClick(index: Int) {
        Log.d(TAG, "onNavItemClick: 导航项被点击，索引: $index")
        when (index) {
            MyBottomNavView.INDEX_HOME -> loadWebUrlByIndex(index) // 首页：加载H5
            MyBottomNavView.INDEX_SCAN -> checkCameraPermission() // 扫一扫：检查权限并跳转原生
            MyBottomNavView.INDEX_ASSISTANT -> {
                // 智能助手：跳转原生Activity
                startActivity(Intent(this, AssistantActivity::class.java))
                Log.d(TAG, "onNavItemClick: 跳转到智能助手页面")
            }
            MyBottomNavView.INDEX_BILLS -> loadWebUrlByIndex(index) // 账单：加载H5
            MyBottomNavView.INDEX_PROFILE -> loadWebUrlByIndex(index) // 我的：加载H5
        }
    }

    // ------------------------------ 生命周期方法 ------------------------------
    /**
     * 返回键逻辑处理（优先处理WebView回退，再处理导航回退）
     */
    override fun onBackPressed() {
        val currentUrl = webView.url ?: run {
            // 无当前URL：直接退出
            super.onBackPressed()
            return
        }

        // 特殊页面：首页直接退出，其他特殊页面回退到首页
        if (SPECIAL_PAGES.any { currentUrl.contains(it) } || currentUrl.contains("login.html")) {
            if (currentUrl.contains("index.html") ||currentUrl.contains("login.html")){
                super.onBackPressed()
                Log.d(TAG, "onBackPressed: 当前不为特殊页，退出Activity")
            } else {
                loadWebUrlByIndex(MyBottomNavView.INDEX_HOME)
                bottomNavView.setSelectedIndex(MyBottomNavView.INDEX_HOME)
                Log.d(TAG, "onBackPressed: 当前为特殊页面，回退到首页")
            }
        } else {
            // 非特殊页面：优先WebView回退，无历史则退出
            if (webView.canGoBack()) {
                webView.goBack()
                Log.d(TAG, "onBackPressed: WebView回退到上一页")
            } else {
                super.onBackPressed()
                Log.d(TAG, "onBackPressed: WebView无历史，退出Activity")
            }
        }
    }

    /**
     * 接收Activity返回结果（用于JS接口的图片选择、扫码结果）
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: 请求码: $requestCode, 结果码: $resultCode")

        // 分发结果到JS接口处理
        when (requestCode) {
            JsInterface.REQUEST_CODE_IMAGE_PICK -> {
                jsInterface.handleImageResult(resultCode, data)
                Log.d(TAG, "onActivityResult: 处理图片选择结果")
            }
            JsInterface.REQUEST_CODE_QR_SCAN -> {
                jsInterface.handleScanResult(resultCode, data)
                Log.d(TAG, "onActivityResult: 处理扫码结果")
            }
        }
    }

    /**
     * 销毁Activity（释放WebView资源，避免内存泄漏）
     */
    override fun onDestroy() {
        // 先移除JS接口，再销毁WebView
        webView.removeJavascriptInterface("AndroidInterface")
        webView.destroy()
        Log.d(TAG, "onDestroy: Activity销毁，释放WebView资源")
        super.onDestroy()
    }
}
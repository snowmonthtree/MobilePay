package com.example.myapplicationw.ui.activity

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationw.Interfaces.JsInterface
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.activity.QRScannerActivity

@ExperimentalGetImage
class TestActivity : AppCompatActivity() {
    // 1. WebView 核心相关
    private lateinit var webView: WebView
    private lateinit var jsInterfaces: JsInterface
    private val specialPages = listOf(
        "index.html",    // 首页（新增为特殊页面，用于返回逻辑）
        "bills.html",
        "profile.html"
    ) // 特殊页面列表：首页+账单+我的

    // 2. 底部导航相关
    private lateinit var bottomNav: LinearLayout // 底部导航根布局（用于显示/隐藏）
    private lateinit var navHome: LinearLayout
    private lateinit var navScan: LinearLayout
    private lateinit var navAssistant: LinearLayout
    private lateinit var navBills: LinearLayout
    private lateinit var navProfile: LinearLayout
    private lateinit var ivHome: ImageView
    private lateinit var tvHome: TextView
    private lateinit var ivScan: ImageView
    private lateinit var tvScan: TextView
    private lateinit var ivBills: ImageView
    private lateinit var tvBills: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var tvProfile: TextView
    private var currentNavIndex = 0 // 当前选中导航索引（默认首页）

    // 3. 导航对应的 URL 列表（本地 asset 文件）
    private val navUrlMap = listOf(
        "file:///android_asset/index.html"    // 0: 首页（特殊页面）
        , ""                                  // 1: 扫一扫（原生页面）
        , ""                                  // 2: 智能助手（原生页面）
        , "file:///android_asset/bills.html"  // 3: 账单（特殊页面）
        , "file:///android_asset/profile.html"// 4: 我的（特殊页面）
    )

    // 4. 相机权限请求器
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        // 处理系统Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // 初始化 JS 交互接口
        jsInterfaces = JsInterface(this)

        // 初始化相机权限请求器
        initCameraPermissionLauncher()

        // 初始化底部导航（绑定根布局，用于显示/隐藏）
        bottomNav = findViewById(R.id.bottom_nav) // 对应 include 标签的 ID
        initBottomNavViews()

        // 初始化 WebView（核心：添加页面加载监听，控制导航显示/隐藏）
        initWebView()

        // 初始化导航点击事件
        initNavClickListeners()

        // 初始加载首页 + 显示导航栏
        loadWebUrlByIndex(-1)
        updateNavSelectedState(0)
        bottomNav.visibility = View.VISIBLE
    }

    /**
     * 初始化 WebView（新增页面加载完成监听，控制导航显示/隐藏）
     */
    private fun initWebView() {
        webView = findViewById(R.id.main_webview)

        // 原有 WebView 配置保留
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.addJavascriptInterface(jsInterfaces, "AndroidInterface")

        // WebViewClient 新增页面加载完成监听（控制导航显示/隐藏）
        webView.webViewClient = object : WebViewClient() {
            // 页面加载完成后，判断是否为特殊页面，控制导航显示/隐藏
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    if (isSpecialPage(it)) {
                        bottomNav.visibility = View.VISIBLE // 特殊页面：显示导航
                    } else {
                        bottomNav.visibility = View.GONE // 非特殊页面：隐藏导航
                    }
                }
            }

            // 原有 URL 拦截逻辑保留
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            // 原有错误处理保留
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "资源加载错误: ${error?.description}")
            }

            // 原有跨域处理保留
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val originalResponse = super.shouldInterceptRequest(view, request)
                if (request.url.host == "graywolf.top") {
                    val modifiedHeaders = mutableMapOf<String, String>().apply {
                        originalResponse?.responseHeaders?.let { putAll(it) }
                        put("Access-Control-Allow-Origin", "*")
                        put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD")
                        put("Access-Control-Allow-Headers", "accept, Content-Type, Authorization")
                        put("Access-Control-Max-Age", "86400")
                    }
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
                return originalResponse
            }
        }
    }

    /**
     * 初始化底部导航控件（绑定 XML）
     */
    private fun initBottomNavViews() {
        navHome = findViewById(R.id.nav_home)
        navScan = findViewById(R.id.nav_scan)
        navAssistant = findViewById(R.id.nav_assistant)
        navBills = findViewById(R.id.nav_bills)
        navProfile = findViewById(R.id.nav_profile)

        ivHome = findViewById(R.id.iv_home)
        tvHome = findViewById(R.id.tv_home)
        ivScan = findViewById(R.id.iv_scan)
        tvScan = findViewById(R.id.tv_scan)
        ivBills = findViewById(R.id.iv_bills)
        tvBills = findViewById(R.id.tv_bills)
        ivProfile = findViewById(R.id.iv_profile)
        tvProfile = findViewById(R.id.tv_profile)
    }

    /**
     * 初始化导航点击事件
     */
    private fun initNavClickListeners() {
        // 首页：加载首页
        navHome.setOnClickListener {
            if (currentNavIndex != 0) {
                updateNavSelectedState(0)
                loadWebUrlByIndex(0)
            }
        }

        // 扫一扫：原生页面
        navScan.setOnClickListener {
            if (currentNavIndex != 1) {
                updateNavSelectedState(1)
                checkCameraPermission {
                    startActivity(Intent(this, QRScannerActivity::class.java))
                }
            }
        }

        // 智能助手：原生页面
        navAssistant.setOnClickListener {
            updateNavSelectedState(2)
            startActivity(Intent(this, AssistantActivity::class.java))
        }

        // 账单：加载账单页面
        navBills.setOnClickListener {
            if (currentNavIndex != 3) {
                updateNavSelectedState(3)
                loadWebUrlByIndex(3)
            }
        }

        // 我的：加载我的页面
        navProfile.setOnClickListener {
            if (currentNavIndex != 4) {
                updateNavSelectedState(4)
                loadWebUrlByIndex(4)
            }
        }
    }

    /**
     * 根据索引加载 URL
     */
    private fun loadWebUrlByIndex(index: Int) {
        if(index==-1){
            webView.loadUrl("file:///android_asset/login.html")
        }
        else {
            val targetUrl = navUrlMap[index]
            if (targetUrl.isNotEmpty() && webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
        }
    }

    /**
     * 更新导航选中状态
     */
    private fun updateNavSelectedState(selectedIndex: Int) {
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        // 重置所有未选中状态
        ivHome.alpha = 0.6f
        tvHome.setTextColor(inactiveColor)
        ivScan.alpha = 0.6f
        tvScan.setTextColor(inactiveColor)
        ivBills.alpha = 0.6f
        tvBills.setTextColor(inactiveColor)
        ivProfile.alpha = 0.6f
        tvProfile.setTextColor(inactiveColor)

        // 设置选中状态
        when (selectedIndex) {
            0 -> {
                ivHome.alpha = 1.0f
                tvHome.setTextColor(ContextCompat.getColor(this, R.color.nav_active))
            }
            1 -> {
                ivScan.alpha = 1.0f
                tvScan.setTextColor(ContextCompat.getColor(this, R.color.nav_active))
            }
            2 -> { /* 助手项可选高亮 */ }
            3 -> {
                ivBills.alpha = 1.0f
                tvBills.setTextColor(ContextCompat.getColor(this, R.color.nav_active))
            }
            4 -> {
                ivProfile.alpha = 1.0f
                tvProfile.setTextColor(ContextCompat.getColor(this, R.color.nav_active))
            }
        }
        currentNavIndex = selectedIndex
    }

    // ---------------------- 核心：特殊页面判断与返回逻辑 ----------------------
    /**
     * 判断当前 URL 是否为特殊页面
     */
    private fun isSpecialPage(url: String): Boolean {
        return specialPages.any { url.contains(it) }
    }

    /**
     * 判断当前页面是否为首页（index.html）
     */
    private fun isHomePage(): Boolean {
        val currentUrl = webView.url ?: return false
        return currentUrl.contains("index.html")
    }

    /**
     * 重写返回键逻辑：
     * - 特殊页面：非首页 → 回首页；首页 → 关Activity
     * - 非特殊页面：正常回退（WebView 历史）
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val currentUrl = webView.url ?: return

        if (isSpecialPage(currentUrl)) {
            // 1. 特殊页面逻辑
            if (isHomePage()) {
                // 1.1 是首页 → 关闭 Activity
                finish()
            } else {
                // 1.2 非首页 → 跳转到首页
                loadWebUrlByIndex(0)
                updateNavSelectedState(0) // 同步导航选中状态为首页
            }
        } else {
            // 2. 非特殊页面逻辑：正常回退 WebView，无历史则关Activity
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    // ---------------------- 原有辅助逻辑保留 ----------------------
    private fun initCameraPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "相机权限已开启，请再次点击扫一扫", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请在设置中开启相机权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted.invoke()
        } else {
            cameraPermissionLauncher.launch(CAMERA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            JsInterface.REQUEST_CODE_IMAGE_PICK -> jsInterfaces.handleImageResult(resultCode, data)
            JsInterface.REQUEST_CODE_QR_SCAN -> jsInterfaces.handleScanResult(resultCode, data)
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
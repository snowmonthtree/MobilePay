package com.example.myapplicationw.ui.activity
import android.Manifest.permission.CAMERA

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.example.myapplicationw.Interfaces.OnNavItemClickListener
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.activity.AssistantActivity
import com.example.myapplicationw.ui.activity.QRScannerActivity
import com.example.myapplicationw.ui.navigation.MyBottomNavView

@ExperimentalGetImage
class TestActivity : AppCompatActivity(), OnNavItemClickListener {

    // 常量定义
    companion object {
        private const val TAG = "TestActivity"
        private const val CORS_TARGET_DOMAIN = "graywolf.top"
        private const val NAV_INDEX_LOGIN = -1
    }

    // 成员变量
    private lateinit var mWebView: WebView
    private lateinit var mJsInterface: JsInterface
    private lateinit var mBottomNavView: MyBottomNavView // 导航组件实例
    private lateinit var mCameraPermissionLauncher: ActivityResultLauncher<String>

    // 导航对应的URL映射
    private val mNavUrlMap = listOf(
        "file:///android_asset/index.html",    // 首页
        "",                                    // 扫一扫（无URL）
        "",                                    // 智能助手（无URL）
        "file:///android_asset/bills.html",    // 账单
        "file:///android_asset/profile.html"   // 我的
    )

    // 特殊页面列表（控制导航栏显示/隐藏）
    private val mSpecialPages = listOf("index.html", "bills.html", "profile.html")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test) // 假设布局中已包含bottom_nav

        // 初始化导航组件（从现有布局中获取，无需修改XML）
        mBottomNavView = findViewById(R.id.bottom_nav)
        mBottomNavView.setOnNavItemClickListener(this) // 设置监听器

        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出1")
        // 初始化其他组件
        initWebView()

        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出2")
        initJsInterface()

        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出3")
        initCameraPermissionLauncher()

        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出4")
        adaptSystemNavBar()

        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出5")

        // 初始加载首页

        Log.d(TAG, "onCreate: 准备加载登录页")
        loadWebUrlByIndex(NAV_INDEX_LOGIN)
        Log.e("aaaaaaaaaaaaaa", "onCreate: 登录页加载指令已发出")
        mWebView.loadUrl("file:///android_asset/login.html")
    }

    // 适配系统导航栏（避免与底部导航重合）
    private fun adaptSystemNavBar() {
        // 为导航栏添加底部内边距（适配手机自带按钮）
        ViewCompat.setOnApplyWindowInsetsListener(mBottomNavView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom + view.paddingBottom // 保留原有paddingBottom
            )
            insets
        }

        // 页面根容器适配
        val mainContainer = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            insets
        }
    }

    // 初始化WebView（保持原有逻辑）
    private fun initWebView() {
        mWebView = findViewById(R.id.main_webview)
        mWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            allowFileAccess = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        mWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "开始加载 URL: $url") // 跟踪所有加载的页面
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 控制导航栏显示/隐藏
                url?.let {
                    val isSpecial = mSpecialPages.any { page -> it.contains(page) }
                    mBottomNavView.visibility = if (isSpecial) View.VISIBLE else View.GONE
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                mWebView.loadUrl(url)
                return true
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView错误: ${error?.description}")
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val originalResponse = super.shouldInterceptRequest(view, request)
                if (request.url.host == CORS_TARGET_DOMAIN) {
                    val modifiedHeaders = mutableMapOf<String, String>().apply {
                        originalResponse?.responseHeaders?.let { putAll(it) }
                        put("Access-Control-Allow-Origin", "*")
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

    // 初始化JS交互接口
    private fun initJsInterface() {
        mJsInterface = JsInterface(this)
        mWebView.addJavascriptInterface(mJsInterface, "AndroidInterface")
    }

    // 初始化相机权限请求器
    private fun initCameraPermissionLauncher() {
        mCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startActivity(Intent(this, QRScannerActivity::class.java))
            } else {
                Toast.makeText(this, "请开启相机权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 加载WebView页面
    private fun loadWebUrlByIndex(index: Int) {
        if (index== NAV_INDEX_LOGIN){
            mWebView.loadUrl("file:///android_asset/login.html")
        }
        else {
            val targetUrl = mNavUrlMap.getOrNull(index) ?: return
            if (targetUrl.isNotEmpty() && mWebView.url != targetUrl) {
                mWebView.loadUrl(targetUrl)
            }
        }
    }

    // 实现导航点击接口（处理跳转逻辑）
    override fun onNavItemClick(index: Int) {
        when (index) {
            MyBottomNavView.INDEX_HOME -> loadWebUrlByIndex(MyBottomNavView.INDEX_HOME)
            MyBottomNavView.INDEX_SCAN -> checkCameraPermission()
            MyBottomNavView.INDEX_ASSISTANT -> startActivity(Intent(this, AssistantActivity::class.java))
            MyBottomNavView.INDEX_BILLS -> loadWebUrlByIndex(MyBottomNavView.INDEX_BILLS)
            MyBottomNavView.INDEX_PROFILE -> loadWebUrlByIndex(MyBottomNavView.INDEX_PROFILE)
        }
    }

    // 相机权限检查
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, QRScannerActivity::class.java))
        } else {
            mCameraPermissionLauncher.launch(CAMERA)
        }
    }

    // 返回键逻辑
    override fun onBackPressed() {
        val currentUrl = mWebView.url ?: run { super.onBackPressed(); return }

        if (mSpecialPages.any { currentUrl.contains(it) }) {
            if (currentUrl.contains("index.html")) {
                super.onBackPressed()
            } else {
                loadWebUrlByIndex(MyBottomNavView.INDEX_HOME)
                mBottomNavView.setSelectedIndex(MyBottomNavView.INDEX_HOME)
            }
        } else {
            if (mWebView.canGoBack()) {
                mWebView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }

    // 其他生命周期方法（保持原有逻辑）
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            JsInterface.REQUEST_CODE_IMAGE_PICK -> mJsInterface.handleImageResult(resultCode, data)
            JsInterface.REQUEST_CODE_QR_SCAN -> mJsInterface.handleScanResult(resultCode, data)
        }
    }

    override fun onDestroy() {
        mWebView.destroy()
        super.onDestroy()
    }
}

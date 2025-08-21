package com.example.myapplicationw.Interfaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.activity.AssistantActivity
import com.example.myapplicationw.ui.activity.QrScannerActivity


/**
 * JavaScript与原生交互接口类
 * 提供JS调用原生功能的方法
 */
@ExperimentalGetImage
class JsInterface(private val context: Context) {
    // 日志标签常量
    private val TAG = "JsInterface"

    // JS回调方法名
    private var jsCallback: String? = null

    // JS回调方法名（用于图片选择）
    private var imageCallback: String? = null

    // 选中的图片Uri
    private var selectedImageUri: Uri? = null
    /**
     * 启动相册选择图片
     * @param callback JS回调函数名（用于返回图片Uri）
     */
    @JavascriptInterface
    fun openImagePicker(callback: String) {
        imageCallback = callback

        val activity = context as? Activity ?: run {
            Log.e(TAG, "启动相册失败：Context不是Activity实例")
            showToast("操作失败，请重试")
            return
        }

        // 检查读取存储权限
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSION_STORAGE
            )
            return
        }

        // 启动系统相册
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        activity.startActivityForResult(
            Intent.createChooser(intent, "选择图片"),
            REQUEST_CODE_IMAGE_PICK
        )
    }

    /**
     * 处理图片选择结果
     */
    fun handleImageResult(resultCode: Int, data: Intent?) {
        val activity = context as? Activity ?: run {
            Log.e(TAG, "处理图片结果失败：Context不是Activity实例")
            return
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            // 获取选中图片的Uri
            val imageUri = data.data ?: run {
                Log.e(TAG, "获取图片Uri失败")
                showToast("获取图片失败")
                return
            }

            // 授权WebView访问该Uri
            grantUriPermissionToWebView(imageUri)

            // 保存Uri供后续使用
            selectedImageUri = imageUri

            // 回调给JavaScript
            val callback = imageCallback ?: run {
                Log.e(TAG, "图片回调失败：未设置回调函数")
                return
            }

            activity.runOnUiThread {
                try {
                    val webView = activity.findViewById<WebView>(R.id.loginWebView)
                    // 转义Uri中的特殊字符
                    val escapedUri = imageUri.toString().replace("'", "\\'")
                    webView.evaluateJavascript("window.$callback('$escapedUri')") {
                        Log.d(TAG, "图片Uri已回调到JS: $escapedUri")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "执行图片JS回调失败", e)
                    showToast("处理图片失败")
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "用户取消图片选择")
        } else {
            Log.e(TAG, "图片选择失败：resultCode=$resultCode")
            showToast("图片选择失败")
        }
    }

    /**
     * 授权WebView访问图片Uri
     */
    private fun grantUriPermissionToWebView(uri: Uri) {
        val activity = context as? Activity ?: return

        try {
            // 获取WebView所在应用的包名
            val packageName = context.packageName

            // 授予读写权限（临时权限，在Activity销毁后失效）
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // 对于content://类型的Uri，需要设置持久权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }

            Log.d(TAG, "已授权WebView访问Uri: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "授权Uri访问失败", e)
        }
    }

    /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == REQUEST_CODE_PERMISSION_STORAGE) {
            val activity = context as? Activity ?: return
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // 权限已授予，重新启动相册
                openImagePicker(imageCallback ?: "")
            } else {
                Log.e(TAG, "用户拒绝存储权限")
                showToast("需要存储权限才能选择图片")
            }
        }
    }
    /**
     * 启动二维码扫描页面
     * @param callback JS回调函数名
     */
    @JavascriptInterface
    fun startQrScannerActivity(callback: String) {
        jsCallback = callback

        // 安全转换Context为Activity
        val activity = context as? Activity
        if (activity != null) {
            val intent = Intent(activity, QrScannerActivity::class.java)
            activity.startActivityForResult(intent, REQUEST_CODE_QR_SCAN)
        } else {
            Log.e(TAG, "启动扫描失败：Context不是Activity实例")
            showToast("操作失败，请重试")
        }
    }

    /**
     * 启动助手聊天页面
     */
    @JavascriptInterface
    fun startAssistantActivity() {
        val activity = context as? Activity
        if (activity != null) {
            val intent = Intent(activity, AssistantActivity::class.java)
            activity.startActivity(intent)
        } else {
            Log.e(TAG, "启动助手失败：Context不是Activity实例")
            showToast("操作失败，请重试")
        }
    }

    /**
     * 处理扫描结果并回调给JavaScript
     * @param resultCode 结果码
     * @param data 包含扫描结果的Intent
     */
    fun handleScanResult(resultCode: Int, data: Intent?) {
        val activity = context as? Activity ?: run {
            Log.e(TAG, "处理扫描结果失败：Context不是Activity实例")
            return
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            val qrContent = data.getStringExtra("SCAN_RESULT") ?: ""
            val callback = jsCallback ?: run {
                Log.e(TAG, "处理扫描结果失败：未设置回调函数")
                return
            }

            // 在主线程执行JS回调
            activity.runOnUiThread {
                try {
                    val webView = activity.findViewById<WebView>(R.id.loginWebView)
                    // 转义特殊字符，防止JS注入问题
                    val escapedContent = qrContent.replace("'", "\\'")
                    webView.evaluateJavascript("window.$callback('$escapedContent')") {
                        Log.d(TAG, "扫描结果已回调到JS")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "执行JS回调失败", e)
                    showToast("处理结果失败")
                }
            }
        } else {
            Log.e(TAG, "扫描结果无效：resultCode=$resultCode, data=$data")
        }
    }

    /**
     * 保存用户信息到SharedPreferences
     * @param token 用户令牌
     * @param userId 用户ID
     * @param phone 用户手机号
     */
    @JavascriptInterface
    fun saveUserToken(token: String, userId: String, phone: String) {
        try {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .putString("USER_TOKEN", token)
                .putString("USER_ID", userId)  // 修复：原代码错误地将token存入userId
                .putString("USER_PHONE", phone) // 修复：原代码错误地将token存入phone
                .apply()

            Log.d(TAG, "用户信息保存成功：userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "保存用户信息失败", e)
            showToast("保存信息失败")
        }
    }

    /**
     * 获取用户信息
     * @return 用户信息的JSON字符串
     */
    @JavascriptInterface
    fun getUserData(): String {
        return try {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPref.getString("USER_TOKEN", "") ?: ""
            val userId = sharedPref.getString("USER_ID", "") ?: ""
            val phone = sharedPref.getString("USER_PHONE", "") ?: ""

            // 构建JSON字符串（确保格式正确）
            """{"token":"$token","userId":"$userId","phone":"$phone"}"""
        } catch (e: Exception) {
            Log.e(TAG, "获取用户信息失败", e)
            "{}" // 返回空JSON对象
        }
    }

    /**
     * 清除保存的用户数据
     */
    @JavascriptInterface
    fun clearUserData() {
        try {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .remove("USER_TOKEN")
                .remove("USER_ID")
                .remove("USER_PHONE")
                .apply()

            Log.d(TAG, "用户信息已清除")
            showToast("已清除用户信息")
        } catch (e: Exception) {
            Log.e(TAG, "清除用户信息失败", e)
            showToast("清除信息失败")
        }
    }

    /**
     * 显示Toast提示
     * @param message 提示内容
     */
    @JavascriptInterface
    fun showToast(message: String) {
        if (message.isBlank()) {
            Log.w(TAG, "Toast消息为空，忽略显示")
            return
        }

        val activity = context as? Activity ?: run {
            Log.e(TAG, "显示Toast失败：Context不是Activity实例")
            return
        }

        activity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "显示Toast：$message")
    }

    companion object {
        // 请求码常量
        const val REQUEST_CODE_QR_SCAN = 1003

        const val REQUEST_CODE_IMAGE_PICK = 1004
        const val REQUEST_CODE_PERMISSION_STORAGE = 1005
    }
}

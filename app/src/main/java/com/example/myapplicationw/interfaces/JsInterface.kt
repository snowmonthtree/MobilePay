package com.example.myapplicationw.interfaces

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
import com.example.myapplicationw.ui.activity.QRScannerActivity

/**
 * JavaScript与原生交互接口类
 * 功能：提供JS调用原生能力（相册选择、二维码扫描、页面跳转、用户数据存储等）
 * 注：所有暴露给JS的方法需添加@JavascriptInterface注解，且运行在独立线程
 */
@ExperimentalGetImage
class JsInterface(private val context: Context) {
    // 日志标签（常量统一管理，便于筛选日志）
    public companion object {
        private const val TAG = "JsInterface"
        // 请求码常量（集中定义，避免散落在代码中）
        const val REQUEST_CODE_QR_SCAN = 1003
        const val REQUEST_CODE_IMAGE_PICK = 1004
        const val REQUEST_CODE_PERMISSION_STORAGE = 1005
        // 提示文本常量（统一管理，便于修改）
        private const val TOAST_OPERATE_FAIL = "操作失败，请重试"
        private const val TOAST_IMAGE_GET_FAIL = "获取图片失败"
        private const val TOAST_IMAGE_SELECT_FAIL = "图片选择失败"
        private const val TOAST_PERMISSION_STORAGE_NEEDED = "需要存储权限才能选择图片"
        private const val TOAST_SAVE_INFO_FAIL = "保存信息失败"
        private const val TOAST_CLEAR_INFO_SUCCESS = "已清除用户信息"
        private const val TOAST_CLEAR_INFO_FAIL = "清除信息失败"
        // SharedPreferences配置（常量统一，避免硬编码）
        private const val SP_NAME_USER = "UserPrefs"
        private const val SP_KEY_TOKEN = "USER_TOKEN"
        private const val SP_KEY_USER_ID = "USER_ID"
        private const val SP_KEY_PHONE = "USER_PHONE"
    }

    // JS回调函数名（单例语义，修正变量名避免复数歧义）
    private var qrScanJsCallback: String? = null
    private var imageSelectJsCallback: String? = null

    // 选中的图片Uri（用于后续WebView授权访问）
    private var selectedImageUri: Uri? = null

    /**
     * 暴露给JS：启动相册选择图片
     * @param callback JS回调函数名（用于返回图片Uri）
     * 注：需在主线程或通过Activity.runOnUiThread处理UI相关操作
     */
    @JavascriptInterface
    fun openImagePicker(callback: String) {
        imageSelectJsCallback = callback
        val activity = getValidActivity() ?: return

        // 检查存储权限（适配Android版本，Android 13+需使用READ_MEDIA_IMAGES）
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            // 权限未授予，发起请求
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(storagePermission),
                REQUEST_CODE_PERMISSION_STORAGE
            )
            return
        }

        // 权限已授予，启动系统相册
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageIntent.type = "image/*" // 仅选择图片类型
        activity.startActivityForResult(
            Intent.createChooser(pickImageIntent, "选择图片"),
            REQUEST_CODE_IMAGE_PICK
        )
    }

    /**
     * 处理图片选择结果（供Activity回调调用）
     * @param resultCode 结果码（Activity.RESULT_OK/RESULT_CANCELED）
     * @param data 包含图片Uri的Intent
     */
    fun handleImageResult(resultCode: Int, data: Intent?) {
        val activity = getValidActivity() ?: return

        when (resultCode) {
            Activity.RESULT_OK -> {
                val imageUri = data?.data ?: run {
                    Log.e(TAG, "处理图片结果：获取图片Uri为空")
                    showToast(TOAST_IMAGE_GET_FAIL)
                    return
                }

                // 授权WebView访问图片Uri（避免权限问题导致图片无法加载）
                grantWebViewImagePermission(imageUri)
                selectedImageUri = imageUri

                // 执行JS回调（需在主线程执行WebView操作）
                val callback = imageSelectJsCallback ?: run {
                    Log.e(TAG, "处理图片结果：JS回调函数名为空")
                    return
                }

                activity.runOnUiThread {
                    try {
                        val webView = activity.findViewById<WebView>(R.id.loginWebView)
                        // 转义Uri中的特殊字符（如单引号），避免JS语法错误
                        val escapedUri = imageUri.toString().replace("'", "\\'")
                        val jsCode = "window.$callback('$escapedUri')"
                        webView.evaluateJavascript(jsCode) {
                            Log.d(TAG, "图片选择回调JS成功，Uri：$escapedUri")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "执行图片选择JS回调异常", e)
                        showToast(TOAST_OPERATE_FAIL)
                    }
                }
            }

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "处理图片结果：用户取消选择")
            }

            else -> {
                Log.e(TAG, "处理图片结果：选择失败，resultCode=$resultCode")
                showToast(TOAST_IMAGE_SELECT_FAIL)
            }
        }
    }

    /**
     * 授权WebView访问图片Uri（解决WebView加载本地图片权限问题）
     * @param imageUri 选中图片的Uri
     */
    private fun grantWebViewImagePermission(imageUri: Uri) {
        val activity = getValidActivity() ?: return

        try {
            val packageName = context.packageName
            // 授予临时读写权限（Activity销毁后自动失效）
            context.grantUriPermission(
                packageName,
                imageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // 适配Android 4.4+，设置持久化权限（避免Uri失效）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(imageUri, takeFlags)
            }

            Log.d(TAG, "授权WebView访问图片Uri成功：$imageUri")
        } catch (e: Exception) {
            Log.e(TAG, "授权WebView访问图片Uri异常", e)
        }
    }

    /**
     * 处理权限请求结果（供Activity回调调用）
     * @param requestCode 权限请求码
     */
    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode != REQUEST_CODE_PERMISSION_STORAGE) return

        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            // 权限授予成功，重新启动相册选择
            openImagePicker(imageSelectJsCallback ?: "")
        } else {
            Log.e(TAG, "权限请求结果：用户拒绝存储权限")
            showToast(TOAST_PERMISSION_STORAGE_NEEDED)
        }
    }

    /**
     * 暴露给JS：启动二维码扫描页面
     * @param callback JS回调函数名（用于返回扫描结果）
     */
    @JavascriptInterface
    fun startQrScannerActivity(callback: String) {
        qrScanJsCallback = callback
        val activity = getValidActivity() ?: return

        val scanIntent = Intent(activity, QRScannerActivity::class.java)
        activity.startActivityForResult(scanIntent, REQUEST_CODE_QR_SCAN)
    }

    /**
     * 处理二维码扫描结果（供Activity回调调用）
     * @param resultCode 结果码（Activity.RESULT_OK/RESULT_CANCELED）
     * @param data 包含扫描结果的Intent
     */
    fun handleScanResult(resultCode: Int, data: Intent?) {
        val activity = getValidActivity() ?: return

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.e(TAG, "处理扫描结果：结果无效，resultCode=$resultCode, data=$data")
            return
        }

        // 获取扫描结果（空安全处理）
        val qrContent = data.getStringExtra("SCAN_RESULT") ?: ""
        val callback = qrScanJsCallback ?: run {
            Log.e(TAG, "处理扫描结果：JS回调函数名为空")
            return
        }

        // 主线程执行JS回调（WebView操作需在主线程）
        activity.runOnUiThread {
            try {
                val webView = activity.findViewById<WebView>(R.id.loginWebView)
                // 转义特殊字符（如单引号、换行符），避免JS语法错误
                val escapedContent = qrContent.replace("'", "\\'").replace("\n", "\\n")
                val jsCode = "window.$callback('$escapedContent')"
                webView.evaluateJavascript(jsCode) {
                    Log.d(TAG, "二维码扫描回调JS成功，内容：$qrContent")
                }
            } catch (e: Exception) {
                Log.e(TAG, "执行扫描结果JS回调异常", e)
                showToast(TOAST_OPERATE_FAIL)
            }
        }
    }

    /**
     * 暴露给JS：启动智能助手聊天页面
     */
    @JavascriptInterface
    fun startAssistantActivity() {
        val activity = getValidActivity() ?: return
        val assistantIntent = Intent(activity, AssistantActivity::class.java)
        activity.startActivity(assistantIntent)
    }

    /**
     * 暴露给JS：保存用户信息到SharedPreferences
     * @param token 用户令牌
     * @param userId 用户ID
     * @param phone 用户手机号
     */
    @JavascriptInterface
    fun saveUserToken(token: String, userId: String, phone: String) {
        try {
            val sharedPref = context.getSharedPreferences(SP_NAME_USER, Context.MODE_PRIVATE)
            sharedPref.edit()
                .putString(SP_KEY_TOKEN, token)
                .putString(SP_KEY_USER_ID, userId) // 修复原代码错误：避免重复存储token
                .putString(SP_KEY_PHONE, phone)    // 修复原代码错误：正确存储手机号
                .apply() // 异步提交，避免阻塞线程

            Log.d(TAG, "保存用户信息成功，userId：$userId")
        } catch (e: Exception) {
            Log.e(TAG, "保存用户信息异常", e)
            showToast(TOAST_SAVE_INFO_FAIL)
        }
    }

    /**
     * 暴露给JS：从SharedPreferences获取用户信息
     * @return 用户信息JSON字符串（格式：{"token":"","userId":"","phone":""}）
     */
    @JavascriptInterface
    fun getUserData(): String {
        return try {
            val sharedPref = context.getSharedPreferences(SP_NAME_USER, Context.MODE_PRIVATE)
            val token = sharedPref.getString(SP_KEY_TOKEN, "") ?: ""
            val userId = sharedPref.getString(SP_KEY_USER_ID, "") ?: ""
            val phone = sharedPref.getString(SP_KEY_PHONE, "") ?: ""

            // 构建标准JSON格式（避免语法错误，特殊字符未处理需根据实际需求扩展）
            """{"token":"$token","userId":"$userId","phone":"$phone"}"""
        } catch (e: Exception) {
            Log.e(TAG, "获取用户信息异常", e)
            "{}" // 异常时返回空JSON对象，避免JS解析错误
        }
    }

    /**
     * 暴露给JS：清除SharedPreferences中的用户信息
     */
    @JavascriptInterface
    fun clearUserData() {
        try {
            val sharedPref = context.getSharedPreferences(SP_NAME_USER, Context.MODE_PRIVATE)
            sharedPref.edit()
                .remove(SP_KEY_TOKEN)
                .remove(SP_KEY_USER_ID)
                .remove(SP_KEY_PHONE)
                .apply()

            Log.d(TAG, "清除用户信息成功")
            showToast(TOAST_CLEAR_INFO_SUCCESS)
        } catch (e: Exception) {
            Log.e(TAG, "清除用户信息异常", e)
            showToast(TOAST_CLEAR_INFO_FAIL)
        }
    }

    /**
     * 暴露给JS：显示Toast提示
     * @param message 提示内容
     * 注：Toast需在主线程显示，通过Activity.runOnUiThread切换线程
     */
    @JavascriptInterface
    fun showToast(message: String) {
        if (message.isBlank()) {
            Log.w(TAG, "显示Toast：提示内容为空，忽略")
            return
        }

        val activity = getValidActivity() ?: return
        activity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "显示Toast：$message")
    }

    /**
     * 安全获取Activity实例（避免Context非Activity导致的崩溃）
     * @return 非空Activity实例，或null（Context无效时）
     */
    private fun getValidActivity(): Activity? {
        return if (context is Activity) {
            context
        } else {
            Log.e(TAG, "Context无效：当前Context不是Activity实例")
            showToast(TOAST_OPERATE_FAIL)
            null
        }
    }
}
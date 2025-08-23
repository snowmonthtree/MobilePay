package com.example.myapplicationw.ui.activity

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationw.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 二维码扫描页面
 * 支持相机实时扫描和相册图片识别
 */
@ExperimentalGetImage
class QRScannerActivity : AppCompatActivity() {
    // 控件定义
    private lateinit var previewView: PreviewView
    private lateinit var ivScanLine: ImageView
    private lateinit var vScanFrame: View
    private lateinit var llBtnAlbum: LinearLayout
    private lateinit var ibtnFlash: ImageButton
    private lateinit var ibtnClose: ImageButton

    // 状态变量
    private var isFlashlightOn = false
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var isScanning = true

    // 权限和请求码常量
    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1002
        private const val TAG = "QRScannerActivity"  // 日志标签常量
        private const val SCAN_ANIMATION_DURATION = 2000  // 扫描动画时长
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qr_scanner)

        // 处理系统栏Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.qrScanner)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化控件
        initViews()
        // 初始化扫描器
        initScanner()
        // 初始化事件监听
        initListeners()
        // 初始化相机
        initCamera()
    }

    /**
     * 初始化视图控件
     */
    private fun initViews() {
        vScanFrame = findViewById(R.id.scanFrame)
        previewView = findViewById(R.id.previewView)
        ivScanLine = findViewById(R.id.scanLine)
        llBtnAlbum = findViewById(R.id.btn_album)
        ibtnFlash = findViewById(R.id.btn_flash)
        ibtnClose = findViewById(R.id.btn_close)
    }

    /**
     * 初始化二维码扫描器
     */
    private fun initScanner() {
        barcodeScanner = BarcodeScanning.getClient()
    }

    /**
     * 初始化事件监听器
     */
    private fun initListeners() {
        ibtnClose.setOnClickListener { finish() }

        llBtnAlbum.setOnClickListener {
            checkStoragePermissionAndOpenAlbum()
        }

        ibtnFlash.setOnClickListener {
            toggleFlashlight()
        }
    }

    /**
     * 初始化相机
     */
    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startCamera()
            startScanAnimation()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 启动相机预览和分析器
     */
    private fun startCamera() {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e(TAG, "相机启动失败", e)
            Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理相机预览帧
     */
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        // 安全处理可空对象
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner?.process(image)
            ?.addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && isScanning) {
                    isScanning = false
                    val qrContent = barcodes[0].rawValue
                    qrContent?.let { handleResult(it) }
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "二维码识别失败", e)
            }
            ?.addOnCompleteListener {
                imageProxy.close()  // 确保资源释放
            }
    }

    /**
     * 检查存储权限并打开相册
     */
    private fun checkStoragePermissionAndOpenAlbum() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        } else {
            openImagePicker()
        }
    }

    /**
     * 打开图片选择器
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "需要存储权限才能从相册选择图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理图片选择结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            // 异步处理图片识别
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    decodeFromGallery(uri)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QRScannerActivity, "识别失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 从相册图片解码二维码
     */
    private suspend fun decodeFromGallery(uri: Uri) {
        try {
            // 从Uri获取Bitmap
            val bmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val intArray = IntArray(bmp.width * bmp.height)
            bmp.getPixels(intArray, 0, bmp.width, 0, 0, bmp.width, bmp.height)

            // 解码二维码
            //灰度图
            val source = RGBLuminanceSource(bmp.width, bmp.height, intArray)
            //二值化处理->二进制位图
            val binary = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binary)

            // 处理识别结果
            handleResult(result.text)
        } catch (e: Exception) {
            Log.e(TAG, "相册图片解码失败", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@QRScannerActivity, "识别失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 切换手电筒状态
     */
    private fun toggleFlashlight() {
        val cameraControl = camera?.cameraControl ?: run {
            Toast.makeText(this, "相机未初始化", Toast.LENGTH_SHORT).show()
            return
        }

        cameraControl.enableTorch(!isFlashlightOn)
            .addListener({
                isFlashlightOn = !isFlashlightOn
                // 可以在这里更新闪光灯按钮图标
            }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 处理扫描结果
     */
    private fun handleResult(qrContent: String) {
        runOnUiThread {
            Toast.makeText(this, "扫描结果: $qrContent", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "扫描结果: $qrContent")
        }

        // 返回结果给上一个页面
        val resultIntent = Intent().apply {
            putExtra("SCAN_RESULT", qrContent)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * 启动扫描线动画
     */
    private fun startScanAnimation() {
        ivScanLine.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(ivScanLine, "translationY", 0f, vScanFrame.height.toFloat())
            .apply {
                duration = SCAN_ANIMATION_DURATION.toLong()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
    }

    /**
     * 释放资源
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        barcodeScanner?.close()
        // 清空引用
        camera = null
        cameraProvider = null
        barcodeScanner = null
    }
}
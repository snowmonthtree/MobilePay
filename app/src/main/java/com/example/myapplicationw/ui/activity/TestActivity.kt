package com.example.myapplicationw.ui.activity
import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.fragment.BillsFragment
import com.example.myapplicationw.ui.fragment.HomeFragment
import com.example.myapplicationw.ui.fragment.ProfileFragment
import com.example.myapplicationw.ui.fragment.ScanFragment

@ExperimentalGetImage class TestActivity : AppCompatActivity() {
    private lateinit var bottomNav:LinearLayout
    private lateinit var homeNav:LinearLayout
    private lateinit var scanNav:LinearLayout
    private lateinit var assistantNav:LinearLayout
    private lateinit var billsNav:LinearLayout
    private lateinit var profileNav:LinearLayout
    private var currentIndex:Int=0
    // 存储所有Fragment实例
    private val fragmentList = listOf(
        HomeFragment(),
        ScanFragment(),
        BillsFragment(),
        ProfileFragment()
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
        val requestCameraPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限已授予，执行回调
                Toast.makeText(this, "权限已授予,请再次点击", Toast.LENGTH_SHORT).show()
            } else {
                // 权限被拒绝，执行回调
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
        initFragments()
        bottomNav=findViewById(R.id.bottom_nav)
        homeNav=findViewById(R.id.nav_home)
        scanNav=findViewById(R.id.nav_scan)
        assistantNav=findViewById(R.id.nav_assistant)
        billsNav=findViewById(R.id.nav_bills)
        profileNav=findViewById(R.id.nav_profile)

        homeNav.setOnClickListener(){
            setSelectedTab(0)
        }
        scanNav.setOnClickListener(){
            setSelectedTab(1)
            checkAndRequestCameraPermission(
                activity = this,
                launcher = requestCameraPermissionLauncher,
                onPermissionGranted = {
                    val intent = Intent(this, QrScannerActivity::class.java)
                    startActivity(intent)
                }
            )
        }
        billsNav.setOnClickListener(){
            setSelectedTab(2)
        }
        profileNav.setOnClickListener(){
            setSelectedTab(3)
        }
        assistantNav.setOnClickListener{
            val intent=Intent(this,AssistantActivity::class.java)
            startActivity(intent)
        }
        /*bottomNavigationView=findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home

        // 初始化导航控制器
        val navController = supportFragmentManager.beginTransaction()
        navController.replace(R.id.fragmentContainer, HomeFragment())
        navController.commit()

        // 设置导航项选中监听器
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 切换到首页
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, HomeFragment())
                    transaction.commit()
                    true
                }
                /*R.id.nav_scan -> {
                    // 切换到扫描页
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, ScanFragment())
                    transaction.commit()
                    true
                }
                R.id.nav_history -> {
                    // 切换到历史页
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, HistoryFragment())
                    transaction.commit()
                    true
                }
                R.id.nav_mine -> {
                    // 切换到我的页
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, MineFragment())
                    transaction.commit()
                    true
                }*/
                else -> false
            }
        }*/
        /*btnTest=findViewById(R.id.test)
        btnTest.setOnClickListener {
            checkAndRequestCameraPermission(
                activity = this,
                launcher = requestCameraPermissionLauncher,
                onPermissionGranted = {
                    val intent = Intent(this, QrScannerActivity::class.java)
                    startActivity(intent)
                }
            )
        }*/
    }
    private fun initFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        fragmentList.forEachIndexed { index, fragment ->
            transaction.add(R.id.fragment_container, fragment, "fragment_$index")
            // 默认只显示第一个Fragment，其他隐藏
            if (index != 0) {
                transaction.hide(fragment)
            }
        }
        transaction.commit()
    }
    private fun setSelectedTab(position: Int) {
        if (currentIndex==position)return
        val tvHome=findViewById<TextView>(R.id.tv_home)
        val ivHome=findViewById<ImageView>(R.id.iv_home)
        val tvScan=findViewById<TextView>(R.id.tv_scan)
        val ivScan=findViewById<ImageView>(R.id.iv_scan)
        val tvBills=findViewById<TextView>(R.id.tv_bills)
        val ivBills=findViewById<ImageView>(R.id.iv_bills)
        val tvProfile=findViewById<TextView>(R.id.tv_profile)
        val ivProfile=findViewById<ImageView>(R.id.iv_profile)
        val transaction = supportFragmentManager.beginTransaction()

        // 隐藏当前Fragment，显示选中的Fragment
        transaction.hide(fragmentList[currentIndex])
        transaction.show(fragmentList[position])
        transaction.commit()
        val tvItems = listOf(
            tvHome,
            tvScan,
            tvBills,
            tvProfile,
        )
        val ivItems = listOf(
            ivHome,
            ivScan,
            ivBills,
            ivProfile,
        )
        val activeColor= ContextCompat.getColor(this,R.color.nav_active)

        val inactiveColor= ContextCompat.getColor(this,R.color.nav_inactive)
        tvItems.forEachIndexed { index, tv ->
            if(position==index){
                tv.setTextIsSelectable(true)
                tv.setTextColor(activeColor)
            }
            else{
                tv.setTextIsSelectable(false)
                tv.setTextColor(inactiveColor)
            }
        }
        ivItems.forEachIndexed { index, iv ->
            if(position==index){
            }
            else{
            }

        }
        currentIndex=position
    }
    fun checkAndRequestCameraPermission(
        activity: AppCompatActivity,
        launcher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: (() -> Unit)? = null
    ) {
        if (ContextCompat.checkSelfPermission(activity, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted.invoke()
        } else {
            launcher.launch(CAMERA)
        }
    }
}
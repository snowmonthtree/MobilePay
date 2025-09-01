package com.example.myapplicationw.ui.navigation
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplicationw.Interfaces.OnNavItemClickListener
import com.example.myapplicationw.R

// 导航项点击事件接口（解耦跳转逻辑）

// 适配固定XML布局的导航栏组件
class MyBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 导航项索引常量（与XML布局顺序对应）
    companion object {
        const val INDEX_HOME = 0
        const val INDEX_SCAN = 1
        const val INDEX_ASSISTANT = 2
        const val INDEX_BILLS = 3
        const val INDEX_PROFILE = 4
    }

    // 绑定XML中的控件（与你的XML布局ID严格对应）
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

    // 状态控制变量
    private var currentSelectedIndex = INDEX_HOME
    private var listener: OnNavItemClickListener? = null
    private val inactiveColor = context.getColor(R.color.nav_inactive)
    private val activeColor = context.getColor(R.color.nav_active)

    init {

        this.clipChildren = false  // 关闭子视图裁剪
        this.clipToPadding = false // 关闭内边距裁剪
        // 从XML加载布局（使用传入的上下文和当前布局）
        View.inflate(context, R.layout.bottom_nav, this)
        // 绑定XML中的控件
        initViews()
        // 初始化点击事件
        initClickListeners()
    }

    // 绑定XML中的控件（ID必须与你的XML完全一致）
    private fun initViews() {
        // 导航项容器
        navHome = findViewById(R.id.nav_home)
        navScan = findViewById(R.id.nav_scan)
        navAssistant = findViewById(R.id.nav_assistant)
        navBills = findViewById(R.id.nav_bills)
        navProfile = findViewById(R.id.nav_profile)

        // 图标和文字
        ivHome = findViewById(R.id.iv_home)
        tvHome = findViewById(R.id.tv_home)
        ivScan = findViewById(R.id.iv_scan)
        tvScan = findViewById(R.id.tv_scan)
        ivBills = findViewById(R.id.iv_bills)
        tvBills = findViewById(R.id.tv_bills)
        ivProfile = findViewById(R.id.iv_profile)
        tvProfile = findViewById(R.id.tv_profile)
    }

    // 初始化导航项点击事件
    private fun initClickListeners() {
        navHome.setOnClickListener {
            if (currentSelectedIndex != INDEX_HOME) {
                updateSelectedState(INDEX_HOME)
                listener?.onNavItemClick(INDEX_HOME)
            }
        }

        navScan.setOnClickListener {
            if (currentSelectedIndex != INDEX_SCAN) {
                updateSelectedState(INDEX_SCAN)
                listener?.onNavItemClick(INDEX_SCAN)
            }
        }

        navAssistant.setOnClickListener {
            if (currentSelectedIndex != INDEX_ASSISTANT) {
                updateSelectedState(INDEX_ASSISTANT)
                listener?.onNavItemClick(INDEX_ASSISTANT)
            }
        }

        navBills.setOnClickListener {
            if (currentSelectedIndex != INDEX_BILLS) {
                updateSelectedState(INDEX_BILLS)
                listener?.onNavItemClick(INDEX_BILLS)
            }
        }

        navProfile.setOnClickListener {
            if (currentSelectedIndex != INDEX_PROFILE) {
                updateSelectedState(INDEX_PROFILE)
                listener?.onNavItemClick(INDEX_PROFILE)
            }
        }
    }

    // 更新选中状态（高亮当前项，重置其他项）
    private fun updateSelectedState(index: Int) {
        // 重置所有项为未选中
        resetAllItems()

        // 高亮选中项
        when (index) {
            INDEX_HOME -> {
                ivHome.alpha = 1.0f
                tvHome.setTextColor(activeColor)
            }
            INDEX_SCAN -> {
                ivScan.alpha = 1.0f
                tvScan.setTextColor(activeColor)
            }
            INDEX_ASSISTANT -> {
                // 智能助手项可根据需求添加高亮逻辑
            }
            INDEX_BILLS -> {
                ivBills.alpha = 1.0f
                tvBills.setTextColor(activeColor)
            }
            INDEX_PROFILE -> {
                ivProfile.alpha = 1.0f
                tvProfile.setTextColor(activeColor)
            }
        }

        currentSelectedIndex = index
    }

    // 重置所有项为未选中状态
    private fun resetAllItems() {
        // 首页
        ivHome.alpha = 0.6f
        tvHome.setTextColor(inactiveColor)
        // 扫一扫
        ivScan.alpha = 0.6f
        tvScan.setTextColor(inactiveColor)
        // 账单
        ivBills.alpha = 0.6f
        tvBills.setTextColor(inactiveColor)
        // 我的
        ivProfile.alpha = 0.6f
        tvProfile.setTextColor(inactiveColor)
    }

    // 设置点击事件监听器（供页面调用）
    fun setOnNavItemClickListener(listener: OnNavItemClickListener) {
        this.listener = listener
    }

    // 外部强制更新选中状态（如页面初始化时）
    fun setSelectedIndex(index: Int) {
        if (index in 0..4) {
            updateSelectedState(index)
        }
    }
}

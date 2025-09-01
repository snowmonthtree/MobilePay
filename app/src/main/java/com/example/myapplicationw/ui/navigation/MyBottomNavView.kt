package com.example.myapplicationw.ui.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplicationw.interfaces.OnNavItemClickListener
import com.example.myapplicationw.R

/**
 * 自定义底部导航栏组件
 * 支持5个导航项：首页、扫一扫、智能助手、账单、我的
 */
class MyBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 导航项索引常量（与XML布局顺序严格对应）
    companion object {
        const val INDEX_HOME = 0
        const val INDEX_SCAN = 1
        const val INDEX_ASSISTANT = 2
        const val INDEX_BILLS = 3
        const val INDEX_PROFILE = 4
    }

    // 导航项容器
    private lateinit var navHome: LinearLayout
    private lateinit var navScan: LinearLayout
    private lateinit var navAssistant: LinearLayout
    private lateinit var navBills: LinearLayout
    private lateinit var navProfile: LinearLayout

    // 图标和文字控件
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
    private val inactiveColor: Int by lazy { context.getColor(R.color.nav_inactive) }
    private val activeColor: Int by lazy { context.getColor(R.color.nav_active) }

    init {
        // 初始化布局
        initLayout()
        // 绑定视图
        initViews()
        // 初始化事件
        initListeners()
        // 设置初始状态
        updateSelectedState(INDEX_HOME)
    }

    /**
     * 初始化布局
     */
    private fun initLayout() {
        clipChildren = false  // 允许子视图超出边界（适用于中间凸起按钮）
        clipToPadding = false
        View.inflate(context, R.layout.bottom_nav, this)
    }

    /**
     * 绑定XML中的视图控件
     * 注意：ID必须与布局文件保持一致
     */
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

    /**
     * 初始化点击事件监听
     */
    private fun initListeners() {
        navHome.setOnClickListener { handleItemClick(INDEX_HOME) }
        navScan.setOnClickListener { handleItemClick(INDEX_SCAN) }
        navAssistant.setOnClickListener { handleItemClick(INDEX_ASSISTANT) }
        navBills.setOnClickListener { handleItemClick(INDEX_BILLS) }
        navProfile.setOnClickListener { handleItemClick(INDEX_PROFILE) }
    }

    /**
     * 处理导航项点击事件
     * @param index 导航项索引
     */
    private fun handleItemClick(index: Int) {
        if (currentSelectedIndex != index) {
            if(index!= INDEX_SCAN&&index!= INDEX_ASSISTANT)updateSelectedState(index)
            listener?.onNavItemClick(index)
        }
    }

    /**
     * 更新选中状态（高亮当前项，重置其他项）
     * @param index 要选中的导航项索引
     */
    private fun updateSelectedState(index: Int) {
        // 重置所有项为未选中状态
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
                // 智能助手项的高亮逻辑（根据需求实现）
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

    /**
     * 重置所有项为未选中状态
     */
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

    /**
     * 设置导航项点击事件监听器
     * @param listener 点击事件监听器
     */
    fun setOnNavItemClickListener(listener: OnNavItemClickListener) {
        this.listener = listener
    }

    /**
     * 外部强制更新选中状态
     * @param index 要选中的导航项索引（0-4）
     */
    fun setSelectedIndex(index: Int) {
        if (index in INDEX_HOME..INDEX_PROFILE) {
            updateSelectedState(index)
        }
    }

    /**
     * 获取当前选中的索引
     * @return 当前选中的导航项索引
     */
    fun getSelectedIndex(): Int = currentSelectedIndex
}

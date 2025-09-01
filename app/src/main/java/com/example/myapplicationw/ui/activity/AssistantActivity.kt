package com.example.myapplicationw.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationw.Interfaces.ChatListener
import com.example.myapplicationw.R
import com.example.myapplicationw.retrofit.RetrofitClient
import com.example.myapplicationw.ui.adapter.MessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 智能助手聊天界面
 * 功能：展示用户与智能助手的聊天记录、发送消息、处理AI回复、支持快捷按钮交互
 */
class AssistantActivity : AppCompatActivity(), ChatListener {
    // 控件定义（命名规范：控件类型前缀+功能描述，增强可读性）
    private lateinit var rvChat: RecyclerView    // 聊天消息列表RecyclerView
    private lateinit var etSendMsg: EditText     // 消息输入框（修正命名：etSend→etSendMsg，语义更明确）
    private lateinit var ivSendBtn: ImageView    // 发送按钮（修正命名：ivSend→ivSendBtn，明确按钮属性）

    // 适配器与网络实例（成员变量命名统一小驼峰，实例语义清晰）
    private lateinit var messageAdapter: MessageAdapter
    private val retrofitClient = RetrofitClient  // 网络请求客户端（复用单例实例）

    // 常量定义（集中管理，便于维护和统一修改）
    companion object {
        private const val TAG = "AssistantActivity"    // 日志标签
        const val SENDER_USER = 1                      // 消息发送方：用户
        const val SENDER_ASSISTANT = 0                 // 消息发送方：智能助手
        private const val LOADING_MSG = "思考中"        // 加载中提示文本（常量化，避免硬编码）
        private const val REQUEST_FAIL_MSG = "抱歉，请求失败，请稍后再试"  // 请求失败提示文本
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assistant)

        // 初始化系统栏适配（拆分独立方法，逻辑分层）
        initSystemBarInsets()
        // 初始化视图、适配器、事件监听（分步执行，代码结构清晰）
        initViews()
        initRecyclerAdapter()
        initEventListeners()
        // 加载初始欢迎消息
        loadWelcomeMessage()
    }

    /**
     * 初始化系统栏Insets，适配沉浸式布局
     * 作用：让页面内容避开状态栏、导航栏等系统控件，避免布局被遮挡
     */
    private fun initSystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * 初始化视图控件
     * 绑定布局中的UI元素，建立代码与布局的关联
     */
    private fun initViews() {
        rvChat = findViewById(R.id.rv_chat)
        etSendMsg = findViewById(R.id.ed_send)
        ivSendBtn = findViewById(R.id.iv_send)
    }

    /**
     * 初始化RecyclerView适配器
     * 配置列表布局管理器、设置适配器，确保列表正常显示
     */
    private fun initRecyclerAdapter() {
        messageAdapter = MessageAdapter(this)
        rvChat.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@AssistantActivity)
            // 优化列表性能：固定列表项高度（若item高度固定，可提升渲染效率）
            setHasFixedSize(true)
        }
    }

    /**
     * 初始化事件监听器
     * 为发送按钮绑定点击事件，触发消息发送逻辑
     */
    private fun initEventListeners() {
        ivSendBtn.setOnClickListener {
            sendUserMessage()
        }
    }

    /**
     * 加载初始欢迎消息
     * 页面创建时自动添加智能助手的欢迎语，引导用户交互
     */
    private fun loadWelcomeMessage() {
        val welcomeContent = "你好！我是你的智能助手 🤖\n" +
                "我可以帮你：\n" +
                "📊 查看消费分析\n" +
                "💰 查询账单记录\n" +
                "🎫 管理优惠券\n" +
                "💡 提供理财建议\n" +
                "试试问我'我这个月花了多少钱？'"
        messageAdapter.addMessage(welcomeContent, SENDER_ASSISTANT)
        // 滚动到最新消息（确保欢迎消息显示在底部）
        scrollToLatestMessage()
    }

    /**
     * 发送用户消息
     * 逻辑：校验输入合法性→添加用户消息→显示加载中→请求AI回复→处理回复结果
     */
    private fun sendUserMessage() {
        val userInput = etSendMsg.text.toString().trim()
        // 校验输入：为空时忽略发送请求
        if (userInput.isEmpty()) {
            Log.d(TAG, "用户输入为空，忽略发送请求")
            return
        }

        // 1. 添加用户消息到列表，清空输入框
        messageAdapter.addMessage(userInput, SENDER_USER)
        etSendMsg.setText("")
        etSendMsg.clearFocus()
        scrollToLatestMessage()

        // 2. 显示"思考中"加载提示（UI操作需在主线程，用post确保线程安全）
        rvChat.post {
            messageAdapter.addMessage(LOADING_MSG, SENDER_ASSISTANT)
            scrollToLatestMessage()
        }

        // 3. 发起AI回复请求（网络请求在IO线程，避免阻塞主线程）
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 网络请求：调用Retrofit接口获取AI回复
                val aiResponse = retrofitClient.sendMessageToAI(userInput)

                // 4. 主线程更新UI：移除加载提示，添加实际回复
                withContext(Dispatchers.Main) {
                    messageAdapter.removeLastMessage()
                    messageAdapter.addMessage(aiResponse, SENDER_ASSISTANT)
                    scrollToLatestMessage()
                }
            } catch (e: Exception) {
                // 异常处理：打印错误日志，显示失败提示
                Log.e(TAG, "AI回复请求失败，用户输入：$userInput", e)
                withContext(Dispatchers.Main) {
                    messageAdapter.removeLastMessage()
                    messageAdapter.addMessage(REQUEST_FAIL_MSG, SENDER_ASSISTANT)
                    scrollToLatestMessage()
                }
            }
        }
    }

    /**
     * 滚动到最新消息位置
     * 使用smoothScrollToPosition实现平滑滚动，提升用户体验
     */
    private fun scrollToLatestMessage() {
        val latestPosition = messageAdapter.itemCount - 1
        if (latestPosition >= 0) {
            rvChat.smoothScrollToPosition(latestPosition)
        }
    }

    /**
     * ChatListener接口实现：快捷按钮点击回调
     * 逻辑与发送用户消息一致，复用AI请求流程
     */
    override fun onButtonClick(buttonText: String) {
        // 1. 添加快捷按钮对应的用户消息
        messageAdapter.addMessage(buttonText, SENDER_USER)
        scrollToLatestMessage()

        // 2. 显示加载中提示
        rvChat.post {
            messageAdapter.addMessage(LOADING_MSG, SENDER_ASSISTANT)
            scrollToLatestMessage()
        }

        // 3. 发起AI回复请求
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val aiResponse = retrofitClient.sendMessageToAI(buttonText)

                // 4. 主线程更新UI
                withContext(Dispatchers.Main) {
                    messageAdapter.removeLastMessage()
                    messageAdapter.addMessage(aiResponse, SENDER_ASSISTANT)
                    scrollToLatestMessage()
                }
            } catch (e: Exception) {
                Log.e(TAG, "快捷按钮AI请求失败，按钮文本：$buttonText", e)
                withContext(Dispatchers.Main) {
                    messageAdapter.removeLastMessage()
                    messageAdapter.addMessage(REQUEST_FAIL_MSG, SENDER_ASSISTANT)
                    scrollToLatestMessage()
                }
            }
        }
    }
}
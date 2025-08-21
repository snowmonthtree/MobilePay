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
 */
class AssistantActivity : AppCompatActivity(), ChatListener {
    // 控件定义
    private lateinit var rvChat: RecyclerView
    private lateinit var etSend: EditText
    private lateinit var ivSend: ImageView

    // 适配器与数据相关
    private lateinit var messageAdapter: MessageAdapter
    private val retrofitClient = RetrofitClient

    // 常量定义
    companion object {
        private const val TAG = "AssistantActivity"
        const val SENDER_USER = 1    // 用户发送
        const val SENDER_ASSISTANT = 0  // 助手发送
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assistant)

        // 处理系统栏Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化视图
        initViews()
        // 初始化适配器
        initAdapter()
        // 初始化事件监听
        initListeners()
        // 加载初始消息
        loadInitialMessage()
    }

    /**
     * 初始化视图控件
     */
    private fun initViews() {
        rvChat = findViewById(R.id.rv_chat)
        etSend = findViewById(R.id.ed_send)
        ivSend = findViewById(R.id.iv_send)
    }

    /**
     * 初始化RecyclerView适配器
     */
    private fun initAdapter() {
        messageAdapter = MessageAdapter(this)
        rvChat.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@AssistantActivity)
        }
    }

    /**
     * 初始化事件监听器
     */
    private fun initListeners() {
        ivSend.setOnClickListener {
            sendMessage()
        }
    }

    /**
     * 加载初始欢迎消息
     */
    private fun loadInitialMessage() {
        val welcomeMsg = " 你好！我是你的智能助手 🤖\n" +
                "我可以帮你：\n" +
                "📊 查看消费分析\n" +
                "💰 查询账单记录\n" +
                "🎫 管理优惠券\n" +
                "💡 提供理财建议\n" +
                "试试问我'我这个月花了多少钱？'"
        messageAdapter.addMessage(welcomeMsg, SENDER_ASSISTANT)
    }

    /**
     * 发送消息处理逻辑
     */
    private fun sendMessage() {
        val messageText = etSend.text.toString().trim()
        if (messageText.isEmpty()) {
            Log.d(TAG, "发送消息为空，忽略")
            return
        }

        // 添加用户消息并清空输入框
        messageAdapter.addMessage(messageText, SENDER_USER)
        etSend.setText("")
        etSend.clearFocus()
        scrollToBottom()

        // 显示"思考中"提示
        rvChat.post {
            messageAdapter.addMessage("思考中", SENDER_ASSISTANT)
            scrollToBottom()
        }

        // 调用AI接口获取回复
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 网络请求在IO线程执行
                val aiResponse = retrofitClient.sendMessageToAI(messageText)

                // 更新UI在主线程执行
                withContext(Dispatchers.Main) {
                    // 移除"思考中"提示并添加实际回复
                    messageAdapter.removeMessage()
                    messageAdapter.addMessage(aiResponse, SENDER_ASSISTANT)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI消息发送失败", e)
                withContext(Dispatchers.Main) {
                    messageAdapter.removeMessage()
                    messageAdapter.addMessage("抱歉，请求失败，请稍后再试", SENDER_ASSISTANT)
                    scrollToBottom()
                }
            }
        }
    }

    /**
     * 滚动到最新消息位置
     */
    private fun scrollToBottom() {
        rvChat.smoothScrollToPosition(messageAdapter.itemCount - 1)
    }

    /**
     * 快捷按钮点击回调
     */
    override fun onButtonClick(buttonText: String) {
        // 添加快捷按钮消息
        messageAdapter.addMessage(buttonText, SENDER_USER)
        scrollToBottom()

        // 显示"思考中"提示
        rvChat.post {
            messageAdapter.addMessage("思考中", SENDER_ASSISTANT)
            scrollToBottom()
        }

        // 调用AI接口获取回复
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val aiResponse = retrofitClient.sendMessageToAI(buttonText)

                withContext(Dispatchers.Main) {
                    messageAdapter.removeMessage()
                    messageAdapter.addMessage(aiResponse, SENDER_ASSISTANT)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Log.e(TAG, "快捷按钮消息发送失败", e)
                withContext(Dispatchers.Main) {
                    messageAdapter.removeMessage()
                    messageAdapter.addMessage("抱歉，请求失败，请稍后再试", SENDER_ASSISTANT)
                    scrollToBottom()
                }
            }
        }
    }
}
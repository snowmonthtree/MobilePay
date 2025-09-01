package com.example.myapplicationw.ui.adapter

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationw.Interfaces.ChatListener
import com.example.myapplicationw.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 消息列表适配器
 * 功能：管理聊天消息数据、适配不同发送方（用户/助手）的UI布局、处理快捷按钮交互
 */
class MessageAdapter(
    private val chatListener: ChatListener // 快捷按钮点击回调接口
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    // 数据集合（命名规范：明确数据含义，避免歧义）
    private val messageContentList: MutableList<String> = mutableListOf() // 消息内容列表
    private val messageSenderList: MutableList<Int> = mutableListOf()    // 消息发送方列表（1-用户，0-助手）
    private val isLoadingList: MutableList<Boolean> = mutableListOf()    // 加载状态标记列表（暂未使用，保留扩展性）

    // 常量定义（集中管理，便于维护和统一修改）
    companion object {
        const val SENDER_USER = 1                 // 消息发送方：用户
        const val SENDER_ASSISTANT = 0            // 消息发送方：智能助手
        private const val LOADING_MSG = "思考中"   // 加载中提示文本（与Activity保持一致）
        private const val TIME_FORMAT = "HH:mm"    // 时间显示格式
        private const val BUBBLE_PADDING = 16      // 消息气泡内边距（dp）
    }

    /**
     * 消息视图持有者
     * 职责：缓存item布局中的控件，避免重复findViewById
     */
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 控件命名规范：类型前缀+功能描述（明确归属和用途）
        val ivAssistantAvatar: ImageView = itemView.findViewById(R.id.iv_assistant_sender) // 助手头像
        val ivUserAvatar: ImageView = itemView.findViewById(R.id.iv_user_sender)           // 用户头像
        val tvMessageContent: TextView = itemView.findViewById(R.id.assistant_message)     // 消息内容文本
        val tvMessageTime: TextView = itemView.findViewById(R.id.tv_time)                  // 消息时间文本
        val llQuickBtnContainer: LinearLayout = itemView.findViewById(R.id.quick_btn)       // 快捷按钮容器
        val llMessageBubble: LinearLayout = itemView.findViewById(R.id.bubble)              // 消息气泡容器
        val llMessageRoot: LinearLayout = itemView.findViewById(R.id.message_bubble)        // 消息根布局
        // 快捷按钮（命名明确按钮功能）
        val llQuickBtnConsumption: LinearLayout = itemView.findViewById(R.id.iv_consumption) // 本月消费按钮
        val llQuickBtnAnalysis: LinearLayout = itemView.findViewById(R.id.iv_analysis)      // 消费分析按钮
        val llQuickBtnFinance: LinearLayout = itemView.findViewById(R.id.iv_finance)        // 理财建议按钮
    }

    /**
     * 创建ViewHolder（加载item布局，初始化控件缓存）
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(itemView)
    }

    /**
     * 绑定ViewHolder（为item设置数据和UI样式，处理交互逻辑）
     */
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // 边界校验：避免数组越界（增强代码健壮性）
        if (position >= messageContentList.size || position >= messageSenderList.size) {
            Log.e("MessageAdapter", "onBindViewHolder: 位置$position 超出数据集合范围")
            return
        }

        // 获取当前item数据
        val currentMsg = messageContentList[position]
        val currentSender = messageSenderList[position]

        // 1. 设置消息内容和时间
        holder.tvMessageContent.text = currentMsg
        holder.tvMessageTime.text = getCurrentTime() // 获取当前时间（实际应存储消息发送时间，此处暂用当前时间）

        // 2. 处理快捷按钮显示逻辑（仅第一条消息显示快捷按钮）
        handleQuickButtonDisplay(holder, position)

        // 3. 根据发送方设置UI布局（用户-居右，助手-居左）
        setMessageLayoutBySender(holder, currentSender)
    }

    /**
     * 处理快捷按钮显示与交互
     * @param holder ViewHolder实例
     * @param position 当前item位置
     */
    private fun handleQuickButtonDisplay(holder: MessageViewHolder, position: Int) {
        if (position == 0) {
            // 第一条消息：显示快捷按钮，并绑定点击事件
            holder.llQuickBtnContainer.visibility = View.VISIBLE
            holder.llQuickBtnConsumption.setOnClickListener {
                chatListener.onButtonClick("本月消费")
            }
            holder.llQuickBtnAnalysis.setOnClickListener {
                chatListener.onButtonClick("消费分析")
            }
            holder.llQuickBtnFinance.setOnClickListener {
                chatListener.onButtonClick("理财建议")
            }
        } else {
            // 非第一条消息：隐藏快捷按钮
            holder.llQuickBtnContainer.visibility = View.GONE
        }
    }

    /**
     * 根据发送方设置消息布局样式
     * @param holder ViewHolder实例
     * @param sender 发送方标识（SENDER_USER/SENDER_ASSISTANT）
     */
    private fun setMessageLayoutBySender(holder: MessageViewHolder, sender: Int) {
        val context = holder.itemView.context
        when (sender) {
            SENDER_USER -> {
                // 用户消息样式：居右、白色文字、用户气泡背景
                holder.ivUserAvatar.visibility = View.VISIBLE
                holder.ivAssistantAvatar.visibility = View.INVISIBLE
                holder.llMessageRoot.gravity = Gravity.RIGHT
                holder.tvMessageContent.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.llMessageBubble.setBackgroundResource(R.drawable.rectangle_bubble_user)
                holder.llMessageBubble.setPadding(BUBBLE_PADDING)
            }
            SENDER_ASSISTANT -> {
                // 助手消息样式：居左、默认文字色、助手气泡背景
                holder.ivUserAvatar.visibility = View.INVISIBLE
                holder.ivAssistantAvatar.visibility = View.VISIBLE
                holder.llMessageRoot.gravity = Gravity.LEFT
                holder.llMessageBubble.setBackgroundResource(R.drawable.rectangle_bubble_assistant)
                // 助手消息气泡内边距可根据设计调整，此处暂不单独设置（复用默认）
            }
        }
    }

    /**
     * 获取当前时间（格式：HH:mm）
     * @return 格式化后的时间字符串
     */
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * 获取列表项数量（RecyclerView必备方法）
     */
    override fun getItemCount(): Int = messageContentList.size

    /**
     * 添加单条消息
     * @param message 消息内容
     * @param sender 发送方标识（SENDER_USER/SENDER_ASSISTANT）
     */
    fun addMessage(message: String, sender: Int) {
        messageContentList.add(message)
        messageSenderList.add(sender)
        isLoadingList.add(false) // 默认为非加载状态
        // 通知RecyclerView插入新项（更新UI）
        notifyItemInserted(messageContentList.size - 1)
    }

    /**
     * 批量添加消息
     * @param messages 消息内容列表（默认发送方为助手，可根据需求扩展多发送方）
     */
    fun addMessages(messages: List<String>) {
        val startPosition = messageContentList.size
        messageContentList.addAll(messages)
        // 批量添加发送方（默认助手发送，若需多发送方需传入对应sender列表）
        messageSenderList.addAll(List(messages.size) { SENDER_ASSISTANT })
        isLoadingList.addAll(List(messages.size) { false })
        // 通知RecyclerView批量插入新项
        notifyItemRangeInserted(startPosition, messages.size)
    }

    /**
     * 移除最后一条消息（主要用于移除"思考中"加载提示）
     */
    fun removeLastMessage() {
        // 边界校验：避免集合为空时操作
        if (messageContentList.isEmpty() || messageSenderList.isEmpty()) {
            Log.d("MessageAdapter", "removeLastMessage: 消息列表为空，无需移除")
            return
        }

        val lastIndex = messageContentList.size - 1
        // 仅移除"思考中"提示（避免误删正常消息）
        if (messageContentList[lastIndex] == LOADING_MSG) {
            messageContentList.removeAt(lastIndex)
            messageSenderList.removeAt(lastIndex)
            isLoadingList.removeAt(lastIndex)
            // 通知RecyclerView移除最后一项
            notifyItemRemoved(lastIndex)
        } else {
            Log.d("MessageAdapter", "removeLastMessage: 最后一条消息非加载提示，无需移除")
        }
    }

    /**
     * 清空所有消息（扩展方法：如需重置聊天记录可调用）
     */
    fun clearAllMessages() {
        val oldSize = messageContentList.size
        messageContentList.clear()
        messageSenderList.clear()
        isLoadingList.clear()
        // 通知RecyclerView清空所有项
        notifyItemRangeRemoved(0, oldSize)
    }
}
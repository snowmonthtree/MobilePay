package com.example.myapplicationw.ui.adapter

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
 * 消息列表适配器，负责展示聊天消息
 */
class MessageAdapter(
    private val chatListener: ChatListener
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messageList: MutableList<String> = mutableListOf()
    private val senderList: MutableList<Int> = mutableListOf()
    private val loadingList: MutableList<Boolean> = mutableListOf()

    /**
     * 消息视图持有者
     */
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAssistantSender: ImageView = itemView.findViewById(R.id.iv_assistant_sender)
        val ivUserSender: ImageView = itemView.findViewById(R.id.iv_user_sender)
        val tvMessage: TextView = itemView.findViewById(R.id.assistant_message)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val llQuickBtn: LinearLayout = itemView.findViewById(R.id.quick_btn)
        val llBubble: LinearLayout = itemView.findViewById(R.id.bubble)
        val llMessage: LinearLayout = itemView.findViewById(R.id.message_bubble)
        val llQuickBtn1: LinearLayout = itemView.findViewById(R.id.iv_consumption)
        val llQuickBtn2: LinearLayout = itemView.findViewById(R.id.iv_analysis)
        val llQuickBtn3: LinearLayout = itemView.findViewById(R.id.iv_finance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        if (position >= messageList.size || position >= senderList.size) {
            return
        }

        val message = messageList[position]
        val sender = senderList[position]

        // 设置消息内容和时间
        holder.tvMessage.text = message
        holder.tvTime.text = getCurrentTime()

        // 控制快捷按钮显示/隐藏
        if (position > 0) {
            holder.llQuickBtn.visibility = View.GONE
        } else {
            holder.llQuickBtn1.setOnClickListener {
                chatListener.onButtonClick("本月消费")
            }
            holder.llQuickBtn2.setOnClickListener {
                chatListener.onButtonClick("消费分析")
            }
            holder.llQuickBtn3.setOnClickListener {
                chatListener.onButtonClick("理财建议")
            }
        }

        // 根据消息来源决定布局
        if (sender == 1) {
            // 用户消息：居右显示
            holder.ivUserSender.visibility = View.VISIBLE
            holder.ivAssistantSender.visibility = View.INVISIBLE
            holder.llMessage.gravity = Gravity.RIGHT
            holder.tvMessage.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.llBubble.setBackgroundResource(R.drawable.rectangle_bubble_user)
            holder.llBubble.setPadding(16)
        } else {
            // 助手消息：居左显示
            holder.ivUserSender.visibility = View.INVISIBLE
            holder.ivAssistantSender.visibility = View.VISIBLE
            holder.llMessage.gravity = Gravity.LEFT
            holder.llBubble.setBackgroundResource(R.drawable.rectangle_bubble_assistant)
        }
    }

    override fun getItemCount(): Int = messageList.size

    /**
     * 添加单条消息
     * @param message 消息内容
     * @param sender 发送者标识（1-用户，其他-助手）
     */
    fun addMessage(message: String, sender: Int) {
        messageList.add(message)
        senderList.add(sender)
        notifyItemInserted(messageList.size - 1)
    }

    /**
     * 批量添加消息
     * @param messages 消息列表
     */
    fun addMessages(messages: List<String>) {
        val startPos = messageList.size
        messageList.addAll(messages)
        notifyItemRangeInserted(startPos, messages.size)
    }

    /**
     * 移除最后一条消息
     */
    fun removeMessage() {
        if (messageList.isEmpty() || senderList.isEmpty()) {
            return
        }

        val lastIndex = messageList.size - 1
        if (messageList[lastIndex] == "思考中") {
            messageList.removeAt(lastIndex)
        }
        senderList.removeLastOrNull()
        notifyItemRemoved(lastIndex)
    }

    /**
     * 获取当前时间，格式为HH:mm
     */
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        const val SENDER_USER = 1  // 用户发送
        const val SENDER_ASSISTANT = 0  // 助手发送
    }
}

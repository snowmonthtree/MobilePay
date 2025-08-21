package com.example.myapplicationw.retrofit

import android.util.Log
import com.google.android.datatransport.BuildConfig
import com.volcengine.ark.runtime.model.bot.completion.chat.BotChatCompletionRequest
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole
import com.volcengine.ark.runtime.service.ArkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.ArrayList

/**
 * 网络请求工具类，负责与AI服务的通信
 */
object RetrofitClient {
    // 常量定义（遵循全大写+下划线命名规范）
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "https://ark.cn-beijing.volces.com/api/"
    private const val API_KEY = "9f574cd7-727b-4cae-af6f-7474f8d1a6b0"
    private const val AI_MODEL_BOT = "bot-20250713093511-9h2vk"

    // 对话消息列表（使用明确命名）
    private val chatMessages = ArrayList<ChatMessage>()

    // OkHttp客户端（懒加载，添加明确的初始化方法）
    private val okHttpClient by lazy {
        buildOkHttpClient()
    }

    // AI服务实例（重命名为arkService，明确含义）
    private val arkService by lazy {
        buildArkService()
    }

    // Retrofit实例（懒加载）
    private val retrofit by lazy {
        buildRetrofit()
    }

    /**
     * 构建OkHttp客户端
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        // 仅在DEBUG模式下添加日志拦截器
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, "HTTP请求日志: $message")
            }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }

        // 添加请求头拦截器
        builder.addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }

        return builder.build()
    }

    /**
     * 构建ArkService实例
     */
    private fun buildArkService(): ArkService {
        return ArkService.builder()
            // .baseUrl("https://ark.cn-beijing.volces.com/api/v3/bots") // 按需启用
            .apiKey(API_KEY)
            .build()
    }

    /**
     * 构建Retrofit实例
     */
    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 发送消息到AI服务
     * @param input 用户输入内容
     * @return AI返回的响应内容
     */
    suspend fun sendMessageToAI(input: String): String = withContext(Dispatchers.IO) {
        try {
            // 校验输入合法性
            if (input.isBlank()) {
                Log.w(TAG, "发送空消息，忽略请求")
                return@withContext "请输入有效消息"
            }

            // 添加用户消息（修正角色为USER）
            val userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(input)
                .build()
            chatMessages.add(userMessage)

            // 构建请求
            val chatRequest = BotChatCompletionRequest.builder()
                .model(AI_MODEL_BOT)
                .messages(chatMessages)
                .build()

            // 执行请求
            val response = arkService.createBotChatCompletion(chatRequest)

            // 处理响应结果
            val aiResponse = response.choices.firstOrNull()?.message?.content
            if (aiResponse==null) {
                Log.w(TAG, "AI返回空内容")
                "未获取到有效回复"
            } else {
                // 保存AI回复到对话历史
                val aiMessage = ChatMessage.builder()
                    .role(ChatMessageRole.ASSISTANT)
                    .content(aiResponse.toString())
                    .build()
                chatMessages.add(aiMessage)
                aiResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI请求失败", e)
            "请求失败: ${e.message ?: "未知错误"}"
        }.toString()
    }


}

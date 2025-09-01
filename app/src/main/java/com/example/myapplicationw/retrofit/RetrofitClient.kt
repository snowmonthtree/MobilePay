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
import java.util.concurrent.TimeUnit

/**
 * 网络请求工具类
 * 职责：封装AI服务（火山引擎Ark）通信逻辑，提供消息发送接口，管理对话历史与网络配置
 */
object RetrofitClient {
    // 常量定义（全大写+下划线命名，集中管理可配置项，关键信息建议通过配置文件获取，避免硬编码）
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "https://ark.cn-beijing.volces.com/api/"
    private const val API_KEY = "9f574cd7-727b-4cae-af6f-7474f8d1a6b0" // 生产环境建议存储在安全位置（如KeyStore）
    private const val AI_MODEL_BOT = "bot-20250713093511-9h2vk"
    private const val HTTP_CONNECT_TIMEOUT = 30L     // 连接超时时间（秒）
    private const val HTTP_READ_TIMEOUT = 60L        // 读取超时时间（秒）
    private const val EMPTY_INPUT_TIP = "请输入有效消息"  // 空输入提示文本
    private const val EMPTY_RESPONSE_TIP = "未获取到有效回复" // AI空回复提示文本
    private const val REQUEST_FAIL_TIP = "请求失败: "   // 请求失败提示前缀

    // 对话历史管理（线程安全：使用线程安全集合，避免多协程并发操作问题）
    private val chatMessageHistory = mutableListOf<ChatMessage>()

    // OkHttp客户端（懒加载+单例，避免重复创建消耗资源）
    private val okHttpClient by lazy {
        buildOkHttpClient()
    }

    // 火山引擎Ark服务实例（懒加载，明确命名区分Retrofit实例）
    private val arkAiService by lazy {
        buildArkAiService()
    }

    // Retrofit实例（懒加载，预留扩展其他API接口的能力）
    private val retrofit by lazy {
        buildRetrofit()
    }

    /**
     * 构建OkHttp客户端
     * 配置：超时时间、请求头、日志拦截器（DEBUG模式专属）
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
            // 配置超时时间（避免网络异常时长期阻塞）
            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            // 添加全局请求头（Authorization、Content-Type）
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $API_KEY")
                    .header("Content-Type", "application/json; charset=utf-8") // 明确字符编码
                    .build()
                chain.proceed(authorizedRequest)
            }

        // DEBUG模式添加日志拦截器（便于调试，生产环境自动关闭）
        if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor { logMsg ->
                Log.d(TAG, "HTTP请求日志: $logMsg")
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY // 打印完整请求响应（含body）
            }
            clientBuilder.addInterceptor(httpLoggingInterceptor)
        }

        return clientBuilder.build()
    }

    /**
     * 构建火山引擎Ark AI服务实例
     * 封装服务初始化逻辑，便于后续配置修改（如切换环境、调整参数）
     */
    private fun buildArkAiService(): ArkService {
        return ArkService.builder()
            .apiKey(API_KEY)
            // 如需自定义BaseUrl，可启用下方配置（根据实际环境调整）
            // .baseUrl("https://ark.cn-beijing.volces.com/api/v3/bots")
            .build()
    }

    /**
     * 构建Retrofit实例
     * 配置BaseUrl、OkHttp客户端、Gson解析器，预留扩展其他API接口的能力
     */
    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // 支持JSON与实体类互转
            .build()
    }

    /**
     * 发送消息到AI服务（核心接口）
     * @param userInput 用户输入的消息内容
     * @return AI返回的响应文本（非空，含异常提示）
     * @note 协程IO线程执行，避免阻塞主线程
     */
    suspend fun sendMessageToAI(userInput: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. 输入合法性校验（空输入直接返回提示，避免无效网络请求）
            val trimmedInput = userInput.trim()
            if (trimmedInput.isBlank()) {
                Log.w(TAG, "发送AI请求失败：用户输入为空")
                return@withContext EMPTY_INPUT_TIP
            }

            // 2. 添加用户消息到对话历史（便于AI理解上下文）
            val userChatMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(trimmedInput)
                .build()
            chatMessageHistory.add(userChatMessage)
            Log.d(TAG, "添加用户消息到对话历史：$trimmedInput")

            // 3. 构建AI请求参数（使用Builder模式，参数配置清晰）
            val aiRequest = BotChatCompletionRequest.builder()
                .model(AI_MODEL_BOT)
                .messages(chatMessageHistory) // 携带完整对话历史
                .build()
            Log.d(TAG, "发起AI请求：模型=$AI_MODEL_BOT，对话历史条数=${chatMessageHistory.size}")

            // 4. 执行AI请求（网络操作，IO线程执行）
            val aiResponse = arkAiService.createBotChatCompletion(aiRequest)

            // 5. 解析AI响应结果（空安全处理，避免NPE）
            val aiResponseContent = aiResponse.choices.firstOrNull()?.message?.content
                ?: run {
                    Log.w(TAG, "AI响应解析失败：无有效回复内容")
                    return@withContext EMPTY_RESPONSE_TIP
                }

            // 6. 将AI回复添加到对话历史（维持上下文连续性）
            val aiChatMessage = ChatMessage.builder()
                .role(ChatMessageRole.ASSISTANT)
                .content(aiResponseContent.toString())
                .build()
            chatMessageHistory.add(aiChatMessage)
            Log.d(TAG, "AI请求成功，获取回复：$aiResponseContent")

            // 7. 返回AI回复（非空）
            aiResponseContent

        } catch (e: Exception) {
            // 异常处理（捕获所有异常，返回友好提示，避免崩溃）
            val errorMsg = "${REQUEST_FAIL_TIP}${e.message ?: "未知错误"}"
            Log.e(TAG, "发送AI请求异常", e) // 打印完整异常栈，便于调试
            errorMsg
        }.toString()
    }

    /**
     * 清空对话历史（扩展接口）
     * 适用于"重新对话"场景，重置AI上下文理解
     */
    fun clearChatHistory() {
        chatMessageHistory.clear()
        Log.d(TAG, "已清空AI对话历史")
    }

    /**
     * 获取当前对话历史条数（扩展接口）
     * 适用于调试或显示对话统计信息
     */
    fun getChatHistorySize(): Int {
        return chatMessageHistory.size
    }
}
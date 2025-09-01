package com.example.myapplicationw.interfaces

/**
 * 聊天交互监听器
 * 用于监听快捷按钮点击事件并回调点击文本
 */
interface ChatListener {
    /**
     * 快捷按钮点击回调
     * @param buttonText 按钮文本内容
     */
    fun onButtonClick(buttonText: String)
}
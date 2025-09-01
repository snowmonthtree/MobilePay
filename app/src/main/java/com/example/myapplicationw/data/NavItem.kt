// 数据模型：存储单个导航项的配置
package com.example.myapplicationw.data

data class NavItem(
    val iconResId: Int,         // 图标资源ID
    val textResId: Int,         // 文字资源ID
    val index: Int              // 导航项索引（用于回调区分）
)
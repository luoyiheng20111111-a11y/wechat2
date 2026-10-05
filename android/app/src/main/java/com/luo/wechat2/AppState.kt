package com.luo.wechat2

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 应用是否在前台（用于判断后台收到消息时要不要发通知）
object AppState {

    private val _foreground = MutableStateFlow(false)
    val foreground: StateFlow<Boolean> = _foreground

    fun setForeground(value: Boolean) {
        _foreground.value = value
    }

    fun isForeground(): Boolean = _foreground.value
}

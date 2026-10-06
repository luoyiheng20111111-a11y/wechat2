package com.luo.wechat2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WeChatGreen = Color(0xFF34C759)

val GroupedBackground = Color(0xFFF2F2F7)

val SystemGray5 = Color(0xFFE9E9EB)

val SystemGray6 = Color(0xFFF2F2F2)

val SecondaryText = Color.Black.copy(alpha = 0.6f)

val InactiveIcon = Color(0xFF8A8A8E)

// MARK: - 聊天页深色
val ChatBackgroundDark = Color(0xFF0D0D0D)   // 聊天背景（近黑，微信深色模式风格）
val ChatBarDark = Color(0xFF1C1C1E)          // 顶栏 / 输入栏
val BubbleIncomingDark = Color(0xFF2C2C2E)   // 对方气泡

private val WeChatColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = Color.White,
    background = GroupedBackground,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun WeChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WeChatColorScheme,
        content = content
    )
}

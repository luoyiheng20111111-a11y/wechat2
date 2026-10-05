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

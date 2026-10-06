package com.luo.wechat2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luo.wechat2.ProactiveMessageManager
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.network.AIService
import kotlinx.coroutines.delay

// MARK: - AI Settings Screen

@Composable
fun SettingsScreen(onBack: () -> Unit) {

    var apiKey by rememberSaveable { mutableStateOf(AppSettings.apiKey) }
    var chatNameText by rememberSaveable { mutableStateOf(AppSettings.chatDisplayName) }
    var userNameText by rememberSaveable { mutableStateOf(AppSettings.userDisplayName) }
    var minutesText by rememberSaveable {
        mutableStateOf(AppSettings.proactiveMinutes.toString())
    }
    var delayText by rememberSaveable {
        mutableStateOf(AppSettings.replyDelayMaxSeconds.toString())
    }
    var promptText by rememberSaveable {
        mutableStateOf(
            AppSettings.customSystemPrompt ?: AIService.defaultSystemPromptText
        )
    }
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            delay(1500)
            showSaved = false
        }
    }

    val onSave = {
        val cleanKey = apiKey.trim()
        val minutes = minutesText.toIntOrNull() ?: 30
        val delaySeconds = delayText.toIntOrNull() ?: AppSettings.replyDelayMaxSeconds

        AppSettings.apiKey = cleanKey
        AppSettings.proactiveMinutes = maxOf(1, minutes)
        AppSettings.replyDelayMaxSeconds = delaySeconds.coerceIn(0, 600)

        // 对方昵称：存储键按名字拼接，改名前先把聊天记录迁移到新名字
        val newChatName = chatNameText.trim().ifEmpty { AppSettings.chatDisplayName }
        if (newChatName != AppSettings.chatDisplayName) {
            ChatStorage.renameChat(AppSettings.chatDisplayName, newChatName)
        }
        AppSettings.chatDisplayName = newChatName
        AppSettings.userDisplayName = userNameText.trim()
            .ifEmpty { AppSettings.userDisplayName }

        chatNameText = AppSettings.chatDisplayName
        userNameText = AppSettings.userDisplayName

        minutesText = AppSettings.proactiveMinutes.toString()
        delayText = AppSettings.replyDelayMaxSeconds.toString()

        // 提示词：清空或改回默认内容 → 恢复内置提示词（与 iOS 一致）
        val trimmedPrompt = promptText.trim()
        val defaultPrompt = AIService.defaultSystemPromptText.trim()

        AppSettings.customSystemPrompt =
            if (trimmedPrompt.isEmpty() || trimmedPrompt == defaultPrompt) null
            else promptText

        promptText = AppSettings.customSystemPrompt
            ?: AIService.defaultSystemPromptText

        // 保存后立即按照新的时间重新计时（用改名后的名字）
        ProactiveMessageManager.resetTimer(chatName = AppSettings.chatDisplayName)

        showSaved = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBackground)
    ) {
        AppTopBar(
            title = "AI 设置",
            showBack = true,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(GroupedBackground)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle(text = "对话")

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    NameRow(
                        label = "对方昵称",
                        value = chatNameText,
                        onValueChange = { chatNameText = it }
                    )

                    NameRow(
                        label = "我的昵称",
                        value = userNameText,
                        onValueChange = { userNameText = it }
                    )

                    HintText(
                        text = "对方昵称显示在聊天列表、顶栏和通知里，同时作为 AI 的人设名字；" +
                            "我的昵称是 AI 对你的称呼。保存后聊天记录自动保留。"
                    )
                }
            }

            SectionTitle(text = "DeepSeek")

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    BasicTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "API Key 只保存在本机，不写入 GitHub 源码。",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            SectionTitle(text = "主动消息")

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    NumberRow(
                        label = "自动回复间隔",
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        unit = "分钟"
                    )

                    HintText(text = "例如填写 30，就是每 30 分钟检查一次主动消息。最少 1 分钟。")
                }
            }

            SectionTitle(text = "回复延迟")

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    NumberRow(
                        label = "最大延迟",
                        value = delayText,
                        onValueChange = { delayText = it.filter { c -> c.isDigit() } },
                        unit = "秒"
                    )

                    HintText(
                        text = "AI 回复前会在 0 到该秒数之间随机等待。0 表示立即回复，最多 600 秒。"
                    )
                }
            }

            SectionTitle(text = "提示词")

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    // 对应 iOS 的 TextEditor，固定高度、内部可滚动
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None
                            )
                        )
                    }

                    HintText(
                        text = "自定义 AI 的系统提示词，保存后立即生效。改回默认内容再保存，即可恢复内置提示词。"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSave)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showSaved) "已保存" else "保存设置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = WeChatGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// MARK: - Number Row

@Composable
private fun NumberRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(80.dp),
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = unit,
            fontSize = 16.sp,
            color = SecondaryText
        )
    }
}

// MARK: - Name Row

@Composable
private fun NameRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(140.dp),
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            ),
            singleLine = true
        )
    }
}

// MARK: - Hint Text

@Composable
private fun HintText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = SecondaryText,
            lineHeight = 17.sp
        )
    }
}

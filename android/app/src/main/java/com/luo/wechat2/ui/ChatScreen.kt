package com.luo.wechat2.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luo.wechat2.ProactiveMessageManager
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.ChatItem
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.data.Message
import com.luo.wechat2.network.AIService
import kotlin.random.Random

// MARK: - Chat Screen

@Composable
fun ChatScreen(chat: ChatItem, onBack: () -> Unit) {

    val messages = remember(chat.name) { mutableStateListOf<Message>() }
    var messageText by remember(chat.name) { mutableStateOf("") }
    var isActive by remember(chat.name) { mutableStateOf(true) }

    val storageVersion by ChatStorage.version.collectAsState()
    val listState = rememberLazyListState()
    val handler = remember { Handler(Looper.getMainLooper()) }

    // MARK: 发送消息

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isEmpty()) return

        messages.add(Message(text = text, isMe = true))
        ChatStorage.saveMessages(messages.toList(), chat.name)
        ChatStorage.saveLastMessage(text, chat.name)
        messageText = ""

        // 用户主动说话后，重新开始主动消息计时
        ProactiveMessageManager.resetTimer(chat.name)

        val maxDelay = AppSettings.replyDelayMaxSeconds
        val delay = if (maxDelay > 0) {
            Random.nextLong(0, maxDelay * 1000L + 1)
        } else {
            0L
        }

        handler.postDelayed({
            AIService.sendMessage(ChatStorage.loadMessages(chat.name)) { reply ->
                val updated = ChatStorage.loadMessages(chat.name).toMutableList()
                updated.add(Message(text = reply, isMe = false))

                ChatStorage.saveMessages(updated, chat.name)
                ChatStorage.saveLastMessage(reply, chat.name)

                if (!isActive) {
                    ChatStorage.addUnread(chat.name)
                }

                // AI 回复完成后，再重新开始计时
                ProactiveMessageManager.resetTimer(chat.name)
            }
        }, delay)
    }

    // MARK: 加载 / 刷新聊天记录

    LaunchedEffect(chat.name, storageVersion) {
        val saved = ChatStorage.loadMessages(chat.name)

        when {
            saved.isNotEmpty() -> {
                if (saved != messages.toList()) {
                    messages.clear()
                    messages.addAll(saved)
                }
                if (isActive) {
                    ChatStorage.markAsRead(chat.name)
                }
            }

            messages.isEmpty() -> {
                messages.add(Message(text = "你h", isMe = false))
                messages.add(Message(text = "你好！", isMe = true))
            }
        }
    }

    // MARK: 自动滚动到底部

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(chat.name) {
        isActive = true
        onDispose { isActive = false }
    }

    // MARK: UI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBackground)
    ) {
        AppTopBar(
            title = chat.name,
            showBack = true,
            onBack = onBack
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(GroupedBackground),
            contentPadding = PaddingValues(top = 15.dp, bottom = 8.dp)
        ) {
            items(
                count = messages.size,
                key = { messages[it].id }
            ) { index ->
                MessageRow(
                    message = messages[index],
                    avatar = chat.avatar
                )
            }
        }

        InputBar(
            text = messageText,
            onTextChange = { messageText = it },
            onSend = { sendMessage() }
        )
    }
}

// MARK: - Message Row

@Composable
private fun MessageRow(message: Message, avatar: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (message.isMe) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (!message.isMe) {
            ChatAvatar(avatar = avatar, size = 50.dp, corner = 10.dp)
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .background(
                        WeChatGreen.copy(alpha = 0.2f),
                        RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

// MARK: - Input Bar

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemGray6)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "语音",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp)
                .background(Color.Black, RoundedCornerShape(5.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp
            ),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.Filled.EmojiEmotions,
            contentDescription = "表情",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(WeChatGreen, RoundedCornerShape(5.dp))
                    .clickable(onClick = onSend)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "发送",
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        } else {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = "更多",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

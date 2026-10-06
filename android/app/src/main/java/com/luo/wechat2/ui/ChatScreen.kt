package com.luo.wechat2.ui

import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.luo.wechat2.AppState
import com.luo.wechat2.ProactiveMessageManager
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.ChatItem
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.data.Message
import com.luo.wechat2.network.AIService
import com.luo.wechat2.network.MiMoTTSService
import kotlin.random.Random

// MARK: - Chat Screen

@Composable
fun ChatScreen(chat: ChatItem, onBack: () -> Unit) {

    val messages = remember(chat.name) { mutableStateListOf<Message>() }
    var messageText by remember(chat.name) { mutableStateOf("") }
    var isActive by remember(chat.name) { mutableStateOf(true) }

    val storageVersion by ChatStorage.version.collectAsState()
    val appForeground by AppState.foreground.collectAsState()
    val listState = rememberLazyListState()
    val handler = remember { Handler(Looper.getMainLooper()) }

    // MARK: 语音播放（语音条点按播放 / 再点停止）

    var playingId by remember(chat.name) { mutableStateOf<String?>(null) }
    var mediaPlayer by remember(chat.name) { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        mediaPlayer?.let { mp ->
            try {
                mp.stop()
            } catch (ignored: Exception) {
            }
            try {
                mp.release()
            } catch (ignored: Exception) {
            }
        }
        mediaPlayer = null
        playingId = null
    }

    fun togglePlay(message: Message) {
        if (message.voicePath == null) return

        if (playingId == message.id) {
            stopPlayback()
            return
        }

        stopPlayback()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(message.voicePath)
            mp.setOnCompletionListener {
                try {
                    mp.release()
                } catch (ignored: Exception) {
                }
                if (playingId == message.id) {
                    mediaPlayer = null
                    playingId = null
                }
            }
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            playingId = message.id
        } catch (ignored: Exception) {
            playingId = null
        }
    }

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
            fun appendTextReply(reply: String) {
                val parts = if (AppSettings.splitReplies) splitReplyBubbles(reply)
                            else listOf(reply)

                // 逐条落地，模拟真人连发；未读/通知只在第一条触发一次
                fun appendBubble(index: Int) {
                    val updated = ChatStorage.loadMessages(chat.name).toMutableList()
                    updated.add(Message(text = parts[index], isMe = false))

                    ChatStorage.saveMessages(updated, chat.name)
                    ChatStorage.saveLastMessage(parts[index], chat.name)

                    if (index == 0) {
                        // 不在会话里（iOS 原逻辑）或 App 已退到后台 → 记未读
                        if (!isActive || !appForeground) {
                            ChatStorage.addUnread(chat.name)
                        }

                        // App 不在前台时直接发通知：后台也能收到消息（显示整条回复）
                        if (!appForeground) {
                            ProactiveMessageManager.showReplyNotification(chat.name, reply)
                        }
                    }

                    if (index == parts.lastIndex) {
                        // AI 回复全部落地后，再重新开始计时
                        ProactiveMessageManager.resetTimer(chat.name)
                    } else {
                        handler.postDelayed(
                            { appendBubble(index + 1) },
                            Random.nextLong(600L, 1501L)
                        )
                    }
                }

                appendBubble(0)
            }

            // 语音条消息：text 仍保留（列表预览 / 通知 / AI 上下文），界面只显示语音条
            fun appendVoiceMessage(message: Message) {
                val updated = ChatStorage.loadMessages(chat.name).toMutableList()
                updated.add(message)

                ChatStorage.saveMessages(updated, chat.name)
                ChatStorage.saveLastMessage(message.text, chat.name)

                if (!isActive || !appForeground) {
                    ChatStorage.addUnread(chat.name)
                }

                if (!appForeground) {
                    ProactiveMessageManager.showReplyNotification(chat.name, message.text)
                }

                ProactiveMessageManager.resetTimer(chat.name)
            }

            AIService.sendMessage(ChatStorage.loadMessages(chat.name)) { reply ->
                if (MiMoTTSService.shouldSynthesize(reply)) {
                    // 后台合成，不阻塞 UI；失败自动回退成文字气泡
                    Thread {
                        val message = MiMoTTSService.synthesize(reply).getOrNull()
                            ?.let { MiMoTTSService.buildVoiceMessage(reply, it) }

                        handler.post {
                            if (message != null) {
                                appendVoiceMessage(message)
                            } else {
                                appendTextReply(reply)
                            }
                        }
                    }.start()
                } else {
                    appendTextReply(reply)
                }
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
                if (isActive && appForeground) {
                    ChatStorage.markAsRead(chat.name)
                }
            }

            messages.isEmpty() -> {
                messages.add(Message(text = "你h", isMe = false))
                messages.add(Message(text = "你好！", isMe = true))
            }
        }
    }

    // 对应 iOS onAppear：进入会话重新开始主动消息计时
    LaunchedEffect(chat.name) {
        ProactiveMessageManager.resetTimer(chat.name)
    }

    // MARK: 自动滚动到底部

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(chat.name) {
        isActive = true
        onDispose {
            isActive = false
            stopPlayback()
        }
    }

    // MARK: UI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackgroundDark)
    ) {
        AppTopBar(
            title = chat.name,
            showBack = true,
            onBack = onBack,
            dark = true
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(ChatBackgroundDark),
            contentPadding = PaddingValues(top = 15.dp, bottom = 8.dp)
        ) {
            items(
                count = messages.size,
                key = { messages[it].id }
            ) { index ->
                MessageRow(
                    message = messages[index],
                    avatar = chat.avatar,
                    isPlaying = playingId == messages[index].id,
                    onTogglePlay = { togglePlay(messages[index]) }
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

// MARK: - 回复拆分（New Bing 气泡机制：每句一个气泡，最多 4 条）

private fun splitReplyBubbles(reply: String): List<String> {
    val text = reply.trim()
    if (text.isEmpty()) return listOf(reply)

    // 1) 空行分块
    val blocks = text.split(Regex("\n{2,}"))
        .map { it.trim() }.filter { it.isNotEmpty() }

    val parts: List<String> = if (blocks.size > 1) {
        blocks
    } else {
        // 2) 换行分行
        val lines = text.split('\n')
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size > 1) {
            lines
        } else {
            // 3) 单行长文本按句号/叹号/问号/分号断句
            text.split(Regex("(?<=[。！？!?；;])\\s*"))
                .map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    val safe = parts.ifEmpty { listOf(text) }
    if (safe.size <= 4) return safe
    // 超过 4 条：前 3 条保留，其余并入第 4 条，防拆太碎
    return safe.take(3) + safe.drop(3).joinToString("\n")
}

// MARK: - Message Row

@Composable
private fun MessageRow(
    message: Message,
    avatar: String,
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {}
) {
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

            if (message.voicePath != null) {
                VoiceBubble(
                    duration = message.voiceDuration,
                    isPlaying = isPlaying,
                    onClick = onTogglePlay
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            BubbleIncomingDark,
                            RoundedCornerShape(5.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        } else {
            if (message.voicePath != null) {
                VoiceBubble(
                    duration = message.voiceDuration,
                    isPlaying = isPlaying,
                    onClick = onTogglePlay
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            WeChatGreen,
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
}

// MARK: - Voice Bubble（语音条：▶ + 秒数，点按播放 / 停止）

@Composable
private fun VoiceBubble(
    duration: Int,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(BubbleIncomingDark, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "停止" else "播放",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${duration.coerceIn(1, 600)}″",
                fontSize = 15.sp,
                color = Color.White
            )
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
            .background(ChatBarDark)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "语音",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp)
                .background(BubbleIncomingDark, RoundedCornerShape(5.dp))
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
            tint = Color.White,
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

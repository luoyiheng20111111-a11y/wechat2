package com.luo.wechat2.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luo.wechat2.data.AvatarImageStore
import com.luo.wechat2.data.ChatItem
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.data.defaultChatList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// MARK: - Main Screen

@Composable
fun MainScreen() {
    var openChatName by rememberSaveable { mutableStateOf("") }
    var openSettings by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }

    val chat = defaultChatList.firstOrNull { it.name == openChatName }

    BackHandler(enabled = openChatName.isNotEmpty() || openSettings) {
        if (openChatName.isNotEmpty()) {
            openChatName = ""
        } else {
            openSettings = false
        }
    }

    when {
        openChatName.isNotEmpty() && chat != null -> {
            ChatScreen(chat = chat, onBack = { openChatName = "" })
        }

        openSettings -> {
            SettingsScreen(onBack = { openSettings = false })
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GroupedBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (tab) {
                        0 -> ChatListScreen(onOpenChat = { openChatName = it })
                        1 -> PlaceholderTab(title = "通讯录")
                        2 -> PlaceholderTab(title = "发现")
                        else -> MeScreen(onOpenAiSettings = { openSettings = true })
                    }
                }

                BottomBar(selected = tab, onSelect = { tab = it })
            }
        }
    }
}

// MARK: - Bottom Bar

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        "微信" to Icons.AutoMirrored.Filled.Message,
        "通讯录" to Icons.Filled.People,
        "发现" to Icons.Filled.Explore,
        "我" to Icons.Filled.Person
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(56.dp)
                .padding(vertical = 6.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val color = if (index == selected) WeChatGreen else InactiveIcon

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.second,
                        contentDescription = tab.first,
                        tint = color,
                        modifier = Modifier.size(25.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.first,
                        fontSize = 10.sp,
                        color = color
                    )
                }
            }
        }
    }
}

// MARK: - Chat List Screen

@Composable
private fun ChatListScreen(onOpenChat: (String) -> Unit) {
    val storageVersion by ChatStorage.version.collectAsState()

    val chatList = remember(storageVersion) {
        defaultChatList.map { it.refresh() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "微信", actionIcon = Icons.Filled.Add)

        SearchBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                count = chatList.size,
                key = { chatList[it].name }
            ) { index ->
                ChatRow(item = chatList[index], onClick = { onOpenChat(chatList[index].name) })
            }
        }
    }
}

// MARK: - Chat Row

@Composable
private fun ChatRow(item: ChatItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ChatAvatar(avatar = item.avatar, size = 60.dp, corner = 10.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.name,
                fontSize = 17.sp,
                color = Color.Black,
                maxLines = 1
            )
            Text(
                text = item.lastMessage,
                fontSize = 14.sp,
                color = SecondaryText,
                maxLines = 1
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.time,
                fontSize = 11.sp,
                color = SecondaryText
            )

            if (item.unread > 0) {
                UnreadBadge(count = item.unread)
            }
        }
    }
}

// MARK: - Placeholder Tab

@Composable
private fun PlaceholderTab(title: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = title)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupedBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = SecondaryText
            )
        }
    }
}

// MARK: - Me Screen

@Composable
private fun MeScreen(onOpenAiSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 对应 iOS 的 PhotosPicker：选图 → 压缩 → 存为黑咲的聊天头像
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val jpeg = AvatarImageStore.resizedJpeg(context, uri)
                if (jpeg != null) {
                    ChatStorage.saveAvatar(jpeg, "administerphoto")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "我")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GroupedBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAiSettings)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = WeChatGreen,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI 设置",
                            fontSize = 17.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "API Key、自动回复时间",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )
                    }

                    Text(
                        text = ">",
                        fontSize = 16.sp,
                        color = Color(0xFFC7C7CC)
                    )
                }
            }

            SectionTitle(text = "聊天头像")

            SectionCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = WeChatGreen,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "更换聊天头像",
                                fontSize = 17.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "给 黑咲 上传一张照片",
                                fontSize = 13.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp)
                            .height(1.dp)
                            .background(Color.Black.copy(alpha = 0.06f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ChatStorage.removeAvatar("administerphoto")
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "恢复默认头像",
                            fontSize = 17.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

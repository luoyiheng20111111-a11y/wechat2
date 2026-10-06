package com.luo.wechat2.data

data class ChatItem(
    val avatar: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unread: Int
) {
    fun refresh(): ChatItem {
        val savedLastMessage = ChatStorage.lastMessage(name)
        val hasUnreadRecord = ChatStorage.hasUnreadRecord(name)
        val savedUnread = ChatStorage.unreadCount(name)

        return copy(
            lastMessage = savedLastMessage ?: lastMessage,
            unread = if (hasUnreadRecord) savedUnread else unread
        )
    }
}

// 动态取值：用户在设置里改了对方昵称后，列表/顶栏标题立即跟着变
val defaultChatList: List<ChatItem>
    get() = listOf(
        ChatItem(
            avatar = "administerphoto",
            name = AppSettings.chatDisplayName,
            lastMessage = "[图片]",
            time = "10:20",
            unread = 0
        )
    )

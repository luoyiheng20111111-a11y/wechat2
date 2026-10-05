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

val defaultChatList: List<ChatItem> = listOf(
    ChatItem(
        avatar = "administerphoto",
        name = "黑咲",
        lastMessage = "[图片]",
        time = "10:20",
        unread = 0
    )
)

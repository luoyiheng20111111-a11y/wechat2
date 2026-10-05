package com.luo.wechat2.data

import com.luo.wechat2.WeChatApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ChatStorage {

    // 对应 iOS 的 ChatStorage.didChangeNotification
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version

    private val prefs get() = WeChatApp.instance.prefs

    private fun lastMessageKey(chatName: String) = "chat_last_message_$chatName"

    private fun unreadKey(chatName: String) = "chat_unread_$chatName"

    private fun messagesKey(chatName: String) = "chat_messages_$chatName"

    // MARK: - Last Message

    fun lastMessage(chatName: String): String? =
        prefs.getString(lastMessageKey(chatName), null)

    fun saveLastMessage(text: String, chatName: String) {
        prefs.edit().putString(lastMessageKey(chatName), text).apply()
        notifyChange()
    }

    // MARK: - Unread

    fun unreadCount(chatName: String): Int =
        prefs.getInt(unreadKey(chatName), 0)

    fun hasUnreadRecord(chatName: String): Boolean =
        prefs.contains(unreadKey(chatName))

    fun addUnread(chatName: String) {
        prefs.edit().putInt(unreadKey(chatName), unreadCount(chatName) + 1).apply()
        notifyChange()
    }

    fun markAsRead(chatName: String) {
        if (unreadCount(chatName) == 0) return
        prefs.edit().putInt(unreadKey(chatName), 0).apply()
        notifyChange()
    }

    // MARK: - Messages

    fun loadMessages(chatName: String): List<Message> {
        val raw = prefs.getString(messagesKey(chatName), null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                Message(
                    id = if (id.isEmpty()) UUID.randomUUID().toString() else id,
                    text = item.optString("text"),
                    isMe = item.optBoolean("isMe", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMessages(messages: List<Message>, chatName: String) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("text", message.text)
                    .put("isMe", message.isMe)
            )
        }
        prefs.edit().putString(messagesKey(chatName), array.toString()).apply()
        notifyChange()
    }

    fun saveIncomingMessage(message: Message, chatName: String) {
        val messages = loadMessages(chatName).toMutableList()
        messages.add(message)
        saveMessages(messages, chatName)
        saveLastMessage(message.text, chatName)
        addUnread(chatName)
    }

    private fun notifyChange() {
        _version.value = _version.value + 1
    }
}

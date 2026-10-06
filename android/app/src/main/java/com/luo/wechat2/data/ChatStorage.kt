package com.luo.wechat2.data

import android.util.Base64
import com.luo.wechat2.WeChatApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ChatStorage {

    // 对应 iOS 的 ChatStorage.didChangeNotification
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version

    private val prefs get() = WeChatApp.instance.prefs

    private fun lastMessageKey(chatName: String) = "chat_last_message_$chatName"

    private fun unreadKey(chatName: String) = "chat_unread_$chatName"

    private fun messagesKey(chatName: String) = "chat_messages_$chatName"

    private fun avatarKey(chatName: String) = "chat_avatar_$chatName"

    // MARK: - Avatar
    // 对应 iOS 的 ChatStorage.avatarData/saveAvatar/removeAvatar
    // SharedPreferences 没有 byte[] 接口，这里用 Base64 字符串存

    fun avatarBase64(chatName: String): String? =
        prefs.getString(avatarKey(chatName), null)

    fun avatarData(chatName: String): ByteArray? {
        val raw = avatarBase64(chatName) ?: return null
        return try {
            Base64.decode(raw, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun saveAvatar(data: ByteArray, chatName: String) {
        prefs.edit()
            .putString(avatarKey(chatName), Base64.encodeToString(data, Base64.NO_WRAP))
            .apply()
        notifyChange()
    }

    fun removeAvatar(chatName: String) {
        prefs.edit().remove(avatarKey(chatName)).apply()
        notifyChange()
    }

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

    // 存储键按名字拼接，改名时整体迁移，保证聊天记录不丢
    // （头像键固定为 administerphoto，与名字无关，不需要迁移）
    @Synchronized
    fun renameChat(oldName: String, newName: String) {
        if (oldName.isEmpty() || newName.isEmpty() || oldName == newName) return

        val editor = prefs.edit()

        prefs.getString(messagesKey(oldName), null)?.let {
            editor.putString(messagesKey(newName), it)
        }
        editor.remove(messagesKey(oldName))

        prefs.getString(lastMessageKey(oldName), null)?.let {
            editor.putString(lastMessageKey(newName), it)
        }
        editor.remove(lastMessageKey(oldName))

        if (prefs.contains(unreadKey(oldName))) {
            editor.putInt(unreadKey(newName), prefs.getInt(unreadKey(oldName), 0))
        }
        editor.remove(unreadKey(oldName))

        editor.apply()
        notifyChange()
    }

    @Synchronized
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

    private fun messagesToJson(messages: List<Message>): JSONArray {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("text", message.text)
                    .put("isMe", message.isMe)
            )
        }
        return array
    }

    @Synchronized
    fun saveMessages(messages: List<Message>, chatName: String) {
        prefs.edit()
            .putString(messagesKey(chatName), messagesToJson(messages).toString())
            .apply()
        notifyChange()
    }

    @Synchronized
    fun saveIncomingMessage(message: Message, chatName: String) {
        val messages = loadMessages(chatName).toMutableList()
        messages.add(message)
        saveMessages(messages, chatName)
        saveLastMessage(message.text, chatName)
        addUnread(chatName)
    }

    // MARK: - 导出 / 导入聊天记录

    // 导出为 JSON 文件（含消息、最后一条预览、头像）
    @Synchronized
    fun exportChat(chatName: String): String {
        val json = JSONObject()
            .put("format", "wechat2-chat-export")
            .put("version", 1)
            .put("chatName", chatName)
            .put(
                "exportedAt",
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
            .put("messages", messagesToJson(loadMessages(chatName)))
            .put("lastMessage", lastMessage(chatName) ?: "")

        avatarBase64(chatName)?.let { json.put("avatar", it) }

        return try {
            json.toString(2)
        } catch (e: Exception) {
            json.toString()
        }
    }

    // 导入：追加到当前记录末尾。先整体解析校验，全部通过才落盘，失败不动现有数据。
    // 返回追加的条数；失败返回原因。
    @Synchronized
    fun importChat(chatName: String, jsonText: String): Result<Int> {
        val (imported, exportedAvatar) = try {
            val root = JSONObject(jsonText)

            if (root.optString("format") != "wechat2-chat-export") {
                return Result.failure(Exception("不是本应用导出的聊天记录文件"))
            }

            val array = root.optJSONArray("messages")
                ?: return Result.failure(Exception("文件里没有消息记录"))
            if (array.length() == 0) {
                return Result.failure(Exception("文件里没有消息记录"))
            }

            val messages = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Message(
                    // 重新生成 id：避免重复导入同一文件时 LazyColumn key 冲突导致崩溃
                    id = UUID.randomUUID().toString(),
                    text = item.optString("text"),
                    isMe = item.optBoolean("isMe", false)
                )
            }
            messages to root.optString("avatar")
        } catch (e: Exception) {
            return Result.failure(Exception("文件格式不正确，无法解析"))
        }

        // 全部解析成功后才开始写入
        saveMessages(loadMessages(chatName) + imported, chatName)
        imported.lastOrNull()?.let { saveLastMessage(it.text, chatName) }

        // 头像：导出文件里有、本地没有时恢复；已有则不覆盖
        if (avatarBase64(chatName) == null && exportedAvatar.isNotEmpty()) {
            prefs.edit().putString(avatarKey(chatName), exportedAvatar).apply()
            notifyChange()
        }

        return Result.success(imported.size)
    }

    private fun notifyChange() {
        _version.value = _version.value + 1
    }
}

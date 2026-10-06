package com.luo.wechat2.data

import com.luo.wechat2.WeChatApp

object AppSettings {

    private const val API_KEY = "deepseek_api_key"
    private const val PROACTIVE_MINUTES = "proactive_minutes"
    private const val REPLY_DELAY_MAX = "reply_delay_max_seconds"
    private const val CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"

    // 对话双方昵称（对应 iOS 端预留；为空时回退到默认值）
    private const val CHAT_DISPLAY_NAME = "chat_display_name"
    private const val USER_DISPLAY_NAME = "user_display_name"

    private const val DEFAULT_CHAT_NAME = "黑咲"
    private const val DEFAULT_USER_NAME = "罗以恒"

    private val prefs get() = WeChatApp.instance.prefs

    // MARK: - DeepSeek API Key

    var apiKey: String
        get() = prefs.getString(API_KEY, "") ?: ""
        set(value) {
            prefs.edit().putString(API_KEY, value).apply()
        }

    // MARK: - Proactive Message Interval

    var proactiveMinutes: Int
        get() {
            val value = prefs.getInt(PROACTIVE_MINUTES, 0)
            return if (value > 0) value else 30
        }
        set(value) {
            prefs.edit().putInt(PROACTIVE_MINUTES, maxOf(1, value)).apply()
        }

    // MARK: - AI Reply Delay

    var replyDelayMaxSeconds: Int
        get() {
            if (!prefs.contains(REPLY_DELAY_MAX)) return 3
            val value = prefs.getInt(REPLY_DELAY_MAX, 3)
            return if (value in 0..600) value else 3
        }
        set(value) {
            prefs.edit().putInt(REPLY_DELAY_MAX, value.coerceIn(0, 600)).apply()
        }

    // MARK: - Custom System Prompt

    var customSystemPrompt: String?
        get() {
            val value = prefs.getString(CUSTOM_SYSTEM_PROMPT, null) ?: return null
            return if (value.trim().isEmpty()) null else value
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(CUSTOM_SYSTEM_PROMPT).apply()
            } else {
                prefs.edit().putString(CUSTOM_SYSTEM_PROMPT, value).apply()
            }
        }

    // MARK: - 对方昵称（聊天列表 / 顶栏 / 通知标题 / 提示词人设名）

    var chatDisplayName: String
        get() = prefs.getString(CHAT_DISPLAY_NAME, null)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_CHAT_NAME
        set(value) {
            val clean = value.trim()
            if (clean.isEmpty() || clean == chatDisplayName) return
            prefs.edit().putString(CHAT_DISPLAY_NAME, clean).apply()
        }

    // MARK: - 我的昵称（提示词里 AI 对用户的称呼）

    var userDisplayName: String
        get() = prefs.getString(USER_DISPLAY_NAME, null)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_USER_NAME
        set(value) {
            val clean = value.trim()
            if (clean.isEmpty() || clean == userDisplayName) return
            prefs.edit().putString(USER_DISPLAY_NAME, clean).apply()
        }
}

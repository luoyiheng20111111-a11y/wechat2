package com.luo.wechat2

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.data.Message
import com.luo.wechat2.network.AIService

object ProactiveMessageManager {

    private const val CHANNEL_ID = "proactive_messages"
    private const val NOTIFICATION_ID = 1001

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var isGenerating = false

    var currentChatName: String = "luo"
        private set

    // MARK: - Start

    fun start(chatName: String) {
        currentChatName = chatName
        createNotificationChannel()
        resetTimer(chatName)
    }

    // MARK: - Reset Timer

    fun resetTimer(chatName: String) {
        currentChatName = chatName

        pending?.let { handler.removeCallbacks(it) }
        pending = null

        val minutes = AppSettings.proactiveMinutes
        val delay = minutes * 60_000L

        val task = Runnable { sendProactiveMessage(chatName) }
        pending = task
        handler.postDelayed(task, delay)
    }

    // MARK: - Generate Proactive Message

    private fun sendProactiveMessage(chatName: String) {
        if (isGenerating) return

        isGenerating = true

        val messages = ChatStorage.loadMessages(chatName)

        AIService.sendProactiveMessage(messages) { reply ->
            isGenerating = false

            val message = Message(text = reply, isMe = false)
            ChatStorage.saveIncomingMessage(message, chatName)

            showNotification(chatName, reply)

            // 开始下一轮计时
            resetTimer(chatName)
        }
    }

    // MARK: - Notification

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "主动消息",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = WeChatApp.instance
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(chatName: String, message: String) {
        createNotificationChannel()

        val context = WeChatApp.instance

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(chatName)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

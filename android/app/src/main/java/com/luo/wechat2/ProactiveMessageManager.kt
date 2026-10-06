package com.luo.wechat2

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.ChatStorage
import com.luo.wechat2.data.Message
import com.luo.wechat2.network.AIService
import java.util.concurrent.atomic.AtomicBoolean

// 对应 iOS 的 ProactiveMessageManager
// 后台可靠计时：不再用 Handler.postDelayed（进程被杀 / Doze 冻结后会失效），
// 改成 AlarmManager.setExactAndAllowWhileIdle + 广播，闹钟由系统负责准时拉起进程
object ProactiveMessageManager {

    private const val CHANNEL_ID = "proactive_messages"
    private const val PROACTIVE_NOTIFICATION_ID = 1001
    private const val REPLY_NOTIFICATION_ID = 1002
    private const val REQUEST_CODE = 2001

    private val isGenerating = AtomicBoolean(false)

    // 进程被杀后由广播拉起时会用默认值（App 只有一个会话，与 iOS 行为一致）
    var currentChatName: String = AppSettings.chatDisplayName
        private set

    // MARK: - Start

    fun start(chatName: String) {
        currentChatName = chatName
        createNotificationChannel()
        resetTimer(chatName)
    }

    // MARK: - Reset Timer（AlarmManager 调度，进程被杀 / Doze 下依然会触发）

    fun resetTimer(chatName: String) {
        currentChatName = chatName

        val context = WeChatApp.instance
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmPendingIntent(context)

        alarmManager.cancel(pending)

        val delay = AppSettings.proactiveMinutes * 60_000L
        val triggerAt = System.currentTimeMillis() + delay

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else {
            // 用户没给“精确闹钟”权限时退化为非精确闹钟，消息会稍有延迟但不会丢
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ProactiveReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // MARK: - Alarm Fired（由 ProactiveReceiver 在后台线程调用）

    fun onAlarmFired() {
        val chatName = currentChatName

        // 先排好下一次计时，再做网络请求：
        // 即使本次请求期间进程被杀，下一轮闹钟也已经在系统里注册好了，链路不会断
        resetTimer(chatName)

        // 上一条还在生成时跳过本次（对应 iOS 的 isGenerating 保护）
        if (!isGenerating.compareAndSet(false, true)) return

        try {
            val messages = ChatStorage.loadMessages(chatName)
            val reply = AIService.sendProactiveMessageBlocking(messages)

            ChatStorage.saveIncomingMessage(
                Message(text = reply, isMe = false),
                chatName
            )

            showNotification(chatName, reply, PROACTIVE_NOTIFICATION_ID)
        } finally {
            isGenerating.set(false)
        }
    }

    // MARK: - Notification

    // 回复到达但 App 不在前台时（对应用户要求的“后台也能收到消息”）
    fun showReplyNotification(chatName: String, message: String) {
        showNotification(chatName, message, REPLY_NOTIFICATION_ID)
    }

    private fun showNotification(chatName: String, message: String, notificationId: Int) {
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

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(chatName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "主动消息",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = WeChatApp.instance
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

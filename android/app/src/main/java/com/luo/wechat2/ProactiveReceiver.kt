package com.luo.wechat2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

// 闹钟到点后由系统回调。进程可能刚被拉起，
// 用 goAsync + 后台线程 + WakeLock 保证能把消息发出去
class ProactiveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "wechat2:proactive_message"
        )
        wakeLock.acquire(3 * 60_000L)

        Thread {
            try {
                ProactiveMessageManager.onAlarmFired()
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }.start()
    }
}

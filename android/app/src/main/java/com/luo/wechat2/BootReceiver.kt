package com.luo.wechat2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 开机 / 应用升级后重新注册闹钟，
// 否则重启后后台主动消息计时会丢失（对应“后台仍能收到消息”的保障）
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ProactiveMessageManager.resetTimer(
                    ProactiveMessageManager.currentChatName
                )
            }
        }
    }
}

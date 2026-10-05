package com.luo.wechat2

import android.app.Application
import android.content.SharedPreferences

class WeChatApp : Application() {

    val prefs: SharedPreferences by lazy {
        getSharedPreferences("wechat_storage", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WeChatApp
            private set
    }
}

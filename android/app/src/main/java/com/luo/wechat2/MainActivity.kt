package com.luo.wechat2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.luo.wechat2.ui.MainScreen
import com.luo.wechat2.ui.WeChatTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            WeChatTheme {
                MainScreen()
            }
        }

        ProactiveMessageManager.start(chatName = "黑咲")
    }

    // 标记前后台：回复到达时据此决定加未读 / 发通知
    override fun onStart() {
        super.onStart()
        AppState.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        AppState.setForeground(false)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

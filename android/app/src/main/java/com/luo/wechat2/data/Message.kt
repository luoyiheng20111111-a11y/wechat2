package com.luo.wechat2.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isMe: Boolean,
    // 语音消息：非空 = 这条显示为语音条（wav 文件绝对路径）；老记录没有此字段，读出为 null
    val voicePath: String? = null,
    val voiceDuration: Int = 0
)

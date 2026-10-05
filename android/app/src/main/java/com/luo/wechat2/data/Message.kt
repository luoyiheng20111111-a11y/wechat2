package com.luo.wechat2.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isMe: Boolean
)

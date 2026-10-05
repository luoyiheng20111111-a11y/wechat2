package com.luo.wechat2.network

import android.os.Handler
import android.os.Looper
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AIService {

    // MARK: - AI 人设提示词

    private const val SYSTEM_PROMPT = """
你是一个名叫“廖苡诺”的女生，是德阳天立九年级三班的学生。

你正在通过微信和“罗以恒”聊天。

你是女生，说话有温度，语气甜甜的、自然、亲切。
你们是在微信上进行真实、日常的聊天。

回复规则：
- 像真实的人聊天，不要像 AI 助手、客服或百科全书。
- 回复要符合真实微信聊天的长度，切记不要过长。
- 普通聊天通常用 1～2 句话即可。
- 一句话能自然表达时，就不要写成好几句话。
- 除非事情确实需要解释，否则不要超过 3 句话。
- 不要为了显得有内容而主动扩写。
- 根据上下文自然回应，不要机械重复罗以恒的话。
- 可以自然使用“哈哈”“嗯嗯”“好呀”“诶”等口语表达，但不要滥用。
- 语气温柔、甜一点、有温度，但不要过度做作。
- 如果只是普通闲聊，就自然聊天。
- 如果罗以恒问问题，也要保持微信聊天的语气，不要突然变成正式说明。
- 不要主动说自己是 AI、语言模型或人工智能。
- 不要使用“作为一个 AI 助手”之类的表达。
- 不要在每次回复最后说“还有什么可以帮助你的吗”。
"""

    private const val PROACTIVE_PROMPT = """
你正在通过微信主动给“罗以恒”发一条消息。
请根据最近的聊天记录，自然地找一个适合主动开启的话题。
只输出聊天消息本身，不要解释原因。
像真实的微信聊天一样简短自然，通常 1～2 句话。
不要提到 AI、模型、提示词或系统。
"""

    private const val ENDPOINT = "https://api.deepseek.com/chat/completions"

    private const val MODEL = "deepseek-chat"

    private val mainHandler = Handler(Looper.getMainLooper())

    // MARK: - Send Message

    fun sendMessage(messages: List<Message>, completion: (String) -> Unit) {
        send(messages, SYSTEM_PROMPT, completion)
    }

    // MARK: - Proactive Message

    fun sendProactiveMessage(messages: List<Message>, completion: (String) -> Unit) {
        send(messages, SYSTEM_PROMPT + "\n\n" + PROACTIVE_PROMPT, completion)
    }

    // MARK: - Request

    private fun send(
        messages: List<Message>,
        systemPrompt: String,
        completion: (String) -> Unit
    ) {
        val apiKey = AppSettings.apiKey

        if (apiKey.isEmpty()) {
            mainHandler.post { completion("请先配置 DeepSeek API Key") }
            return
        }

        val history = messages.toList()

        Thread {
            val reply = try {
                request(apiKey, systemPrompt, history)
            } catch (e: IOException) {
                "网络请求失败，请检查网络连接"
            } catch (e: Exception) {
                "AI 返回的数据无法解析"
            }
            mainHandler.post { completion(reply) }
        }.start()
    }

    private fun request(
        apiKey: String,
        systemPrompt: String,
        messages: List<Message>
    ): String {
        val deepSeekMessages = JSONArray()
        deepSeekMessages.put(
            JSONObject().put("role", "system").put("content", systemPrompt)
        )
        messages.forEach { message ->
            deepSeekMessages.put(
                JSONObject()
                    .put("role", if (message.isMe) "user" else "assistant")
                    .put("content", message.text)
            )
        }

        val body = JSONObject()
            .put("model", MODEL)
            .put("messages", deepSeekMessages)
            .put("stream", false)

        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            if (statusCode != 200) {
                return "AI 请求失败，请检查 API Key 和网络"
            }

            val choices = JSONObject(responseText).getJSONArray("choices")
            if (choices.length() == 0) {
                return "AI 没有返回内容"
            }

            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content")

            return if (content.isEmpty()) "AI 没有返回内容" else content
        } finally {
            connection.disconnect()
        }
    }
}

package com.luo.wechat2.network

import android.util.Base64
import com.luo.wechat2.WeChatApp
import com.luo.wechat2.data.AppSettings
import com.luo.wechat2.data.Message
import com.luo.wechat2.data.VoiceSampleStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.random.Random

// 小米 MiMo TTS（音色克隆）：文本 → wav 音频
// POST https://api.xiaomimimo.com/v1/chat/completions，model mimo-v2.5-tts-voiceclone（限时免费）
object MiMoTTSService {

    private const val ENDPOINT = "https://api.xiaomimimo.com/v1/chat/completions"
    private const val MODEL = "mimo-v2.5-tts-voiceclone"

    // 防止超长回复拖慢合成
    private const val MAX_TEXT = 500

    // 24kHz 单声道 pcm16 → 每秒 48000 字节，wav 头按标准 44 字节算
    private const val BYTES_PER_SECOND = 48_000

    // 出错文案不去合成语音（照样回退文字气泡，但别浪费一次请求）
    private val ERROR_PREFIXES = listOf(
        "网络请求失败",
        "AI 请求失败",
        "请求失败"
    )

    // MARK: - 概率判定（所有 AI 文本统一入口：语音开关 → 就绪 → 错误跳过 → 随机 x%）

    fun shouldSynthesize(text: String): Boolean {
        if (AppSettings.voicePercent <= 0) return false
        if (!ready()) return false

        val clean = text.trim()
        if (clean.isEmpty()) return false
        if (ERROR_PREFIXES.any { clean.startsWith(it) }) return false

        return Random.nextInt(100) < AppSettings.voicePercent
    }

    // MARK: - 前置条件（是否具备合成能力）

    fun ready(): Boolean =
        AppSettings.mimoApiKey.isNotBlank() && VoiceSampleStore.hasSample()

    // MARK: - 合成

    fun synthesize(text: String): Result<ByteArray> {
        val apiKey = AppSettings.mimoApiKey.trim()
        if (apiKey.isEmpty()) {
            return Result.failure(Exception("未配置语音 Key"))
        }
        val voice = VoiceSampleStore.dataUri()
            ?: return Result.failure(Exception("未选择参考音色"))

        val body = JSONObject()
            .put("model", MODEL)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "user").put("content", ""))
                    .put(
                        JSONObject()
                            .put("role", "assistant")
                            .put("content", text.take(MAX_TEXT))
                    )
            )
            .put(
                "audio",
                JSONObject()
                    .put("format", "wav")
                    .put("voice", voice)
            )
            .put("stream", false)

        return try {
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 90_000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("api-key", apiKey)
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
                    return Result.failure(Exception("语音服务返回 $statusCode"))
                }

                val audioData = JSONObject(responseText)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optJSONObject("audio")
                    ?.optString("data")

                if (audioData.isNullOrEmpty()) {
                    return Result.failure(Exception("语音服务没有返回音频"))
                }

                Result.success(Base64.decode(audioData, Base64.NO_WRAP))
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络请求失败"))
        } catch (e: Exception) {
            Result.failure(Exception("语音数据解析失败"))
        }
    }

    // MARK: - 落盘

    // 把合成结果存成 wav 文件，返回带语音字段的消息；失败返回 null（调用方回退文字）
    fun buildVoiceMessage(text: String, wav: ByteArray): Message? {
        return try {
            val dir = File(WeChatApp.instance.filesDir, "voice")
            dir.mkdirs()
            val file = File(dir, "${UUID.randomUUID()}.wav")
            file.writeBytes(wav)

            Message(
                text = text,
                isMe = false,
                voicePath = file.absolutePath,
                voiceDuration = durationSeconds(wav.size)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun durationSeconds(wavBytes: Int): Int {
        val seconds = (wavBytes - 44) / BYTES_PER_SECOND
        return seconds.coerceIn(1, 600)
    }
}

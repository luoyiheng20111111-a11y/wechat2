package com.luo.wechat2.data

import android.net.Uri
import android.util.Base64
import com.luo.wechat2.WeChatApp
import java.io.File

// 参考音色样本（音色克隆用）：SAF 选 mp3/wav → 拷贝到 filesDir 持久保存
// 大小限制：base64 后不能超过 10MB → 原始文件按 7.5MB 卡（base64 膨胀 4/3）
object VoiceSampleStore {

    private const val NAME_KEY = "voice_sample_name"
    private const val MIME_KEY = "voice_sample_mime"
    private const val MAX_RAW_BYTES = 7_500_000L

    private val prefs get() = WeChatApp.instance.prefs

    private const val WAV_FILE = "voice_sample.wav"
    private const val MP3_FILE = "voice_sample.mp3"

    // MARK: - 保存（从 SAF Uri 导入）

    fun save(uri: Uri): Result<String> {
        val context = WeChatApp.instance
        val mime = context.contentResolver.getType(uri) ?: ""

        val isWav = mime.contains("wav")
        val isMp3 = mime.contains("mpeg") || mime.contains("mp3")
        if (!isWav && !isMp3) {
            return Result.failure(Exception("只支持 mp3 和 wav 格式"))
        }

        val tmp = File(context.filesDir, "voice_sample.tmp")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取文件"))
            input.use { ins ->
                tmp.outputStream().use { outs -> ins.copyTo(outs) }
            }

            if (tmp.length() > MAX_RAW_BYTES) {
                return Result.failure(Exception("文件太大，超过 7.5MB"))
            }

            // 清掉另一种格式的旧样本，只保留一份
            File(context.filesDir, WAV_FILE).delete()
            File(context.filesDir, MP3_FILE).delete()

            val target = File(context.filesDir, if (isWav) WAV_FILE else MP3_FILE)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }

            prefs.edit()
                .putString(NAME_KEY, target.name)
                .putString(MIME_KEY, if (isWav) "audio/wav" else "audio/mpeg")
                .apply()

            return Result.success(target.name)
        } catch (e: Exception) {
            return Result.failure(Exception("导入失败：${e.message ?: "未知错误"}"))
        } finally {
            tmp.delete()
        }
    }

    // MARK: - 查询

    fun hasSample(): Boolean {
        val name = prefs.getString(NAME_KEY, null) ?: return false
        return File(WeChatApp.instance.filesDir, name).exists()
    }

    fun label(): String? {
        val name = prefs.getString(NAME_KEY, null) ?: return null
        val file = File(WeChatApp.instance.filesDir, name)
        if (!file.exists()) return null
        return "$name（${file.length() / 1024}KB）"
    }

    // MARK: - data URI（给 MiMo audio.voice 用）

    fun dataUri(): String? {
        val name = prefs.getString(NAME_KEY, null) ?: return null
        val mime = prefs.getString(MIME_KEY, null) ?: return null
        val file = File(WeChatApp.instance.filesDir, name)
        if (!file.exists()) return null

        val bytes = file.readBytes()
        val prefix = if (mime.contains("wav")) "data:audio/wav;base64," else "data:audio/mpeg;base64,"
        return prefix + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

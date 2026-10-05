package com.luo.wechat2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

// 对应 iOS 的 AvatarImageStore.resizedJPEG：
// 最长边 512、JPEG 质量 0.85，并按 EXIF 方向摆正（防止竖拍照片旋转 90°）
object AvatarImageStore {

    private const val MAX_DIMENSION = 512
    private const val JPEG_QUALITY = 85

    fun resizedJpeg(context: Context, uri: Uri): ByteArray? {
        return try {
            // 第一遍：只读尺寸，算采样率，避免大图直接解码 OOM
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = open(context, uri)
                ?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null

            val oriented = applyExifOrientation(context, uri, decoded)
            val scaled = scaleDown(oriented, MAX_DIMENSION)

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun open(context: Context, uri: Uri) =
        context.contentResolver.openInputStream(uri)

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / longest
        val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            open(context, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}

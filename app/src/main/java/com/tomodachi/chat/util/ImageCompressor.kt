package com.tomodachi.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * يضغط صورة الستيكر المختارة من المعرض تلقائياً: يعيد تحجيمها إلى حد أقصى
 * 512x512 بكسل ويضبط جودة JPEG بشكل تنازلي حتى يصبح حجمها أقل من 300KB،
 * لتوفير استهلاك بيانات المستخدمين الآخرين عند تحميل الستيكر.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 512
    private const val TARGET_MAX_BYTES = 300 * 1024

    fun compress(context: Context, uri: Uri): ByteArray? {
        val original = decodeSampledBitmap(context, uri) ?: return null
        val scaled = scaleDown(original)

        var quality = 90
        var output = toJpegBytes(scaled, quality)
        while (output.size > TARGET_MAX_BYTES && quality > 30) {
            quality -= 10
            output = toJpegBytes(scaled, quality)
        }
        return output
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        return inputStream.use { BitmapFactory.decodeStream(it) }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun toJpegBytes(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}

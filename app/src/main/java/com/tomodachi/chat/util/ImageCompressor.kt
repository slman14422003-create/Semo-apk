package com.tomodachi.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * يضغط الصور المختارة من المعرض تلقائياً: يعيد تحجيمها إلى حد أقصى للأبعاد
 * ويضبط جودة JPEG بشكل تنازلي حتى يصبح حجمها أقل من الحد المطلوب، لتوفير
 * استهلاك بيانات المستخدمين الآخرين وحجم وثائق Firestore.
 */
object ImageCompressor {

    // حدود الستيكرات (تُرفع إلى Firebase Storage كملف، فتتحمّل حجماً أكبر قليلاً)
    private const val STICKER_MAX_DIMENSION = 512
    private const val STICKER_TARGET_MAX_BYTES = 300 * 1024

    // حدود صورة الملف الشخصي (تُخزَّن كنص Base64 داخل وثيقة المستخدم نفسها على
    // Firestore، ووثيقة Firestore الواحدة محدودة بـ 1MB، لذا نبقيها صغيرة جداً).
    private const val PROFILE_MAX_DIMENSION = 480
    private const val PROFILE_TARGET_MAX_BYTES = 160 * 1024

    /** ضغط صورة ستيكر — يُستخدم مع StickerRepository (رفع إلى Firebase Storage). */
    fun compress(context: Context, uri: Uri): ByteArray? =
        compressInternal(context, uri, STICKER_MAX_DIMENSION, STICKER_TARGET_MAX_BYTES)

    /** ضغط صورة الملف الشخصي إلى حجم صغير جداً مناسب للتخزين كـ Base64 في Firestore. */
    fun compressForProfile(context: Context, uri: Uri): ByteArray? =
        compressInternal(context, uri, PROFILE_MAX_DIMENSION, PROFILE_TARGET_MAX_BYTES)

    /** يحوّل بايتات الصورة المضغوطة إلى نص Base64 جاهز للتخزين المباشر في حقل نصّي. */
    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun compressInternal(context: Context, uri: Uri, maxDimension: Int, targetMaxBytes: Int): ByteArray? {
        val original = decodeSampledBitmap(context, uri) ?: return null
        val scaled = scaleDown(original, maxDimension)

        var quality = 90
        var output = toJpegBytes(scaled, quality)
        while (output.size > targetMaxBytes && quality > 30) {
            quality -= 10
            output = toJpegBytes(scaled, quality)
        }
        return output
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        return inputStream.use { BitmapFactory.decodeStream(it) }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
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

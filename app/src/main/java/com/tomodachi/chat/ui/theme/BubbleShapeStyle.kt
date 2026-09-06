package com.tomodachi.chat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * أشكال فقاعات الدردشة القابلة للاختيار من شاشة الإعدادات — ميزة جديدة لم تكن
 * موجودة سابقاً (كان الشكل ثابتاً دوماً على "حديث" فقط). تُخزَّن كنص بسيط في
 * DataStore عبر [id] وتُحمَّل عند بدء التطبيق.
 */
enum class BubbleShapeStyle(val id: String, val labelAr: String) {
    MODERN("modern", "حديث"),       // زوايا دائرية مع ذيل خفيف — الافتراضي الحالي
    ROUNDED("rounded", "دائري كامل"), // كبسولة مستديرة بالكامل من كل الجهات
    SHARP("sharp", "حاد"),          // زوايا شبه مربّعة، مظهر رسمي/مضغوط
    WHATSAPP("whatsapp", "ذيل واتساب"), // ذيل حاد في الزاوية السفلية بأسلوب واتساب
    IMESSAGE("imessage", "فقاعة iMessage"); // فقاعة بيضاوية ناعمة بأسلوب آيمِسج

    companion object {
        fun fromId(id: String): BubbleShapeStyle = entries.firstOrNull { it.id == id } ?: MODERN
    }
}

/** يبني شكل الفقاعة الفعلي حسب النمط المختار واتجاه الرسالة (مِنّي/من غيري). */
fun bubbleShapeFor(style: BubbleShapeStyle, isMine: Boolean): RoundedCornerShape {
    val full: Dp = 18.dp
    return when (style) {
        BubbleShapeStyle.MODERN -> RoundedCornerShape(
            topStart = full, topEnd = full,
            bottomStart = if (isMine) full else 4.dp,
            bottomEnd = if (isMine) 4.dp else full
        )
        BubbleShapeStyle.ROUNDED -> RoundedCornerShape(50)
        BubbleShapeStyle.SHARP -> RoundedCornerShape(
            topStart = 6.dp, topEnd = 6.dp,
            bottomStart = if (isMine) 6.dp else 2.dp,
            bottomEnd = if (isMine) 2.dp else 6.dp
        )
        BubbleShapeStyle.WHATSAPP -> RoundedCornerShape(
            topStart = 12.dp, topEnd = 12.dp,
            bottomStart = if (isMine) 12.dp else 0.dp,
            bottomEnd = if (isMine) 0.dp else 12.dp
        )
        BubbleShapeStyle.IMESSAGE -> RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isMine) 20.dp else 6.dp,
            bottomEnd = if (isMine) 6.dp else 20.dp
        )
    }
}

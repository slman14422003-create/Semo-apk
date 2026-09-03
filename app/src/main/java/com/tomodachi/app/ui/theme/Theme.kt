package com.tomodachi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1877F2)
val BrandDark = Color(0xFF1A1A2E)

private fun safeColor(hex: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    fallback
}

/**
 * ثيم Compose ديناميكي: الوضع (فاتح/داكن) ولون فقاعة الرسائل المُرسَلة/
 * المستلمة تُقرأ من ThemeSettings المحفوظة بمستند المستخدم بفايرستور -
 * تماماً كما كان js/themes.js يطبّقها كمتغيرات CSS.
 */
@Composable
fun TomodachiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    sentColorHex: String = "#0084FF",
    receivedColorHex: String = "#E4E6EB",
    content: @Composable () -> Unit
) {
    val sent = safeColor(sentColorHex, BrandBlue)
    val received = safeColor(receivedColorHex, Color(0xFFE4E6EB))

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = sent,
            secondary = received,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = sent,
            secondary = received,
            background = Color(0xFFF5F6FA),
            surface = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

/** ألوان فقاعات الرسائل الفعلية - منفصلة عن colorScheme العام حتى تبقى
 * قابلة للتخصيص الحر من المستخدم بغض النظر عن الوضع الفاتح/الداكن. */
data class BubbleColors(val sent: Color, val received: Color)

@Composable
fun rememberBubbleColors(sentHex: String, receivedHex: String): BubbleColors =
    BubbleColors(safeColor(sentHex, BrandBlue), safeColor(receivedHex, Color(0xFFE4E6EB)))

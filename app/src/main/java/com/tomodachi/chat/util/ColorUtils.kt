package com.tomodachi.chat.util

import androidx.compose.ui.graphics.Color

/**
 * يحسب سطوع اللون المختار لفقاعة الرسائل ويقرر لون النص المناسب (أبيض/أسود)
 * تلقائياً، بحيث يبقى النص مقروءاً دائماً بغض النظر عن اللون المختار.
 */
fun readableTextColorFor(bubbleColor: Color): Color {
    val luminance = 0.299 * bubbleColor.red + 0.587 * bubbleColor.green + 0.114 * bubbleColor.blue
    return if (luminance > 0.6) Color.Black else Color.White
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFFFF6F61)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

val BUBBLE_COLOR_PALETTE = listOf(
    "#FF6F61", "#5DB7DE", "#8686F5", "#6BCB77",
    "#FFC75F", "#FF6B6B", "#4D96FF", "#B5B9FF",
    "#FFAF7A", "#00C2A8", "#D65DB1", "#2C3E50"
)

package com.tomodachi.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.ui.theme.BrandGradient

/**
 * أيقونة العلامة: مربّع بحواف دائرية بتدرّج ألوان مستوحى من انستقرام.
 */
@Composable
fun BrandMark(
    size: androidx.compose.ui.unit.Dp = 84.dp,
    emoji: String = "🎌",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(BrandGradient),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.42f).sp)
    }
}

/**
 * الاسم النصي للعلامة بتدرّج لوني، بأسلوب شعارات انستقرام النصية.
 */
@Composable
fun BrandWordmark(
    text: String = "Semo",
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            brush = Brush.linearGradient(BrandGradient),
            shadow = Shadow(color = Color.Black.copy(alpha = 0.05f), blurRadius = 2f)
        )
    )
}

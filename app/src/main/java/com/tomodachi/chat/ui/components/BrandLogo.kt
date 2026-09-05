package com.tomodachi.chat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.ui.theme.BrandGradientBlue
import com.tomodachi.chat.ui.theme.IconBlueBright

/**
 * أيقونة العلامة: مربّع بحواف دائرية بتدرّج أزرق مطابق تماماً لأيقونة التطبيق
 * الفعلية على الجهاز (mipmap/ic_launcher)، مع فقاعة دردشة بيضاء وثلاث نقاط
 * زرقاء بداخلها — نفس تكوين الأيقونة الحقيقية حرفياً، بدل رمز تعبيري عشوائي
 * لا علاقة له بهوية التطبيق البصرية.
 */
@Composable
fun BrandMark(
    size: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(BrandGradientBlue),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val h = this.size.height
            val bubbleHeight = h * 0.74f
            val cornerRadius = bubbleHeight * 0.42f

            // جسم فقاعة الدردشة البيضاء
            drawRoundRect(
                color = Color.White,
                topLeft = Offset.Zero,
                size = Size(w, bubbleHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // ذيل الفقاعة الصغير المشير للأسفل، بنفس موضع الأيقونة الأصلية
            val tailWidth = w * 0.17f
            val tailCenterX = w * 0.5f
            val tailPath = Path().apply {
                moveTo(tailCenterX - tailWidth / 2f, bubbleHeight - 1f)
                lineTo(tailCenterX + tailWidth / 2f, bubbleHeight - 1f)
                lineTo(tailCenterX, h)
                close()
            }
            drawPath(tailPath, color = Color.White)

            // النقاط الثلاث داخل الفقاعة
            val dotRadius = bubbleHeight * 0.1f
            val dotY = bubbleHeight * 0.5f
            val spacing = w * 0.26f
            listOf(-1, 0, 1).forEach { i ->
                drawCircle(
                    color = IconBlueBright,
                    radius = dotRadius,
                    center = Offset(tailCenterX + i * spacing, dotY)
                )
            }
        }
    }
}

/**
 * الاسم النصي للعلامة بتدرّج أزرق مطابق للون الأيقونة الفعلية.
 */
@Composable
fun BrandWordmark(
    text: String = "Tomodachi",
    fontSize: TextUnit = 34.sp,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            brush = Brush.linearGradient(BrandGradientBlue),
            shadow = Shadow(color = Color.Black.copy(alpha = 0.05f), blurRadius = 2f)
        )
    )
}

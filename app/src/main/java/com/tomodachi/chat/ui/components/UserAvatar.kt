package com.tomodachi.chat.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.ui.theme.WhatsAppGreen

/**
 * يحوّل نص صورة الملف الشخصي (Base64) إلى ImageBitmap قابل للعرض مباشرة،
 * أو null إن كانت فارغة/تالفة — عندها يُعرض الرمز التعبيري كبديل دائماً.
 */
@Composable
fun rememberProfileImageBitmap(base64: String?) = remember(base64) {
    if (base64.isNullOrBlank()) {
        null
    } else {
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

/**
 * صورة رمزية موحّدة تُستخدم في كل الواجهة (الدردشة، الملف الشخصي، لوحة الإدارة):
 * تعرض صورة الملف الشخصي الفعلية إن وُجدت، وإلا الرمز التعبيري فوق حلقة بتدرّج
 * العلامة (بأسلوب "قصص" انستقرام)، مع نقطة خضراء اختيارية بأسلوب واتساب للدلالة
 * على أن المستخدم متصل الآن.
 */
@Composable
fun UserAvatar(
    avatarEmoji: String,
    profileImageBase64: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    showGradientRing: Boolean = true,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val imageBitmap = rememberProfileImageBitmap(profileImageBase64)

    Box(modifier = modifier.size(size)) {
        var circleModifier: Modifier = Modifier.size(size).clip(CircleShape)
        if (showGradientRing) {
            circleModifier = circleModifier
                .background(Brush.sweepGradient(BrandGradient))
                .padding(size * 0.05f)
                .clip(CircleShape)
        }
        circleModifier = circleModifier.background(MaterialTheme.colorScheme.surfaceVariant)
        if (onClick != null) {
            circleModifier = circleModifier.clickable(onClick = onClick)
        }

        Box(modifier = circleModifier, contentAlignment = Alignment.Center) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "الصورة الشخصية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(if (showGradientRing) size * 0.9f else size).clip(CircleShape)
                )
            } else {
                Text(avatarEmoji, fontSize = (size.value * 0.42f).sp)
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.22f)
                        .clip(CircleShape)
                        .background(WhatsAppGreen)
                )
            }
        }
    }
}

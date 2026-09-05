package com.tomodachi.chat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(typingUsers: List<String>) {
    AnimatedVisibility(visible = typingUsers.isNotEmpty()) {
        val label = when {
            typingUsers.isEmpty() -> ""
            typingUsers.size == 1 -> "${typingUsers.first()} يكتب الآن"
            else -> "عدة أشخاص يكتبون الآن"
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            TypingDots(modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(modifier = modifier) {
        repeat(3) { index ->
            val delayMillis = index * 150
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = delayMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(4.dp)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

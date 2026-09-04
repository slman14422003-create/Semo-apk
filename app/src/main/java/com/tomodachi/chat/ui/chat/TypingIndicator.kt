package com.tomodachi.chat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(typingUsers: List<String>) {
    AnimatedVisibility(visible = typingUsers.isNotEmpty()) {
        val label = when {
            typingUsers.isEmpty() -> ""
            typingUsers.size == 1 -> "${typingUsers.first()} يكتب الآن…"
            else -> "عدة أشخاص يكتبون الآن…"
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

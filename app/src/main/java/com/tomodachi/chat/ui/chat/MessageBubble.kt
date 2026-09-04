package com.tomodachi.chat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageStatus
import com.tomodachi.chat.data.model.MessageType
import com.tomodachi.chat.util.parseHexColor
import com.tomodachi.chat.util.readableTextColorFor

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isAdmin: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onReact: (String) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val bubbleColor = parseHexColor(message.bubbleColorHex)
    val textColor = readableTextColorFor(bubbleColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                "${message.senderAvatarEmoji} ${message.senderUsername}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(if (message.isDeleted) MaterialTheme.colorScheme.surfaceVariant else bubbleColor)
                .combinedClickable(onClick = { showActions = true }, onLongClick = { showActions = true })
                .padding(10.dp)
        ) {
            Column {
                if (message.replyToMessageId.isNotBlank() && !message.isDeleted) {
                    Column(
                        modifier = Modifier
                            .background((if (isMine) textColor else bubbleColor).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            message.replyToUsername,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            message.replyToPreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                when {
                    message.isDeleted -> Text(
                        "تم حذف هذه الرسالة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    message.type == MessageType.STICKER -> Text("🖼️ ${message.text.ifBlank { "ستيكر" }}", color = textColor)
                    else -> Text(message.text, color = textColor)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (message.isEdited && !message.isDeleted) {
                        Text(
                            "(معدَّل)",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                    if (message.status == MessageStatus.SENDING) {
                        Spacer(Modifier.width(4.dp))
                        Text("… يُرسل", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                    }
                    if (message.status == MessageStatus.FAILED) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onRetry, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "إعادة المحاولة", tint = textColor)
                        }
                        Text("فشل الإرسال", style = MaterialTheme.typography.labelSmall, color = textColor)
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty() && !message.isDeleted) {
            Row(modifier = Modifier.padding(top = 2.dp)) {
                message.reactions.values.filter { it.count > 0 }.forEach { reaction ->
                    AssistChip(
                        onClick = { onReact(reaction.emoji) },
                        label = { Text("${reaction.emoji} ${reaction.count}", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }

    if (showActions) {
        MessageActionsSheet(
            message = message,
            isMine = isMine,
            isAdmin = isAdmin,
            onDismiss = { showActions = false },
            onReply = { showActions = false; onReply() },
            onEdit = { showActions = false; onEdit() },
            onDelete = { showActions = false; onDelete() },
            onReact = { emoji -> showActions = false; onReact(emoji) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageActionsSheet(
    message: Message,
    isMine: Boolean,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Message.DEFAULT_REACTIONS.forEach { emoji ->
                    Text(
                        emoji,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(onClick = { onReact(emoji) })
                            .padding(4.dp)
                    )
                }
            }
            HorizontalDivider()
            if (!message.isDeleted) {
                ListItem(headlineContent = { Text("رد") }, modifier = Modifier.combinedClickable(onClick = onReply))
                if (isMine || isAdmin) {
                    if (message.type == MessageType.TEXT) {
                        ListItem(headlineContent = { Text("تعديل") }, modifier = Modifier.combinedClickable(onClick = onEdit))
                    }
                    ListItem(
                        headlineContent = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.combinedClickable(onClick = onDelete)
                    )
                }
            }
        }
    }
}

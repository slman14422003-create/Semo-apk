package com.tomodachi.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.data.ChatMessage
import com.tomodachi.app.data.MessageStatus
import com.tomodachi.app.ui.theme.BubbleColors
import java.text.SimpleDateFormat
import java.util.Locale

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    canModify: Boolean, // نفس تحقق owns أو isAdmin بدالتي edit/deleteMessage الأصليتين
    myUsername: String,
    bubbleColors: BubbleColors,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onToggleReaction: (String) -> Unit
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (isMine) bubbleColors.sent else bubbleColors.received
    val textColor = if (isMine) Color.White else Color.Black

    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .pointerInput(message.id) {
                detectHorizontalDragGestures { _, dragAmount ->
                    // سحب بسيط لتفعيل الرد - يقابل setupSwipeToReply() الأصلي
                    if (dragAmount > 20f) onReply()
                }
            },
        horizontalAlignment = alignment
    ) {
        if (!isMine) {
            Text(
                message.username,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 1.dp)
            )
        }

        Box {
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
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = { if (message.status == MessageStatus.FAILED) onRetry() },
                        onLongClick = { showMenu = true }
                    )
                    .padding(10.dp)
            ) {
                Column {
                    if (message.replyTo != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                message.replyToUser ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                message.replyText ?: "",
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (message.sticker) {
                        StickerContent(message.stickerData)
                    } else {
                        Text(message.text, color = textColor, fontSize = 15.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        if (message.edited) {
                            Text("معدَّل · ", fontSize = 9.sp, color = textColor.copy(alpha = 0.6f))
                        }
                        Text(
                            formatTime(message.timestamp),
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        when (message.status) {
                            MessageStatus.SENDING -> Text(" ⏳", fontSize = 9.sp)
                            MessageStatus.FAILED -> Text(" ⚠️", fontSize = 9.sp)
                            else -> {}
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("رد") }, onClick = { showMenu = false; onReply() })
                DropdownMenuItem(text = { Text("تفاعل") }, onClick = { showMenu = false; showReactionPicker = true })
                if (canModify && !message.sticker) {
                    DropdownMenuItem(text = { Text("تعديل") }, onClick = { showMenu = false; showEditDialog = true })
                }
                if (canModify) {
                    DropdownMenuItem(text = { Text("حذف") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            ReactionsRow(message.reactions, myUsername) { emoji -> onToggleReaction(emoji) }
        }
    }

    if (showReactionPicker) {
        AlertDialog(
            onDismissRequest = { showReactionPicker = false },
            title = { Text("اختر تفاعلاً") },
            text = {
                Row {
                    QUICK_REACTIONS.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .padding(6.dp)
                                .clickable {
                                    onToggleReaction(emoji)
                                    showReactionPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReactionPicker = false }) { Text("إغلاق") } }
        )
    }

    if (showEditDialog) {
        var editText by remember { mutableStateOf(message.text) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("تعديل الرسالة") },
            text = {
                OutlinedTextField(value = editText, onValueChange = { editText = it })
            },
            confirmButton = {
                TextButton(onClick = { onEdit(editText); showEditDialog = false }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ReactionsRow(reactions: Map<String, List<String>>, myUsername: String, onToggle: (String) -> Unit) {
    LazyRow(modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)) {
        items(reactions.entries.toList()) { (emoji, users) ->
            val mine = users.contains(myUsername)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onToggle(emoji) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(emoji, fontSize = 12.sp)
                    if (users.size > 1) {
                        Text(" ${users.size}", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerContent(data: String?) {
    if (data == null) { Text("🎨"); return }
    if (data.startsWith("data:image")) {
        val bitmap = remember(data) { decodeBase64Image(data) }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "sticker",
                modifier = Modifier.size(96.dp)
            )
        } else {
            Text("🎨")
        }
    } else {
        Text(data, fontSize = 40.sp)
    }
}

private fun decodeBase64Image(dataUrl: String): android.graphics.Bitmap? = try {
    val base64 = dataUrl.substringAfter(",")
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}

private fun formatTime(date: java.util.Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("HH:mm", Locale("ar")).format(date)
}

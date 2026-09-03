package com.tomodachi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.data.ChatMessage
import com.tomodachi.app.data.ChatUser
import com.tomodachi.app.data.Sticker
import com.tomodachi.app.ui.components.EmojiStickerSheet
import com.tomodachi.app.ui.components.MessageBubble
import com.tomodachi.app.ui.theme.rememberBubbleColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    user: ChatUser,
    myUid: String,
    messages: List<ChatMessage>,
    typingUsers: List<String>,
    stickers: List<Sticker>,
    favoriteStickerIds: Set<String>,
    recentEmojis: List<String>,
    replyingTo: ChatMessage?,
    onSendText: (String) -> Unit,
    onSendSticker: (Sticker) -> Unit,
    onTyping: (Boolean) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onCancelReply: () -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (ChatMessage) -> Unit,
    onEdit: (ChatMessage, String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onToggleFavoriteSticker: (String) -> Boolean,
    onUploadSticker: (String, String) -> Unit,
    onEmojiUsed: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val bubbleColors = rememberBubbleColors(user.themeSettings.sentColor, user.themeSettings.receivedColor)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Semo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        if (typingUsers.isNotEmpty()) {
                            Text(
                                "${typingUsers.joinToString("، ")} يكتب...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (user.isAdmin) {
                        IconButton(onClick = onOpenAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "لوحة الأدمن")
                        }
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Person, contentDescription = "الملف الشخصي")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                replyingTo?.let { reply ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("رد على ${reply.username}", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(reply.text, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = onCancelReply) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء الرد")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "إيموجي وستيكرات")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onTyping(it.isNotEmpty())
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اكتب رسالة...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSendText(text)
                                text = ""
                                onTyping(false)
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id.ifEmpty { it.clientId ?: it.hashCode().toString() } }) { msg ->
                MessageBubble(
                    message = msg,
                    isMine = msg.username == user.username,
                    canModify = user.isAdmin || msg.isOwnedBy(user.username, myUid),
                    myUsername = user.username,
                    bubbleColors = bubbleColors,
                    onReply = { onReply(msg) },
                    onDelete = { onDelete(msg.id) },
                    onRetry = { onRetry(msg) },
                    onEdit = { newText -> onEdit(msg, newText) },
                    onToggleReaction = { emoji -> onToggleReaction(msg.id, emoji) }
                )
            }
        }
    }

    if (showPicker) {
        EmojiStickerSheet(
            stickers = stickers,
            favoriteIds = favoriteStickerIds,
            recentEmojis = recentEmojis,
            isAdmin = user.isAdmin,
            onDismiss = { showPicker = false },
            onEmoji = { emoji -> text += emoji; onEmojiUsed(emoji) },
            onSticker = { onSendSticker(it) },
            onToggleFavorite = onToggleFavoriteSticker,
            onUploadSticker = onUploadSticker
        )
    }
}

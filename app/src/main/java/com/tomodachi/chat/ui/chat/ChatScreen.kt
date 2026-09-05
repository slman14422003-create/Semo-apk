package com.tomodachi.chat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.emoji.EmojiPickerSheet
import com.tomodachi.chat.ui.stickers.StickerPickerSheet
import com.tomodachi.chat.ui.theme.BrandGradient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: User,
    favoriteStickers: Set<String>,
    onToggleFavoriteSticker: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdmin: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(currentUser.usernameLower) { viewModel.start(currentUser) }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val typingUsers by viewModel.typingUsers.collectAsStateWithLifecycle()
    val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val bannedWarning by viewModel.bannedWordWarning.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var recentEmojis by remember { mutableStateOf(listOf<String>()) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    LaunchedEffect(bannedWarning) {
        if (bannedWarning != null) {
            snackbarHostState.showSnackbar("تم إيقافك مؤقتاً بسبب استخدام كلمة ممنوعة")
            viewModel.consumeBannedWordWarning()
        }
    }

    LaunchedEffect(editingMessage) {
        editingMessage?.let { inputText = it.text }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(BrandGradient))
                            .clickable(onClick = onOpenProfile),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentUser.avatarEmoji, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tomodachi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "الدردشة الجماعية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentUser.isAdmin) {
                        IconButton(onClick = onOpenAdmin) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = "لوحة التحكم",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.6.dp)
                TypingIndicator(typingUsers)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val activePreview = editingMessage ?: replyTarget
                if (activePreview != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (editingMessage != null) "تعديل رسالتك" else "رد على ${activePreview.senderUsername}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                activePreview.text.ifBlank { "🖼️ ستيكر" },
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            if (editingMessage != null) { viewModel.cancelEditing(); inputText = "" }
                            else viewModel.setReplyTarget(null)
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showStickerSheet = true }, modifier = Modifier.size(38.dp)) {
                        Text("🖼️", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(2.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmojiSheet = true }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Filled.EmojiEmotions,
                                contentDescription = "إيموجي",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.onInputChanged(it)
                            },
                            placeholder = { Text("اكتب رسالة…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 4
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val canSend = inputText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) Brush.linearGradient(BrandGradient)
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                            )
                            .clickable(enabled = canSend) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderUid == currentUser.uid,
                    isAdmin = currentUser.isAdmin,
                    onReply = { viewModel.setReplyTarget(message) },
                    onEdit = { viewModel.startEditing(message) },
                    onDelete = { viewModel.deleteMessage(message) },
                    onRetry = { viewModel.retrySend(message) },
                    onReact = { emoji ->
                        viewModel.toggleReaction(message, emoji)
                        recentEmojis = (listOf(emoji) + recentEmojis).distinct().take(30)
                    }
                )
            }
        }
    }

    if (showEmojiSheet) {
        EmojiPickerSheet(
            recentEmojis = recentEmojis,
            onEmojiSelected = { emoji ->
                inputText += emoji
                recentEmojis = (listOf(emoji) + recentEmojis).distinct().take(30)
            },
            onDismiss = { showEmojiSheet = false }
        )
    }

    if (showStickerSheet) {
        StickerPickerSheet(
            currentUserUid = currentUser.uid,
            currentUsername = currentUser.username,
            favoriteIds = favoriteStickers,
            onToggleFavorite = onToggleFavoriteSticker,
            onStickerSelected = { sticker ->
                showStickerSheet = false
                // يُرسل الستيكر كرسالة من نوع STICKER فعلياً (وليس كنص بديل)
                viewModel.sendSticker(sticker)
            },
            onDismiss = { showStickerSheet = false }
        )
    }
}

package com.tomodachi.chat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageType
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.emoji.EmojiPickerSheet
import com.tomodachi.chat.ui.stickers.StickerPickerSheet
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("دردشة Tomodachi") },
                    actions = {
                        if (currentUser.isAdmin) {
                            IconButton(onClick = onOpenAdmin) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "لوحة التحكم")
                            }
                        }
                        IconButton(onClick = onOpenProfile) {
                            Icon(Icons.Filled.Person, contentDescription = "الملف الشخصي")
                        }
                    }
                )
                TypingIndicator(typingUsers)
            }
        },
        bottomBar = {
            Column {
                val activePreview = editingMessage ?: replyTarget
                if (activePreview != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (editingMessage != null) "تعديل رسالتك" else "رد على ${activePreview.senderUsername}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(activePreview.text.ifBlank { "🖼️ ستيكر" }, maxLines = 1)
                        }
                        IconButton(onClick = {
                            if (editingMessage != null) { viewModel.cancelEditing(); inputText = "" }
                            else viewModel.setReplyTarget(null)
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEmojiSheet = true }) {
                        Icon(Icons.Filled.EmojiEmotions, contentDescription = "إيموجي")
                    }
                    IconButton(onClick = { showStickerSheet = true }) {
                        Text("🖼️")
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.onInputChanged(it)
                        },
                        placeholder = { Text("اكتب رسالة…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "إرسال")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
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
                val previewText = sticker.label.ifBlank { "ستيكر" }
                viewModel.sendMessage("[STICKER]$previewText") // نص احتياطي؛ نوع الرسالة الفعلي STICKER يُضبط أدناه
            },
            onDismiss = { showStickerSheet = false }
        )
    }
}

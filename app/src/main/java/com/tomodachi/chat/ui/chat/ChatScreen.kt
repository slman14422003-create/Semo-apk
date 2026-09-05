package com.tomodachi.chat.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.components.UserAvatar
import com.tomodachi.chat.ui.emoji.EmojiPickerSheet
import com.tomodachi.chat.ui.stickers.StickerPickerSheet
import com.tomodachi.chat.ui.theme.BrandGradient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        avatarEmoji = currentUser.avatarEmoji,
                        profileImageBase64 = currentUser.profileImageBase64,
                        size = 42.dp,
                        showGradientRing = true,
                        isOnline = currentUser.isOnline,
                        onClick = onOpenProfile
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenProfile)) {
                        Text(
                            "Tomodachi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedContent(targetState = typingUsers.isNotEmpty(), label = "subtitle") { isTyping ->
                            if (isTyping) {
                                TypingIndicator(typingUsers)
                            } else {
                                Text(
                                    "الدردشة الجماعية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (currentUser.isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            IconButton(onClick = onOpenAdmin) {
                                Icon(
                                    Icons.Filled.AdminPanelSettings,
                                    contentDescription = "لوحة التحكم",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                // خط تدرّج رفيع بأسلوب انستقرام بدل الفاصل الرمادي التقليدي
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Brush.horizontalGradient(BrandGradient))
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .shadow(elevation = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                val activePreview = editingMessage ?: replyTarget
                if (activePreview == null) {
                    // خط تدرّج رفيع أعلى الشريط السفلي أيضاً، ليطابق الخط أعلى شريط
                    // الدردشة ويعطي إحساساً موحّداً بهوية العلامة من أعلى الشاشة لأسفلها.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Brush.horizontalGradient(BrandGradient))
                    )
                }
                AnimatedVisibility(visible = activePreview != null) {
                    if (activePreview != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.verticalGradient(BrandGradient))
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                if (editingMessage != null) Icons.Filled.EditNote else Icons.Filled.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
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
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(onClick = { showStickerSheet = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "الستيكرات",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(2.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
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
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 4
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val canSend = inputText.isNotBlank()
                    val sendScale by animateFloatAsState(if (canSend) 1f else 0.92f, label = "send_scale")
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .scale(sendScale)
                            .shadow(if (canSend) 4.dp else 0.dp, CircleShape)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp)
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

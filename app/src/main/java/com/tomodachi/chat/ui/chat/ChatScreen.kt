package com.tomodachi.chat.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.components.UserAvatar
import com.tomodachi.chat.ui.emoji.EmojiPickerSheet
import com.tomodachi.chat.ui.stickers.StickerPickerSheet
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.ui.theme.BubbleShapeStyle
import kotlinx.coroutines.launch

/**
 * شاشة الدردشة الرئيسية — تصميم مستوحى من أحدث واجهات إنستقرام للمحادثات
 * الجماعية: شريط علوي مسطّح بلمسة تدرّج خفيفة، خلفية محادثة نظيفة بلون واحد
 * (بدل التدرّج الثقيل سابقاً)، وشريط كتابة بأسلوب "pill" حديث.
 *
 * إصلاح الفراغ الأسود فوق لوحة المفاتيح:
 * المشكلة الفعلية لم تكن في هذا الملف فقط، بل في أن النشاط لم يكن يحمل
 * `android:windowSoftInputMode="adjustResize"` — بدونها يُعيد النظام رسم
 * النافذة بطريقة "pan" بينما تحاول Compose في الوقت نفسه إضافة حشوة IME
 * الخاصة بها، فينتج فراغان متراكبان بدل فراغ واحد صحيح. بعد إضافة
 * adjustResize في AndroidManifest، أصبح كافياً هنا استخدام
 * `navigationBarsPadding()` + `imePadding()` بأسلوب قياسي مباشر (بدل union
 * يدوي)، مع تصفير `contentWindowInsets` الخاصة بـ Scaffold حتى لا يُحتسب أي
 * جزء من الحشوة مرتين.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    currentUser: User,
    favoriteStickers: Set<String>,
    onToggleFavoriteSticker: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    bubbleShapeStyle: BubbleShapeStyle = BubbleShapeStyle.MODERN,
    fontScale: Float = 1.0f,
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
        // نُصفّر حشوة Scaffold الافتراضية (والتي تشمل safeDrawing، أي شريط
        // الحالة + شريط التنقل + IME) لأننا نتولى كل واحدة منها يدوياً بالضبط
        // حيث نحتاجها (statusBarsPadding في الأعلى، navigationBars+ime في
        // الأسفل) — احتسابها مرتين هو ما يُنتج فراغات فارغة غير متوقعة.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                currentUser = currentUser,
                typingUsers = typingUsers,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings,
                onOpenAdmin = onOpenAdmin
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputTextChange = {
                    inputText = it
                    viewModel.onInputChanged(it)
                },
                activePreviewMessage = editingMessage ?: replyTarget,
                isEditing = editingMessage != null,
                onCancelPreview = {
                    if (editingMessage != null) { viewModel.cancelEditing(); inputText = "" }
                    else viewModel.setReplyTarget(null)
                },
                onOpenStickers = { showStickerSheet = true },
                onOpenEmoji = { showEmojiSheet = true },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (messages.isEmpty()) {
                EmptyChatState()
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderUid == currentUser.uid,
                        isAdmin = currentUser.isAdmin,
                        bubbleShapeStyle = bubbleShapeStyle,
                        fontScale = fontScale,
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

/** شريط علوي مسطّح بأسلوب إنستقرام: صورة رمزية + اسم + حالة الكتابة، وخط تدرّج شعري. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun ChatTopBar(
    currentUser: User,
    typingUsers: List<String>,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarEmoji = currentUser.avatarEmoji,
                profileImageBase64 = currentUser.profileImageBase64,
                size = 40.dp,
                showGradientRing = true,
                isOnline = currentUser.isOnline,
                onClick = onOpenProfile
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenProfile)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // اسم المستخدم الحالي بدل اسم التطبيق الثابت — كل مستخدم
                    // يرى اسمه هو في أعلى شاشته، لا اسم "Tomodachi" العام.
                    Text(
                        currentUser.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.Filled.Groups,
                        contentDescription = "دردشة جماعية",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                AnimatedContent(targetState = typingUsers.isNotEmpty(), label = "subtitle") { isTyping ->
                    if (isTyping) {
                        TypingIndicator(typingUsers)
                    } else {
                        Text(
                            "نشِطون الآن",
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
                Spacer(Modifier.width(6.dp))
            }
            // زر الإعدادات الجديد — متاح لكل مستخدم عادي (وليس المسؤولين فقط)،
            // يفتح شاشة الإعدادات المستقلة (المظهر، شكل الفقاعات، تحديث التطبيق...).
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "الإعدادات",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        // خط تدرّج شعري بأسلوب إنستقرام بدل الفاصل الرمادي التقليدي — أنحف
        // ممّا كان سابقاً كي يبقى التصميم مسطّحاً وهادئاً بصرياً.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(Brush.horizontalGradient(BrandGradient))
        )
    }
}

/** حالة فارغة لطيفة تُعرض قبل وصول أول رسالة، بدل شاشة سوداء صامتة. */
@Composable
private fun EmptyChatState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(BrandGradient.map { it.copy(alpha = 0.14f) })),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "لا رسائل بعد",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "كن أول من يبدأ الحديث في المجموعة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * شريط الكتابة السفلي — يحمل معاينة الرد/التعديل واستمارة الإدخال، ويطبّق
 * حشوة لوحة المفاتيح وشريط التنقل بالطريقة القياسية الموصى بها من Compose:
 * `navigationBarsPadding()` ثم `imePadding()` كخطوتين منفصلتين ومباشرتين،
 * بدل union يدوي كان يسبب ازدواج الحشوة مع سلوك النافذة قبل adjustResize.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    activePreviewMessage: com.tomodachi.chat.data.model.Message?,
    isEditing: Boolean,
    onCancelPreview: () -> Unit,
    onOpenStickers: () -> Unit,
    onOpenEmoji: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 0.6.dp
        )

        AnimatedVisibility(
            visible = activePreviewMessage != null,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
        ) {
            if (activePreviewMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
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
                        if (isEditing) Icons.Filled.EditNote else Icons.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEditing) "تعديل رسالتك" else "رد على ${activePreviewMessage.senderUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            activePreviewMessage.text.ifBlank { "🖼️ ستيكر" },
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCancelPreview, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // شريط الإدخال بأسلوب إنستقرام بالكامل: أيقونة كاميرا دائرية أولاً، ثم
        // حقل الإدخال بشكل كبسولة يحوي زر الإيموجي داخله يميناً، ثم خارج
        // الكبسولة أزرار الستيكرات/المعرض، وأخيراً زر يتبدّل بين المايكروفون
        // (عند فراغ الحقل) وزر الإرسال المتدرّج (عند وجود نص) — بالضبط كما في
        // تطبيق إنستقرام الفعلي.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputRoundIconButton(
                icon = Icons.Filled.CameraAlt,
                contentDescription = "كاميرا",
                onClick = { /* لم تُفعَّل بعد — محجوزة لالتقاط صورة مباشرة */ }
            )

            Spacer(Modifier.width(6.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenEmoji, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.EmojiEmotions,
                        contentDescription = "إيموجي",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    TextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                "اكتب رسالة…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        maxLines = 4
                    )
                }
                // زر الستيكرات — داخل الكبسولة، ملاصق للطرف الآخر من حقل النص
                // تماماً كأزرار الملصقات/GIF داخل شريط إنستقرام الفعلي.
                IconButton(onClick = onOpenStickers, modifier = Modifier.size(36.dp)) {
                    Text("🎨", fontSize = 19.sp)
                }
            }

            Spacer(Modifier.width(6.dp))

            InputRoundIconButton(
                icon = Icons.Filled.Image,
                contentDescription = "المعرض",
                onClick = { /* لم تُفعَّل بعد — محجوزة لإرسال صورة من المعرض */ }
            )

            Spacer(Modifier.width(6.dp))

            val canSend = inputText.isNotBlank()
            val sendScale by animateFloatAsState(if (canSend) 1f else 0.88f, label = "send_scale")
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(sendScale)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Brush.linearGradient(BrandGradient)
                        else MaterialTheme.colorScheme.surfaceVariant.let { Brush.linearGradient(listOf(it, it)) }
                    )
                    .clickable(enabled = true) { if (canSend) onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (canSend) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "تسجيل صوتي",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** زر أيقونة دائري موحّد المقاس (44dp) لكل أزرار شريط الكتابة الجانبية،
 * بخلفية ناعمة ثابتة — هذا التوحيد هو ما يحل مشكلة عدم اتساق الشريط. */
@Composable
private fun InputRoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

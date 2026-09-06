package com.tomodachi.chat.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageStatus
import com.tomodachi.chat.data.model.MessageType
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.util.formatMessageTime
import com.tomodachi.chat.util.parseHexColor
import com.tomodachi.chat.util.readableTextColorFor
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val SWIPE_REPLY_THRESHOLD_DP = 56.dp
private val SWIPE_REPLY_MAX_DP = 84.dp

/** يبقي محتوى الـ Popup مثبّتاً في نقطة الأصل (0,0) للنافذة بأكملها، بدل أن
 * يحاول Compose وضعه ملاصقاً للعنصر المرساة عليه (السلوك الافتراضي) —
 * بهذا يمكننا التحكم يدوياً بموضع كل عنصر داخل القائمة العائمة استناداً
 * إلى إحداثيات الفقاعة الفعلية التي التقطناها مسبقاً بالنافذة كاملة. */
private object FullWindowPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

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
    // إحداثيات فقاعة الرسالة نفسها بالنسبة لنافذة التطبيق كاملة — تُلتَقط في كل
    // مرة تُعاد فيها القياسات (تمرير القائمة مثلاً)، وتُستخدَم فقط لحظة فتح
    // القائمة العائمة كي تظهر ملاصقة للفقاعة تماماً كما في إنستقرام.
    var bubbleBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    val bubbleColor = parseHexColor(message.bubbleColorHex)
    val textColor = readableTextColorFor(bubbleColor)
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp
    )

    // --- سحب أفقي للرد على الرسالة (بأي اتجاه)، على طراز واتساب/تيليجرام ---
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val dragScope = rememberCoroutineScope()
    var liveOffset by remember { mutableStateOf(0f) }
    val snapBack = remember { Animatable(0f) }
    val thresholdPx = with(density) { SWIPE_REPLY_THRESHOLD_DP.toPx() }
    val maxPx = with(density) { SWIPE_REPLY_MAX_DP.toPx() }
    var thresholdCrossed by remember { mutableStateOf(false) }
    var isSnapping by remember { mutableStateOf(false) }
    val currentOffset = if (isSnapping) snapBack.value else liveOffset
    val replyIconProgress = (currentOffset.absoluteValue / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id, message.isDeleted) {
                if (message.isDeleted) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { isSnapping = false },
                    onDragEnd = {
                        val crossed = liveOffset.absoluteValue >= thresholdPx
                        isSnapping = true
                        dragScope.launch {
                            snapBack.snapTo(liveOffset)
                            snapBack.animateTo(0f, tween(180))
                            isSnapping = false
                            liveOffset = 0f
                        }
                        if (crossed) onReply()
                        thresholdCrossed = false
                    },
                    onDragCancel = {
                        isSnapping = true
                        dragScope.launch {
                            snapBack.snapTo(liveOffset)
                            snapBack.animateTo(0f, tween(180))
                            isSnapping = false
                            liveOffset = 0f
                        }
                        thresholdCrossed = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        liveOffset = (liveOffset + dragAmount).coerceIn(-maxPx, maxPx)
                        val nowCrossed = liveOffset.absoluteValue >= thresholdPx
                        if (nowCrossed && !thresholdCrossed) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        thresholdCrossed = nowCrossed
                    }
                )
            },
        contentAlignment = if (currentOffset >= 0) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        if (replyIconProgress > 0f) {
            Icon(
                Icons.Filled.Reply,
                contentDescription = "رد",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = replyIconProgress),
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .size((16 + 10 * replyIconProgress).dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // صورة رمزية صغيرة بجانب رسائل الآخرين، بأسلوب مجموعات واتساب/تيليجرام
            if (!isMine) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(message.senderAvatarEmoji, fontSize = 14.sp)
                }
            }

            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (!isMine) {
                    Text(
                        message.senderUsername,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                    )
                }

                // نلتقط حدود الفقاعة الفعلية بالنسبة للنافذة هنا فقط (بدون اسم
                // المُرسل أو صف الإيموجي أسفلها) كي تظهر القائمة العائمة لاحقاً
                // ملاصقة تماماً لحواف الفقاعة كما في إنستقرام.
                Box(modifier = Modifier.onGloballyPositioned { bubbleBoundsInWindow = it.boundsInWindow() }) {
                    if (message.type == MessageType.STICKER && !message.isDeleted) {
                        // فقاعة ستيكر: صورة فعلية إن وُجد رابط، وإلا بطاقة بتدرّج العلامة كبديل
                        Box(
                            modifier = Modifier
                                .combinedClickable(onClick = { showActions = true }, onLongClick = { showActions = true })
                        ) {
                            if (message.stickerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = message.stickerUrl,
                                    contentDescription = message.text.ifBlank { "ستيكر" },
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(BrandGradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        message.text.ifBlank { "🖼️" },
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .shadow(
                                    elevation = if (message.isDeleted) 0.dp else 1.dp,
                                    shape = bubbleShape,
                                    clip = false
                                )
                                .clip(bubbleShape)
                                .background(if (message.isDeleted) MaterialTheme.colorScheme.surfaceVariant else bubbleColor)
                                .combinedClickable(onClick = { showActions = true }, onLongClick = { showActions = true })
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                if (message.replyToMessageId.isNotBlank() && !message.isDeleted) {
                                    Column(
                                        modifier = Modifier
                                            .background((if (isMine) textColor else bubbleColor).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(8.dp)
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

                                if (message.isDeleted) {
                                    Text(
                                        "تم حذف هذه الرسالة",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                } else {
                                    Text(message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    if (message.createdAtMillis > 0L && !message.isDeleted) {
                                        Text(
                                            formatMessageTime(message.createdAtMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (message.isEdited && !message.isDeleted) {
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "(معدَّل)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isMine && !message.isDeleted) {
                                        Spacer(Modifier.width(4.dp))
                                        MessageStatusTick(status = message.status, tint = textColor.copy(alpha = 0.75f))
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
                    }
                }

                if (message.reactions.isNotEmpty() && !message.isDeleted) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        message.reactions.values.filter { it.count > 0 }.forEach { reaction ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .shadow(1.dp, RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .combinedClickable(onClick = { onReact(reaction.emoji) })
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("${reaction.emoji} ${reaction.count}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActions) {
        MessageContextMenu(
            message = message,
            isMine = isMine,
            isAdmin = isAdmin,
            bubbleBoundsInWindow = bubbleBoundsInWindow,
            onDismiss = { showActions = false },
            onReply = { showActions = false; onReply() },
            onEdit = { showActions = false; onEdit() },
            onDelete = { showActions = false; onDelete() },
            onReact = { emoji -> showActions = false; onReact(emoji) }
        )
    }
}

/** أيقونات حالة الرسالة بأسلوب واتساب: ساعة (يُرسَل)، صح واحد (أُرسِل). */
@Composable
private fun MessageStatusTick(status: MessageStatus, tint: androidx.compose.ui.graphics.Color) {
    when (status) {
        MessageStatus.SENDING -> Icon(
            Icons.Filled.Schedule,
            contentDescription = "يُرسل",
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        MessageStatus.SENT -> Icon(
            Icons.Filled.Done,
            contentDescription = "أُرسل",
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        MessageStatus.FAILED -> {}
    }
}

/**
 * القائمة العائمة عند الضغط المطوّل — بأسلوب إنستقرام: خلفية معتّمة، صف
 * ردود أفعال سريعة ملاصق للفقاعة، وبطاقة إجراءات (رد/تعديل/نسخ/حذف) أسفله
 * مباشرة (أو أعلاه إن لم تكن هناك مساحة كافية بالأسفل) — بدل قائمة سفلية
 * عامة لا علاقة بصرية لها بموضع الرسالة نفسها.
 */
@Composable
private fun MessageContextMenu(
    message: Message,
    isMine: Boolean,
    isAdmin: Boolean,
    bubbleBoundsInWindow: Rect,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit
) {
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun dismissAnimated() {
        visible = false
    }

    Popup(
        popupPositionProvider = FullWindowPositionProvider,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = false),
        onDismissRequest = onDismiss
    ) {
        var windowSizePx by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { windowSizePx = it.size }
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(140)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { dismissAnimated(); onDismiss() }
                )
            }

            if (windowSizePx.width > 0 && bubbleBoundsInWindow != Rect.Zero) {
                val menuWidthDp = 232.dp
                val menuWidthPx = with(density) { menuWidthDp.toPx() }
                val gapPx = with(density) { 10.dp.toPx() }
                val screenPaddingPx = with(density) { 12.dp.toPx() }
                val estimatedMenuHeightPx = with(density) { 300.dp.toPx() }

                val spaceBelow = windowSizePx.height - bubbleBoundsInWindow.bottom
                val showBelow = spaceBelow > estimatedMenuHeightPx || spaceBelow > bubbleBoundsInWindow.top

                val menuTopPx = if (showBelow) {
                    bubbleBoundsInWindow.bottom + gapPx
                } else {
                    (bubbleBoundsInWindow.top - gapPx - estimatedMenuHeightPx).coerceAtLeast(screenPaddingPx)
                }

                // نحاذي القائمة مع نفس جهة الفقاعة أفقياً (يمين لرسائلي، يسار
                // لرسائل الآخرين) بدل توسيطها دوماً — أقرب لسلوك إنستقرام الفعلي.
                val menuLeftPx = if (isMine) {
                    (bubbleBoundsInWindow.right - menuWidthPx).coerceIn(screenPaddingPx, windowSizePx.width - menuWidthPx - screenPaddingPx)
                } else {
                    bubbleBoundsInWindow.left.coerceIn(screenPaddingPx, windowSizePx.width - menuWidthPx - screenPaddingPx)
                }

                val menuOffset = with(density) {
                    IntOffset(menuLeftPx.roundToInt(), menuTopPx.roundToInt())
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f),
                    modifier = Modifier
                        .offset { menuOffset }
                        .width(menuWidthDp)
                ) {
                    Column {
                        if (!message.isDeleted) {
                            // صف الإيموجي السريع، بطاقة كبسولة مستقلة فوق بطاقة الإجراءات
                            Row(
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Message.DEFAULT_REACTIONS.forEach { emoji ->
                                    Text(
                                        emoji,
                                        fontSize = 22.sp,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { dismissAnimated(); onReact(emoji) }
                                            .padding(5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Column(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (!message.isDeleted) {
                                ContextMenuRow(icon = Icons.Filled.Reply, label = "رد") {
                                    dismissAnimated(); onReply()
                                }
                                if (message.type == MessageType.TEXT) {
                                    ContextMenuRow(icon = Icons.Filled.ContentCopy, label = "نسخ") {
                                        clipboard.setText(AnnotatedString(message.text))
                                        dismissAnimated(); onDismiss()
                                    }
                                }
                                if ((isMine || isAdmin) && message.type == MessageType.TEXT) {
                                    ContextMenuRow(icon = Icons.Filled.EditNote, label = "تعديل") {
                                        dismissAnimated(); onEdit()
                                    }
                                }
                                if (isMine || isAdmin) {
                                    ContextMenuRow(
                                        icon = Icons.Filled.Delete,
                                        label = "حذف",
                                        tint = MaterialTheme.colorScheme.error
                                    ) {
                                        dismissAnimated(); onDelete()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

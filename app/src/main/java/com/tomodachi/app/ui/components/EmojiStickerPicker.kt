package com.tomodachi.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.data.EmojiData
import com.tomodachi.app.data.Sticker
import java.io.ByteArrayOutputStream

/**
 * لوحة إيموجي + ستيكرات كاملة - معادل initFullEmojiPanel() و
 * filterEmojiPanel() الأصليتين: تصنيفات كاملة (938 رمز)، شريط قفز سريع
 * بين التصنيفات، مربع بحث بالاسم، وتصنيف "الأخيرة" يُبنى من آخر الرموز
 * المُستخدَمة محلياً.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiStickerSheet(
    stickers: List<Sticker>,
    favoriteIds: Set<String>,
    recentEmojis: List<String>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEmoji: (String) -> Unit,
    onSticker: (Sticker) -> Unit,
    onToggleFavorite: (String) -> Boolean,
    onUploadSticker: (base64: String, name: String) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0=emoji, 1=stickers
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = compressImageToBase64(context, it)
            if (base64 != null) onUploadSticker(base64, "ستيكر")
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("إيموجي") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("ستيكرات") })
        }
        Box(modifier = Modifier.heightIn(min = 260.dp, max = 420.dp)) {
            if (tab == 0) {
                FullEmojiPanel(recentEmojis = recentEmojis, onDismiss = onDismiss, onEmoji = onEmoji)
            } else {
                StickerGrid(
                    stickers = stickers,
                    favoriteIds = favoriteIds,
                    onDismiss = onDismiss,
                    onSticker = onSticker,
                    onToggleFavorite = onToggleFavorite,
                    onUpload = { imagePicker.launch("image/*") }
                )
            }
        }
    }
}

@Composable
private fun FullEmojiPanel(
    recentEmojis: List<String>,
    onDismiss: () -> Unit,
    onEmoji: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val categories = remember(recentEmojis) {
        val map = LinkedHashMap<String, List<String>>()
        if (recentEmojis.isNotEmpty()) map["الأخيرة"] = recentEmojis
        EmojiData.categoryOrder.forEach { cat -> EmojiData.categories[cat]?.let { map[cat] = it } }
        map
    }
    val visibleCategories = remember(query, categories) {
        if (query.isBlank()) categories
        else categories.filterKeys { it.contains(query.trim()) }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("ابحث عن تصنيف (وجوه، حيوانات...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
        )
        // شريط القفز السريع بين التصنيفات
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(visibleCategories.keys.toList()) { cat ->
                AssistChip(
                    onClick = { },
                    label = { Text(EmojiData.categoryIcons[cat] ?: "🔸", fontSize = 16.sp) },
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
            visibleCategories.forEach { (cat, emojis) ->
                item {
                    Text(
                        cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                item {
                    EmojiRowGrid(emojis) { emoji ->
                        onEmoji(emoji)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiRowGrid(emojis: List<String>, onClick: (String) -> Unit) {
    // شبكة بسيطة FlowRow-ية بدون مكتبة خارجية: صفوف يدوية من 8 أعمدة
    val chunked = emojis.chunked(8)
    Column {
        chunked.forEach { row ->
            Row {
                row.forEach { emoji ->
                    Text(
                        emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { onClick(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerGrid(
    stickers: List<Sticker>,
    favoriteIds: Set<String>,
    onDismiss: () -> Unit,
    onSticker: (Sticker) -> Unit,
    onToggleFavorite: (String) -> Boolean,
    onUpload: () -> Unit
) {
    val grouped = remember(stickers) { stickers.groupBy { it.pack ?: "ستيكراتي" } }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onUpload) { Text("📷 رفع ستيكر") }
        }
        androidx.compose.foundation.lazy.LazyColumn {
            grouped.forEach { (pack, packStickers) ->
                item {
                    Text(pack, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.padding(12.dp).heightIn(max = 220.dp)
                    ) {
                        items(packStickers) { sticker ->
                            Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { onSticker(sticker); onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StickerThumbnail(sticker)
                                }
                                val isFav = favoriteIds.contains(sticker.id)
                                Text(
                                    if (isFav) "⭐" else "☆",
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable { onToggleFavorite(sticker.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerThumbnail(sticker: Sticker) {
    when (sticker.type) {
        "image" -> {
            val bmp = remember(sticker.data) { sticker.data?.let(::decodeB64) }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = sticker.name,
                    modifier = Modifier.size(56.dp)
                )
            } else Text("🎨")
        }
        "vector" -> VectorStickerBadge(sticker, size = 56.dp)
        else -> Text(sticker.emoji ?: "😊", fontSize = 28.sp)
    }
}

/** يرسم شارة دائرية متدرّجة اللون + رمز يونيكود بمنتصفها - نفس فكرة
 * badgeSvg()/chibiFaceSvg() الأصليتين لكن برسم Compose أصلي مباشر. */
@Composable
fun VectorStickerBadge(sticker: Sticker, size: androidx.compose.ui.unit.Dp) {
    val from = parseColorOrDefault(sticker.vectorFrom, Color(0xFF4FACFE))
    val to = parseColorOrDefault(sticker.vectorTo, Color(0xFF00F2FE))
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(from, to))),
        contentAlignment = Alignment.Center
    ) {
        Text(sticker.emoji ?: "✨", fontSize = (size.value * 0.45).sp)
    }
}

private fun parseColorOrDefault(hex: String?, fallback: Color): Color = try {
    if (hex == null) fallback else Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { fallback }

private fun decodeB64(dataUrl: String): Bitmap? = try {
    val base64 = dataUrl.substringAfter(",")
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) { null }

/** نفس فكرة compressImageSmart() الأصلية: تصغير + ضغط JPEG تدريجي حتى
 * يبقى حجم الـ Base64 الناتج صغيراً بما يكفي (أقل من 100 كيلوبايت تقريباً)
 * لأن كل الستيكرات تُخزَّن معاً بمستند واحد stickers/all. */
private fun compressImageToBase64(context: android.content.Context, uri: Uri): String? = try {
    val input = context.contentResolver.openInputStream(uri)
    val original = BitmapFactory.decodeStream(input)
    input?.close()
    if (original == null) null else {
        val maxDim = 300
        val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
        val scaled = Bitmap.createScaledBitmap(
            original, (original.width * ratio).toInt().coerceAtLeast(1),
            (original.height * ratio).toInt().coerceAtLeast(1), true
        )
        var quality = 80
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 15
        } while (bytes.size > 100 * 1024 && quality > 20)
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
} catch (e: Exception) { null }

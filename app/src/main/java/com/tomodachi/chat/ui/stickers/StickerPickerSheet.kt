package com.tomodachi.chat.ui.stickers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tomodachi.chat.data.model.Sticker
import com.tomodachi.chat.util.parseHexColor

private const val ONLINE_TAB_LABEL = "أونلاين"
private const val CUSTOM_TAB_LABEL = "مخصّصة"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    viewModel: StickerPickerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    currentUserUid: String,
    currentUsername: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onStickerSelected: (Sticker) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val customStickers by viewModel.customStickers.collectAsStateWithLifecycle()
    val uploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val onlineStickers by viewModel.onlineStickers.collectAsStateWithLifecycle()
    val onlineQuery by viewModel.onlineQuery.collectAsStateWithLifecycle()
    val isLoadingOnline by viewModel.isLoadingOnline.collectAsStateWithLifecycle()
    val onlineError by viewModel.onlineError.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val packs = viewModel.builtinPacks
    // ترتيب الألسنة: الحزم المدمجة، ثم "أونلاين" (Giphy)، ثم "مخصّصة" (رفع المستخدم) أخيراً
    val onlineTabIndex = packs.size
    val customTabIndex = packs.size + 1

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadSticker(context, uri, currentUserUid, currentUsername)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.75f)) {
            // رأس اللوحة الجديد — عنوان واضح بأسلوب أنيق بدل القفز مباشرة إلى الألسنة
            Text(
                "الملصقات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) {
                packs.forEachIndexed { index, pack ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(pack.nameAr) }
                    )
                }
                Tab(
                    selected = selectedTabIndex == onlineTabIndex,
                    onClick = { selectedTabIndex = onlineTabIndex },
                    text = { Text(ONLINE_TAB_LABEL) },
                    icon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == customTabIndex,
                    onClick = { selectedTabIndex = customTabIndex },
                    text = { Text(CUSTOM_TAB_LABEL) }
                )
            }
            HorizontalDivider()

            when (selectedTabIndex) {
                onlineTabIndex -> OnlineStickersTab(
                    query = onlineQuery,
                    onQueryChange = viewModel::onOnlineQueryChanged,
                    stickers = onlineStickers,
                    isLoading = isLoadingOnline,
                    errorMessage = onlineError,
                    onRetry = viewModel::retryLoadingOnline,
                    onStickerSelected = onStickerSelected
                )

                customTabIndex -> CustomStickersTab(
                    stickers = customStickers,
                    uploading = uploading,
                    onUploadClick = { pickImageLauncher.launch("image/*") },
                    onStickerSelected = onStickerSelected
                )

                else -> BuiltinPackTab(
                    pack = packs[selectedTabIndex],
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    onStickerSelected = onStickerSelected
                )
            }
        }
    }
}

/** حزمة الستيكرات المدمجة (شارات متدرّجة الألوان بدون اعتماد على الإنترنت) —
 * بطاقات مرفوعة قليلاً بظل خفيف بدل دوائر مسطّحة لمظهر أكثر حداثة. */
@Composable
private fun BuiltinPackTab(
    pack: com.tomodachi.chat.data.model.StickerPack,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onStickerSelected: (Sticker) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        items(pack.stickers) { sticker ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(parseHexColor(sticker.gradientStartHex), parseHexColor(sticker.gradientEndHex))
                            )
                        )
                        .clickable { onStickerSelected(sticker) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(sticker.emojiFallback, fontSize = 28.sp)
                }
                Text(
                    if (sticker.id in favoriteIds) "★" else "☆",
                    color = if (sticker.id in favoriteIds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onToggleFavorite(sticker.id) }
                )
            }
        }
    }
}

/** تبويب "أونلاين": حقل بحث بشكل كبسولة حديث + شبكة ستيكرات حقيقية مجلوبة من
 * Giphy مجاناً، وحالة فشل مُعاد تصميمها بالكامل مع أيقونة واضحة وزر
 * "إعادة المحاولة" فعلي (لم يكن موجوداً سابقاً — كان المستخدم يعلق دون أي إجراء). */
@Composable
private fun OnlineStickersTab(
    query: String,
    onQueryChange: (String) -> Unit,
    stickers: List<Sticker>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onStickerSelected: (Sticker) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("ابحث عن ستيكرات (بالإنجليزية أفضل)…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        when {
            isLoading && stickers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            errorMessage != null && stickers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onRetry, shape = RoundedCornerShape(50)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }

            stickers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا نتائج", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp)
            ) {
                items(stickers, key = { it.id }) { sticker ->
                    AsyncImage(
                        model = sticker.imageUrl,
                        contentDescription = sticker.label,
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .shadow(1.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onStickerSelected(sticker) }
                    )
                }
            }
        }
    }
}

/** ستيكرات المستخدمين المرفوعة يدوياً — بطاقة إرشادية أعلى الشبكة توضّح
 * الغرض من التبويب بدل الاكتفاء بزر رفع مجرّد. */
@Composable
private fun CustomStickersTab(
    stickers: List<Sticker>,
    uploading: Boolean,
    onUploadClick: () -> Unit,
    onStickerSelected: (Sticker) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ستيكراتك المخصّصة", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onUploadClick, enabled = !uploading, shape = RoundedCornerShape(50)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (uploading) "جارٍ الرفع…" else "رفع ستيكر")
            }
        }
        if (stickers.isEmpty() && !uploading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "لا ستيكرات مخصّصة بعد — اضغط \"رفع ستيكر\" لإضافة أول واحد",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp)) {
            items(stickers, key = { it.id }) { sticker ->
                AsyncImage(
                    model = sticker.imageUrl,
                    contentDescription = sticker.label,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(72.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable { onStickerSelected(sticker) }
                )
            }
        }
    }
}

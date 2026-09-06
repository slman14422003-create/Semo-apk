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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
        Column(modifier = Modifier.fillMaxHeight(0.72f)) {
            ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 12.dp) {
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

/** حزمة الستيكرات المدمجة (شارات متدرّجة الألوان بدون اعتماد على الإنترنت). */
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
                    modifier = Modifier.clickable { onToggleFavorite(sticker.id) }
                )
            }
        }
    }
}

/** تبويب "أونلاين": حقل بحث + شبكة ستيكرات حقيقية مجلوبة من Giphy مجاناً. */
@Composable
private fun OnlineStickersTab(
    query: String,
    onQueryChange: (String) -> Unit,
    stickers: List<Sticker>,
    isLoading: Boolean,
    errorMessage: String?,
    onStickerSelected: (Sticker) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("ابحث عن ستيكرات (بالإنجليزية أفضل)…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        when {
            isLoading && stickers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            errorMessage != null && stickers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(24.dp))
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onStickerSelected(sticker) }
                    )
                }
            }
        }
    }
}

/** ستيكرات المستخدمين المرفوعة يدوياً (تبقى كما كانت، بدون تغيير وظيفي). */
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
            Button(onClick = onUploadClick, enabled = !uploading) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (uploading) "جارٍ الرفع…" else "رفع ستيكر")
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
                        .clip(CircleShape)
                        .clickable { onStickerSelected(sticker) }
                )
            }
        }
    }
}

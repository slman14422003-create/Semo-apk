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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var selectedTabIndex by remember { mutableStateOf(0) }
    val packs = viewModel.builtinPacks

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadSticker(context, uri, currentUserUid, currentUsername)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.7f)) {
            ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                packs.forEachIndexed { index, pack ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(pack.nameAr) }
                    )
                }
                Tab(
                    selected = selectedTabIndex == packs.size,
                    onClick = { selectedTabIndex = packs.size },
                    text = { Text("مخصّصة") }
                )
            }

            if (selectedTabIndex == packs.size) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ستيكراتك المخصّصة", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { pickImageLauncher.launch("image/*") }, enabled = !uploading) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (uploading) "جارٍ الرفع…" else "رفع ستيكر")
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f)) {
                    items(customStickers) { sticker ->
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
            } else {
                val pack = packs[selectedTabIndex]
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f)) {
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
                                            listOf(
                                                parseHexColor(sticker.gradientStartHex),
                                                parseHexColor(sticker.gradientEndHex)
                                            )
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
        }
    }
}

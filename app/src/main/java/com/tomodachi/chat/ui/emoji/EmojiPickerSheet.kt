package com.tomodachi.chat.ui.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.util.EmojiData

private const val RECENTS_CATEGORY_ID = "recent"
private const val MAX_RECENTS = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    recentEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(if (recentEmojis.isNotEmpty()) RECENTS_CATEGORY_ID else EmojiData.categories.first().id) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.75f)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("ابحث في التصنيفات…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(modifier = Modifier.padding(vertical = 4.dp)) {
                if (recentEmojis.isNotEmpty()) {
                    item {
                        CategoryTab("🕓", "الأخيرة", selectedCategoryId == RECENTS_CATEGORY_ID) {
                            selectedCategoryId = RECENTS_CATEGORY_ID
                        }
                    }
                }
                items(EmojiData.categories) { category ->
                    CategoryTab(category.icon, category.nameAr, selectedCategoryId == category.id) {
                        selectedCategoryId = category.id
                    }
                }
            }
            HorizontalDivider()

            val emojisToShow: List<String> = when {
                query.isNotBlank() -> EmojiData.search(query)
                selectedCategoryId == RECENTS_CATEGORY_ID -> recentEmojis
                else -> EmojiData.categories.first { it.id == selectedCategoryId }.emojis
            }

            LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier.weight(1f)) {
                items(emojisToShow) { emoji ->
                    Text(
                        emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickableEmoji { onEmojiSelected(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$icon $label") },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

private fun Modifier.clickableEmoji(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

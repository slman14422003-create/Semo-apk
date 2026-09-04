package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomodachi.chat.data.model.BannedWord

@Composable
fun AdminBannedWordsTab(bannedWords: List<BannedWord>, currentUserUid: String, viewModel: AdminViewModel) {
    var newWord by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            "أي رسالة تحتوي كلمة ممنوعة تُطبَّق عليها عقوبة حظر مؤقت تلقائي متصاعد (60 ثانية × عدد المخالفات، بحد أقصى 10 دقائق).",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWord,
                onValueChange = { newWord = it },
                label = { Text("كلمة ممنوعة جديدة") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newWord.isNotBlank()) {
                    viewModel.addBannedWord(newWord.trim(), currentUserUid)
                    newWord = ""
                }
            }) { Text("إضافة") }
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(bannedWords, key = { it.id }) { word ->
                ListItem(
                    headlineContent = { Text(word.word) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeBannedWord(word.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

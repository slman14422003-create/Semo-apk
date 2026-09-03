package com.tomodachi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.data.ChatUser
import com.tomodachi.app.data.Sticker

private enum class AdminTab { USERS, BADWORDS, STICKERS, DANGER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    users: List<ChatUser>,
    badWords: List<String>,
    stickers: List<Sticker>,
    onBack: () -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onUnban: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onMakeAdmin: (String) -> Unit,
    onWarn: (String, String) -> Unit,
    onAddBadWord: (String) -> Unit,
    onRemoveBadWord: (String) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onClearAllMessages: () -> Unit
) {
    var tab by remember { mutableStateOf(AdminTab.USERS) }
    var badWordInput by remember { mutableStateOf("") }
    var warnDialogFor by remember { mutableStateOf<String?>(null) }
    var warnReason by remember { mutableStateOf("") }
    var confirmClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة المسؤول") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(selected = tab == AdminTab.USERS, onClick = { tab = AdminTab.USERS }, text = { Text("المستخدمون") })
                Tab(selected = tab == AdminTab.BADWORDS, onClick = { tab = AdminTab.BADWORDS }, text = { Text("كلمات ممنوعة") })
                Tab(selected = tab == AdminTab.STICKERS, onClick = { tab = AdminTab.STICKERS }, text = { Text("ستيكرات") })
                Tab(selected = tab == AdminTab.DANGER, onClick = { tab = AdminTab.DANGER }, text = { Text("⚠️ خطر") })
            }

            when (tab) {
                AdminTab.USERS -> LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(users, key = { it.username }) { u ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("${u.avatar} ${u.username}" + if (u.isAdmin) " ⭐" else "", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Text(
                                            listOfNotNull(
                                                if (u.online) "متصل الآن" else null,
                                                if (u.blocked) "محظور دائم" else null,
                                                if (u.banned) "محظور مؤقت" else null,
                                                if (u.warnCount > 0) "تحذيرات: ${u.warnCount}" else null
                                            ).joinToString(" · ").ifEmpty { "—" },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (u.username != "slx23m") {
                                        if (u.blocked) {
                                            AssistChip(onClick = { onUnblock(u.username) }, label = { Text("فك الحظر") })
                                        } else {
                                            AssistChip(onClick = { onBlock(u.username) }, label = { Text("حظر") })
                                        }
                                        if (u.banned) {
                                            AssistChip(onClick = { onUnban(u.username) }, label = { Text("فك المؤقت") })
                                        }
                                        AssistChip(onClick = { warnDialogFor = u.username }, label = { Text("تحذير") })
                                        if (!u.isAdmin) {
                                            AssistChip(onClick = { onMakeAdmin(u.username) }, label = { Text("ترقية") })
                                        }
                                        AssistChip(onClick = { onDeleteUser(u.username) }, label = { Text("حذف") })
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.BADWORDS -> Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = badWordInput,
                            onValueChange = { badWordInput = it },
                            label = { Text("كلمة ممنوعة جديدة") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onAddBadWord(badWordInput); badWordInput = "" }) {
                            Text("➕")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(badWords) { word ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(word)
                                IconButton(onClick = { onRemoveBadWord(word) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                            Divider()
                        }
                    }
                }

                AdminTab.STICKERS -> LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(stickers, key = { it.id }) { sticker ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text((sticker.emoji ?: sticker.name ?: "🎨") + "  " + (sticker.uploadedBy ?: ""), fontSize = 13.sp)
                            IconButton(onClick = { onDeleteSticker(sticker.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف")
                            }
                        }
                        Divider()
                    }
                }

                AdminTab.DANGER -> Column(modifier = Modifier.padding(16.dp)) {
                    Text("منطقة خطرة", color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { confirmClearAll = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("🗑️ حذف جميع الرسائل نهائياً") }
                }
            }
        }
    }

    warnDialogFor?.let { username ->
        AlertDialog(
            onDismissRequest = { warnDialogFor = null },
            title = { Text("تحذير $username") },
            text = {
                OutlinedTextField(
                    value = warnReason,
                    onValueChange = { warnReason = it },
                    label = { Text("السبب (اختياري)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onWarn(username, warnReason)
                    warnReason = ""
                    warnDialogFor = null
                }) { Text("إرسال") }
            },
            dismissButton = { TextButton(onClick = { warnDialogFor = null }) { Text("إلغاء") } }
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("تأكيد") },
            text = { Text("هل تريد حذف جميع الرسائل نهائياً؟ لا يمكن التراجع.") },
            confirmButton = {
                TextButton(onClick = { onClearAllMessages(); confirmClearAll = false }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("إلغاء") } }
        )
    }
}

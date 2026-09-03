package com.tomodachi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.data.ChatUser
import com.tomodachi.app.data.ThemeSettings

private val AVATAR_EMOJIS = listOf("👥","😊","😎","🥳","🤓","👽","🐱","🐶","🦊","🐼","🦁","🐸","🌟","🔥","💎","🎮")
private val PRESET_COLORS = listOf("#0084FF","#E74C3C","#2ECC71","#9B59B6","#F39C12","#1ABC9C","#E91E63","#34495E")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    user: ChatUser,
    onDismiss: () -> Unit,
    onSaveProfile: (avatar: String, bio: String) -> Unit,
    onSaveTheme: (ThemeSettings) -> Unit,
    onLogout: () -> Unit
) {
    var avatar by remember { mutableStateOf(user.avatar) }
    var bio by remember { mutableStateOf(user.bio) }
    var isDark by remember { mutableStateOf(user.themeSettings.mode == "dark") }
    var sentColor by remember { mutableStateOf(user.themeSettings.sentColor) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الملف الشخصي", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            Text("الرمز التعبيري", fontSize = 13.sp)
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(AVATAR_EMOJIS) { emoji ->
                    Surface(
                        onClick = { avatar = emoji },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (avatar == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(4.dp).size(48.dp)
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 60) bio = it },
                label = { Text("نبذة عني") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("الثيم", fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(if (isDark) "🌙 وضع داكن" else "☀️ وضع فاتح")
                Switch(checked = isDark, onCheckedChange = { isDark = it })
            }

            Text("لون فقاعتي", fontSize = 13.sp)
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(PRESET_COLORS) { hex ->
                    val color = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
                        catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                    Surface(
                        onClick = { sentColor = hex },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = color,
                        border = if (sentColor == hex) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier.padding(4.dp).size(40.dp)
                    ) {}
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSaveProfile(avatar, bio)
                    onSaveTheme(ThemeSettings(mode = if (isDark) "dark" else "light", sentColor = sentColor, receivedColor = user.themeSettings.receivedColor))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onLogout(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تسجيل الخروج") }

            Spacer(Modifier.height(8.dp))
        }
    }
}

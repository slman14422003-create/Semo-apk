package com.tomodachi.chat.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.util.AVATAR_EMOJI_CHOICES
import com.tomodachi.chat.util.BUBBLE_COLOR_PALETTE
import com.tomodachi.chat.util.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: User,
    darkModePref: String,
    onDarkModePrefChanged: (String) -> Unit,
    onUserUpdated: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    var bio by remember { mutableStateOf(currentUser.bio) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملفي الشخصي") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text(currentUser.avatarEmoji, fontSize = 44.sp)
            }
            Text(
                "اضغط لتغيير الصورة الرمزية",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                currentUser.username,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (currentUser.isAdmin) {
                AssistChip(
                    onClick = {},
                    label = { Text("مسؤول") },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= User.MAX_BIO_LENGTH) bio = it },
                label = { Text("نبذة عنك (60 حرفاً كحد أقصى)") },
                supportingText = { Text("${bio.length}/${User.MAX_BIO_LENGTH}") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = { viewModel.updateBio(currentUser.usernameLower, bio, onUserUpdated) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) { Text("حفظ النبذة") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("الوضع الداكن/الفاتح", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmented(
                options = listOf("system" to "النظام", "light" to "فاتح", "dark" to "داكن"),
                selected = darkModePref,
                onSelected = onDarkModePrefChanged
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("لون فقاعة رسائلك", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(110.dp)
            ) {
                items(BUBBLE_COLOR_PALETTE) { hex ->
                    val isSelected = hex.equals(currentUser.bubbleColorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.updateBubbleColor(currentUser.usernameLower, hex, onUserUpdated)
                            }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل الخروج")
            }
        }
    }

    if (showAvatarPicker) {
        ModalBottomSheet(onDismissRequest = { showAvatarPicker = false }) {
            Text(
                "اختر صورة رمزية",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(AVATAR_EMOJI_CHOICES) { emoji ->
                    Text(
                        emoji,
                        fontSize = 34.sp,
                        modifier = Modifier
                            .padding(10.dp)
                            .clickable {
                                showAvatarPicker = false
                                viewModel.updateAvatar(currentUser.usernameLower, emoji, onUserUpdated)
                            }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("تسجيل الخروج") },
            text = { Text("هل تريد تسجيل الخروج من حسابك؟") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SingleChoiceSegmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(value) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

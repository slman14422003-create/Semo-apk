package com.tomodachi.chat.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.components.UserAvatar
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.ui.theme.WhatsAppGreen
import com.tomodachi.chat.util.AVATAR_EMOJI_CHOICES
import com.tomodachi.chat.util.BUBBLE_COLOR_PALETTE
import com.tomodachi.chat.util.formatJoinDate
import com.tomodachi.chat.util.parseHexColor
import kotlinx.coroutines.launch

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
    var showAvatarSourceSheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val isUploadingImage by viewModel.isUploadingImage.collectAsStateWithLifecycle()
    val imageError by viewModel.imageError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.updateProfileImage(currentUser.usernameLower, uri, onUserUpdated)
        }
    }

    LaunchedEffect(imageError) {
        imageError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeImageError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ملفي الشخصي",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "رجوع", modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        val scrollState = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // --- رأس الملف الشخصي: شعاع تدرّج علوي بأسلوب غلاف انستقرام/واتساب ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Brush.horizontalGradient(BrandGradient))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-52).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        avatarEmoji = currentUser.avatarEmoji,
                        profileImageBase64 = currentUser.profileImageBase64,
                        size = 104.dp,
                        showGradientRing = true,
                        modifier = Modifier
                            .shadow(6.dp, CircleShape)
                            .clickable { showAvatarSourceSheet = true }
                    )

                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(BrandGradient))
                            .clickable { showAvatarSourceSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "تغيير صورة الملف الشخصي",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // النقر على الاسم ينسخه للحافظة — ميزة صغيرة عملية (مشاركة
                // اسم المستخدم مع صديق مثلاً) لم تكن موجودة سابقاً.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(currentUser.username))
                        coroutineScope.launch { snackbarHostState.showSnackbar("تم نسخ اسم المستخدم") }
                    }
                ) {
                    Text(
                        currentUser.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentUser.isAdmin) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = "مسؤول",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "نسخ اسم المستخدم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "متصل الآن",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // تاريخ الانضمام — معلومة كانت مخزَّنة أصلاً (createdAtMillis)
                // لكنها لم تُعرض للمستخدم أبداً في أي مكان بالتطبيق.
                if (currentUser.createdAtMillis > 0L) {
                    Text(
                        formatJoinDate(currentUser.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentUser.isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(BrandGradient))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text("مسؤول النظام", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // شارة تحذيرات — تنبيه لطيف للمستخدم نفسه بعدد المخالفات
                    // المسجَّلة عليه (كانت هذه البيانات موجودة لكن غير مرئية إطلاقاً).
                    if (currentUser.warningsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${currentUser.warningsCount}/${User.MAX_WARNINGS_BEFORE_BAN} تحذيرات",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.offset(y = (-40).dp)) {
                SectionCard {
                    SectionHeader(icon = Icons.Filled.EditNote, title = "نبذة عنك")
                    Spacer(Modifier.height(10.dp))
                    TextField(
                        value = bio,
                        onValueChange = { if (it.length <= User.MAX_BIO_LENGTH) bio = it },
                        placeholder = { Text("اكتب نبذة قصيرة عنك…") },
                        supportingText = { Text("${bio.length}/${User.MAX_BIO_LENGTH}") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Button(
                        onClick = { viewModel.updateBio(currentUser.usernameLower, bio, onUserUpdated) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 10.dp)
                    ) { Text("حفظ النبذة") }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    SectionHeader(icon = Icons.Filled.DarkMode, title = "المظهر")
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmented(
                        options = listOf("system" to "النظام", "light" to "فاتح", "dark" to "داكن"),
                        selected = darkModePref,
                        onSelected = onDarkModePrefChanged
                    )
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    SectionHeader(icon = Icons.Filled.Palette, title = "لون فقاعة رسائلك")
                    Spacer(Modifier.height(10.dp))
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
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateBubbleColor(currentUser.usernameLower, hex, onUserUpdated)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تسجيل الخروج")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAvatarSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSourceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "صورة الملف الشخصي",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                ListItem(
                    headlineContent = { Text("اختيار صورة من المعرض") },
                    leadingContent = {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable {
                        showAvatarSourceSheet = false
                        pickImageLauncher.launch("image/*")
                    }
                )
                ListItem(
                    headlineContent = { Text("اختيار رمز تعبيري بدلاً من ذلك") },
                    leadingContent = { Text(currentUser.avatarEmoji, fontSize = 20.sp) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable {
                        showAvatarSourceSheet = false
                        showAvatarPicker = true
                    }
                )
            }
        }
    }

    if (showAvatarPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarPicker = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Text(
                "اختر صورة رمزية",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(AVATAR_EMOJI_CHOICES) { emoji ->
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                showAvatarPicker = false
                                viewModel.updateAvatar(currentUser.usernameLower, emoji, onUserUpdated)
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 30.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("تسجيل الخروج") },
            text = { Text("هل تريد تسجيل الخروج من حسابك؟") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("تأكيد", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SingleChoiceSegmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

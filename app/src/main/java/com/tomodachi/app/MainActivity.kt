package com.tomodachi.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tomodachi.app.data.ChatMessage
import com.tomodachi.app.data.MessageStatus
import com.tomodachi.app.ui.components.ProfileSheet
import com.tomodachi.app.ui.screens.AdminPanelScreen
import com.tomodachi.app.ui.screens.ChatScreen
import com.tomodachi.app.ui.screens.LoginScreen
import com.tomodachi.app.ui.theme.TomodachiTheme
import com.tomodachi.app.viewmodel.ChatViewModel
import com.tomodachi.app.viewmodel.LoginState

/**
 * Semo — نسخة كوتلن أصلية بالكامل (Jetpack Compose + Firebase Firestore
 * Android SDK). لا يوجد WebView أو أي كود جافاسكربت هنا إطلاقاً؛ كل
 * الواجهة والمنطق مكتوبان بالكوتلن مباشرة.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        NotifyHelper.ensureMessagesChannel(this)
        requestRuntimePermissions()

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val loginState by viewModel.loginState.collectAsState()
            val theme = currentUser?.themeSettings

            TomodachiTheme(
                darkTheme = theme?.mode == "dark",
                sentColorHex = theme?.sentColor ?: "#0084FF",
                receivedColorHex = theme?.receivedColor ?: "#E4E6EB"
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(viewModel, loginState)
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val wanted = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else emptyArray()
        val toRequest = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 1)
        }
    }

    /** يبدأ خدمة المزامنة بالخلفية (Firestore حقيقي، بدون WebView) عند
     * مغادرة التطبيق للواجهة، تماماً بنفس فكرة MessageSyncService الأصلية. */
    override fun onStart() {
        super.onStart()
        stopService(Intent(this, MessageSyncService::class.java))
    }

    override fun onStop() {
        super.onStop()
        runCatching { ContextCompat.startForegroundService(this, Intent(this, MessageSyncService::class.java)) }
    }
}

@Composable
private fun AppRoot(viewModel: ChatViewModel, loginState: LoginState) {
    val currentUser by viewModel.currentUser.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val stickers by viewModel.stickers.collectAsState()
    val badWords by viewModel.badWords.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var showProfile by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        val user = currentUser
        when {
            user == null -> LoginScreen(loginState = loginState, onLogin = viewModel::loginOrRegister)
            showAdmin -> AdminPanelScreen(
                users = allUsers,
                badWords = badWords,
                stickers = stickers,
                onBack = { showAdmin = false },
                onBlock = viewModel::blockUser,
                onUnblock = viewModel::unblockUser,
                onUnban = viewModel::unbanUser,
                onDeleteUser = viewModel::deleteUser,
                onMakeAdmin = viewModel::makeAdmin,
                onWarn = viewModel::warnUser,
                onAddBadWord = viewModel::addBadWord,
                onRemoveBadWord = viewModel::removeBadWord,
                onDeleteSticker = viewModel::deleteSticker,
                onClearAllMessages = viewModel::clearAllMessages
            )
            else -> {
                ChatScreen(
                    user = user,
                    myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    messages = messages,
                    typingUsers = typingUsers,
                    stickers = stickers,
                    favoriteStickerIds = viewModel.favoriteStickerIds(),
                    recentEmojis = viewModel.recentEmojis(),
                    replyingTo = replyingTo,
                    onSendText = viewModel::sendText,
                    onSendSticker = viewModel::sendSticker,
                    onTyping = viewModel::setTyping,
                    onReply = viewModel::startReply,
                    onCancelReply = viewModel::cancelReply,
                    onDelete = viewModel::deleteMessage,
                    onRetry = { msg: ChatMessage -> if (msg.status == MessageStatus.FAILED) viewModel.sendText(msg.text) },
                    onEdit = viewModel::editMessage,
                    onToggleReaction = { id, emoji -> viewModel.toggleReaction(id, emoji) },
                    onToggleFavoriteSticker = viewModel::toggleFavoriteSticker,
                    onUploadSticker = viewModel::uploadSticker,
                    onEmojiUsed = viewModel::addRecentEmoji,
                    onOpenProfile = { showProfile = true },
                    onOpenAdmin = { showAdmin = true }
                )

                if (showProfile) {
                    ProfileSheet(
                        user = user,
                        onDismiss = { showProfile = false },
                        onSaveProfile = viewModel::updateProfile,
                        onSaveTheme = viewModel::updateThemeSettings,
                        onLogout = viewModel::logout
                    )
                }
            }
        }
    }
}

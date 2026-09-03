package com.tomodachi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tomodachi.app.data.ChatMessage
import com.tomodachi.app.data.ChatRepository
import com.tomodachi.app.data.ChatUser
import com.tomodachi.app.data.LocalMessageCache
import com.tomodachi.app.data.MessageStatus
import com.tomodachi.app.data.SessionManager
import com.tomodachi.app.data.Sticker
import com.tomodachi.app.data.StickerPacks
import com.tomodachi.app.data.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginState {
    object LoggedOut : LoginState()
    object LoggingIn : LoginState()
    data class Error(val message: String) : LoginState()
    object LoggedIn : LoginState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ChatRepository()
    private val session = SessionManager(application)
    private val localCache = LocalMessageCache(application)
    private val auth = FirebaseAuth.getInstance()

    val loginState = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val currentUser = MutableStateFlow<ChatUser?>(null)
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val typingUsers = MutableStateFlow<List<String>>(emptyList())
    val stickers = MutableStateFlow<List<Sticker>>(emptyList())
    val badWords = MutableStateFlow<List<String>>(emptyList())
    val allUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val toast = MutableStateFlow<String?>(null)

    val replyingTo = MutableStateFlow<ChatMessage?>(null)

    init {
        viewModelScope.launch {
            runCatching { if (auth.currentUser == null) auth.signInAnonymously().await() }
            session.lastUser?.let { tryAutoLogin(it) }
        }
        viewModelScope.launch { repo.listenTyping().collect { map ->
            val me = currentUser.value?.username
            typingUsers.value = map.filter { it.value && it.key != me }.keys.toList()
        } }
        viewModelScope.launch { repo.listenUsers().collect { allUsers.value = it } }
        viewModelScope.launch {
            runCatching {
                stickers.value = StickerPacks.mergeInto(repo.loadStickers())
                badWords.value = repo.loadBadWords()
            }
        }
    }

    private fun notify(msg: String) { toast.value = msg }
    fun consumeToast() { toast.value = null }

    private suspend fun tryAutoLogin(username: String) {
        loginState.value = LoginState.LoggingIn
        val user = repo.getUser(username)
        if (user != null && !user.blocked && !(user.banned && repo.isBanStillActive(user))) {
            enterChat(user)
        } else {
            loginState.value = LoginState.LoggedOut
        }
    }

    /** يسجّل دخول لاسم مستخدم موجود، أو ينشئ حساباً جديداً لو غير موجود. */
    fun loginOrRegister(username: String) {
        val name = username.trim()
        if (name.length < 2) { notify("❌ الاسم قصير جداً"); return }
        viewModelScope.launch {
            loginState.value = LoginState.LoggingIn
            runCatching {
                val existing = repo.getUser(name)
                if (existing != null) {
                    if (existing.blocked) {
                        loginState.value = LoginState.Error("⛔ تم حظر حسابك من قبل المسؤول")
                        return@launch
                    }
                    if (existing.banned && repo.isBanStillActive(existing)) {
                        loginState.value = LoginState.Error("⛔ أنت محظور: ${existing.banReason ?: ""}")
                        return@launch
                    }
                    if (existing.banned) repo.clearExpiredBan(name)
                    repo.markOnline(name)
                    enterChat(existing.copy(banned = false))
                } else {
                    val uid = auth.currentUser?.uid ?: ""
                    val created = repo.createUser(name, uid)
                    enterChat(created)
                }
            }.onFailure {
                loginState.value = LoginState.Error("❌ تعذّر الدخول، تحقق من الاتصال")
            }
        }
    }

    private fun enterChat(user: ChatUser) {
        currentUser.value = user
        session.lastUser = user.username
        loginState.value = LoginState.LoggedIn
        listenMessages()
    }

    fun logout() {
        val u = currentUser.value?.username
        viewModelScope.launch { u?.let { repo.markOffline(it) } }
        session.clearSession()
        currentUser.value = null
        loginState.value = LoginState.LoggedOut
        messages.value = emptyList()
    }

    private fun listenMessages() {
        // نعرض النسخة المحفوظة محلياً فوراً قبل اكتمال اتصال فايرستور - نفس
        // فكرة loadMessagesFromStorage() الأصلية لتجنب شاشة فارغة عند الفتح.
        val cached = localCache.load()
        if (cached.isNotEmpty()) messages.value = cached

        viewModelScope.launch {
            repo.listenRecentMessages().collect { list ->
                messages.value = list
                localCache.save(list)
            }
        }
    }

    fun setTyping(isTyping: Boolean) {
        val u = currentUser.value?.username ?: return
        viewModelScope.launch { repo.setTyping(u, isTyping) }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            if (!user.isAdmin && repo.checkAndApplyBadWords(trimmed, user.username, badWords.value)) {
                notify("⛔ تم حظرك مؤقتاً لاستخدام كلمة ممنوعة")
                return@launch
            }
            val reply = replyingTo.value
            val msg = ChatMessage(
                username = user.username,
                uid = auth.currentUser?.uid ?: "",
                text = trimmed,
                avatar = user.avatar,
                isAdmin = user.isAdmin,
                replyTo = reply?.id,
                replyText = reply?.text,
                replyToUser = reply?.username
            )
            replyingTo.value = null
            runCatching { repo.sendMessage(msg) }.onFailure { notify("❌ فشل الإرسال") }
            repo.setTyping(user.username, false)
        }
    }

    fun sendSticker(sticker: Sticker) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val msg = ChatMessage(
                username = user.username,
                uid = auth.currentUser?.uid ?: "",
                text = "🎨 ستيكر",
                avatar = user.avatar,
                isAdmin = user.isAdmin,
                sticker = true,
                stickerData = sticker.data ?: sticker.emoji
            )
            runCatching { repo.sendMessage(msg) }.onFailure { notify("❌ فشل إرسال الستيكر") }
        }
    }

    fun deleteMessage(id: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching {
                // نفس تحقق الملكية بدالة deleteMessage() الأصلية: المسؤول
                // يقدر يحذف أي رسالة، وغيره فقط رسائله هو.
                if (!user.isAdmin) {
                    val msg = repo.getMessage(id)
                    if (msg == null) { notify("❌ الرسالة غير موجودة"); return@launch }
                    if (!msg.isOwnedBy(user.username, auth.currentUser?.uid ?: "")) {
                        notify("❌ لا يمكنك حذف رسالة أخرى")
                        return@launch
                    }
                }
                repo.deleteMessage(id)
            }.onFailure { notify("❌ فشل الحذف") }
        }
    }

    fun editMessage(message: ChatMessage, newText: String) {
        val user = currentUser.value ?: return
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        if (!message.isOwnedBy(user.username, auth.currentUser?.uid ?: "")) {
            notify("❌ لا يمكنك تعديل رسالة أخرى")
            return
        }
        viewModelScope.launch {
            runCatching { repo.editMessage(message.id, trimmed) }.onFailure { notify("❌ فشل التعديل") }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repo.toggleReaction(messageId, emoji, user.username) }
        }
    }

    fun addRecentEmoji(emoji: String) = session.addRecentEmoji(emoji)
    fun recentEmojis() = session.recentEmojis()

    fun startReply(message: ChatMessage) { replyingTo.value = message }
    fun cancelReply() { replyingTo.value = null }

    fun toggleFavoriteSticker(id: String) = session.toggleFavoriteSticker(id)
    fun favoriteStickerIds() = session.favoriteStickerIds()

    fun updateThemeSettings(settings: ThemeSettings) {
        val u = currentUser.value ?: return
        currentUser.value = u.copy(themeSettings = settings)
        viewModelScope.launch { runCatching { repo.updateThemeSettings(u.username, settings) } }
    }

    fun updateProfile(avatar: String, bio: String) {
        val u = currentUser.value ?: return
        currentUser.value = u.copy(avatar = avatar, bio = bio)
        viewModelScope.launch { runCatching { repo.updateProfile(u.username, avatar, bio) } }
    }

    fun uploadSticker(base64: String, name: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val newSticker = Sticker(
                id = "sticker_${System.currentTimeMillis()}",
                type = "image",
                data = base64,
                name = name.take(30),
                uploadedBy = user.username
            )
            val updated = stickers.value + newSticker
            runCatching {
                repo.saveStickers(updated)
                stickers.value = updated
                notify("✅ تم رفع الستيكر")
            }.onFailure { notify("❌ فشل رفع الستيكر") }
        }
    }

    fun deleteSticker(id: String) {
        viewModelScope.launch {
            val updated = stickers.value.filterNot { it.id == id }
            runCatching {
                repo.saveStickers(updated)
                stickers.value = updated
            }
        }
    }

    // ============================================================
    //  إجراءات المسؤول
    // ============================================================

    fun blockUser(username: String) = viewModelScope.launch {
        val admin = currentUser.value?.username ?: return@launch
        runCatching { repo.blockUser(username, admin); notify("✅ تم حظر $username") }
    }

    fun unblockUser(username: String) = viewModelScope.launch {
        runCatching { repo.unblockUser(username); notify("✅ تم إلغاء حظر $username") }
    }

    fun unbanUser(username: String) = viewModelScope.launch {
        runCatching { repo.unbanUser(username); notify("✅ تم إلغاء حظر $username") }
    }

    fun deleteUser(username: String) = viewModelScope.launch {
        runCatching { repo.deleteUser(username); notify("✅ تم حذف $username") }
    }

    fun makeAdmin(username: String) = viewModelScope.launch {
        runCatching { repo.makeAdmin(username); notify("✅ تم ترقية $username") }
    }

    fun warnUser(username: String, reason: String) = viewModelScope.launch {
        val admin = currentUser.value?.username ?: return@launch
        runCatching {
            val count = repo.warnUser(username, reason, admin)
            notify("⚠️ تم إرسال التحذير رقم $count إلى $username")
        }
    }

    fun addBadWord(word: String) = viewModelScope.launch {
        val w = word.trim().lowercase()
        if (w.isEmpty()) return@launch
        if (badWords.value.contains(w)) { notify("⚠️ الكلمة موجودة بالفعل"); return@launch }
        runCatching {
            repo.addBadWord(w)
            badWords.value = badWords.value + w
        }
    }

    fun removeBadWord(word: String) = viewModelScope.launch {
        runCatching {
            repo.removeBadWord(word)
            badWords.value = badWords.value.filterNot { it == word }
        }
    }

    fun clearAllMessages() = viewModelScope.launch {
        runCatching { repo.clearAllMessages(); notify("✅ تم مسح جميع الرسائل") }
    }
}

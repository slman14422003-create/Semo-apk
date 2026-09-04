package com.tomodachi.chat.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageStatus
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.repository.BannedWordException
import com.tomodachi.chat.data.repository.ChatRepository
import com.tomodachi.chat.data.repository.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ServiceLocator.provideChatRepository(application)
    private val authRepository = ServiceLocator.provideAuthRepository(application)

    private var currentUser: User? = null
    private var typingResetJob: Job? = null
    private var isCurrentlyTyping = false

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _replyTarget = MutableStateFlow<Message?>(null)
    val replyTarget: StateFlow<Message?> = _replyTarget.asStateFlow()

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    private val _bannedWordWarning = MutableStateFlow<String?>(null)
    val bannedWordWarning: StateFlow<String?> = _bannedWordWarning.asStateFlow()

    private var started = false

    fun start(user: User) {
        currentUser = user
        if (started) return
        started = true

        viewModelScope.launch {
            chatRepository.observeCachedMessages().collect { cached ->
                if (_messages.value.isEmpty() && cached.isNotEmpty()) {
                    _messages.value = cached
                }
            }
        }
        viewModelScope.launch {
            chatRepository.observeRemoteMessages().collect { remote ->
                _messages.value = remote
                chatRepository.cacheMessages(remote)
            }
        }
        viewModelScope.launch {
            chatRepository.observeTypingUsers(user.usernameLower).collect { names ->
                _typingUsers.value = names
            }
        }
        viewModelScope.launch {
            runCatching {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                authRepository.saveFcmToken(user.usernameLower, token)
            }
        }
    }

    fun onInputChanged(text: String) {
        val user = currentUser ?: return
        val shouldBeTyping = text.isNotBlank()
        if (shouldBeTyping != isCurrentlyTyping) {
            isCurrentlyTyping = shouldBeTyping
            viewModelScope.launch {
                chatRepository.setTyping(user.usernameLower, user.username, shouldBeTyping)
            }
        }
        typingResetJob?.cancel()
        if (shouldBeTyping) {
            typingResetJob = viewModelScope.launch {
                kotlinx.coroutines.delay(6000)
                isCurrentlyTyping = false
                chatRepository.setTyping(user.usernameLower, user.username, false)
            }
        }
    }

    fun sendMessage(text: String) {
        val user = currentUser ?: return
        if (text.isBlank()) return
        val reply = _replyTarget.value
        val editing = _editingMessage.value

        viewModelScope.launch {
            if (editing != null) {
                chatRepository.editMessage(editing.id, text.trim())
                _editingMessage.value = null
                return@launch
            }
            isCurrentlyTyping = false
            chatRepository.setTyping(user.usernameLower, user.username, false)
            val result = chatRepository.sendMessage(
                senderUsernameLower = user.usernameLower,
                senderUid = user.uid,
                senderUsername = user.username,
                senderAvatarEmoji = user.avatarEmoji,
                bubbleColorHex = user.bubbleColorHex,
                text = text.trim(),
                replyTo = reply
            )
            _replyTarget.value = null
            result.exceptionOrNull()?.let { error ->
                if (error is BannedWordException) {
                    _bannedWordWarning.value = error.message
                }
            }
        }
    }

    fun sendSticker(sticker: com.tomodachi.chat.data.model.Sticker) {
        val user = currentUser ?: return
        viewModelScope.launch {
            chatRepository.sendSticker(
                senderUid = user.uid,
                senderUsername = user.username,
                senderAvatarEmoji = user.avatarEmoji,
                bubbleColorHex = user.bubbleColorHex,
                stickerUrl = sticker.imageUrl,
                stickerLabel = sticker.label.ifBlank { sticker.emojiFallback }
            )
        }
    }

    fun retrySend(message: Message) {
        viewModelScope.launch { chatRepository.retrySend(message) }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch { chatRepository.deleteMessage(message.id) }
    }

    fun startEditing(message: Message) {
        _editingMessage.value = message
        _replyTarget.value = null
    }

    fun cancelEditing() {
        _editingMessage.value = null
    }

    fun setReplyTarget(message: Message?) {
        _replyTarget.value = message
        _editingMessage.value = null
    }

    fun toggleReaction(message: Message, emoji: String) {
        val user = currentUser ?: return
        viewModelScope.launch { chatRepository.toggleReaction(message.id, emoji, user.uid) }
    }

    fun consumeBannedWordWarning() {
        _bannedWordWarning.value = null
    }

    fun failedMessages(): List<Message> = _messages.value.filter { it.status == MessageStatus.FAILED }
}

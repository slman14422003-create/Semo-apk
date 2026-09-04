package com.tomodachi.chat.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.BannedWord
import com.tomodachi.chat.data.model.Sticker
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.repository.AdminRepository
import com.tomodachi.chat.data.repository.ServiceLocator
import com.tomodachi.chat.data.repository.StickerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository: AdminRepository = ServiceLocator.provideAdminRepository()
    private val stickerRepository: StickerRepository = ServiceLocator.provideStickerRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _bannedWords = MutableStateFlow<List<BannedWord>>(emptyList())
    val bannedWords: StateFlow<List<BannedWord>> = _bannedWords.asStateFlow()

    private val _uploadedStickers = MutableStateFlow<List<Sticker>>(emptyList())
    val uploadedStickers: StateFlow<List<Sticker>> = _uploadedStickers.asStateFlow()

    init {
        viewModelScope.launch { adminRepository.observeUsers().collect { _users.value = it } }
        viewModelScope.launch { adminRepository.observeBannedWords().collect { _bannedWords.value = it } }
        viewModelScope.launch { stickerRepository.observeCustomStickers().collect { _uploadedStickers.value = it } }
    }

    fun banPermanently(usernameLower: String, reason: String) =
        viewModelScope.launch { adminRepository.banPermanently(usernameLower, reason) }

    fun unban(usernameLower: String) =
        viewModelScope.launch { adminRepository.unban(usernameLower) }

    fun unbanTemporary(usernameLower: String) =
        viewModelScope.launch { adminRepository.unbanTemporary(usernameLower) }

    fun sendWarning(usernameLower: String, reason: String) =
        viewModelScope.launch { adminRepository.sendWarning(usernameLower, reason) }

    fun promoteToAdmin(usernameLower: String) =
        viewModelScope.launch { adminRepository.promoteToAdmin(usernameLower) }

    fun deleteUserAccount(usernameLower: String) =
        viewModelScope.launch { adminRepository.deleteUserAccount(usernameLower) }

    fun addBannedWord(word: String, addedByUid: String) =
        viewModelScope.launch { adminRepository.addBannedWord(word, addedByUid) }

    fun removeBannedWord(wordId: String) =
        viewModelScope.launch { adminRepository.removeBannedWord(wordId) }

    fun deleteSticker(sticker: Sticker) =
        viewModelScope.launch { stickerRepository.deleteSticker(sticker) }

    fun deleteAllMessages() =
        viewModelScope.launch { adminRepository.deleteAllMessages() }
}

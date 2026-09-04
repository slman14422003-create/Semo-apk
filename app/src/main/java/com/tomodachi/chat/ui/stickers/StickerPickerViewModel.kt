package com.tomodachi.chat.ui.stickers

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.Sticker
import com.tomodachi.chat.data.model.StickerPack
import com.tomodachi.chat.data.repository.ServiceLocator
import com.tomodachi.chat.data.repository.StickerRepository
import com.tomodachi.chat.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val stickerRepository: StickerRepository = ServiceLocator.provideStickerRepository()

    val builtinPacks: List<StickerPack> = stickerRepository.builtinPacks()

    private val _customStickers = MutableStateFlow<List<Sticker>>(emptyList())
    val customStickers: StateFlow<List<Sticker>> = _customStickers.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        viewModelScope.launch {
            stickerRepository.observeCustomStickers().collect { _customStickers.value = it }
        }
    }

    fun uploadSticker(context: Context, uri: Uri, uploaderUid: String, uploaderUsername: String) {
        viewModelScope.launch {
            _isUploading.value = true
            val bytes = withContext(Dispatchers.Default) { ImageCompressor.compress(context, uri) }
            if (bytes != null) {
                stickerRepository.uploadCustomSticker(bytes, uploaderUid, uploaderUsername)
            }
            _isUploading.value = false
        }
    }
}

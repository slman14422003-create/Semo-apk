package com.tomodachi.chat.ui.stickers

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.Sticker
import com.tomodachi.chat.data.model.StickerPack
import com.tomodachi.chat.data.remote.GiphyStickerApi
import com.tomodachi.chat.data.repository.ServiceLocator
import com.tomodachi.chat.data.repository.StickerRepository
import com.tomodachi.chat.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // --- ستيكرات أونلاين (Giphy) ---
    private val _onlineQuery = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()

    private val _onlineStickers = MutableStateFlow<List<Sticker>>(emptyList())
    val onlineStickers: StateFlow<List<Sticker>> = _onlineStickers.asStateFlow()

    private val _isLoadingOnline = MutableStateFlow(false)
    val isLoadingOnline: StateFlow<Boolean> = _isLoadingOnline.asStateFlow()

    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            stickerRepository.observeCustomStickers().collect { _customStickers.value = it }
        }
        loadTrendingOnlineStickers()
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

    private fun loadTrendingOnlineStickers() {
        viewModelScope.launch {
            _isLoadingOnline.value = true
            _onlineError.value = null
            GiphyStickerApi.trending()
                .onSuccess { _onlineStickers.value = it }
                .onFailure { _onlineError.value = "تعذّر تحميل الستيكرات — تحقق من الاتصال بالإنترنت" }
            _isLoadingOnline.value = false
        }
    }

    /** تُستدعى في كل ضغطة على حقل البحث؛ تُطبَّق بتأخير بسيط (debounce) كي لا
     * نُرسل طلب شبكة عند كل حرف يُكتب، بل بعد توقّف المستخدم عن الكتابة قليلاً. */
    fun onOnlineQueryChanged(query: String) {
        _onlineQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(400)
            _isLoadingOnline.value = true
            _onlineError.value = null
            val result = if (query.isBlank()) GiphyStickerApi.trending() else GiphyStickerApi.search(query)
            result
                .onSuccess { _onlineStickers.value = it }
                .onFailure { _onlineError.value = "تعذّر تحميل الستيكرات — تحقق من الاتصال بالإنترنت" }
            _isLoadingOnline.value = false
        }
    }
}

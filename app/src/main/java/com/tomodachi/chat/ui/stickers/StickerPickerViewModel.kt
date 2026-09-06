package com.tomodachi.chat.ui.stickers

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /** يعيد محاولة تحميل الستيكرات الرائجة — تُستخدم من زر "إعادة المحاولة"
     * الذي يظهر في شاشة الفشل الجديدة بتبويب "أونلاين" بدل ترك المستخدم عالقاً
     * بدون أي إجراء عند انقطاع الإنترنت لحظياً. */
    fun retryLoadingOnline() {
        if (_onlineQuery.value.isBlank()) {
            loadTrendingOnlineStickers()
        } else {
            onOnlineQueryChanged(_onlineQuery.value)
        }
    }

    /** يميّز رسالة الخطأ بين "لا يوجد اتصال إنترنت فعلياً على الجهاز" (يمكن
     * التأكد منها محلياً عبر ConnectivityManager) وبين خطأ خادم/شبكة آخر —
     * بدل رسالة عامة واحدة تقول دوماً "تحقق من الاتصال" حتى لو كان الإنترنت
     * فعلاً متصلاً ومشكلة أخرى هي السبب الحقيقي. */
    private fun isDeviceOnline(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun onlineErrorMessage(): String =
        if (isDeviceOnline()) "تعذّر تحميل الستيكرات — حاول مرة أخرى بعد قليل"
        else "لا يوجد اتصال بالإنترنت — تحقّق من الشبكة ثم أعد المحاولة"

    private fun loadTrendingOnlineStickers() {
        viewModelScope.launch {
            _isLoadingOnline.value = true
            _onlineError.value = null
            GiphyStickerApi.trending()
                .onSuccess { _onlineStickers.value = it }
                .onFailure { _onlineError.value = onlineErrorMessage() }
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
                .onFailure { _onlineError.value = onlineErrorMessage() }
            _isLoadingOnline.value = false
        }
    }
}

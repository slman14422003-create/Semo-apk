package com.tomodachi.chat.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import com.tomodachi.chat.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val usersRef get() = FirebaseModule.firestore.collection(FirestorePaths.USERS)

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _imageError = MutableStateFlow<String?>(null)
    val imageError: StateFlow<String?> = _imageError.asStateFlow()

    fun updateAvatar(usernameLower: String, emoji: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { usersRef.document(usernameLower).update("avatarEmoji", emoji).await() }
            onDone()
        }
    }

    fun updateBio(usernameLower: String, bio: String, onDone: () -> Unit) {
        val trimmed = bio.take(User.MAX_BIO_LENGTH)
        viewModelScope.launch {
            runCatching { usersRef.document(usernameLower).update("bio", trimmed).await() }
            onDone()
        }
    }

    fun updateBubbleColor(usernameLower: String, colorHex: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { usersRef.document(usernameLower).update("bubbleColorHex", colorHex).await() }
            onDone()
        }
    }

    /**
     * يضغط صورة الملف الشخصي المختارة من المعرض ويحوّلها لنص Base64، ثم يرفعها
     * مباشرة كحقل نصّي داخل وثيقة المستخدم على Firestore (بدون Firebase Storage
     * ودون أي رابط خارجي)، بناءً على الطلب الصريح لتخزينها بصيغة Base64.
     */
    fun updateProfileImage(usernameLower: String, imageUri: Uri, onDone: () -> Unit) {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _isUploadingImage.value = true
            _imageError.value = null
            val result = runCatching {
                val compressedBytes = withContext(Dispatchers.Default) {
                    ImageCompressor.compressForProfile(context, imageUri)
                } ?: error("تعذّر معالجة الصورة المختارة")
                val base64 = ImageCompressor.toBase64(compressedBytes)
                usersRef.document(usernameLower).update("profileImageBase64", base64).await()
            }
            _isUploadingImage.value = false
            if (result.isFailure) {
                _imageError.value = "تعذّر رفع الصورة، حاول مجدداً"
            }
            onDone()
        }
    }

    fun consumeImageError() {
        _imageError.value = null
    }
}

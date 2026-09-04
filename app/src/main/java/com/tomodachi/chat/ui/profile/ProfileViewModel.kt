package com.tomodachi.chat.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val usersRef get() = FirebaseModule.firestore.collection(FirestorePaths.USERS)

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
}

package com.tomodachi.chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.repository.AuthRepository
import com.tomodachi.chat.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)
    private val sessionManager = ServiceLocator.provideSessionManager(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val darkModePref: StateFlow<String> = MutableStateFlow("system").also { flow ->
        viewModelScope.launch {
            sessionManager.darkModePref.collect { flow.value = it }
        }
    }.asStateFlow()

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun refreshCurrentUser() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            authRepository.observeCurrentUser(user.usernameLower)?.let { _currentUser.value = it }
        }
    }

    fun setDarkModePref(value: String) {
        viewModelScope.launch { sessionManager.setDarkModePref(value) }
    }

    fun logout(onDone: () -> Unit) {
        val user = _currentUser.value
        viewModelScope.launch {
            if (user != null) authRepository.setOffline(user.usernameLower)
            authRepository.logout()
            _currentUser.value = null
            onDone()
        }
    }
}

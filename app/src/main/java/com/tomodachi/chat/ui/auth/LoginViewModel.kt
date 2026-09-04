package com.tomodachi.chat.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.repository.AuthRepository
import com.tomodachi.chat.data.repository.LoginResult
import com.tomodachi.chat.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Success(val user: User) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val rememberedUsername = authRepository.rememberedUsername

    fun login(username: String) {
        if (username.isBlank()) {
            _uiState.value = LoginUiState.Error("empty")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(username)) {
                is LoginResult.Success -> _uiState.value = LoginUiState.Success(result.user)
                is LoginResult.BannedPermanently -> _uiState.value =
                    LoginUiState.Error("permanent:${result.reason}")
                is LoginResult.BannedTemporarily -> {
                    val remainingMs = result.untilMillis - System.currentTimeMillis()
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs.coerceAtLeast(0))
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs.coerceAtLeast(0)) % 60
                    _uiState.value = LoginUiState.Error("temporary:${minutes}:${seconds}")
                }
                is LoginResult.Error -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}

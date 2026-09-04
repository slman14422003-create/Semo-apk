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
    data object CheckingSession : LoginUiState()
    data object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Success(val user: User) : LoginUiState()
}

enum class AuthMode { LOGIN, REGISTER }

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.CheckingSession)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(AuthMode.LOGIN)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.tryAutoLogin()
            _uiState.value = if (user != null) LoginUiState.Success(user) else LoginUiState.Idle
        }
    }

    fun setMode(newMode: AuthMode) {
        _mode.value = newMode
        resetError()
    }

    fun login(username: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            handleResult(authRepository.login(username, password))
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            handleResult(authRepository.register(username, password, confirmPassword))
        }
    }

    private fun handleResult(result: LoginResult) {
        _uiState.value = when (result) {
            is LoginResult.Success -> LoginUiState.Success(result.user)
            is LoginResult.BannedPermanently -> LoginUiState.Error("permanent:${result.reason}")
            is LoginResult.BannedTemporarily -> {
                val remainingMs = result.untilMillis - System.currentTimeMillis()
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs.coerceAtLeast(0))
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs.coerceAtLeast(0)) % 60
                LoginUiState.Error("temporary:${minutes}:${seconds}")
            }
            is LoginResult.Error -> LoginUiState.Error(result.message)
        }
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}

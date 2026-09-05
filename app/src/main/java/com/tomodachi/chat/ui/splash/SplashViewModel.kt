package com.tomodachi.chat.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.repository.AuthRepository
import com.tomodachi.chat.data.repository.ServiceLocator
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * الوجهة التي يقرّرها السبلاش بعد انتهاء التهيئة: إمّا أن المستخدم مسجّل دخوله
 * فعلاً وننتقل مباشرة للواجهة الأساسية، أو أنه يحتاج لتسجيل الدخول أولاً.
 */
sealed class SplashDestination {
    data class LoggedIn(val user: User) : SplashDestination()
    data object NeedsLogin : SplashDestination()
}

/**
 * يجمع كل عمليات تهيئة التطبيق عند الإقلاع في مكان واحد:
 * - محاولة استرجاع الجلسة الحالية (تسجيل دخول تلقائي) عبر [AuthRepository.tryAutoLogin].
 * - ضمان حدّ أدنى من مدة عرض السبلاش (حتى لا يومض الشعار لجزء من الثانية على
 *   الأجهزة/الشبكات السريعة، وحتى تكتمل حركة الدخول البصرية بشكل مريح للعين).
 * تُنفَّذ المهمّتان بالتوازي عبر async ثم ننتظر أبطأهما فقط، فلا تُهدر أي وقت.
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    companion object {
        // أقل مدة يظهر بها السبلاش، بالمللي ثانية، كي تكتمل حركة الدخول ولا يبدو
        // التطبيق وكأنه "يقفز" عند تسجيل الدخول التلقائي السريع.
        private const val MIN_SPLASH_DURATION_MS = 1200L
    }

    init {
        viewModelScope.launch {
            val minDelayJob = async { delay(MIN_SPLASH_DURATION_MS) }
            val autoLoginJob = async { runCatching { authRepository.tryAutoLogin() }.getOrNull() }

            minDelayJob.await()
            val user = autoLoginJob.await()
            _destination.value = if (user != null) {
                SplashDestination.LoggedIn(user)
            } else {
                SplashDestination.NeedsLogin
            }
        }
    }
}

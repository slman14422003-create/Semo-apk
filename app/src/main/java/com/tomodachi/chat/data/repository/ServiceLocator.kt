package com.tomodachi.chat.data.repository

import android.content.Context
import com.tomodachi.chat.data.local.AppDatabase
import com.tomodachi.chat.data.local.SessionManager

/**
 * مُوفّر بسيط (Service Locator) يبني المستودعات مرة واحدة ويشاركها في كل التطبيق.
 * تم اختياره بدل Hilt/Dagger لإبقاء المشروع بسيطاً وسهل القراءة، ويمكن استبداله
 * لاحقاً بأي إطار حقن تبعيات دون تغيير طبقة الواجهة.
 */
object ServiceLocator {
    @Volatile private var sessionManager: SessionManager? = null
    @Volatile private var authRepository: AuthRepository? = null
    @Volatile private var chatRepository: ChatRepository? = null
    @Volatile private var adminRepository: AdminRepository? = null
    @Volatile private var stickerRepository: StickerRepository? = null

    fun provideSessionManager(context: Context): SessionManager =
        sessionManager ?: synchronized(this) {
            sessionManager ?: SessionManager(context.applicationContext).also { sessionManager = it }
        }

    fun provideAuthRepository(context: Context): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: AuthRepository(provideSessionManager(context)).also { authRepository = it }
        }

    fun provideChatRepository(context: Context): ChatRepository =
        chatRepository ?: synchronized(this) {
            chatRepository ?: ChatRepository(AppDatabase.getInstance(context).messageDao()).also { chatRepository = it }
        }

    fun provideAdminRepository(): AdminRepository =
        adminRepository ?: synchronized(this) {
            adminRepository ?: AdminRepository().also { adminRepository = it }
        }

    fun provideStickerRepository(): StickerRepository =
        stickerRepository ?: synchronized(this) {
            stickerRepository ?: StickerRepository().also { stickerRepository = it }
        }
}

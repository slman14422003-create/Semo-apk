package com.tomodachi.chat.data.repository

import com.tomodachi.chat.data.local.SessionManager
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.tasks.await

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class BannedPermanently(val reason: String) : LoginResult()
    data class BannedTemporarily(val reason: String, val untilMillis: Long) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * يطبّق منطق "دخول بالاسم فقط": مصادقة مجهولة من Firebase Auth + وثيقة مستخدم
 * في Firestore يكون معرّفها اسم المستخدم بأحرف صغيرة، بحيث لو الاسم موجود
 * مسبقاً يُسجَّل دخول لنفس الحساب، ولو جديد يُنشأ تلقائياً.
 */
class AuthRepository(private val sessionManager: SessionManager) {

    private val auth get() = FirebaseModule.auth
    private val usersRef get() = FirebaseModule.firestore.collection(FirestorePaths.USERS)

    val rememberedUsername = sessionManager.rememberedUsername

    private suspend fun ensureAnonymousAuth(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("تعذّرت المصادقة المجهولة")
    }

    suspend fun login(rawUsername: String): LoginResult {
        val username = rawUsername.trim()
        if (username.isEmpty() || username.length > 24) {
            return LoginResult.Error("اسم المستخدم غير صالح")
        }
        return try {
            val uid = ensureAnonymousAuth()
            val usernameLower = username.lowercase()
            val docRef = usersRef.document(usernameLower)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val existing = snapshot.toObject(User::class.java) ?: User()
                when {
                    existing.isBannedPermanently -> return LoginResult.BannedPermanently(existing.banReason)
                    existing.isTemporarilyBanned -> return LoginResult.BannedTemporarily(
                        existing.banReason,
                        existing.bannedUntilMillis
                    )
                    else -> {
                        val updates = mapOf(
                            "uid" to uid,
                            "isOnline" to true,
                            "lastSeenMillis" to System.currentTimeMillis()
                        )
                        docRef.update(updates).await()
                        sessionManager.rememberUsername(username)
                        return LoginResult.Success(existing.copy(uid = uid, isOnline = true))
                    }
                }
            } else {
                val isInitialAdmin = usernameLower == User.INITIAL_ADMIN_USERNAME.lowercase()
                val newUser = User(
                    uid = uid,
                    username = username,
                    usernameLower = usernameLower,
                    isAdmin = isInitialAdmin,
                    isOnline = true,
                    createdAtMillis = System.currentTimeMillis()
                )
                docRef.set(newUser).await()
                sessionManager.rememberUsername(username)
                LoginResult.Success(newUser)
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "حدث خطأ غير متوقع")
        }
    }

    suspend fun observeCurrentUser(usernameLower: String): User? {
        val snapshot = usersRef.document(usernameLower).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun logout() {
        sessionManager.clearSession()
        auth.signOut()
    }

    suspend fun setOffline(usernameLower: String) {
        runCatching {
            usersRef.document(usernameLower).update(
                mapOf(
                    "isOnline" to false,
                    "lastSeenMillis" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun saveFcmToken(usernameLower: String, token: String) {
        runCatching { usersRef.document(usernameLower).update("fcmToken", token).await() }
    }
}

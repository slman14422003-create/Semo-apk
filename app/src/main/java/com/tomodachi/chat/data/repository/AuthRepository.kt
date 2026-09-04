package com.tomodachi.chat.data.repository

import com.google.firebase.auth.FirebaseAuthException
import com.tomodachi.chat.data.local.SessionManager
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class BannedPermanently(val reason: String) : LoginResult()
    data class BannedTemporarily(val reason: String, val untilMillis: Long) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * تسجيل دخول / إنشاء حساب حقيقيين باسم مستخدم وكلمة سر، مبنيين فوق
 * Firebase Auth (Email/Password) عبر إيميل مصطنع (username@tomodachi.local)
 * كي لا يحتاج المستخدم لإدخال إيميل حقيقي. وثيقة المستخدم في Firestore
 * يكون معرّفها اسم المستخدم بأحرف صغيرة.
 */
class AuthRepository(private val sessionManager: SessionManager) {

    private val auth get() = FirebaseModule.auth
    private val usersRef get() = FirebaseModule.firestore.collection(FirestorePaths.USERS)

    val rememberedUsername = sessionManager.rememberedUsername

    private fun emailFor(usernameLower: String) = "$usernameLower@tomodachi.local"

    private fun validate(username: String, password: String, confirmPassword: String? = null): String? {
        val cleanUsername = username.trim()
        return when {
            cleanUsername.isEmpty() -> "empty_username"
            cleanUsername.length > 24 -> "username_too_long"
            !cleanUsername.matches(Regex("^[A-Za-z0-9_\\u0600-\\u06FF]+$")) -> "invalid_username_chars"
            password.length < 6 -> "short_password"
            confirmPassword != null && password != confirmPassword -> "password_mismatch"
            else -> null
        }
    }

    /** يحاول استرجاع الجلسة الحالية دون الحاجة لإعادة إدخال كلمة السر. */
    suspend fun tryAutoLogin(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val usernameLower = sessionManager.rememberedUsername.first()?.lowercase() ?: return null
            val snapshot = usersRef.document(usernameLower).get().await()
            if (!snapshot.exists()) return null
            val user = snapshot.toObject(User::class.java) ?: return null
            if (user.isBanned) return null
            if (user.uid != firebaseUser.uid) return null
            usersRef.document(usernameLower).update(
                mapOf("isOnline" to true, "lastSeenMillis" to System.currentTimeMillis())
            ).await()
            user.copy(isOnline = true)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(rawUsername: String, password: String, confirmPassword: String): LoginResult {
        val username = rawUsername.trim()
        validate(username, password, confirmPassword)?.let { return LoginResult.Error(it) }
        val usernameLower = username.lowercase()
        return try {
            val docRef = usersRef.document(usernameLower)
            if (docRef.get().await().exists()) {
                return LoginResult.Error("username_taken")
            }
            val authResult = auth.createUserWithEmailAndPassword(emailFor(usernameLower), password).await()
            val uid = authResult.user?.uid ?: return LoginResult.Error("unknown")

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
        } catch (e: FirebaseAuthException) {
            LoginResult.Error(mapAuthError(e))
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "unknown")
        }
    }

    suspend fun login(rawUsername: String, password: String): LoginResult {
        val username = rawUsername.trim()
        validate(username, password)?.let { return LoginResult.Error(it) }
        val usernameLower = username.lowercase()
        return try {
            val docRef = usersRef.document(usernameLower)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                return LoginResult.Error("user_not_found")
            }
            val authResult = auth.signInWithEmailAndPassword(emailFor(usernameLower), password).await()
            val uid = authResult.user?.uid ?: return LoginResult.Error("unknown")

            val user = existing.toObject(User::class.java) ?: User()
            when {
                user.isBannedPermanently -> LoginResult.BannedPermanently(user.banReason)
                user.isTemporarilyBanned -> LoginResult.BannedTemporarily(user.banReason, user.bannedUntilMillis)
                else -> {
                    docRef.update(
                        mapOf(
                            "uid" to uid,
                            "isOnline" to true,
                            "lastSeenMillis" to System.currentTimeMillis()
                        )
                    ).await()
                    sessionManager.rememberUsername(username)
                    LoginResult.Success(user.copy(uid = uid, isOnline = true))
                }
            }
        } catch (e: FirebaseAuthException) {
            LoginResult.Error(mapAuthError(e))
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "unknown")
        }
    }

    private fun mapAuthError(e: FirebaseAuthException): String = when (e.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "username_taken"
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "wrong_password"
        "ERROR_USER_NOT_FOUND" -> "user_not_found"
        "ERROR_WEAK_PASSWORD" -> "short_password"
        "ERROR_TOO_MANY_REQUESTS" -> "too_many_requests"
        else -> e.message ?: "unknown"
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

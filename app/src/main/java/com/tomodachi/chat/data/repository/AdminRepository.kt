package com.tomodachi.chat.data.repository

import com.tomodachi.chat.data.model.BannedWord
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminRepository {

    private val firestore get() = FirebaseModule.firestore
    private val usersRef get() = firestore.collection(FirestorePaths.USERS)
    private val bannedWordsRef get() = firestore.collection(FirestorePaths.BANNED_WORDS)
    private val messagesRef get() = firestore.collection(FirestorePaths.MESSAGES)

    // --- المستخدمون ---

    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val registration = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                // ما منكسر التطبيق على خطأ صلاحيات أو اتصال — منرجّع لائحة فاضية بدل ما نرمي استثناء
                trySend(emptyList())
                return@addSnapshotListener
            }
            // وثائق قديمة/تالفة (من نظام المصادقة السابق) ما لازم توقف كل اللائحة — نتجاهلها فقط
            val safeUsers = snapshot.documents.mapNotNull { doc ->
                runCatching { doc.toObject(User::class.java) }.getOrNull()
            }
            // نضمن مفاتيح فريدة بمنع تكرار usernameLower (وثائق ناقصة usernameLower="")
            trySend(safeUsers.distinctBy { it.usernameLower.ifBlank { fallbackKey(it) } })
        }
        awaitClose { registration.remove() }
    }

    private fun fallbackKey(user: User) = user.username.ifBlank { "unknown_${user.hashCode()}" }

    suspend fun banPermanently(usernameLower: String, reason: String) {
        usersRef.document(usernameLower).update(
            mapOf("isBannedPermanently" to true, "banReason" to reason)
        ).await()
    }

    suspend fun unban(usernameLower: String) {
        usersRef.document(usernameLower).update(
            mapOf(
                "isBannedPermanently" to false,
                "bannedUntilMillis" to 0L,
                "banReason" to "",
                "warningsCount" to 0L
            )
        ).await()
    }

    suspend fun unbanTemporary(usernameLower: String) {
        usersRef.document(usernameLower).update(
            mapOf("bannedUntilMillis" to 0L, "banReason" to "")
        ).await()
    }

    suspend fun sendWarning(usernameLower: String, reason: String) {
        firestore.runTransaction { tx ->
            val docRef = usersRef.document(usernameLower)
            val snapshot = tx.get(docRef)
            val newCount = (snapshot.getLong("warningsCount") ?: 0L) + 1
            val updates = mutableMapOf<String, Any>("warningsCount" to newCount)
            if (newCount >= User.MAX_WARNINGS_BEFORE_BAN) {
                updates["isBannedPermanently"] = true
                updates["banReason"] = "تجاوز الحد الأقصى للتحذيرات (3 تحذيرات)"
            }
            tx.update(docRef, updates)
        }.await()
    }

    suspend fun promoteToAdmin(usernameLower: String) {
        usersRef.document(usernameLower).update("isAdmin", true).await()
    }

    suspend fun deleteUserAccount(usernameLower: String) {
        // حذف كل رسائل المستخدم أولاً
        val userMessages = messagesRef.whereEqualTo("senderUsername", usernameLower).get().await()
        val batch = firestore.batch()
        userMessages.documents.forEach { batch.delete(it.reference) }
        batch.delete(usersRef.document(usernameLower))
        batch.commit().await()
    }

    // --- الكلمات الممنوعة ---

    fun observeBannedWords(): Flow<List<BannedWord>> = callbackFlow {
        val registration = bannedWordsRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val safeWords = snapshot.documents.mapNotNull { doc ->
                runCatching { doc.toObject(BannedWord::class.java)?.copy(id = doc.id) }.getOrNull()
            }
            trySend(safeWords)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addBannedWord(word: String, addedByUid: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        val docRef = bannedWordsRef.document()
        docRef.set(
            BannedWord(id = docRef.id, word = trimmed, addedByUid = addedByUid, createdAtMillis = System.currentTimeMillis())
        ).await()
    }

    suspend fun removeBannedWord(wordId: String) {
        bannedWordsRef.document(wordId).delete().await()
    }

    // --- منطقة الخطر ---

    /** حذف جميع رسائل الدردشة نهائياً — يُستدعى فقط بعد تأكيد صريح من واجهة المستخدم. */
    suspend fun deleteAllMessages() {
        val allMessages = messagesRef.get().await()
        val batches = allMessages.documents.chunked(400) // حد Firestore للدفعة الواحدة هو 500
        batches.forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}

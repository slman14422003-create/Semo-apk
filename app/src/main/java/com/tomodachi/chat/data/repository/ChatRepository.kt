package com.tomodachi.chat.data.repository

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.tomodachi.chat.data.local.MessageDao
import com.tomodachi.chat.data.local.toEntity
import com.tomodachi.chat.data.local.toModel
import com.tomodachi.chat.data.model.BannedWord
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageStatus
import com.tomodachi.chat.data.model.MessageType
import com.tomodachi.chat.data.model.Reaction
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val messageDao: MessageDao) {

    private val firestore get() = FirebaseModule.firestore
    private val messagesRef get() = firestore.collection(FirestorePaths.MESSAGES)
    private val usersRef get() = firestore.collection(FirestorePaths.USERS)
    private val bannedWordsRef get() = firestore.collection(FirestorePaths.BANNED_WORDS)
    private val typingRef get() = firestore.collection(FirestorePaths.TYPING)

    /** الرسائل المخزّنة محلياً — تُعرض فوراً حتى قبل اتصال الشبكة (Offline-first). */
    fun observeCachedMessages(): Flow<List<Message>> =
        messageDao.observeMessages().map { entities -> entities.map { it.toModel() } }

    /** الاستماع اللحظي لآخر 200 رسالة من Firestore، مع تحديث الكاش المحلي تلقائياً. */
    fun observeRemoteMessages(limit: Long = 200): Flow<List<Message>> = callbackFlow {
        val registration: ListenerRegistration = messagesRef
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }.sortedBy { it.createdAtMillis }
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun cacheMessages(messages: List<Message>) {
        messageDao.upsertAll(messages.map { it.toEntity() })
        messageDao.trimToLimit(Message.MAX_CACHED_MESSAGES)
    }

    /** يفحص الكلمات الممنوعة ويطبّق حظراً مؤقتاً متصاعداً عند المخالفة، ثم يرسل الرسالة. */
    suspend fun sendMessage(
        senderUsernameLower: String,
        senderUid: String,
        senderUsername: String,
        senderAvatarEmoji: String,
        bubbleColorHex: String,
        text: String,
        replyTo: Message?
    ): Result<Unit> {
        val localId = UUID.randomUUID().toString()
        val pendingMessage = Message(
            id = localId,
            senderUid = senderUid,
            senderUsername = senderUsername,
            senderAvatarEmoji = senderAvatarEmoji,
            bubbleColorHex = bubbleColorHex,
            type = MessageType.TEXT,
            text = text,
            replyToMessageId = replyTo?.id.orEmpty(),
            replyToUsername = replyTo?.senderUsername.orEmpty(),
            replyToPreview = replyTo?.previewText().orEmpty(),
            createdAtMillis = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )
        messageDao.upsert(pendingMessage.toEntity())

        return try {
            val violation = findBannedWordViolation(text)
            if (violation != null) {
                applyEscalatingTempBan(senderUsernameLower)
            }
            val docRef = messagesRef.document()
            val finalMessage = pendingMessage.copy(id = docRef.id, status = MessageStatus.SENT)
            docRef.set(finalMessage).await()
            messageDao.deleteById(localId)
            messageDao.upsert(finalMessage.toEntity())
            if (violation != null) {
                Result.failure(BannedWordException())
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            messageDao.upsert(pendingMessage.copy(status = MessageStatus.FAILED).toEntity())
            Result.failure(e)
        }
    }

    suspend fun sendSticker(
        senderUid: String,
        senderUsername: String,
        senderAvatarEmoji: String,
        bubbleColorHex: String,
        stickerUrl: String,
        stickerLabel: String
    ): Result<Unit> {
        val localId = UUID.randomUUID().toString()
        val pendingMessage = Message(
            id = localId,
            senderUid = senderUid,
            senderUsername = senderUsername,
            senderAvatarEmoji = senderAvatarEmoji,
            bubbleColorHex = bubbleColorHex,
            type = MessageType.STICKER,
            text = stickerLabel,
            stickerUrl = stickerUrl,
            createdAtMillis = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )
        messageDao.upsert(pendingMessage.toEntity())
        return try {
            val docRef = messagesRef.document()
            val finalMessage = pendingMessage.copy(id = docRef.id, status = MessageStatus.SENT)
            docRef.set(finalMessage).await()
            messageDao.deleteById(localId)
            messageDao.upsert(finalMessage.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            messageDao.upsert(pendingMessage.copy(status = MessageStatus.FAILED).toEntity())
            Result.failure(e)
        }
    }

    suspend fun retrySend(message: Message): Result<Unit> {
        return try {
            val docRef = messagesRef.document()
            val finalMessage = message.copy(id = docRef.id, status = MessageStatus.SENT)
            docRef.set(finalMessage).await()
            messageDao.deleteById(message.id)
            messageDao.upsert(finalMessage.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, newText: String) {
        messagesRef.document(messageId).update(
            mapOf(
                "text" to newText,
                "isEdited" to true,
                "editedAtMillis" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun deleteMessage(messageId: String) {
        messagesRef.document(messageId).update(
            mapOf("isDeleted" to true, "text" to "", "stickerUrl" to "")
        ).await()
        messageDao.deleteById(messageId)
    }

    suspend fun toggleReaction(messageId: String, emoji: String, userId: String) {
        firestore.runTransaction { tx ->
            val docRef = messagesRef.document(messageId)
            val snapshot = tx.get(docRef)
            val message = snapshot.toObject(Message::class.java) ?: return@runTransaction
            val existing = message.reactions[emoji] ?: Reaction(emoji = emoji)
            val updatedIds = if (userId in existing.userIds) {
                existing.userIds - userId
            } else {
                existing.userIds + userId
            }
            val updatedReactions = message.reactions.toMutableMap()
            if (updatedIds.isEmpty()) {
                updatedReactions.remove(emoji)
            } else {
                updatedReactions[emoji] = existing.copy(userIds = updatedIds)
            }
            tx.update(docRef, "reactions", updatedReactions)
        }.await()
    }

    // --- مؤشر "يكتب الآن..." ---

    suspend fun setTyping(usernameLower: String, username: String, isTyping: Boolean) {
        val doc = typingRef.document(usernameLower)
        if (isTyping) {
            doc.set(mapOf("username" to username, "updatedAtMillis" to System.currentTimeMillis())).await()
        } else {
            doc.delete().await()
        }
    }

    fun observeTypingUsers(excludeUsernameLower: String): Flow<List<String>> = callbackFlow {
        val registration = typingRef.addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            val now = System.currentTimeMillis()
            val names = snapshot.documents
                .filter { it.id != excludeUsernameLower }
                .filter { (now - (it.getLong("updatedAtMillis") ?: 0L)) < 8000 }
                .mapNotNull { it.getString("username") }
            trySend(names)
        }
        awaitClose { registration.remove() }
    }

    // --- الكلمات الممنوعة والحظر التلقائي المتصاعد ---

    private suspend fun findBannedWordViolation(text: String): String? {
        val snapshot = bannedWordsRef.get().await()
        val words = snapshot.documents.mapNotNull { it.toObject(BannedWord::class.java)?.word }
        val lowerText = text.lowercase()
        return words.firstOrNull { it.isNotBlank() && lowerText.contains(it.lowercase()) }
    }

    private suspend fun applyEscalatingTempBan(usernameLower: String) {
        val userRef = usersRef.document(usernameLower)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(userRef)
            val currentViolations = (snapshot.getLong("violationsCount") ?: 0L) + 1
            val banSeconds = (BannedWord.BASE_BAN_SECONDS * currentViolations)
                .coerceAtMost(BannedWord.MAX_BAN_SECONDS.toLong())
            val bannedUntil = System.currentTimeMillis() + banSeconds * 1000
            tx.update(
                userRef,
                mapOf(
                    "violationsCount" to currentViolations,
                    "bannedUntilMillis" to bannedUntil,
                    "banReason" to "استخدام كلمة ممنوعة"
                )
            )
        }.await()
    }
}

class BannedWordException : Exception("الرسالة تحتوي على كلمة ممنوعة، تم إيقافك مؤقتاً")

private fun Message.previewText(): String =
    if (type == MessageType.STICKER) "🖼️ ستيكر" else text.take(60)

package com.tomodachi.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * الطبقة الوحيدة اللي تتكلم مع Firestore مباشرة. كل دالة هنا هي المعادل
 * الكوتلني الحقيقي (Firestore Android SDK) لدالة جافاسكربت مقابلة كانت
 * بملف app.js القديم - نفس أسماء المجموعات (messages, users, stickers,
 * badwords, bans, typing, blocked) بنفس مشروع Firebase (semo-chat-f5fdf)
 * تماماً، حتى تبقى البيانات القديمة (لو وجدت) متوافقة بدون أي هجرة.
 */
class ChatRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val messagesRef get() = db.collection("messages")
    private val usersRef get() = db.collection("users")
    private val stickersRef get() = db.collection("stickers")
    private val badWordsRef get() = db.collection("badwords")
    private val bansRef get() = db.collection("bans")
    private val blockedRef get() = db.collection("blocked")
    private val typingRef get() = db.collection("typing").document("status")

    // ============================================================
    //  المستخدمون / تسجيل الدخول
    // ============================================================

    suspend fun getUser(username: String): ChatUser? {
        val snap = usersRef.document(username).get().await()
        return if (snap.exists()) snap.toObject(ChatUser::class.java)?.copy(username = username) else null
    }

    suspend fun createUser(username: String, uid: String): ChatUser {
        val newUser = ChatUser(
            username = username,
            uid = uid,
            avatar = "👥",
            bio = "📝 مرحباً، أنا في Semo!",
            isAdmin = username == ADMIN_USERNAME
        )
        usersRef.document(username).set(
            mapOf(
                "username" to username,
                "uid" to uid,
                "avatar" to newUser.avatar,
                "bio" to newUser.bio,
                "isAdmin" to newUser.isAdmin,
                "online" to true,
                "blocked" to false,
                "banned" to false,
                "banCount" to 0L,
                "warnCount" to 0L,
                "themeSettings" to mapOf(
                    "mode" to "light",
                    "sentColor" to "#0084FF",
                    "receivedColor" to "#E4E6EB"
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "lastSeen" to FieldValue.serverTimestamp()
            )
        ).await()
        return newUser
    }

    suspend fun markOnline(username: String) {
        usersRef.document(username).update(
            mapOf("online" to true, "lastSeen" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun markOffline(username: String) {
        runCatching {
            usersRef.document(username).update(
                mapOf("online" to false, "lastSeen" to FieldValue.serverTimestamp())
            ).await()
        }
    }

    fun listenUsers(): Flow<List<ChatUser>> = callbackFlow {
        val reg = usersRef.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull {
                it.toObject(ChatUser::class.java)?.copy(username = it.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateThemeSettings(username: String, settings: ThemeSettings) {
        usersRef.document(username).update(
            "themeSettings", mapOf(
                "mode" to settings.mode,
                "sentColor" to settings.sentColor,
                "receivedColor" to settings.receivedColor
            )
        ).await()
    }

    suspend fun updateProfile(username: String, avatar: String, bio: String) {
        usersRef.document(username).update(mapOf("avatar" to avatar, "bio" to bio)).await()
    }

    // ============================================================
    //  الرسائل
    // ============================================================

    suspend fun sendMessage(msg: ChatMessage) {
        val map = mutableMapOf<String, Any?>(
            "username" to msg.username,
            "uid" to msg.uid,
            "text" to msg.text,
            "avatar" to msg.avatar,
            "isAdmin" to msg.isAdmin,
            "timestamp" to FieldValue.serverTimestamp(),
            "deleted" to false,
            "reactions" to emptyMap<String, String>(),
            "edited" to false,
            "sticker" to msg.sticker,
            "stickerData" to msg.stickerData
        )
        if (msg.replyTo != null) {
            map["replyTo"] = msg.replyTo
            map["replyText"] = msg.replyText
            map["replyToUser"] = msg.replyToUser
        }
        messagesRef.add(map).await()
    }

    suspend fun deleteMessage(id: String) {
        messagesRef.document(id).update("deleted", true).await()
    }

    suspend fun editMessage(id: String, newText: String) {
        messagesRef.document(id).update(
            mapOf("text" to newText, "edited" to true, "editedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    /** يرجع خريطة التفاعلات المحدَّثة بعد التبديل (إضافة/إزالة)، بنفس
     * منطق toggleReaction() الأصلي بالضبط. */
    suspend fun toggleReaction(messageId: String, emoji: String, username: String): Map<String, List<String>> {
        val docRef = messagesRef.document(messageId)
        val doc = docRef.get().await()
        if (!doc.exists()) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val raw = (doc.get("reactions") as? Map<String, List<String>>) ?: emptyMap()
        val reactions = raw.mapValues { it.value.toMutableList() }.toMutableMap()
        val users = reactions.getOrPut(emoji) { mutableListOf() }
        if (users.contains(username)) users.remove(username) else users.add(username)
        if (users.isEmpty()) reactions.remove(emoji)
        docRef.update("reactions", reactions).await()
        return reactions
    }

    /** يرجع doc موجود من عدمه + بيانات الملكية اللازمة للتحقق قبل الحذف/التعديل. */
    suspend fun getMessage(id: String): ChatMessage? =
        messagesRef.document(id).get().await().toObject(ChatMessage::class.java)

    /** آخر 100 رسالة غير محذوفة، الأحدث أولاً - نفس حد loadMessages() الأصلي. */
    fun listenRecentMessages(limit: Long = 100): Flow<List<ChatMessage>> = callbackFlow {
        val reg = messagesRef
            .whereEqualTo("deleted", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) }
                    ?.reversed() ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun clearAllMessages() {
        val snap = messagesRef.get().await()
        val batch = db.batch()
        snap.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    // ============================================================
    //  الكتابة الآن (typing indicator)
    // ============================================================

    suspend fun setTyping(username: String, isTyping: Boolean) {
        runCatching { typingRef.set(mapOf(username to isTyping), com.google.firebase.firestore.SetOptions.merge()).await() }
    }

    fun listenTyping(): Flow<Map<String, Boolean>> = callbackFlow {
        val reg = typingRef.addSnapshotListener { snap, _ ->
            @Suppress("UNCHECKED_CAST")
            val data = (snap?.data as? Map<String, Boolean>) ?: emptyMap()
            trySend(data)
        }
        awaitClose { reg.remove() }
    }

    // ============================================================
    //  الستيكرات
    // ============================================================

    suspend fun loadStickers(): List<Sticker> {
        val doc = stickersRef.document("all").get().await()
        if (!doc.exists()) return defaultStickers()
        val raw = doc.get("stickers") as? List<*> ?: return defaultStickers()
        return raw.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            Sticker(
                id = m["id"] as? String ?: return@mapNotNull null,
                type = m["type"] as? String ?: "emoji",
                emoji = m["emoji"] as? String,
                data = m["data"] as? String,
                name = m["name"] as? String,
                pack = m["pack"] as? String,
                uploadedBy = m["uploadedBy"] as? String
            )
        }.ifEmpty { defaultStickers() }
    }

    private fun defaultStickers() = listOf(
        Sticker(id = "sticker_1", emoji = "😊", type = "emoji"),
        Sticker(id = "sticker_2", emoji = "😂", type = "emoji"),
        Sticker(id = "sticker_3", emoji = "❤️", type = "emoji")
    )

    suspend fun saveStickers(stickers: List<Sticker>) {
        val raw = stickers.map {
            mapOf(
                "id" to it.id, "type" to it.type, "emoji" to it.emoji,
                "data" to it.data, "name" to it.name, "pack" to it.pack,
                "uploadedBy" to it.uploadedBy
            )
        }
        stickersRef.document("all").set(
            mapOf("stickers" to raw, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    // ============================================================
    //  الكلمات الممنوعة + الحظر المؤقت التلقائي
    // ============================================================

    suspend fun loadBadWords(): List<String> =
        badWordsRef.get().await().documents.mapNotNull { it.getString("word") }

    suspend fun addBadWord(word: String) {
        badWordsRef.document(word).set(mapOf("word" to word, "createdAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun removeBadWord(word: String) {
        badWordsRef.document(word).delete().await()
    }

    /** يطبّق نفس منطق checkBadWords() الأصلي: حظر مؤقت متصاعد (60 ثانية *
     * عدد المخالفات، حد أقصى 600). يرجع true لو حُظر المستخدم الآن. */
    suspend fun checkAndApplyBadWords(text: String, username: String, badWords: List<String>): Boolean {
        val lower = text.lowercase()
        val found = badWords.filter { lower.contains(it) }
        if (found.isEmpty()) return false

        val banDoc = bansRef.document(username).get().await()
        val count = if (banDoc.exists()) (banDoc.getLong("count") ?: 0) + 1 else 1
        bansRef.document(username).set(
            mapOf(
                "username" to username, "count" to count,
                "lastWord" to found.joinToString(", "),
                "lastUpdated" to FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge()
        ).await()

        val banDurationSec = minOf(count * 60, 600)
        val banExpiresMs = System.currentTimeMillis() + banDurationSec * 1000
        usersRef.document(username).update(
            mapOf(
                "banned" to true,
                "banReason" to "استخدام كلمات ممنوعة (${found.joinToString(", ")})",
                "banExpires" to banExpiresMs,
                "banCount" to count
            )
        ).await()
        return true
    }

    fun isBanStillActive(user: ChatUser): Boolean {
        val expires = user.banExpires ?: return false
        return System.currentTimeMillis() < expires
    }

    suspend fun clearExpiredBan(username: String) {
        usersRef.document(username).update(
            mapOf("banned" to false, "banReason" to null, "banExpires" to null)
        ).await()
    }

    // ============================================================
    //  إجراءات المسؤول
    // ============================================================

    suspend fun blockUser(username: String, byAdmin: String) {
        usersRef.document(username).update("blocked", true).await()
        blockedRef.document(username).set(
            mapOf("blockedBy" to byAdmin, "blockedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun unblockUser(username: String) {
        usersRef.document(username).update(
            mapOf("blocked" to false, "warnCount" to 0L)
        ).await()
        runCatching { blockedRef.document(username).delete().await() }
    }

    suspend fun unbanUser(username: String) {
        usersRef.document(username).update(
            mapOf("banned" to false, "banReason" to null, "banExpires" to null)
        ).await()
        runCatching { bansRef.document(username).delete().await() }
    }

    suspend fun deleteUser(username: String) {
        usersRef.document(username).delete().await()
        val msgs = messagesRef.whereEqualTo("username", username).get().await()
        val batch = db.batch()
        msgs.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun makeAdmin(username: String) {
        usersRef.document(username).update("isAdmin", true).await()
    }

    /** يرجع رقم التحذير الجديد، ويحظر تلقائياً عند الوصول لـ 3. */
    suspend fun warnUser(username: String, reason: String, byAdmin: String): Long {
        val userSnap = usersRef.document(username).get().await()
        val newCount = (userSnap.getLong("warnCount") ?: 0) + 1
        usersRef.document(username).update(
            mapOf(
                "warnCount" to newCount,
                "lastWarning" to mapOf(
                    "reason" to reason.ifBlank { "مخالفة قوانين الدردشة" },
                    "by" to byAdmin, "at" to FieldValue.serverTimestamp()
                )
            )
        ).await()
        if (newCount >= 3) {
            blockUser(username, "$byAdmin (تلقائي بعد 3 تحذيرات)")
        }
        return newCount
    }

    companion object {
        const val ADMIN_USERNAME = "slx23m"
    }
}

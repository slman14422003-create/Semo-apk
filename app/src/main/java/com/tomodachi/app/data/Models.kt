package com.tomodachi.app.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * نفس مخطط مستند "messages" بفايرستور تماماً كما كان يُنشئه sendMessage()
 * بملف app.js القديم - نفس أسماء الحقول حتى تبقى الرسائل القديمة (لو
 * وُجدت بنفس المشروع) قابلة للقراءة بدون أي هجرة بيانات.
 */
data class ChatMessage(
    @DocumentId val id: String = "",
    val username: String = "",
    val uid: String = "",
    val text: String = "",
    val avatar: String = "",
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null,
    val deleted: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val edited: Boolean = false,
    val editedAt: Date? = null,
    val replyTo: String? = null,
    val replyText: String? = null,
    val replyToUser: String? = null,
    val sticker: Boolean = false,
    val stickerData: String? = null,
    // حالة محلية بحتة (إرسال متفائل) - لا تُكتب أبداً لفايرستور
    val status: MessageStatus = MessageStatus.SENT,
    val clientId: String? = null
) {
    /** نفس تحقق owns بدالتي editMessage/deleteMessage الأصليتين: uid
     * الحقيقي أولاً لو متوفر، وإلا اسم المستخدم النصي كخيار احتياطي. */
    fun isOwnedBy(username: String, uid: String): Boolean =
        if (this.uid.isNotEmpty()) uid.isNotEmpty() && this.uid == uid else this.username == username
}

enum class MessageStatus { SENDING, SENT, FAILED }

data class ChatUser(
    @DocumentId val username: String = "",
    val uid: String = "",
    val avatar: String = "👥",
    val bio: String = "",
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    val online: Boolean = false,
    val blocked: Boolean = false,
    val banned: Boolean = false,
    val banReason: String? = null,
    val banExpires: Long? = null,
    val banCount: Long = 0,
    val warnCount: Long = 0,
    val lastSeen: Date? = null,
    val themeSettings: ThemeSettings = ThemeSettings()
)

data class ThemeSettings(
    val mode: String = "light", // "light" | "dark"
    val sentColor: String = "#0084FF",
    val receivedColor: String = "#E4E6EB"
)

/** نفس بنية عناصر مصفوفة "stickers" داخل مستند stickers/all.
 *  النوع "vector" إضافي محلي بحت (غير مخزَّن بفايرستور) للحزم الافتراضية
 *  المجانية (أنمي/ردود أفعال/حيوانات) - تُرسَم بـ Compose Canvas مباشرة
 *  بدل الاعتماد على SVG نصي كما كان بالأصل، فتبقى العملية أخف وأسرع. */
data class Sticker(
    val id: String = "",
    val type: String = "emoji", // "emoji" | "image" | "vector"
    val emoji: String? = null,
    val data: String? = null, // Base64 data URL للنوع image
    val name: String? = null,
    val pack: String? = null,
    val uploadedBy: String? = null,
    val vectorFrom: String? = null, // لون بداية التدرج (النوع vector)
    val vectorTo: String? = null    // لون نهاية التدرج (النوع vector)
)

data class BadWordEntry(
    @DocumentId val word: String = ""
)

data class BanRecord(
    @DocumentId val username: String = "",
    val count: Long = 0,
    val lastWord: String = ""
)

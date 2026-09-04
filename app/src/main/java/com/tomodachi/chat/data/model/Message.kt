package com.tomodachi.chat.data.model

enum class MessageStatus { SENDING, SENT, FAILED }
enum class MessageType { TEXT, STICKER }

data class Reaction(
    val emoji: String = "",
    val userIds: List<String> = emptyList()
) {
    val count: Int get() = userIds.size
}

/**
 * وثيقة الرسالة في مجموعة "messages" على Firestore.
 */
data class Message(
    val id: String = "",
    val senderUid: String = "",
    val senderUsername: String = "",
    val senderAvatarEmoji: String = "😀",
    val bubbleColorHex: String = "#FF6F61",
    val type: MessageType = MessageType.TEXT,
    val text: String = "",
    val stickerUrl: String = "",
    val replyToMessageId: String = "",
    val replyToUsername: String = "",
    val replyToPreview: String = "",
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAtMillis: Long = 0L,
    val editedAtMillis: Long = 0L,
    val reactions: Map<String, Reaction> = emptyMap(), // emoji -> Reaction
    // حقول محلية فقط (لا تُحفظ على السيرفر)، تُستخدم أثناء الإرسال:
    val status: MessageStatus = MessageStatus.SENT,
    val localOnly: Boolean = false
) {
    companion object {
        const val COLLECTION = "messages"
        const val MAX_CACHED_MESSAGES = 500
        val DEFAULT_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
    }
}

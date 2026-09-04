package com.tomodachi.chat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomodachi.chat.data.model.Message
import com.tomodachi.chat.data.model.MessageStatus
import com.tomodachi.chat.data.model.MessageType
import com.tomodachi.chat.data.model.Reaction

/**
 * تخزين مؤقت محلي لآخر 500 رسالة (Offline-first) — يضمن ظهور الدردشة فوراً
 * عند فتح التطبيق حتى قبل اكتمال الاتصال بالإنترنت.
 */
@Entity(tableName = "cached_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderUid: String,
    val senderUsername: String,
    val senderAvatarEmoji: String,
    val bubbleColorHex: String,
    val type: String,
    val text: String,
    val stickerUrl: String,
    val replyToMessageId: String,
    val replyToUsername: String,
    val replyToPreview: String,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val reactionsJson: String
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    senderUid = senderUid,
    senderUsername = senderUsername,
    senderAvatarEmoji = senderAvatarEmoji,
    bubbleColorHex = bubbleColorHex,
    type = type.name,
    text = text,
    stickerUrl = stickerUrl,
    replyToMessageId = replyToMessageId,
    replyToUsername = replyToUsername,
    replyToPreview = replyToPreview,
    isEdited = isEdited,
    isDeleted = isDeleted,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    reactionsJson = MessageConverters.reactionsToJson(reactions)
)

fun MessageEntity.toModel(): Message = Message(
    id = id,
    senderUid = senderUid,
    senderUsername = senderUsername,
    senderAvatarEmoji = senderAvatarEmoji,
    bubbleColorHex = bubbleColorHex,
    type = runCatching { MessageType.valueOf(type) }.getOrDefault(MessageType.TEXT),
    text = text,
    stickerUrl = stickerUrl,
    replyToMessageId = replyToMessageId,
    replyToUsername = replyToUsername,
    replyToPreview = replyToPreview,
    isEdited = isEdited,
    isDeleted = isDeleted,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    reactions = MessageConverters.reactionsFromJson(reactionsJson),
    status = MessageStatus.SENT
)

object MessageConverters {
    fun reactionsToJson(reactions: Map<String, Reaction>): String {
        // تسلسل بسيط بدون مكتبة خارجية: emoji=uid1,uid2;emoji2=uid3
        return reactions.values.joinToString(";") { r ->
            "${r.emoji}=${r.userIds.joinToString(",")}"
        }
    }

    fun reactionsFromJson(json: String): Map<String, Reaction> {
        if (json.isBlank()) return emptyMap()
        return json.split(";").filter { it.isNotBlank() }.mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size != 2) return@mapNotNull null
            val emoji = parts[0]
            val ids = parts[1].split(",").filter { it.isNotBlank() }
            emoji to Reaction(emoji = emoji, userIds = ids)
        }.toMap()
    }
}


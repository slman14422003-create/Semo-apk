package com.tomodachi.chat.data.model

/**
 * وثيقة المستخدم في مجموعة "users" على Firestore.
 * معرّف الوثيقة (document id) = اسم المستخدم بأحرف صغيرة (username.lowercase()).
 */
data class User(
    val uid: String = "",                  // Firebase Auth UID (مصادقة مجهولة)
    val username: String = "",             // الاسم كما كتبه المستخدم
    val usernameLower: String = "",         // نسخة موحّدة صغيرة للبحث/المطابقة
    val avatarEmoji: String = "😀",
    val bio: String = "",
    val bubbleColorHex: String = "#FF6F61",
    val isAdmin: Boolean = false,
    val isBannedPermanently: Boolean = false,
    val bannedUntilMillis: Long = 0L,       // 0 = غير موقوف مؤقتاً
    val banReason: String = "",
    val warningsCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeenMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val favoriteStickerIds: List<String> = emptyList(),
    val fcmToken: String = ""
) {
    val isTemporarilyBanned: Boolean
        get() = bannedUntilMillis > System.currentTimeMillis()

    val isBanned: Boolean
        get() = isBannedPermanently || isTemporarilyBanned

    companion object {
        const val COLLECTION = "users"
        const val INITIAL_ADMIN_USERNAME = "slx23m"
        const val MAX_BIO_LENGTH = 60
        const val MAX_WARNINGS_BEFORE_BAN = 3
    }
}

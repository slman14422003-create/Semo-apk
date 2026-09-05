package com.tomodachi.chat.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * وثيقة المستخدم في مجموعة "users" على Firestore.
 * معرّف الوثيقة (document id) = اسم المستخدم بأحرف صغيرة (username.lowercase()).
 *
 * ملاحظة مهمة: الحقول البوليانية التي تبدأ بـ "is" (isAdmin, isOnline, isBannedPermanently)
 * تحتاج @PropertyName صريحة، لأن مكتبة Firestore تحوّل أسماء getters من نوع isXxx() إلى اسم
 * حقل مختلف تلقائياً (مثلاً isOnline قد تُخزَّن كـ "online")، مما يسبب عدم تطابق بين القراءة
 * والكتابة. هذه الأسطر تجبر المكتبة على استخدام نفس الاسم دائماً في القراءة والكتابة.
 */
data class User(
    val uid: String = "",                  // Firebase Auth UID
    val username: String = "",             // الاسم كما كتبه المستخدم
    val usernameLower: String = "",         // نسخة موحّدة صغيرة للبحث/المطابقة
    val avatarEmoji: String = "😀",
    // صورة الملف الشخصي مخزَّنة كنص Base64 مباشرة داخل وثيقة المستخدم على Firestore
    // (بدل رابط Firebase Storage) بناءً على طلب صريح، وتبقى فارغة إن لم يرفع
    // المستخدم صورة بعد — عندها يُستخدم avatarEmoji كبديل دائماً.
    val profileImageBase64: String = "",
    val bio: String = "",
    val bubbleColorHex: String = "#FF6F61",

    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    @get:PropertyName("isBannedPermanently") @set:PropertyName("isBannedPermanently")
    var isBannedPermanently: Boolean = false,

    val bannedUntilMillis: Long = 0L,       // 0 = غير موقوف مؤقتاً
    val banReason: String = "",
    val warningsCount: Int = 0,

    @get:PropertyName("isOnline") @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    val lastSeenMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val favoriteStickerIds: List<String> = emptyList(),
    val fcmToken: String = ""
) {
    // خاصية محسوبة فقط (مش مخزَّنة بـ Firestore) — نستثنيها صراحة عشان ما تنكتب كحقل زائد
    @get:Exclude
    val isTemporarilyBanned: Boolean
        get() = bannedUntilMillis > System.currentTimeMillis()

    @get:Exclude
    val isBanned: Boolean
        get() = isBannedPermanently || isTemporarilyBanned

    companion object {
        const val COLLECTION = "users"
        const val INITIAL_ADMIN_USERNAME = "slx23m"
        const val MAX_BIO_LENGTH = 60
        const val MAX_WARNINGS_BEFORE_BAN = 3
    }
}

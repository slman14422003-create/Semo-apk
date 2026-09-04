package com.tomodachi.chat.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

/**
 * نقطة وصول واحدة لكل خدمات Firebase المستخدَمة في التطبيق.
 * لا يوجد أي WebView أو استدعاء REST يدوي — فقط Firebase Android SDK مباشرة.
 */
object FirebaseModule {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }
}

object FirestorePaths {
    const val USERS = "users"
    const val MESSAGES = "messages"
    const val BANNED_WORDS = "banned_words"
    const val STICKERS = "stickers"
    const val TYPING = "typing_status"      // مجموعة صغيرة: doc id = usernameLower
    const val META = "meta"                 // إعدادات عامة (مثلاً وقت آخر حذف شامل للرسائل)
}

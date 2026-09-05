package com.tomodachi.chat.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tomodachi.chat.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tomodachi_session")

/**
 * تذكّر آخر اسم مستخدم محلياً على الجهاز لتسجيل الدخول التلقائي، بالإضافة إلى
 * تفضيلات المظهر (وضع داكن/فاتح لا يعتمد فقط على إعدادات النظام) والستيكرات المفضّلة.
 *
 * كما تحتفظ بنسخة محلية خفيفة من ملف تعريف المستخدم (uid، الاسم، الصورة الرمزية...)
 * كي يبقى تسجيل الدخول قائماً حتى لو تعذّر الوصول لـ Firestore عند إقلاع التطبيق
 * (شبكة بطيئة/منقطعة لحظياً)، تماماً كما تفعل تطبيقات مثل واتساب.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val USERNAME = stringPreferencesKey("remembered_username")
        val DARK_MODE = stringPreferencesKey("dark_mode_pref") // "system" | "dark" | "light"
        val FAVORITE_STICKERS = stringSetPreferencesKey("favorite_stickers")

        // نسخة محلية مخبّأة من ملف المستخدم لتسجيل دخول تلقائي يعمل حتى بدون اتصال فوري
        val CACHED_UID = stringPreferencesKey("cached_uid")
        val CACHED_USERNAME = stringPreferencesKey("cached_username")
        val CACHED_AVATAR = stringPreferencesKey("cached_avatar")
        val CACHED_BUBBLE_COLOR = stringPreferencesKey("cached_bubble_color")
        val CACHED_IS_ADMIN = booleanPreferencesKey("cached_is_admin")
        val CACHED_CREATED_AT = longPreferencesKey("cached_created_at")
    }

    val rememberedUsername: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }

    suspend fun rememberUsername(username: String) {
        context.dataStore.edit { it[Keys.USERNAME] = username }
    }

    /** يحفظ لقطة خفيفة من المستخدم محلياً؛ تُستخدم كخط دفاع أخير لو تعذّر الوصول لـ Firestore. */
    suspend fun cacheUserProfile(user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_UID] = user.uid
            prefs[Keys.CACHED_USERNAME] = user.username
            prefs[Keys.CACHED_AVATAR] = user.avatarEmoji
            prefs[Keys.CACHED_BUBBLE_COLOR] = user.bubbleColorHex
            prefs[Keys.CACHED_IS_ADMIN] = user.isAdmin
            prefs[Keys.CACHED_CREATED_AT] = user.createdAtMillis
        }
    }

    /** يعيد بناء مستخدم مبسّط من آخر نسخة محفوظة محلياً، أو null إن لم توجد. */
    suspend fun readCachedUserProfile(usernameLower: String): User? {
        val prefs = context.dataStore.data.first()
        val uid = prefs[Keys.CACHED_UID] ?: return null
        val username = prefs[Keys.CACHED_USERNAME] ?: return null
        if (username.lowercase() != usernameLower) return null
        return User(
            uid = uid,
            username = username,
            usernameLower = usernameLower,
            avatarEmoji = prefs[Keys.CACHED_AVATAR] ?: "😀",
            bubbleColorHex = prefs[Keys.CACHED_BUBBLE_COLOR] ?: "#FF6F61",
            isAdmin = prefs[Keys.CACHED_IS_ADMIN] ?: false,
            isOnline = true,
            createdAtMillis = prefs[Keys.CACHED_CREATED_AT] ?: 0L
        )
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(Keys.USERNAME)
            it.remove(Keys.CACHED_UID)
            it.remove(Keys.CACHED_USERNAME)
            it.remove(Keys.CACHED_AVATAR)
            it.remove(Keys.CACHED_BUBBLE_COLOR)
            it.remove(Keys.CACHED_IS_ADMIN)
            it.remove(Keys.CACHED_CREATED_AT)
        }
    }

    val darkModePref: Flow<String> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: "system" }

    suspend fun setDarkModePref(value: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = value }
    }

    val favoriteStickers: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAVORITE_STICKERS] ?: emptySet() }

    suspend fun toggleFavoriteSticker(stickerId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_STICKERS] ?: emptySet()
            prefs[Keys.FAVORITE_STICKERS] = if (stickerId in current) {
                current - stickerId
            } else {
                current + stickerId
            }
        }
    }
}

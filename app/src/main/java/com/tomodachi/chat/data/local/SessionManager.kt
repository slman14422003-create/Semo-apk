package com.tomodachi.chat.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tomodachi_session")

/**
 * تذكّر آخر اسم مستخدم محلياً على الجهاز لتسجيل الدخول التلقائي، بالإضافة إلى
 * تفضيلات المظهر (وضع داكن/فاتح لا يعتمد فقط على إعدادات النظام) والستيكرات المفضّلة.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val USERNAME = stringPreferencesKey("remembered_username")
        val DARK_MODE = stringPreferencesKey("dark_mode_pref") // "system" | "dark" | "light"
        val FAVORITE_STICKERS = stringSetPreferencesKey("favorite_stickers")
    }

    val rememberedUsername: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }

    suspend fun rememberUsername(username: String) {
        context.dataStore.edit { it[Keys.USERNAME] = username }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.remove(Keys.USERNAME) }
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

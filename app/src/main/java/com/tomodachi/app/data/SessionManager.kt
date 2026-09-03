package com.tomodachi.app.data

import android.content.Context

/**
 * معادل localStorage البسيط - يحفظ آخر مستخدم دخل، ومعرّفات الستيكرات
 * المفضّلة محلياً على الجهاز (نفس مفاتيح الأصل: lastUser، tomodachi_favStickers).
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("tomodachi_prefs", Context.MODE_PRIVATE)

    var lastUser: String?
        get() = prefs.getString("lastUser", null)
        set(value) = prefs.edit().putString("lastUser", value).apply()

    fun favoriteStickerIds(): Set<String> =
        prefs.getStringSet("tomodachi_favStickers", emptySet()) ?: emptySet()

    fun toggleFavoriteSticker(id: String): Boolean {
        val current = favoriteStickerIds().toMutableSet()
        val nowFav = if (current.contains(id)) {
            current.remove(id); false
        } else {
            current.add(id); true
        }
        prefs.edit().putStringSet("tomodachi_favStickers", current).apply()
        return nowFav
    }

    fun clearSession() {
        prefs.edit().remove("lastUser").apply()
    }

    /** نفس getRecentEmojis()/addRecentEmoji() الأصليتين: حتى 24 عنصر،
     * الأحدث أولاً، لبناء تصنيف "الأخيرة" أعلى لوحة الإيموجي. */
    fun recentEmojis(): List<String> {
        val raw = prefs.getString("tomodachi_recentEmojis", null) ?: return emptyList()
        return raw.split("§").filter { it.isNotEmpty() }
    }

    fun addRecentEmoji(emoji: String) {
        val current = recentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val trimmed = current.take(24)
        prefs.edit().putString("tomodachi_recentEmojis", trimmed.joinToString("§")).apply()
    }
}

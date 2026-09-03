package com.tomodachi.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * معادل saveMessagesToStorage() / loadMessagesFromStorage() الأصليتين:
 * يحفظ آخر 500 رسالة محلياً (بدون بيانات الستيكرات/الصور الثقيلة - هي
 * أصلاً موجودة بفايرستور وتعود فوراً عبر المستمع الحي) حتى تظهر الدردشة
 * فوراً عند فتح التطبيق قبل اكتمال اتصال فايرستور، بدل شاشة فارغة.
 */
class LocalMessageCache(context: Context) {
    private val prefs = context.getSharedPreferences("tomodachi_msg_cache", Context.MODE_PRIVATE)

    fun save(messages: List<ChatMessage>) {
        val trimmed = messages.takeLast(500)
        val arr = JSONArray()
        trimmed.forEach { msg ->
            if (msg.sticker) return@forEach // نفس استبعاد imageData/stickerData الأصلي
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("username", msg.username)
            obj.put("uid", msg.uid)
            obj.put("text", msg.text)
            obj.put("avatar", msg.avatar)
            obj.put("isAdmin", msg.isAdmin)
            obj.put("timestamp", msg.timestamp?.time ?: 0L)
            obj.put("replyTo", msg.replyTo)
            obj.put("replyText", msg.replyText)
            obj.put("replyToUser", msg.replyToUser)
            obj.put("edited", msg.edited)
            arr.put(obj)
        }
        prefs.edit().putString("cached_messages", arr.toString()).apply()
    }

    fun load(): List<ChatMessage> {
        val raw = prefs.getString("cached_messages", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ts = o.optLong("timestamp", 0L)
                ChatMessage(
                    id = o.optString("id"),
                    username = o.optString("username"),
                    uid = o.optString("uid"),
                    text = o.optString("text"),
                    avatar = o.optString("avatar"),
                    isAdmin = o.optBoolean("isAdmin"),
                    timestamp = if (ts > 0) java.util.Date(ts) else null,
                    replyTo = o.optString("replyTo").ifEmpty { null },
                    replyText = o.optString("replyText").ifEmpty { null },
                    replyToUser = o.optString("replyToUser").ifEmpty { null },
                    edited = o.optBoolean("edited"),
                    // أي رسالة كانت محلياً "قيد الإرسال" وقت إغلاق التطبيق لا نعرف
                    // مصيرها الحقيقي - نعرضها SENT افتراضياً بدل أن تبقى عالقة.
                    status = MessageStatus.SENT
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

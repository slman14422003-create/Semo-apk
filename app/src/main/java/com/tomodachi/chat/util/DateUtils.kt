package com.tomodachi.chat.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ينسّق وقت الرسالة لعرضه أسفل فقاعة الدردشة: "HH:mm" لليوم الحالي،
 * و"يوم HH:mm" مختصر إن كانت الرسالة من يوم سابق.
 */
fun formatMessageTime(millis: Long): String {
    if (millis <= 0L) return ""
    val messageDate = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val isToday = messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return if (isToday) {
        timeFormat.format(Date(millis))
    } else {
        val dateFormat = SimpleDateFormat("d/M HH:mm", Locale.getDefault())
        dateFormat.format(Date(millis))
    }
}

/** ينسّق تاريخ انضمام المستخدم لعرضه في الملف الشخصي — مثال: "عضو منذ أبريل 2025". */
fun formatJoinDate(millis: Long): String {
    if (millis <= 0L) return ""
    val format = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    return "عضو منذ ${format.format(Date(millis))}"
}

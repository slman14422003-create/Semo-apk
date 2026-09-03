package com.tomodachi.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * منطق إشعارات الرسائل المشترك بين MainActivity (وقت التطبيق مفتوح) و
 * MessageSyncService (وقت التطبيق مغلق بالخلفية) - حتى لا يتكرر نفس الكود
 * بمكانين وتتفرّق سلوكياتهما بمرور الوقت.
 */
object NotifyHelper {

    const val MESSAGES_CHANNEL_ID = "tomodachi_messages"

    /** قناة إشعارات الرسائل الفعلية (تظهر للمستخدم وتصدر صوت/اهتزاز). */
    @JvmStatic
    fun ensureMessagesChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MESSAGES_CHANNEL_ID,
                "رسائل الدردشة",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "إشعارات وصول رسائل جديدة في Tomodachi"
            channel.enableVibration(true)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    fun postMessageNotification(context: Context, title: String?, body: String?) {
        ensureMessagesChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val builder = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_bg))
            .setContentTitle(if (title == null || title.isEmpty()) "Tomodachi" else title)
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 33
        ) {
            return  // لا صلاحية، لا نحاول إظهار إشعار قد يرمي استثناء
        }
        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

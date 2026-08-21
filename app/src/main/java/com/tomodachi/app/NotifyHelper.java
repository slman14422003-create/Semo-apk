package com.tomodachi.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * منطق إشعارات الرسائل المشترك بين MainActivity (وقت التطبيق مفتوح) و
 * MessageSyncService (وقت التطبيق مغلق بالخلفية) - حتى لا يتكرر نفس الكود
 * بمكانين وتتفرّق سلوكياتهما بمرور الوقت.
 */
final class NotifyHelper {

    static final String MESSAGES_CHANNEL_ID = "tomodachi_messages";

    private NotifyHelper() {}

    /** قناة إشعارات الرسائل الفعلية (تظهر للمستخدم وتصدر صوت/اهتزاز). */
    static void ensureMessagesChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    "رسائل الدردشة",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("إشعارات وصول رسائل جديدة في Tomodachi");
            channel.enableVibration(true);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    static void postMessageNotification(Context context, String title, String body) {
        ensureMessagesChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.brand_bg))
                .setContentTitle(title == null || title.isEmpty() ? "Tomodachi" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body == null ? "" : body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 33) {
            return; // لا صلاحية، لا نحاول إظهار إشعار قد يرمي استثناء
        }
        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());
    }
}

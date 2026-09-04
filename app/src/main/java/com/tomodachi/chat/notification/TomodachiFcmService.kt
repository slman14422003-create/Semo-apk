package com.tomodachi.chat.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomodachi.chat.MainActivity
import com.tomodachi.chat.R
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths

/**
 * تستقبل إشعارات الرسائل الجديدة المُرسَلة عبر Cloud Function (انظر functions/index.js)
 * التي تُطلَق تلقائياً عند إنشاء أي وثيقة جديدة في مجموعة "messages"، وتحفظ توكن
 * الجهاز في وثيقة المستخدم حتى يتمكن السيرفر من مخاطبته.
 */
class TomodachiFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUser = FirebaseModule.auth.currentUser ?: return
        // نبحث عن وثيقة المستخدم المرتبطة بهذا الـ uid ونحدّث توكنها.
        FirebaseModule.firestore.collection(FirestorePaths.USERS)
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.update("fcmToken", token)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Tomodachi"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).apply {
            runCatching { notify(System.currentTimeMillis().toInt(), notification) }
        }
    }
}

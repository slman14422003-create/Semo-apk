package com.tomodachi.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.tomodachi.app.data.SessionManager

/**
 * ================================================================
 *  خدمة مراقبة الرسائل بالخلفية - نسخة أصلية بالكامل
 * ================================================================
 * كانت هذه الخدمة سابقاً تحمّل WebView خفي بصفحة notify-listener.html
 * لأنها لم تكن تريد تكرار منطق فايرستور بجافا وقتها. الآن، بما أن التطبيق
 * كله أصبح كوتلن أصلي ومكتبة Firestore Android SDK موجودة أصلاً بالمشروع،
 * لا حاجة لأي WebView إطلاقاً - نراقب مجموعة "messages" مباشرة بنفس مشروع
 * Firebase (semo-chat-f5fdf) ونُصدر إشعاراً حقيقياً عند وصول رسالة جديدة
 * ليست من المستخدم الحالي نفسه.
 *
 * نفس حدود الحل الأصلي: "أفضل جهد" بدون Firebase Cloud Messaging - قد
 * يوقف النظام الخدمة بعد فترة طويلة بالخلفية على بعض الهواتف المتشددة
 * بإدارة البطارية.
 */
class MessageSyncService : Service() {

    private var registration: ListenerRegistration? = null
    private var isFirstSnapshot = true

    override fun onCreate() {
        super.onCreate()
        NotifyHelper.ensureMessagesChannel(this)
        startForeground(SERVICE_NOTIF_ID, buildServiceNotification())
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // عمداً: لا نوقف الخدمة عند إزالة التطبيق من قائمة "الأخيرة".
    }

    private fun startListening() {
        val session = SessionManager(applicationContext)
        val myUsername = session.lastUser
        if (myUsername == null) {
            stopSelf()
            return
        }

        registration = FirebaseFirestore.getInstance()
            .collection("messages")
            .whereEqualTo("deleted", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (isFirstSnapshot) {
                    // أول قراءة عند بدء الخدمة هي الرسالة الحالية الموجودة
                    // مسبقاً - لا نُنبّه عنها، فقط عن أي رسالة تصل بعدها.
                    isFirstSnapshot = false
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                val sender = doc.getString("username") ?: return@addSnapshotListener
                if (sender == myUsername) return@addSnapshotListener

                val isSticker = doc.getBoolean("sticker") == true
                val text = if (isSticker) "🎨 أرسل ستيكر" else (doc.getString("text") ?: "")
                NotifyHelper.postMessageNotification(this, sender, text)
            }
    }

    private fun buildServiceNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "مزامنة الخلفية",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "يبقي Semo يرصد الرسائل الجديدة أثناء إغلاق التطبيق"
            channel.setShowBadge(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Semo")
            .setContentText("جاري رصد الرسائل الجديدة بالخلفية")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        registration?.remove()
        registration = null
        super.onDestroy()
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "tomodachi_bg_sync"
        private const val SERVICE_NOTIF_ID = 9001
    }
}

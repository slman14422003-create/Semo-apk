package com.tomodachi.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.webkit.WebViewAssetLoader;

/**
 * ================================================================
 *  خدمة مراقبة الرسائل بالخلفية (حل مجاني بدون Firebase Cloud Messaging)
 * ================================================================
 * تبدأ فقط لما MainActivity يغيب عن الواجهة (راجع MainActivity.onStop) وتبقى
 * تعمل حتى لو المستخدم أغلق التطبيق كلياً من قائمة التطبيقات الأخيرة - لأنها
 * Foreground Service مستقلة عن دورة حياة الـ Activity، ولا نوقفها من
 * onTaskRemoved عمداً.
 *
 * الفكرة: بدل تكرار منطق فايرستور بجافا (يحتاج مكتبة Firestore Android SDK
 * منفصلة + google-services.json جديد)، نحمّل صفحة ويب خفيفة جداً
 * (assets/notify-listener.html) بنفس نطاق appassets.androidplatform.net
 * المستخدم بـ MainActivity، فتشترك بنفس تخزين WebView (localStorage/
 * IndexedDB) وبالتالي بنفس جلسة Firebase Auth المحفوظة - فتقدر تراقب
 * مجموعة "messages" بفايرستور مباشرة بنفس كود جافاسكربت الموجود أصلاً،
 * وتنادي AndroidBridge.showMessageNotification() بالضبط متل ما تفعل
 * MainActivity عادي.
 *
 * حدود هذا الحل (المستخدم اختاره عن قصد بدل FCM المدفوع): نظام أندرويد قد
 * يوقف الخدمة بعد فترة طويلة بالخلفية توفيراً للبطارية (خصوصاً على بعض
 * الهواتف اللي تتشدد بإدارة التطبيقات بالخلفية) - فهو "أفضل جهد" وليس
 * ضمان 100% مثل FCM الحقيقي.
 */
public class MessageSyncService extends Service {

    private static final String ASSET_DOMAIN = "appassets.androidplatform.net";
    private static final String SERVICE_CHANNEL_ID = "tomodachi_bg_sync";
    private static final int SERVICE_NOTIF_ID = 9001;

    private WebView webView;

    @Override
    public void onCreate() {
        super.onCreate();
        NotifyHelper.ensureMessagesChannel(this);
        startForeground(SERVICE_NOTIF_ID, buildServiceNotification());
        setupWebView();
        webView.loadUrl("https://" + ASSET_DOMAIN + "/assets/notify-listener.html");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY: لو النظام قتل العملية لضغط ذاكرة، يحاول إعادة تشغيل
        // الخدمة لاحقاً (بدون نية الـ Intent الأصلية) بدل تركها متوقفة تماماً.
        return START_STICKY;
    }

    /** لا نوقف الخدمة عند إزالة التطبيق من قائمة "الأخيرة" - هذا هو صلب
     *  ميزة "إشعارات حتى لو خرجت من التطبيق" المطلوبة. */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // عمداً: لا شيء هنا (لا نستدعي stopSelf()).
    }

    private void setupWebView() {
        webView = new WebView(getApplicationContext());
        var settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        var assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        webView.addJavascriptInterface(new SyncBridge(), "AndroidBridge");
    }

    private class SyncBridge {
        @android.webkit.JavascriptInterface
        public void showMessageNotification(String title, String body) {
            NotifyHelper.postMessageNotification(MessageSyncService.this, title, body);
        }

        /** تُنادى من notify-listener.html لو ما فيه جلسة مستخدم فعالة، حتى
         *  لا تبقى الخدمة (وإشعارها الدائم بالشريط) شغّالة بلا فائدة. */
        @android.webkit.JavascriptInterface
        public void onListenerIdle(String reason) {
            stopSelf();
        }
    }

    private android.app.Notification buildServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "مزامنة الخلفية",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("يبقي Tomodachi يرصد الرسائل الجديدة أثناء إغلاق التطبيق");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Tomodachi")
                .setContentText("جاري رصد الرسائل الجديدة بالخلفية")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

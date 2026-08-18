package com.tomodachi.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tomodachi - native WebView wrapper around the app's own web bundle
 * (assets/www). The web app itself talks to Firebase directly using the
 * fixed project configuration already embedded in js/firebase.js, so it
 * is copied into the APK unmodified - same keys, same behaviour.
 *
 * Assets are served over a virtual https:// domain via WebViewAssetLoader
 * instead of file:// so that relative paths, the service worker and the
 * Firebase SDK's storage/auth persistence all behave exactly like they do
 * on a normal website.
 */
public class MainActivity extends AppCompatActivity {

    private static final String ASSET_DOMAIN = "appassets.androidplatform.net";
    private static final int PERMISSION_REQUEST_CODE = 4321;
    public static final String NOTIF_CHANNEL_ID = "tomodachi_messages";

    private WebView webView;
    private boolean contentReady = false;

    private ValueCallback<Uri[]> filePathCallback;
    private String cameraPhotoPath;
    private final androidx.activity.result.ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> onFileChooserResult(result.getResultCode(), result.getData())
            );

    private PermissionRequest pendingWebPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // شاشة البدء الرسمية (Splash Screen API): تبقى معروضة مع أنيميشن
        // الأيقونة الافتراضي السلس لأندرويد 12+ إلى أن يصير محتوى الويب
        // جاهزاً فعلياً (contentReady)، بدل الاختفاء فوراً قبل اكتمال التحميل.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !contentReady);
        // انتقال سلس (تلاشي + تصغير خفيف) من شاشة البدء إلى المحتوى بدل
        // اختفاء مفاجئ - يعطي إحساس تطبيق أصلي حديث.
        splashScreen.setOnExitAnimationListener(splashView -> {
            splashView.getView().animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(220)
                    .withEndAction(splashView::remove)
                    .start();
        });

        super.onCreate(savedInstanceState);
        createNotificationChannel();

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // ملاحظة مهمة (سبب مشكلة "كل ما مررت تحدث الصفحة"):
        // كان WebView ملفوفاً بـ SwipeRefreshLayout، وهو مكوّن أندرويد أصلي
        // يراقب فقط تمرير WebView نفسه (scrollY الخاص بالصفحة كاملة). لكن هذا
        // التطبيق (كأي تطبيق دردشة حديث) لا يُمرّر الصفحة كاملة؛ التمرير يحدث
        // داخل حاوية الرسائل الداخلية (overflow-y: auto) بينما WebView نفسه
        // يبقى scrollY=0 دائماً. النتيجة: أي سحبة لأسفل داخل الدردشة كانت
        // تُفسَّر خطأً كـ "اسحب للتحديث" فيعيد webView.reload() تحميل الصفحة
        // بالكامل - يقطع اتصال فايرستور، يفقد مكان التمرير وحالة الكتابة.
        // الحل: إزالة SwipeRefreshLayout نهائياً (لا يوجد تطبيق دردشة حقيقي
        // - واتساب، تيليجرام، مسنجر - يُحدّث كامل الصفحة بالسحب لأسفل).
        setupWebView();
        requestRuntimePermissions();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        webView.loadUrl("https://" + ASSET_DOMAIN + "/assets/index.html");
    }

    /** قناة إشعارات أندرويد 8+ لازمة حتى تظهر إشعارات الرسائل الجديدة. */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "رسائل الدردشة",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("إشعارات وصول رسائل جديدة في Tomodachi");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString() + " TomodachiApp/1.0");

        // طبقات تحسينية للأداء - لتقريب سلاسة WebView من تطبيق أصلي (واتساب-مستوى):
        // - أولوية أعلى لعمليات الرندر حتى لا يتأخر التمرير/الأنيميشن خلف عمليات
        //   الخلفية الأخرى بالتطبيق.
        // - تفعيل تسريع الأجهزة (Hardware layer) على WebView نفسه لتصيير أسلس
        //   للأنيميشن والتمرير (CSS transitions/transform تُرندر عبر GPU).
        webView.setRenderPriority(WebView.RenderPriority.HIGH);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        settings.setOffscreenPreRaster(true);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                // Keep in-app navigation for our own asset domain; open anything
                // truly external (e.g. links the user taps) in the system browser.
                if (ASSET_DOMAIN.equals(uri.getHost())) {
                    return false;
                }
                String scheme = uri.getScheme();
                if ("https".equals(scheme) || "http".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                contentReady = true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Never silently trust bad certificates.
                handler.cancel();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                              FileChooserParams fileChooserParams) {
                filePathCallback = callback;
                return launchFileChooser(fileChooserParams);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Camera/mic access requested by the web page (e.g. avatar capture).
                pendingWebPermissionRequest = request;
                runOnUiThread(() -> {
                    boolean hasCamera = ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (hasCamera) {
                        request.grant(request.getResources());
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                    }
                });
            }
        });

        // جسر JS <-> Native: يسمح لملف pwa.js باستدعاء إشعار أندرويد حقيقي
        // (خارج Notification API القياسي بالمتصفح) مضمون الظهور طالما التطبيق
        // بالخلفية والعملية حيّة. راجع js/pwa.js -> notifyNewMessages().
        webView.addJavascriptInterface(new NativeBridge(), "AndroidBridge");
    }

    /** يُستدعى من جافاسكربت عبر window.AndroidBridge.* */
    private class NativeBridge {
        @android.webkit.JavascriptInterface
        public void showMessageNotification(String title, String body) {
            runOnUiThread(() -> postNotification(title, body));
        }
    }

    private void postNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title == null || title.isEmpty() ? "Tomodachi" : title)
                        .setContentText(body == null ? "" : body)
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(body == null ? "" : body))
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 33) {
            return; // لا صلاحية، لا نحاول إظهار إشعار قد يرمي استثناء
        }
        androidx.core.app.NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private boolean launchFileChooser(WebChromeClient.FileChooserParams params) {
        List<Intent> intents = new ArrayList<>();

        // Camera capture option.
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ignored) {
            // Fall back to gallery-only chooser if we can't create a temp file.
        }
        if (photoFile != null) {
            cameraPhotoPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (captureIntent.resolveActivity(getPackageManager()) != null) {
                intents.add(captureIntent);
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(contentSelectionIntent, "اختر صورة");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Intent[0]));

        try {
            fileChooserLauncher.launch(chooserIntent);
        } catch (Exception e) {
            filePathCallback = null;
            return false;
        }
        return true;
    }

    private void onFileChooserResult(int resultCode, @Nullable Intent data) {
        if (filePathCallback == null) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            } else if (cameraPhotoPath != null) {
                results = new Uri[]{Uri.fromFile(new File(cameraPhotoPath))};
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
        cameraPhotoPath = null;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);
    }

    private void requestRuntimePermissions() {
        List<String> toRequest = new ArrayList<>();
        String[] wanted;
        if (Build.VERSION.SDK_INT >= 33) {
            wanted = new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS};
        } else {
            wanted = new String[]{Manifest.permission.CAMERA};
        }
        for (String p : wanted) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }
        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && pendingWebPermissionRequest != null) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                pendingWebPermissionRequest.grant(pendingWebPermissionRequest.getResources());
            } else {
                pendingWebPermissionRequest.deny();
            }
            pendingWebPermissionRequest = null;
        }
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
          }

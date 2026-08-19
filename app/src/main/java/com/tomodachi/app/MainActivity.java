package com.tomodachi.app;

import android.Manifest;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
    private FrameLayout rootView;
    private boolean contentReady = false;
    // آخر قيم insets حقيقية (بالبكسل CSS) تم حسابها فعلياً من نظام
    // أندرويد - نحتفظ بها لإعادة حقنها بالصفحة عند كل تحميل/إعادة تحميل
    // (مثلاً عند فتح التطبيق من الخلفية)، لأن env(safe-area-inset-*) وحدها
    // غير موثوقة عبر كل الشركات المصنّعة (راجع setupNativeInsetsBridge).
    private int lastSafeTopPx = 0;
    private int lastSafeBottomPx = 0;
    private int lastKeyboardPx = 0;

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

        // Edge-to-edge حقيقي: المحتوى (WebView) يُرسم تحت شريط الحالة/التنقل
        // بدل أن يتوقف عنده. هذا هو الفرق الأساسي بين "تطبيق فيه WebView"
        // و"تطبيق حقيقي" بصرياً - المتصفحات لا تفعل هذا، التطبيقات الأصلية
        // (واتساب/تيليجرام) نعم. صفحات الويب تستخدم أصلاً env(safe-area-inset-*)
        // (راجع css/theme.css) فهي جاهزة لعرض محتواها الصحيح تحت الشريطين.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applyStatusBarStyle(false); // أيقونات فاتحة مبدئياً (خلفية العلامة التجارية داكنة)

        FrameLayout root = new FrameLayout(this);
        rootView = root;
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // إصلاح "الشاشة/شريط الإرسال غير مضبوط" على بعض أجهزة/شركات معيّنة:
        // env(safe-area-inset-*) وحدها بملف CSS غير كافية - كل شركة (هواوي/
        // شاومي/سامسونج/أوبو...) تُعدّل نسخة WebView النظامية بجهازها بطريقة
        // مختلفة، وبعضها لا يُبلّغ قيم safe-area الصحيحة لصفحة الويب إطلاقاً
        // (تحديداً ارتفاع شريط التنقل السفلي بالأزرار الثلاثة، وارتفاع لوحة
        // المفاتيح). الحل الموثوق: نقيس الـ insets الحقيقية من نظام أندرويد
        // نفسه (WindowInsetsCompat - نفس المصدر اللي تعتمد عليه كل التطبيقات
        // الأصلية) ونحقنها كمتغيرات CSS مباشرة بالصفحة، فتصير القيمة صحيحة
        // ومضمونة على أي شركة أو حجم شاشة (هاتف أو تابلت) بدل الاعتماد على
        // تطبيق WebView الجزئي/غير المتّسق لـ env().
        setupNativeInsetsBridge(root);

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
        NotifyHelper.ensureMessagesChannel(this);
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
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

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
                // الصفحة الجديدة (أول تحميل أو أي إعادة تحميل) تبدأ بدون
                // متغيرات CSS المحقونة سابقاً - نعيد دفع آخر قيم insets
                // معروفة فوراً حتى لا تظهر لحظة واحدة بواجهة غير مضبوطة.
                pushInsetsToWebView(lastSafeTopPx, lastSafeBottomPx, lastKeyboardPx);
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

    /**
     * يقيس status bar / navigation bar / لوحة المفاتيح الحقيقية من نظام
     * أندرويد نفسه (بدل الاعتماد فقط على env(safe-area-inset-*) داخل
     * WebView غير الموثوقة عبر كل الشركات) ويحقنها كمتغيرات CSS بالصفحة،
     * مع إعادة الحساب تلقائياً كل مرة تتغيّر فيها (فتح/غلق لوحة المفاتيح،
     * تدوير الشاشة، تغيّر شريط التنقل). يعمل بنفس الطريقة على أي هاتف أو
     * تابلت بغض النظر عن الشركة المصنّعة.
     */
    private void setupNativeInsetsBridge(FrameLayout root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets ime =
                    insets.getInsets(WindowInsetsCompat.Type.ime());
            float density = getResources().getDisplayMetrics().density;

            int safeTopPx = Math.round(systemBars.top / density);
            int safeBottomPx = Math.round(systemBars.bottom / density);
            // وقت ظهور لوحة المفاتيح، ارتفاعها عادة يشمل ارتفاع شريط
            // التنقل السفلي أيضاً على أغلب الأجهزة - نستخدم الفرق فقط حتى
            // لا يُضاف الارتفاعان فوق بعض (تباعد إضافي غير مرغوب).
            int keyboardPx = Math.round(Math.max(0, ime.bottom - systemBars.bottom) / density);

            lastSafeTopPx = safeTopPx;
            lastSafeBottomPx = safeBottomPx;
            lastKeyboardPx = keyboardPx;
            pushInsetsToWebView(safeTopPx, safeBottomPx, keyboardPx);

            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void pushInsetsToWebView(int safeTopPx, int safeBottomPx, int keyboardPx) {
        if (webView == null) return;
        String js = "document.documentElement.style.setProperty('--native-safe-top','" + safeTopPx + "px');"
                + "document.documentElement.style.setProperty('--native-safe-bottom','" + safeBottomPx + "px');"
                + "document.documentElement.style.setProperty('--native-kb-inset','" + keyboardPx + "px');";
        webView.evaluateJavascript(js, null);
    }

    /**
     * يبدّل لون أيقونات شريط الحالة/التنقل (فاتحة على خلفية داكنة، أو داكنة
     * على خلفية فاتحة) لتبقى مقروءة دائماً مهما كان ثيم التطبيق الحالي.
     * @param lightBackground true لو خلفية الواجهة الحالية فاتحة (فيصير لون
     *                         الأيقونات داكناً حتى تظهر فوقها بوضوح).
     */
    private void applyStatusBarStyle(boolean lightBackground) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(lightBackground);
        controller.setAppearanceLightNavigationBars(lightBackground);
    }

    /** يُستدعى من جافاسكربت عبر window.AndroidBridge.* */
    private class NativeBridge {
        @android.webkit.JavascriptInterface
        public void showMessageNotification(String title, String body) {
            runOnUiThread(() -> postNotification(title, body));
        }

        /** يُستدعى من js/themes.js في كل مرة يتغيّر فيها الوضع الليلي/النهاري. */
        @android.webkit.JavascriptInterface
        public void setStatusBarStyle(boolean lightBackground) {
            runOnUiThread(() -> applyStatusBarStyle(lightBackground));
        }
    }

    private void postNotification(String title, String body) {
        NotifyHelper.postMessageNotification(this, title, body);
    }

    // ================================================================
    // خدمة مراقبة الرسائل بالخلفية (راجع MessageSyncService.java): تعمل
    // فقط أثناء غياب هذا الـ Activity عن الواجهة (بعد الضغط على الرئيسية،
    // تبديل التطبيق، أو حتى إغلاقه تماماً من قائمة التطبيقات الأخيرة) حتى
    // تصل إشعارات رسائل جديدة رغم إن التطبيق نفسه غير مفتوح. تُوقَف فوراً
    // عند عودة المستخدم للتطبيق (onStart) لتفادي استماعين لنفس البيانات
    // بنفس الوقت (استهلاك بطارية/بيانات مضاعف بلا فائدة).
    // ملاحظة: هذا حل "أفضل جهد" مجاني بدون Firebase Cloud Messaging - لا
    // يضمن وصول الإشعار 100% إذا أوقف النظام الخدمة لتوفير البطارية بعد
    // فترة طويلة من الخلفية (خصوصاً بعض هواتف شاومي/هواوي المتشددة بهذا
    // الجانب)، لكنه يعمل بشكل موثوق لفترة معقولة على أغلب الأجهزة.
    @Override
    protected void onStart() {
        super.onStart();
        stopService(new Intent(this, MessageSyncService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            ContextCompat.startForegroundService(this, new Intent(this, MessageSyncService.class));
        } catch (Exception ignored) {
            // لو فشل التشغيل لأي سبب (مثلاً قيود نظام على بعض الأجهزة)، لا
            // نكسر تجربة إغلاق/تصغير التطبيق العادية بسببه.
        }
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

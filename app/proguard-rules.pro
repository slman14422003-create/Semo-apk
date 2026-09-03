# ================================================================
# قواعد R8/ProGuard - نسخة كوتلن أصلية (لا يوجد WebView بعد الآن)
# ================================================================

# نماذج البيانات في data/ تُقرأ عبر انعكاس Firestore (toObject) - يجب
# الاحتفاظ بأسماء الحقول والـ constructors كما هي بدون تعتيم/حذف.
-keepclassmembers class com.tomodachi.app.data.** {
    <init>(...);
    <fields>;
}
-keep class com.tomodachi.app.data.** { *; }

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.core.splashscreen.** { *; }
-keep class androidx.core.content.FileProvider { *; }
-dontwarn androidx.**

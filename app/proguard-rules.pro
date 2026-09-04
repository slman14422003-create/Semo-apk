# ================================================================
# قواعد R8/ProGuard لنسخة الـ release
# ================================================================
# التطبيق كوتلن أصلي بالكامل (Jetpack Compose + Firestore SDK) بدون أي
# WebView أو جسر جافاسكربت، لذا لا حاجة لقواعد @JavascriptInterface.

# نماذج البيانات (Models.kt) تُقرأ/تُكتب بفايرستور عبر Reflection
# (toObject/PropertyName) - احتفظ بأسماء حقولها كاملة وإلا فشلت قراءة/كتابة
# المستندات بصمت بعد التصغير.
-keepclassmembers class com.tomodachi.app.data.** {
    <fields>;
    <init>(...);
}
-keep class com.tomodachi.app.data.** { *; }

-keep class androidx.core.splashscreen.** { *; }
-keep class androidx.core.content.FileProvider { *; }

# قواعد Firebase الموصى بها رسمياً
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }

-dontwarn androidx.**

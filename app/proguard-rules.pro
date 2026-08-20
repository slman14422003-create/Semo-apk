# ================================================================
# قواعد R8/ProGuard لنسخة الـ release
# ================================================================
# تفعيل التصغير/التعتيم (minifyEnabled) بدون هذه القواعد يكسر جسر الجافاسكربت
# <-> الجافا (addJavascriptInterface) لأن R8 يحذف/يعيد تسمية أي دالة غير
# مستخدَمة ظاهرياً من كود جافا آخر - وميثودات @JavascriptInterface تُستدعى
# فقط من جهة WebView/جافاسكربت، فيظنها R8 "غير مستخدمة" ويحذفها، فتتوقف
# الإشعارات وتبديل لون شريط الحالة عن العمل بصمت بدون أي خطأ ظاهر.

# احتفظ بكل دوال أي كلاس يحمل ميثودات @JavascriptInterface + الكلاس نفسه
# (مطلوب أيضاً عدم تغيير اسمه لأن WebView يربطه بالاسم المُمرَّر لـ
# addJavascriptInterface وقت التشغيل، وليس عبر مرجع نوع ثابت وقت الترجمة).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep,allowobfuscation @interface android.webkit.JavascriptInterface

# احتفظ بكل شيء داخل حزمة التطبيق نفسها (طبقة رقيقة جداً - 3 ملفات فقط
# بلا أي منطق حساس يستحق التعتيم، وأي حذف خاطئ هنا يكسر MainActivity كاملاً).
-keep class com.tomodachi.app.** { *; }

# القواعد القياسية الموصى بها من AndroidX/Material نفسها (تُدرَج تلقائياً
# عادة عبر consumer-rules.pro بكل مكتبة، هذا احتياط إضافي فقط).
-keep class androidx.core.splashscreen.** { *; }
-keep class androidx.core.content.FileProvider { *; }

# لا داعي لتحذيرات مكتبات AndroidX نفسها وقت البناء (لا تؤثر على سلامة الكود).
-dontwarn androidx.**

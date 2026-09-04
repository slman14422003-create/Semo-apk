# Semo — تطبيق دردشة Android (Kotlin أصلي + Jetpack Compose + Firebase)

تطبيق دردشة جماعية مباشرة، مكتوب بالكامل بلغة **Kotlin** باستخدام **Jetpack Compose**
للواجهة و **Firebase (Auth + Firestore)** كقاعدة بيانات لحظية. لا يوجد أي WebView أو
كود جافاسكربت داخل التطبيق — كل شيء (تسجيل الدخول، الرسائل، الردود، التفاعلات،
الستيكرات، لوحة تحكم المسؤول، الثيمات) منطق Kotlin/Compose أصلي مباشر.

## ما الذي تغيّر في هذه المراجعة

كانت شيفرة الواجهة (Kotlin/Compose) وملفات إعداد البناء (Gradle) في نسختين غير
متطابقتين: الكود مكتوب بالكامل كتطبيق Compose/Firebase أصلي، لكن `app/build.gradle`
كان لا يزال معدّاً لتطبيق WebView قديم (بلا أي اعتماديات Compose أو Firebase على
الإطلاق) — أي أن **المشروع لم يكن قابلاً للبناء إطلاقاً** بصيغته السابقة. أهم الإصلاحات:

- إضافة كل اعتماديات Jetpack Compose (BOM، material3، material-icons-extended،
  activity-compose، lifecycle-viewmodel-compose) وتفعيل `buildFeatures.compose`.
- إضافة اعتماديات Firebase (BOM، Auth، Firestore) وربط بلجن `google-services`
  (كان مفقوداً بالكامل من `build.gradle` الجذري رغم وجود `google-services.json`).
- إضافة `kotlinx-coroutines-play-services` (مطلوبة لـ `.await()` المستخدَمة في
  طبقة `ChatRepository`).
- حذف مهمة `downloadVendorLibs` وقواعد `@JavascriptInterface` بـ ProGuard —
  بقايا من نسخة WebView قديمة لم يعد لها أي استخدام (لا يوجد مجلد `assets/` أصلاً).
- إصلاح لون نص فقاعة الرسالة ليتكيّف تلقائياً (أبيض/أسود) حسب سطوع لون الفقاعة
  الفعلي بدل لون ثابت، حتى يبقى مقروءاً مهما كان اللون الذي يختاره المستخدم.
- إصلاح أيقونة الرجوع بلوحة المسؤول لتنعكس صح مع اتجاه الواجهة العربي (RTL).
- تفعيل شرائح "القفز السريع" بين تصنيفات الإيموجي (كانت بلا أي وظيفة).
- تحديث الثيم العام (ألوان/خطوط/زوايا) وتصميم شاشتي الدخول والدردشة بشكل أكثر
  اتساقاً وحداثة.

> ⚠️ **تنبيه أمان**: ملف `keystore.properties` المحلي (مستثنى من git عبر
> `.gitignore`) يحتوي كلمة مرور keystore بنص صريح. يُنصح بشدة بتوليد keystore
> جديد بكلمة مرور جديدة قبل أي استخدام فعلي/نشر، خاصة إن كانت هذه القيمة قد
> ظهرت لأي طرف آخر من قبل.

## البنية

```
Semo/
├── app/src/main/java/com/tomodachi/app/
│   ├── MainActivity.kt              ← نقطة البداية + التنقل بين الشاشات
│   ├── MessageSyncService.kt        ← إشعارات الرسائل بالخلفية
│   ├── NotifyHelper.kt              ← بناء الإشعارات
│   ├── data/                        ← النماذج + طبقة Firestore (ChatRepository)
│   └── ui/
│       ├── theme/                   ← الثيم العام (Theme.kt)
│       ├── components/              ← فقاعة الرسالة، لوحة الإيموجي، الملف الشخصي
│       ├── screens/                 ← تسجيل الدخول، الدردشة، لوحة المسؤول
│       └── viewmodel/                ← ChatViewModel (منطق الحالة الكامل)
├── app/google-services.json         ← إعداد مشروع Firebase (semo-chat-f5fdf)
└── .github/workflows/build-apk.yml  ← بناء تلقائي عبر GitHub Actions
```

## البناء محلياً

افتح المجلد في Android Studio (Iguana أو أحدث) واختر Run. تأكد أولاً من:

1. وجود `app/google-services.json` (موجود بالفعل بهذا المستودع).
2. اتصال إنترنت لتنزيل اعتماديات Gradle (Compose BOM، Firebase BOM...) عند أول بناء.

أو عبر الطرفية:

```bash
./gradlew assembleDebug
```

## البناء عبر GitHub Actions

ورشة العمل `.github/workflows/build-apk.yml` تبني `debug` و`release` تلقائياً
وتُنشئ Release جاهزاً بمجرد الدفع لفرع `main`، أو عند إنشاء Tag بصيغة `v1.0.0`.

```bash
git init
git add .
git commit -m "Semo — Kotlin/Compose/Firebase chat app"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

## التثبيت على الهاتف

نزّل `Semo-release.apk` من صفحة Releases، فعّل "تثبيت من مصادر غير معروفة" إذا
طُلب منك ذلك، ثم افتح الملف للتثبيت.

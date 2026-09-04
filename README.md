# Tomodachi 🎌

تطبيق دردشة جماعية حقيقي بلغة **Kotlin** (Jetpack Compose) لأندرويد — **ليس PWA ولا WebView**، بل مشروع Android Studio كامل يبني ملف APK/AAB حقيقياً.

## المزايا المطبَّقة

- تسجيل دخول بالاسم فقط (بدون كلمة مرور)، والاسم `slx23m` يصبح مسؤولاً تلقائياً عند أول تسجيل.
- دردشة جماعية لحظية (Firestore Realtime) مع رد، تعديل، حذف، تفاعلات إيموجي، ومؤشر "يكتب الآن".
- تخزين محلي (Room) لآخر 500 رسالة — Offline-first، تظهر الدردشة فوراً حتى بدون إنترنت.
- إعادة محاولة تلقائية عند فشل الإرسال.
- حظر تلقائي متصاعد عند استخدام كلمة ممنوعة (60 ثانية × عدد المخالفات، حتى 10 دقائق).
- لوحة إيموجي كاملة (10 تصنيفات) + 3 حزم ستيكرات افتراضية مرسومة بالكامل + رفع ستيكرات مخصّصة (بضغط تلقائي للصورة).
- ملف شخصي: صورة رمزية، نبذة، لون فقاعة رسائل، وضع داكن/فاتح قابل للتبديل الحر.
- لوحة تحكم مسؤول: إدارة المستخدمين (حظر دائم/مؤقت، تحذيرات، ترقية، حذف حساب)، الكلمات الممنوعة، الستيكرات، ومنطقة خطر لحذف كل الرسائل.
- إشعارات Push حقيقية عبر Firebase Cloud Messaging + Cloud Function مرفقة.

## ⚠️ خطوة إلزامية قبل البناء: google-services.json

الإعدادات التي أرسلتها هي **Firebase Web SDK config** (تبدأ بـ `initializeApp` من `firebase/app`)، وهذه **لا تصلح مباشرة لتطبيق أندرويد**. يجب تسجيل تطبيق أندرويد جديد داخل نفس مشروع Firebase `semo-chat-f5fdf`:

1. من [Firebase Console](https://console.firebase.google.com) افتح مشروع `semo-chat-f5fdf`.
2. أضف تطبيق جديد → أندرويد → معرّف الحزمة **بالضبط**: `com.tomodachi.chat`.
3. نزّل ملف `google-services.json` الحقيقي وضعه في `app/google-services.json` (يوجد قالب توضيحي في `app/google-services.json.template`).
4. فعّل من قسم Build في الكونسول: **Authentication → تسجيل الدخول المجهول (Anonymous)**، و**Firestore Database**، و**Storage**، و**Cloud Messaging**.

## البناء محلياً

```bash
# يتطلب Android Studio (Koala أو أحدث) أو Android SDK + JDK 17
./gradlew assembleRelease
# الملف الناتج: app/build/outputs/apk/release/app-release-unsigned.apk
```

لبناء نسخة موقَّعة جاهزة للنشر، أنشئ keystore وأضف توقيعاً في `app/build.gradle.kts` ضمن `signingConfigs`، أو وقّع الملف يدوياً عبر `apksigner`.

## نشر Cloud Function للإشعارات

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only functions,firestore:rules
```

هذه الدالة (`functions/index.js`) تُطلَق تلقائياً عند وصول أي رسالة جديدة وترسل إشعاراً لكل المستخدمين المتصلين بتوكن FCM محفوظ.

## رفع المشروع على GitHub (Release حقيقي، بدون PWA)

```bash
cd Tomodachi
git init
git add .
git commit -m "Initial commit: Tomodachi Android app"
git branch -M main
git remote add origin https://github.com/USERNAME/tomodachi.git
git push -u origin main

# لإصدار Release مرفق بملف APK:
git tag v1.0.0
git push origin v1.0.0
# ثم من صفحة GitHub → Releases → Draft a new release → ارفع app-release.apk يدوياً
```

> ملاحظة: `app/google-services.json` مُدرَج ضمن `.gitignore` عمداً لأنه يحتوي مفاتيح المشروع؛ لا ترفعه إلى مستودع عام. استخدم GitHub Actions Secrets إن رغبت ببناء تلقائي (CI).

## GitHub Actions (CI/CD) — بناء ونشر تلقائي

المشروع يحتوي 3 ووركفلوهات جاهزة في `.github/workflows/`:

| الملف | يعمل متى | الوظيفة |
|---|---|---|
| `build.yml` | كل push/PR على `main` | يبني نسخة debug ويشغّل lint، ويرفع الـ APK كـ Artifact |
| `release.yml` | عند دفع تاغ مثل `v1.0.0` | يبني APK للإصدار ويوقّعه (إن توفر keystore) وينشئ GitHub Release تلقائياً مع إرفاق الملف |
| `deploy-functions.yml` | عند تعديل `functions/` أو `firestore.rules` | ينشر Cloud Function والقواعد إلى Firebase تلقائياً |

### الأسرار (Secrets) المطلوب ضبطها

من **Settings → Secrets and variables → Actions** في مستودعك على GitHub، أضف:

| اسم السر | إلزامي؟ | كيف تحصل عليه |
|---|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | نعم (لعمل Release) | بعد تنزيل `google-services.json` الحقيقي: `base64 -w0 app/google-services.json` وانسخ الناتج |
| `KEYSTORE_BASE64` | اختياري (لتوقيع الـ APK) | `keytool -genkey -v -keystore release.keystore -alias tomodachi -keyalg RSA -keysize 2048 -validity 10000` ثم `base64 -w0 release.keystore` |
| `KEYSTORE_PASSWORD` | مطلوب إذا استخدمت `KEYSTORE_BASE64` | كلمة مرور الـ keystore |
| `KEY_ALIAS` | مطلوب إذا استخدمت `KEYSTORE_BASE64` | مثال: `tomodachi` |
| `KEY_PASSWORD` | مطلوب إذا استخدمت `KEYSTORE_BASE64` | كلمة مرور المفتاح |
| `FIREBASE_TOKEN` | اختياري (لنشر Cloud Functions تلقائياً) | شغّل `firebase login:ci` محلياً وانسخ التوكن الناتج |

بدون `KEYSTORE_BASE64`، سيبني `release.yml` ملف APK **غير موقّع** (unsigned) — يعمل تماماً لكن لا يُنصح بتوزيعه للمستخدمين النهائيين قبل توقيعه.

### تشغيل إصدار جديد

```bash
git add .
git commit -m "chore: bump version"
git tag v1.0.1
git push origin main --tags
```

بعدها راقب تبويب **Actions** في GitHub؛ عند اكتمال `release.yml` سيظهر إصدار جديد تلقائياً في تبويب **Releases** مع ملف الـ APK مرفقاً.

## هيكل المشروع

```
Tomodachi/
├── .github/
│   └── workflows/          ← ووركفلوهات البناء والإصدار ونشر Cloud Functions
├── app/                    ← تطبيق أندرويد (Kotlin + Compose)
│   └── src/main/java/com/tomodachi/chat/
│       ├── data/           ← النماذج، Room، مستودعات Firebase
│       ├── ui/              ← الشاشات (تسجيل الدخول، الدردشة، الملف الشخصي، لوحة التحكم)
│       ├── notification/    ← FCM
│       └── util/            ← أدوات مساعدة (إيموجي، ألوان، ضغط الصور)
├── functions/               ← Cloud Function لإرسال الإشعارات
├── firestore.rules          ← قواعد أمان Firestore (مبدئية — راجعها قبل الإنتاج)
└── README.md
```

## معرّف الحزمة (Package ID)

`com.tomodachi.chat` — غيّره في `app/build.gradle.kts` (`applicationId` و `namespace`) وفي `AndroidManifest.xml` إن أردت معرّفاً مختلفاً، وسجّله بنفس الاسم في Firebase.

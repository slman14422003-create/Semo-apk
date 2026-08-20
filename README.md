# Tomodachi — تطبيق Android (Java WebView)

هذا مشروع Android Studio كامل بلغة **Java** يحوّل تطبيق Tomodachi (الموجود أصلاً كـ PWA بـ HTML/CSS/JS + Firebase)
إلى **APK حقيقي قابل للتثبيت**. كل إعدادات Firebase (المفتاح الثابت `apiKey`, `projectId`... إلخ)
منسوخة كما هي بدون أي تعديل من `js/firebase.js` الأصلي — نفس المشروع، نفس القاعدة، بدون تغيير.

الفكرة: `MainActivity.java` يفتح WebView يعرض ملفات التطبيق (`app/src/main/assets/`) عبر نطاق
افتراضي آمن (`https://appassets.androidplatform.net`) بدل `file://`، فتعمل كل مكتبات Firebase وخدمة
الـ Service Worker تماماً كما تعمل على موقع حقيقي، مع دعم كامل لرفع الصور من الكاميرا/المعرض
(لصفحة تعديل الأفاتار مثلاً) وصلاحيات وقت التشغيل.

## أنا لا أستطيع رفعه لجيتهاب نيابةً عنك

ليس لدي اتصال إنترنت ولا صلاحيات دخول لحساب جيتهاب الخاص بك (ولا يجب أن يكون لأي أداة ذكاء
اصطناعي ذلك). لكن الجزء الذي طلبته — أن يبني جيتهاب الـ APK تلقائياً ويجهزه كـ Release جاهز
للتحميل بمجرد الرفع — **موجود بالكامل** في `.github/workflows/build-apk.yml`. أنت فقط تحتاج ترفع
هذا المجلد لمرة واحدة.

## خطوات الرفع (دقيقتان)

```bash
cd Tomodachi-android          # هذا المجلد
git init
git add .
git commit -m "Initial Android (Java) build of Tomodachi"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

بمجرد الرفع (push) لفرع `main`، ستجد في تبويب **Actions** بالمستودع أن ورشة العمل
`Build Tomodachi APK` بدأت تلقائياً. عند اكتمالها (٥-٨ دقائق) اذهب لتبويب **Releases**
بالمستودع — ستجد إصدار جديد جاهز يحتوي على:
- `Tomodachi-debug.apk`
- `Tomodachi-release.apk` (هذا هو المُوصى بتثبيته على الجهاز)

كل عملية `push` جديدة لفرع `main`، أو كل مرة تنشئ فيها Tag بصيغة `v1.0.0` مثلاً
(`git tag v1.0.0 && git push origin v1.0.0`)، تُنشئ Release جديد تلقائياً بنفس الطريقة.

## التثبيت على الهاتف

نزّل `Tomodachi-release.apk` من صفحة Releases على هاتف Android، وفعّل خيار
"تثبيت من مصادر غير معروفة" إذا طُلب منك ذلك، ثم افتح الملف للتثبيت.

> ملاحظة: الـ APK موقّع حالياً بمفتاح تصحيح (debug key) الجاهز تلقائياً من أدوات جوجل — هذا
> يجعله يعمل ويُثبّت فوراً بدون أي إعداد إضافي، لكنه غير صالح للنشر على متجر Google Play.
> لو احتجت لاحقاً نشره على المتجر، تحتاج مفتاح توقيع release حقيقي (keystore) تحتفظ به سرياً
> كـ GitHub Secret — أخبرني وأجهزه لك.

## بنية المشروع

```
Tomodachi-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/tomodachi/app/MainActivity.java   ← الكود الأساسي (Java)
│   │   ├── assets/                                     ← تطبيق الويب كاملاً (HTML/CSS/JS + Firebase)
│   │   ├── res/                                        ← أيقونات، شاشة البداية، الألوان
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── .github/workflows/build-apk.yml   ← بناء الـ APK تلقائياً + إنشاء Release
├── build.gradle
└── settings.gradle
```

## ⚠️ مهم: مكتبات الطرف الثالث (Firebase SDK وغيرها)

صفحة `assets/index.html` تحمّل Firebase SDK و dayjs و sweetalert2 و lodash و uuid من نسخة
محلية داخل `assets/libs/` أولاً، وترجع لـ CDN خارجي فقط لو الملف المحلي غير موجود. سير عمل
GitHub Actions (`.github/workflows/build-apk.yml`) يُنزّل هذه الملفات تلقائياً بخطوة
"Fetch bundled vendor libraries" **قبل** كل بناء، لذا أي APK ناتج عن الرفع لجيتهاب يحتوي عليها
مضمّنة فعلياً ولا يعتمد على الإنترنت وقت التشغيل. لو تبني الـ APK يدوياً محلياً (بدون CI)،
تأكد من تشغيل نفس أوامر `curl` الموجودة بتلك الخطوة أولاً، وإلا سيعتمد التطبيق على الوصول
لـ `gstatic.com`/`jsdelivr.net` من جهاز المستخدم نفسه، وأي شبكة بطيئة أو مقيّدة ستجعل شاشة
التحميل تبدو عالقة لفترة طويلة أو تفشل تهيئة Firebase كلياً.

## التطوير محلياً (اختياري)

افتح المجلد مباشرة في Android Studio (Iguana أو أحدث) واختر Run — لست مضطراً لاستخدام
جيتهاب إن كنت تريد تجربته على جهازك أو المحاكي مباشرة أولاً.

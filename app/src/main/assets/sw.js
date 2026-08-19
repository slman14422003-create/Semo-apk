// رقم إصدار الكاش: كل مرة تُحدَّث ملفات JS/CSS لازم تزيد هذا الرقم، وإلا
// سيستمر Service Worker بتقديم نسخة قديمة/معطوبة من الملفات للمستخدمين
// الذين ثبّتوا التطبيق مسبقاً حتى لو رفعت إصلاحات جديدة على السيرفر.
const CACHE_NAME = 'tomodachi-v11';

// مهم جداً: كل المسارات هنا نسبية (بدون "/" في البداية) لأنها تُحسب بالنسبة
// لمكان ملف sw.js نفسه وليس جذر الدومين. لو استخدمنا مسارات مطلقة مثل
// "/index.html" فهذا يفترض أن الموقع منشور على جذر الدومين مباشرة، وهذا
// يفشل لو كان التطبيق منشوراً داخل مجلد فرعي (مثال: نشر على GitHub Pages
// كموقع مشروع على الرابط username.github.io/repo-name/ بدل جذر الحساب) —
// وهو بالضبط سبب ظهور صفحة 404 الخاصة بـ GitHub Pages بعد تثبيت التطبيق.
const SCOPE = self.registration.scope; // يحسم مكان الموقع الفعلي تلقائياً
const urlsToCache = [
    './',
    './index.html',
    './offline.html',
    './css/style.css',
    './css/theme.css',
    './css/login.css',
    './css/chat.css',
    './css/admin.css',
    './js/app.js',
    './js/utils.js',
    './js/firebase.js',
    './js/themes.js',
    './js/pwa.js',
    './manifest.json',
    './libs/firebase-app-compat.js',
    './libs/firebase-firestore-compat.js',
    './libs/firebase-storage-compat.js',
    './libs/firebase-auth-compat.js',
    './libs/dayjs/dayjs.min.js',
    './libs/dayjs/plugin/relativeTime.js',
    './libs/dayjs/plugin/localizedFormat.js',
    './libs/sweetalert2.min.js',
    './libs/lodash.min.js',
    './libs/uuid.min.js',
    './libs/fontawesome/css/all.min.css'
].map(url => new URL(url, SCOPE).href);

// تثبيت Service Worker وتخزين الملفات
self.addEventListener('install', event => {
    // ================================================================
    // إصلاح جذري لمشكلة "لازم أمسح بيانات المتصفح مع كل تحديث":
    // ================================================================
    // بدون skipWaiting()، أي Service Worker جديد يدخل بحالة "انتظار" ولا
    // يتفعّل إلا بعد إغلاق كل تبويبات/نوافذ التطبيق المفتوحة يدوياً من
    // المستخدم — وهذا نادراً ما يصير فعلياً (خصوصاً بتطبيق مثبّت يبقى
    // بالخلفية)، فكان يبقى عالقاً على نسخة قديمة من الكود لأيام، ويحس
    // المستخدم إنه "لازم يمسح بيانات المتصفح" ليشتغل التحديث. الآن الـ SW
    // الجديد يتفعّل فوراً بمجرد اكتمال التثبيت.
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => {
                console.log('✅ تم فتح الكاش');
                return cache.addAll(urlsToCache);
            })
            .catch(error => {
                console.error('❌ فشل تخزين الملفات:', error);
            })
    );
});

// تفعيل Service Worker وحذف كل الكاش القديم تلقائياً (بدون أي تدخل يدوي
// من المستخدم) + الاستيلاء الفوري على كل التبويبات المفتوحة حالياً
// (clients.claim) بدل انتظار إعادة تحميلها يدوياً.
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheName !== CACHE_NAME) {
                        console.log('🗑️ حذف الكاش القديم تلقائياً:', cacheName);
                        return caches.delete(cacheName);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

// اعتراض الطلبات وتقديمها:
// - لملفات JS/CSS وصفحة HTML الرئيسية: نجرب الشبكة أولاً (Network First) حتى
//   يحصل المستخدم دائماً على آخر إصلاحات الكود مباشرة، ونستخدم الكاش فقط لو
//   ما فيه إنترنت. هذا يمنع مشكلة "بقاء نسخة قديمة/معطوبة" من التطبيق مخزّنة
//   عند المستخدم حتى بعد رفع تحديثات جديدة.
// - لبقية الملفات (أيقونات، خطوط...): الكاش أولاً (Cache First) للسرعة.
const NETWORK_FIRST_EXTENSIONS = ['.js', '.css', '.html'];

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;

    const url = event.request.url;

    // ================================================================
    // إصلاح حرج: استثناء طلبات Firebase/Firestore بالكامل من اعتراض الـ SW
    // ================================================================
    // السبب الحقيقي لخطأ "WebChannelConnection RPC 'Listen' stream ... transport
    // errored" اللي كان يظهر بالكونسول عند إرسال رسالة: كان الـ Service Worker
    // يعترض حتى طلبات فايرستور اللحظية (Listen/Write streams عبر WebChannel
    // وهي اتصال طويل الأمد شبه Streaming) ويحاول يلفّها بمنطق cache/fetch
    // الخاص فينا — وهذا يكسر الاتصال اللحظي لأن هذا النوع من الطلبات لازم
    // يمر للشبكة مباشرة وبدون أي وسيط. الحل: أي طلب لدومينات Google/Firebase
    // الخلفية يمرّ للمتصفح مباشرة (بدون event.respondWith إطلاقاً).
    const isFirebaseBackend =
        url.includes('firestore.googleapis.com') ||
        url.includes('googleapis.com') ||
        url.includes('firebaseinstallations.googleapis.com') ||
        url.includes('firebaseremoteconfig.googleapis.com') ||
        url.includes('gstatic.com/firebasejs');
    if (isFirebaseBackend) {
        return; // لا نستدعي respondWith إطلاقاً — يمر الطلب طبيعياً بدون اعتراض
    }

    const isNetworkFirst = NETWORK_FIRST_EXTENSIONS.some(ext => url.includes(ext)) ||
        event.request.mode === 'navigate';

    if (isNetworkFirst) {
        event.respondWith(
            fetch(event.request)
                .then(response => {
                    if (response && response.status === 200 && response.type === 'basic') {
                        const responseToCache = response.clone();
                        caches.open(CACHE_NAME).then(cache => cache.put(event.request, responseToCache));
                    }
                    return response;
                })
                .catch(() => caches.match(event.request).then(cached => cached || caches.match(new URL('./offline.html', SCOPE).href)))
        );
        return;
    }

    event.respondWith(
        caches.match(event.request)
            .then(response => {
                // إذا وجد في الكاش، أرجعه
                if (response) {
                    return response;
                }
                // وإلا، قم بطلب من الشبكة
                return fetch(event.request)
                    .then(response => {
                        // لا تخزن الطلبات التي تفشل
                        if (!response || response.status !== 200 || response.type !== 'basic') {
                            return response;
                        }
                        // نسخ الاستجابة لتخزينها
                        const responseToCache = response.clone();
                        caches.open(CACHE_NAME)
                            .then(cache => {
                                cache.put(event.request, responseToCache);
                            });
                        return response;
                    })
                    .catch(() => {
                        // عرض صفحة أوفلاين إذا كانت متوفرة
                        return caches.match(new URL('./offline.html', SCOPE).href);
                    });
            })
    );
});

// التعامل مع الإشعارات
self.addEventListener('push', event => {
    const options = {
        body: event.data.text(),
        icon: 'icons/icon-192x192.png',
        badge: 'icons/icon-72x72.png',
        vibrate: [200, 100, 200],
        data: {
            dateOfArrival: Date.now(),
            primaryKey: 1
        },
        actions: [
            { action: 'open', title: 'فتح التطبيق' },
            { action: 'close', title: 'إغلاق' }
        ]
    };
    event.waitUntil(
        self.registration.showNotification('📩 Tomodachi', options)
    );
});

// التعامل مع النقر على الإشعار
self.addEventListener('notificationclick', event => {
    event.notification.close();
    if (event.action === 'open') {
        event.waitUntil(
            clients.openWindow(SCOPE)
        );
    }
});

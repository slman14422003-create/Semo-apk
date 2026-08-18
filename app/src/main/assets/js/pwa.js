// ============================================================
// PWA - تطبيق الويب التقدمي
// ============================================================

// تسجيل Service Worker
// ملاحظة مهمة: نستخدم مساراً نسبياً (sw.js) وليس مطلقاً (/sw.js) لأن المسار
// المطلق يفترض أن الموقع منشور على جذر الدومين مباشرة. لو نُشر التطبيق داخل
// مجلد فرعي (كحال GitHub Pages لمستودع مشروع: username.github.io/repo-name/)
// فإن "/sw.js" يشير خطأً لجذر الدومين بالكامل ويفشل التسجيل والتثبيت.
//
// حماية إضافية: Service Worker يتطلب "سياق آمن" (HTTPS، أو http://localhost
// للتطوير المحلي فقط). فتح ملف index.html مباشرة من جهازك (رابط يبدأ بـ
// file://) يجعل أصل الصفحة origin = 'null' ولا يدعمه المتصفح إطلاقاً — هذا
// ليس خطأ بالكود، بل طريقة فتح الملف نفسها. نتحقق من هذا أولاً بدل ما نحاول
// التسجيل ونطبع خطأ مربك بالكونسول؛ بدلها نطبع توضيحاً عملياً واحداً وبس.
function canUseServiceWorker() {
    if (!('serviceWorker' in navigator)) return false;
    const isSecure = window.isSecureContext === true;
    const isLocalhost = ['localhost', '127.0.0.1'].includes(location.hostname);
    if (location.protocol === 'file:') {
        console.warn('ℹ️ Service Worker لا يعمل عند فتح الملف مباشرة (file://). ' +
            'شغّل التطبيق عبر سيرفر محلي (مثل: npx serve .) أو انشره على ' +
            'GitHub Pages / Firebase Hosting ليعمل التثبيت والإشعارات بشكل كامل.');
        return false;
    }
    return isSecure || isLocalhost;
}

function registerServiceWorker() {
    if (!canUseServiceWorker()) return;
    navigator.serviceWorker.register('sw.js')
        .then(registration => {
            console.log('✅ Service Worker registered successfully:', registration);
        })
        .catch(error => {
            console.log('⚠️ Service Worker registration failed:', error);
        });
}

// التحقق من التثبيت
function checkInstallation() {
    // التحقق من وضع العرض المستقل
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches ||
                         window.navigator.standalone ||
                         document.referrer.includes('android-app://');

    if (isStandalone) {
        console.log('📱 التطبيق يعمل في وضع التثبيت');
        document.body.classList.add('installed');
    }
}

// إعداد زر التثبيت المباشر (تثبيت التطبيق فعلياً كـ PWA وليس مجرد اختصار)
// ملاحظة مهمة: التثبيت الحقيقي (WebAPK على أندرويد) يتطلب:
//  1) manifest.json حقيقي يُجلب عبر رابط شبكة فعلي (وليس blob: مولّد بالجافاسكربت)
//  2) أيقونات PNG حقيقية بأحجام كافية (192 و512 على الأقل) — موجودة بمجلد /icons
//  3) Service Worker مُسجَّل وفيه معالج fetch — موجود بالفعل في sw.js
// هذه الشروط الثلاثة متوفرة الآن، لذلك سيعرض المتصفح خيار "تثبيت التطبيق"
// الحقيقي بدل "إضافة اختصار" فقط.
window.deferredInstallPrompt = null;

function setupInstallButton() {
    // داخل التطبيق الأصلي (APK) الأمر مثبّت أصلاً - لا داعي لأي منطق تثبيت PWA.
    if (window.AndroidBridge) return;

    const installBtn = document.getElementById('installBtn');

    window.addEventListener('beforeinstallprompt', (e) => {
        // منع ظهور الـ popup التلقائي من المتصفح، ونحتفظ بالحدث لعرضه من زر مخصص أنيق
        e.preventDefault();
        window.deferredInstallPrompt = e;
        if (installBtn) installBtn.style.display = 'flex';
        console.log('📲 التطبيق جاهز للتثبيت المباشر');
    });

    window.addEventListener('appinstalled', () => {
        console.log('📱 تم تثبيت التطبيق بنجاح');
        window.deferredInstallPrompt = null;
        if (installBtn) installBtn.style.display = 'none';
        document.body.classList.add('installed');
        if (typeof showNotification === 'function') {
            showNotification('✅ تم تثبيت Tomodachi على جهازك');
        }
    });
}

// دالة تثبيت عامة يمكن استدعاؤها من أي زر بالواجهة (مثل زر القائمة الجانبية)
window.installPWA = async function() {
    const installBtn = document.getElementById('installBtn');

    // iOS Safari لا يدعم beforeinstallprompt إطلاقاً؛ نعرض تعليمات بديلة
    const isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
    if (isIOS && !window.navigator.standalone) {
        if (typeof showNotification === 'function') {
            showNotification('📲 لتثبيت التطبيق: اضغط زر المشاركة ثم "إضافة إلى الشاشة الرئيسية"');
        }
        return;
    }

    if (!window.deferredInstallPrompt) {
        if (typeof showNotification === 'function') {
            showNotification('ℹ️ التطبيق مثبّت بالفعل أو غير جاهز للتثبيت الآن');
        }
        return;
    }

    window.deferredInstallPrompt.prompt();
    const choice = await window.deferredInstallPrompt.userChoice;
    console.log(`✅ نتيجة التثبيت: ${choice.outcome}`);
    window.deferredInstallPrompt = null;
    if (installBtn) installBtn.style.display = 'none';
};

// ================================================================
// الإشعارات
// ================================================================
// ملاحظة صادقة: هذا تطبيق بدون سيرفر خاص (Firestore فقط بدون Cloud Functions)،
// فلا يمكن تفعيل "Push" حقيقي يصل للجهاز والتطبيق مغلق تماماً (هذا يتطلب
// سيرفر يرسل عبر FCM). ما نقدر نوفره بأمانة هو إشعارات محلية فورية تظهر
// طالما المتصفح يعمل بالخلفية (تبويب مفتوح أو التطبيق المثبّت شغّال بالخلفية)،
// وهذا يغطي الاستخدام الفعلي الشائع (تطبيق مثبّت ومفتوح لكن المستخدم بشاشة ثانية).
function setupPushNotifications() {
    // طلب الصلاحية تلقائياً عند بدء التطبيق (بدل انتظار ضغطة زر قد لا يجدها
    // المستخدم أبداً). داخل التطبيق الأصلي (AndroidBridge موجود) تكون الصلاحية
    // الفعلية مرتبطة أصلاً بـ POST_NOTIFICATIONS التي يطلبها MainActivity عند
    // فتح التطبيق، فهذا الطلب هنا مجرد مزامنة لحالة الـ Notification API بالويب.
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission().catch(() => {});
    }

    if ('Notification' in window && 'serviceWorker' in navigator) {
        const notifBtn = document.getElementById('notifBtn');
        if (notifBtn) {
            notifBtn.addEventListener('click', async () => {
                if (Notification.permission === 'granted') {
                    showNotification('✅ الإشعارات مفعلة');
                } else if (Notification.permission === 'denied') {
                    showNotification('❌ الإشعارات ممنوعة، قم بتفعيلها من إعدادات المتصفح');
                } else {
                    const permission = await Notification.requestPermission();
                    if (permission === 'granted') {
                        showNotification('✅ تم تفعيل الإشعارات');
                    } else {
                        showNotification('❌ لم يتم تفعيل الإشعارات');
                    }
                }
            });
        }
    }
}

// دالة إرسال إشعار محلي عام (تأكيد تثبيت، تنبيهات عامة...)
function sendLocalNotification(title, body, icon = 'icons/icon-192x192.png') {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    // نفضّل عرض الإشعار عبر الـ Service Worker (registration.showNotification)
    // بدل new Notification() مباشرة، لأنها تعمل بشكل أوثق مع التطبيق المثبّت
    // (تظهر بشكل صحيح حتى لو الصفحة بالخلفية الكاملة على أندرويد) وتدعم
    // أزرار إجراءات (actions) لا يدعمها الـ constructor العادي.
    if (navigator.serviceWorker && navigator.serviceWorker.ready) {
        navigator.serviceWorker.ready.then(reg => {
            reg.showNotification(title, {
                body, icon,
                badge: 'icons/icon-72x72.png',
                vibrate: [200, 100, 200],
                silent: false,
                tag: 'tomodachi-general',
                data: { date: Date.now() }
            }).catch(() => { try { new Notification(title, { body, icon }); } catch (e) {} });
        });
    } else {
        try { new Notification(title, { body, icon }); } catch (e) {}
    }
}

// ================================================================
// إشعارات الرسائل الجديدة + Badging API (شارة عدد على أيقونة التطبيق)
// ================================================================
// تُستدعى من js/app.js (listenMessages) عند وصول رسائل جديدة. تعرض إشعاراً
// فقط لو كانت الرسالة من شخص آخر (مو رسالتي أنا) والصفحة مخفية/بالخلفية —
// حتى لا نزعج المستخدم بإشعار لمحادثة يشاهدها أمامه مباشرة.
window.notifyNewMessages = function(newMessages) {
    if (!Array.isArray(newMessages) || newMessages.length === 0) return;
    if (window.settings && window.settings.notifications === false) return;

    const others = newMessages.filter(m => m.username && m.username !== window.currentUser && !m.deleted);
    if (others.length === 0) return;

    if ('setAppBadge' in navigator) {
        navigator.setAppBadge(window.unreadCount || others.length).catch(() => {});
    }

    const isPageVisible = document.visibilityState === 'visible' && document.hasFocus();
    if (isPageVisible) return;

    const last = others[others.length - 1];
    const title = others.length === 1 ? `💬 ${last.username}` : `💬 ${others.length} رسائل جديدة`;
    const bodyText = others.length === 1
        ? decompressText(last.text || '').slice(0, 120)
        : others.slice(-3).map(m => `${m.username}: ${decompressText(m.text || '').slice(0, 40)}`).join('\n');

    // المسار المفضّل: جسر أندرويد الأصلي (راجع MainActivity.NativeBridge). أوثق
    // وأكثر ثباتاً من Notification API داخل WebView (يظهر في شريط الإشعارات
    // الحقيقي حتى لو كان دعم Service Worker للإشعارات متذبذباً بهذا الجهاز).
    if (window.AndroidBridge && typeof window.AndroidBridge.showMessageNotification === 'function') {
        try {
            window.AndroidBridge.showMessageNotification(title, bodyText || '📩 رسالة جديدة');
            return;
        } catch (e) { /* نكمل للمسار البديل بالأسفل لو فشل الجسر لأي سبب */ }
    }

    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    if (!(navigator.serviceWorker && navigator.serviceWorker.ready)) return;

    navigator.serviceWorker.ready.then(reg => {
        reg.showNotification(title, {
            body: bodyText || '📩 رسالة جديدة',
            icon: last.avatar && typeof isValidImageUrl === 'function' && isValidImageUrl(last.avatar) ? last.avatar : 'icons/icon-192x192.png',
            badge: 'icons/icon-72x72.png',
            vibrate: [150, 80, 150],
            tag: 'tomodachi-messages',
            renotify: true,
            data: { url: './index.html' }
        }).catch(() => {});
    });
};

// تصفير شارة الأيقونة عند عودة المستخدم للتطبيق
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && 'clearAppBadge' in navigator) {
        navigator.clearAppBadge().catch(() => {});
    }
});

// دالة فحص تحديث التطبيق يدوياً
function checkForUpdates() {
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.ready.then(registration => registration.update()).catch(() => {});
    }
}

// ================================================================
// إتمام التحديث التلقائي: بعد ما صار عندنا skipWaiting() + clients.claim()
// بملف sw.js، يتفعّل الإصدار الجديد فوراً ويستولي على التبويب، لكن الصفحة
// المفتوحة أصلاً لسه محمّلة بملفات JS/CSS القديمة بالذاكرة. هذا الجزء يعيد
// تحميل الصفحة تلقائياً مرة واحدة فقط بمجرد سيطرة النسخة الجديدة، فيحصل
// المستخدم على آخر تحديث فوراً وبدون أي تدخل يدوي منه (لا حاجة لمسح بيانات
// المتصفح أو إعادة التثبيت). حارس sessionStorage يمنع أي حلقة تحديث لا نهائية.
if ('serviceWorker' in navigator) {
    let hasReloaded = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (hasReloaded) return;
        if (sessionStorage.getItem('tomodachi_sw_reloaded') === '1') return;
        hasReloaded = true;
        sessionStorage.setItem('tomodachi_sw_reloaded', '1');
        console.log('🔄 تحديث جديد مفعّل — إعادة تحميل تلقائية...');
        window.location.reload();
    });
    // إعادة ضبط الحارس عند تحميل صفحة جديدة بشكل طبيعي (مو بسبب تحديث)
    window.addEventListener('load', () => {
        setTimeout(() => sessionStorage.removeItem('tomodachi_sw_reloaded'), 3000);
    });
}

// ================================================================
// طبقة "لا تسبب مشاكل" عند رفع نسخة جديدة من التطبيق: نكتشف وجود Service
// Worker جديد مثبَّت وجاهز (بدل ما يبقى المستخدم عالقاً بصمت على نسخة قديمة
// من الكود تتعارض مع شكل بيانات فايرستور الجديد) ونعرض تنبيهاً بسيطاً.
function watchForServiceWorkerUpdates() {
    if (!canUseServiceWorker()) return;
    navigator.serviceWorker.register('sw.js').then(registration => {
        registration.addEventListener('updatefound', () => {
            const newWorker = registration.installing;
            if (!newWorker) return;
            newWorker.addEventListener('statechange', () => {
                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                    console.log('🔄 يوجد تحديث جديد للتطبيق — سيُطبَّق تلقائياً خلال لحظات');
                    if (typeof showNotification === 'function') {
                        showNotification('🔄 جاري تحديث التطبيق لآخر إصدار...');
                    }
                }
            });
        });
    }).catch(() => {});
}

// التهيئة
function initPWA() {
    registerServiceWorker();
    checkInstallation();
    setupInstallButton();
    setupPushNotifications();
    watchForServiceWorkerUpdates();

    // التحقق من التحديثات كل ساعة
    setInterval(checkForUpdates, 3600000);
}

// تصدير الدوال
window.initPWA = initPWA;
window.sendLocalNotification = sendLocalNotification;

// تشغيل التهيئة
document.addEventListener('DOMContentLoaded', () => {
    initPWA();
});

console.log('✅ pwa.js تم التحميل');

// ============================================================
// إعداد Firebase
// ============================================================
// إصلاح مهم: كان هذا الملف يفترض أن كائن `firebase` العام موجود دائماً
// (يأتي من libs/firebase-app-compat.js وباقي ملفات compat المحمّلة قبله
// بسلسلة __loadScriptChain بملف index.html). لو فشلت تلك الملفات
// بالتحميل (لا نسخة محلية بمجلد libs/ ولا اتصال إنترنت للرجوع لـ CDN)،
// كان أول سطر يستخدم `firebase` هنا يرمي ReferenceError يوقف تنفيذ
// باقي هذا الملف بالكامل بصمت (بما فيها window.withRetry وتفعيل
// auth/persistence)، ويترك `db` غير معرَّف أصلاً - فبقية التطبيق تفشل
// بطريقة غامضة بدل رسالة واضحة. الآن نتحقق من وجود `firebase` أولاً
// ونعلن حالة جاهزة/فاشلة صراحة عبر window.__firebaseReady حتى تقدر
// startApp() (بملف app.js) تتصرف بوضوح بدل الاعتماد على استثناء عشوائي.
window.__firebaseReady = false;
window.__firebaseInitError = null;

const firebaseConfig = {
    apiKey: "AIzaSyBVNXAFHyynNL7rD6LaYc4iYgvYPDdDH0c",
    authDomain: "semo-chat-f5fdf.firebaseapp.com",
    projectId: "semo-chat-f5fdf",
    storageBucket: "semo-chat-f5fdf.firebasestorage.app",
    messagingSenderId: "390244231579",
    appId: "1:390244231579:web:d6664b936abae9a730993e",
    measurementId: "G-0C6RXTC6LX"
};

let db = null;
let auth = null;

if (typeof firebase === 'undefined') {
    // مكتبات Firebase SDK نفسها لم تصل أصلاً (لا محلياً ولا عبر CDN).
    window.__firebaseInitError = 'firebase-sdk-missing';
    console.error('❌ حزمة Firebase SDK غير محمّلة إطلاقاً - تحقق من libs/firebase-*-compat.js أو الاتصال بالإنترنت');
} else {
    try {
        // تجنّب "Firebase App named '[DEFAULT]' already exists" لو نُفِّذ
        // هذا الملف أكثر من مرة بنفس صفحة الويب (مثلاً بعد إعادة تحميل
        // جزئية أو تحميل مزدوج للسكربت).
        firebase.initializeApp(firebaseConfig);
    } catch (e) {
        if (!(e && /already exists/i.test(e.message || ''))) {
            window.__firebaseInitError = e;
            console.error('❌ فشل تهيئة Firebase:', e);
        }
    }
}

// تصدير المراجع للاستخدام في الملفات الأخرى
if (typeof firebase !== 'undefined' && !window.__firebaseInitError) {
    try {
        db = firebase.firestore();
        window.__firebaseReady = true;
    } catch (e) {
        window.__firebaseInitError = e;
        console.error('❌ فشل الاتصال بـ Firestore:', e);
    }
}

// ================================================================
// المصادقة (Firebase Authentication) — تُستخدم فقط للتحقق من هوية المسؤول
// ================================================================
// السبب: كان التحقق من كلمة مرور المسؤول يتم بالكامل داخل كود الواجهة
// (JavaScript يعمل على جهاز الزائر) بمقارنة نصية مباشرة لاسم مستخدم وكلمة
// مرور ثابتين. أي شخص يفتح "أدوات المطور" بالمتصفح يقدر يقرأهما بسهولة من
// الكود نفسه، أو حتى يتجاوز الفحص كلياً وينادي دوال فايرستور مباشرة من الكونسول
// لتفعيل صلاحية المسؤول لنفسه (isAdmin: true) لأن قواعد فايرستور كانت تسمح
// بالكتابة لأي شخص بدون أي تحقق من هوية حقيقية. هذا أخطر ضعف بالمشروع.
// الحل الصحيح: نستخدم Firebase Authentication (تحقق حقيقي على سيرفرات جوجل،
// لا يمكن تزويره من المتصفح)، وتُقيَّد كل الصلاحيات الخطيرة (حظر مستخدم،
// كتم الكل، إدارة الكلمات الممنوعة...) بقاعدة أمان بفايرستور تتحقق فعلياً
// من request.auth، فتصير الحماية حقيقية من جهة السيرفر وليست شكلية بالواجهة فقط.
if (window.__firebaseReady) {
    try {
        auth = firebase.auth();
        auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL);
    } catch (e) {
        console.warn('⚠️ تعذّر تهيئة Firebase Auth:', e);
    }
}
window.firebaseAuth = auth;


// ============================================================
// طبقة مزامنة إضافية: تفعيل التخزين المحلي المستمر (Offline Persistence)
// ============================================================
// هذا يجعل Firestore يحتفظ بنسخة محلية (IndexedDB) من البيانات، فيصير:
// - فتح التطبيق أسرع بكثير (يعرض آخر بيانات محفوظة فوراً قبل اكتمال الاتصال)
// - الكتابة (إرسال رسالة) تُطبَّق محلياً فوراً ثم تُزامَن بالخلفية تلقائياً
//   حتى لو انقطع الاتصال لحظياً — بدون أي كود إضافي منّا، فايرستور يتكفّل بها
// - تقليل عدد الطلبات الفعلية للسيرفر عند إعادة فتح نفس البيانات
// ملاحظة: تفعيلها يفشل لو كان التطبيق مفتوح بأكثر من تبويب بنفس الوقت
// (failed-precondition) أو غير مدعوم بالمتصفح (unimplemented) — نتعامل مع
// الحالتين بهدوء بدون كسر باقي التطبيق.
if (window.__firebaseReady) {
    try {
        db.enablePersistence({ synchronizeTabs: true }).catch(err => {
            if (err.code === 'failed-precondition') {
                console.warn('⚠️ التخزين المحلي مفعّل بتبويب آخر بالفعل — سيُستخدم synchronizeTabs');
            } else if (err.code === 'unimplemented') {
                console.warn('⚠️ المتصفح لا يدعم التخزين المحلي المستمر لفايرستور');
            } else {
                console.warn('⚠️ تعذّر تفعيل التخزين المحلي:', err);
            }
        });
    } catch (e) {
        console.warn('⚠️ enablePersistence غير متاحة بهذا الإصدار من SDK:', e);
    }
}

// ============================================================
// طبقة إعادة محاولة عامة (Retry with backoff) لأي عملية كتابة على فايرستور
// ============================================================
// تُستخدم لأي عملية حرجة (إرسال رسالة، تحديث ثيم...) بدل تركها تفشل بصمت
// عند تذبذب الشبكة. تعيد المحاولة 3 مرات بفواصل متزايدة (500ms, 1500ms, 3000ms)
// قبل أن ترفض نهائياً وتترك الطبقة الأعلى تتعامل مع الفشل (مثل زر "إعادة المحاولة").
window.withRetry = async function(operation, { retries = 3, baseDelay = 500 } = {}) {
    let lastError;
    for (let attempt = 0; attempt <= retries; attempt++) {
        try {
            return await operation();
        } catch (error) {
            lastError = error;
            // لا فائدة من إعادة محاولة أخطاء الصلاحيات أو البيانات غير الصالحة
            if (error && (error.code === 'permission-denied' || error.code === 'invalid-argument')) {
                throw error;
            }
            if (attempt < retries) {
                const delay = baseDelay * Math.pow(2, attempt);
                await new Promise(resolve => setTimeout(resolve, delay));
            }
        }
    }
    throw lastError;
};

// ملاحظة: Firebase Storage غير مفعّل حالياً على هذا المشروع، لذا الصور تُخزَّن
// كـ base64 مباشرة داخل مستندات Firestore (راجع compressImageSmart بملف utils.js).
// نغلّف التهيئة بـ try/catch احتياطاً حتى لا يتعطل تحميل التطبيق بالكامل لو لم
// تكن خدمة Storage مفعّلة على مشروع Firebase.
let storage = null;
if (window.__firebaseReady) {
    try {
        if (firebase.storage) storage = firebase.storage();
    } catch (e) {
        console.warn('⚠️ Firebase Storage غير مفعّل على هذا المشروع - سيتم استخدام base64 فقط');
    }
}

if (window.__firebaseReady) {
    console.log('✅ Firebase تم التهيئة بنجاح');
} else {
    console.error('❌ Firebase لم يُهيَّأ - التطبيق سيعمل بوضع غير متصل بدون دردشة حية حتى يُحل السبب أعلاه');
}

// ================================================================
//  Tomodachi - دوال مساعدة عامة (Utilities)
//  دوال مستقلة لا تعتمد على حالة التطبيق - قابلة لإعادة الاستخدام
//  ملاحظة: هذا الملف لازم يتحمّل قبل js/app.js في index.html
// ================================================================

/**
 * ضغط النص (إزالة المسافات الزائدة)
 */
function compressText(text) {
    if (!text || text.length < 10) return text;
    return text.replace(/\s+/g, ' ').trim();
}

/**
 * فك ضغط النص (محجوزة للاستخدام المستقبلي لو اتضاف ضغط حقيقي)
 */
function decompressText(text) {
    return text;
}

/**
 * تنسيق الوقت لعرضه في الرسائل (مثال: 05:30 PM)
 */
function formatTime(timestamp) {
    if (!timestamp) return '';
    try {
        let date = (timestamp && typeof timestamp.toDate === 'function') ? timestamp.toDate() : new Date(timestamp);
        if (isNaN(date.getTime())) return '';
        return dayjs(date).format('hh:mm A');
    } catch (e) {
        return '';
    }
}

/**
 * تنسيق التاريخ لفاصل التاريخ بين الرسائل (مثال: 14 أغسطس 2026)
 */
function formatDate(timestamp) {
    if (!timestamp) return '';
    try {
        let date = (timestamp && typeof timestamp.toDate === 'function') ? timestamp.toDate() : new Date(timestamp);
        if (isNaN(date.getTime())) return '';
        return dayjs(date).format('DD MMMM YYYY');
    } catch (e) {
        return '';
    }
}

/**
 * التحقق من أن الرابط صورة صالحة (رابط عادي، Data URL، أو Dicebear avatar)
 */
function isValidImageUrl(url) {
    return url && (url.startsWith('http') || url.startsWith('data:') || url.startsWith('https://api.dicebear.com'));
}

/**
 * مجموعة أفاتارات حيوانات كيوت (SVG محلي بالكامل - لا يعتمد على أي API خارجي).
 * كل أفاتار عبارة عن حيوان + لون خلفية باستيل، ويتحدد بشكل ثابت (deterministic)
 * حسب اسم المستخدم، بحيث يحصل كل مستخدم دائماً على نفس الأفاتار الافتراضي.
 */
const CUTE_ANIMALS = [
    { emoji: '🐱', bg: '#FFD3E0' }, // قطة
    { emoji: '🐶', bg: '#D4E8FF' }, // كلب
    { emoji: '🐰', bg: '#FFE8D6' }, // أرنب
    { emoji: '🐻', bg: '#E9DFFB' }, // دب
    { emoji: '🦊', bg: '#FFE0B3' }, // ثعلب
    { emoji: '🐼', bg: '#DFF5E3' }, // باندا
    { emoji: '🐨', bg: '#DCE9F0' }, // كوالا
    { emoji: '🐯', bg: '#FFF0B8' }, // نمر
    { emoji: '🦁', bg: '#FFE9C7' }, // أسد
    { emoji: '🐸', bg: '#DBF5DC' }, // ضفدع
    { emoji: '🐷', bg: '#FFDCE5' }, // خنزير
    { emoji: '🐹', bg: '#FBEBCF' }, // هامستر
    { emoji: '🐧', bg: '#E1EEF5' }, // بطريق
    { emoji: '🦉', bg: '#EEE3D8' }, // بومة
    { emoji: '🐵', bg: '#F3E3CE' }, // قرد
    { emoji: '🦄', bg: '#F3DFF7' }  // يونيكورن
];

/**
 * دالة hash بسيطة وثابتة (deterministic) لتحويل نص إلى رقم
 */
function _simpleHash(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash);
}

/**
 * توليد رابط أفاتار افتراضي (حيوان كيوت) بناءً على اسم المستخدم.
 * الأفاتار عبارة عن SVG مولّد محلياً (data URI) - يعمل بدون إنترنت ودائماً بنفس الشكل لنفس الاسم.
 */
function generateAvatarUrl(username) {
    const idx = _simpleHash(String(username || 'user')) % CUTE_ANIMALS.length;
    const { emoji, bg } = CUTE_ANIMALS[idx];
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">` +
        `<circle cx="64" cy="64" r="64" fill="${bg}"/>` +
        `<text x="64" y="82" font-size="64" text-anchor="middle" dominant-baseline="middle">${emoji}</text>` +
        `</svg>`;
    return `data:image/svg+xml,${encodeURIComponent(svg)}`;
}

/**
 * توليد أفاتار حيوان عشوائي (يُستخدم عند اختيار "أفاتار عشوائي" من واجهة البروفايل)
 */
function generateRandomAnimalAvatar() {
    const randomSeed = Math.random().toString(36).slice(2) + Date.now();
    return generateAvatarUrl(randomSeed);
}

/**
 * ضغط صورة (Data URL) وتصغيرها عبر Canvas، وإرجاع Data URL جديد بصيغة JPEG.
 * كانت هذه الدالة تُستدعى في المشروع بدون أن تكون معرّفة أبداً، مما كان يسبب
 * فشل صامت (ReferenceError) في: تحديث صورة البروفايل، وإرسال الصور بالدردشة.
 * @param {string} dataUrl - نتيجة FileReader.readAsDataURL
 * @param {number} quality - جودة الضغط (0 إلى 1)
 * @param {number} maxWidth - أقصى عرض بالبكسل (الارتفاع يُحسب تناسبياً)
 * @returns {Promise<string>} Data URL للصورة بعد الضغط
 */
function compressImage(dataUrl, quality = 0.7, maxWidth = 1024) {
    return new Promise((resolve, reject) => {
        try {
            const img = new Image();
            img.onload = () => {
                let { width, height } = img;
                if (width > maxWidth) {
                    height = Math.round(height * (maxWidth / width));
                    width = maxWidth;
                }
                const canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                // خلفية بيضاء (لتفادي شفافية سوداء عند تحويل PNG إلى JPEG)
                ctx.fillStyle = '#FFFFFF';
                ctx.fillRect(0, 0, width, height);
                ctx.drawImage(img, 0, 0, width, height);
                resolve(canvas.toDataURL('image/jpeg', quality));
            };
            img.onerror = () => reject(new Error('تعذّر تحميل الصورة للمعالجة'));
            img.src = dataUrl;
        } catch (e) {
            reject(e);
        }
    });
}

/**
 * تحويل Data URL إلى Blob (يُستخدم قبل الرفع إلى Firebase Storage)
 */
async function dataUrlToBlob(dataUrl) {
    const res = await fetch(dataUrl);
    return await res.blob();
}

/**
 * ضغط "ذكي" للصورة: يحاول تصغير الجودة/الأبعاد تدريجياً حتى يضمن أن حجم
 * النص النهائي (base64) يبقى تحت حد آمن. نستخدم هذا بدل الرفع لـ Storage
 * لأن التخزين يكون مباشرة داخل مستند Firestore (الذي حده الأقصى تقريباً 1MB
 * لكامل المستند)، فلازم نضمن مساحة كافية لبقية حقول الرسالة/المستخدم.
 * @param {string} dataUrl
 * @param {object} options {maxWidth, startQuality, minQuality, maxBase64Bytes}
 * @returns {Promise<string>} Data URL مضغوط بأمان
 */
async function compressImageSmart(dataUrl, options = {}) {
    const {
        maxWidth = 800,
        startQuality = 0.7,
        minQuality = 0.25,
        maxBase64Bytes = 700 * 1024
    } = options;

    let width = maxWidth;
    let quality = startQuality;
    let result = await compressImage(dataUrl, quality, width);

    let attempts = 0;
    while (result.length > maxBase64Bytes && attempts < 8) {
        attempts++;
        if (quality > minQuality) {
            quality = Math.max(minQuality, quality - 0.12);
        } else {
            width = Math.round(width * 0.75);
        }
        result = await compressImage(dataUrl, quality, width);
    }

    if (result.length > maxBase64Bytes) {
        throw new Error('الصورة كبيرة جداً حتى بعد أقصى ضغط ممكن، جرّب صورة أخرى أصغر');
    }
    return result;
}

/**
 * تحويل Data URL إلى Blob (يبقى مفيداً لو احتاج المشروع لاحقاً رفع فعلي)
 */
async function dataUrlToBlob(dataUrl) {
    const res = await fetch(dataUrl);
    return await res.blob();
}

/**
 * استخراج قيمة رقمية (milliseconds) من timestamp بأي شكل كان
 * (Firestore Timestamp، رقم، نص، أو Date)
 */
function getTimestampValue(timestamp) {
    if (!timestamp) return 0;
    if (timestamp.toDate) return timestamp.toDate().getTime();
    if (typeof timestamp === 'number') return timestamp;
    if (timestamp instanceof Date) return timestamp.getTime();
    if (typeof timestamp === 'string') return new Date(timestamp).getTime();
    return 0;
}

/**
 * تعقيم نص عشان يتحط بأمان جوه HTML (نص عادي أو داخل attribute بين علامتي تنصيص "").
 * لازم تتستخدم مع أي بيانات جاية من مستخدم (اسم مستخدم، نص رسالة، bio...) قبل
 * ما تتحط بـ innerHTML أو template literal.
 */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * تعقيم نص عشان يتحط بأمان جوه onclick="fn('VALUE')" (سلسلة JS محاطة بعلامة تنصيص
 * مفردة، وهي كمان جوه attribute محاط بعلامة تنصيص مزدوجة). مهم نستخدمها بدل
 * escapeHtml العادية في السياق ده، لأن escapeHtml لوحدها مش كافية لمنع كسر
 * الـ onclick handler لو القيمة فيها علامة تنصيص مفردة (').
 */
function escapeJsAttr(str) {
    if (str === null || str === undefined) return '';
    const jsEscaped = String(str)
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '\\r');
    return escapeHtml(jsEscaped);
}

/**
 * التحقق من أن اسم المستخدم يحتوي فقط على حروف (عربي/إنجليزي) وأرقام
 * ومسافات وشرطات - بدون أي رموز HTML/JS خطيرة زي < > " ' & \ `
 * (طبقة حماية إضافية جنب الـ escaping، مش بديلة عنه)
 */
function isValidUsername(username) {
    return /^[\p{L}\p{N}_\- ]+$/u.test(username);
}

window.escapeHtml = escapeHtml;
window.escapeJsAttr = escapeJsAttr;
window.isValidUsername = isValidUsername;

// تصدير صريح على window (احتياطًا لو الملف اتحمل كـ module في المستقبل)
window.compressText = compressText;
window.decompressText = decompressText;
window.formatTime = formatTime;
window.formatDate = formatDate;
window.isValidImageUrl = isValidImageUrl;
window.generateAvatarUrl = generateAvatarUrl;
window.generateRandomAnimalAvatar = generateRandomAnimalAvatar;
window.compressImage = compressImage;
window.compressImageSmart = compressImageSmart;
window.dataUrlToBlob = dataUrlToBlob;
window.getTimestampValue = getTimestampValue;

console.log('✅ utils.js تم التحميل بنجاح');

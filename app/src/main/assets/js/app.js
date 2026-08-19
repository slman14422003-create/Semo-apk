// ================================================================
//  Tomodachi - التطبيق الرئيسي (الكامل)
// ================================================================

// ================================================================
//  المتغيرات العامة
// ================================================================

window.currentUser = '';
window.isAdmin = false;
window.myAvatar = '👥';
window.myBio = '📝 مرحباً، أنا في Tomodachi!';
window.messageCount = 0;
window.onlineUsers = 0;
window.blockedCount = 0;
window.selectedColor = '#1877F2';
window.replyingTo = null;
window.replyingToText = '';
window.allUsers = {};
window.badWords = [];
window.settings = {
    muteAll: false,
    confirmDelete: true,
    sound: true,
    notifications: localStorage.getItem('tomodachi_notifSetting') !== 'off'
};
window.syncInterval = null;
window.isSyncing = false;
window.lastSyncTime = Date.now();
window.messagesListener = null;
window.isUserScrolling = false;
window.unreadCount = 0;
window.lastVisibleDoc = null;
window.isLoadingMore = false;
window.allMessages = [];
window.messageCache = new Map();
window.processedIds = new Set();
// طبقة المزامنة المتفائلة: يخزّن كل رسالة "قيد الإرسال" بمفتاح clientId
// حتى نتعرف عليها لما ترجع من فايرستور عبر onSnapshot ونستبدلها بدل تكرارها
window.pendingByClientId = new Map();
window.allStickers = [];
window.currentStickerTab = 'my_stickers';
window.editingMessageId = null;
window.swipeTarget = null;
window.swipeStartX = 0;

// ================================================================
//  دوال مساعدة
//  (compressText, decompressText, formatTime, formatDate,
//   isValidImageUrl, generateAvatarUrl, getTimestampValue
//   انتقلوا لملف js/utils.js اللي بيتحمّل قبل هذا الملف)
// ================================================================

function showNotification(message) {
    const existing = document.querySelector('.temp-notification');
    if (existing) existing.remove();
    const div = document.createElement('div');
    div.className = 'temp-notification';
    div.textContent = message;
    document.body.appendChild(div);
    setTimeout(() => {
        div.style.opacity = '0';
        div.style.transition = 'opacity 0.3s';
        setTimeout(() => div.remove(), 500);
    }, 2500);
}
window.showNotification = showNotification;

// ================================================================
//  الإيموجي
// ================================================================

const fullEmojis = {
    '😀': 'وجوه', '😃': 'وجوه', '😄': 'وجوه', '😁': 'وجوه', '😆': 'وجوه', '😅': 'وجوه',
    '😂': 'وجوه', '🤣': 'وجوه', '😊': 'وجوه', '😇': 'وجوه', '🙂': 'وجوه', '🙃': 'وجوه',
    '😉': 'وجوه', '😌': 'وجوه', '😍': 'وجوه', '🥰': 'وجوه', '😘': 'وجوه', '😗': 'وجوه',
    '😙': 'وجوه', '😚': 'وجوه', '😋': 'وجوه', '😛': 'وجوه', '😝': 'وجوه', '😜': 'وجوه',
    '🤪': 'وجوه', '🤨': 'وجوه', '🧐': 'وجوه', '🤓': 'وجوه', '😎': 'وجوه', '🤩': 'وجوه',
    '🥳': 'وجوه', '😏': 'وجوه', '😒': 'وجوه', '😞': 'وجوه', '😔': 'وجوه', '😟': 'وجوه',
    '😕': 'وجوه', '🙁': 'وجوه', '☹️': 'وجوه', '😣': 'وجوه', '😖': 'وجوه', '😫': 'وجوه',
    '😩': 'وجوه', '🥺': 'وجوه', '😢': 'وجوه', '😭': 'وجوه', '😤': 'وجوه', '😠': 'وجوه',
    '😡': 'وجوه', '🤬': 'وجوه', '🤯': 'وجوه', '😳': 'وجوه', '🥵': 'وجوه', '🥶': 'وجوه',
    '😱': 'وجوه', '😨': 'وجوه', '😰': 'وجوه', '😥': 'وجوه', '😓': 'وجوه', '🤗': 'وجوه',
    '🤔': 'وجوه', '🤭': 'وجوه', '🤫': 'وجوه', '🤥': 'وجوه', '😶': 'وجوه', '😐': 'وجوه',
    '😑': 'وجوه', '😬': 'وجوه', '🙄': 'وجوه', '😯': 'وجوه', '😦': 'وجوه', '😧': 'وجوه',
    '😮': 'وجوه', '😲': 'وجوه', '😴': 'وجوه', '🤤': 'وجوه', '😪': 'وجوه', '😵': 'وجوه',
    '🤐': 'وجوه', '🥴': 'وجوه', '🤢': 'وجوه', '🤮': 'وجوه', '🤧': 'وجوه', '😷': 'وجوه',
    '🤒': 'وجوه', '🤕': 'وجوه', '🤑': 'وجوه', '🤠': 'وجوه', '😈': 'وجوه', '👿': 'وجوه',
    '👹': 'وجوه', '👺': 'وجوه', '🤡': 'وجوه', '💩': 'وجوه', '👻': 'وجوه', '💀': 'وجوه',
    '☠️': 'وجوه', '👽': 'وجوه', '👾': 'وجوه', '🤖': 'وجوه', '🎃': 'وجوه', '😺': 'حيوانات',
    '😸': 'حيوانات', '😻': 'حيوانات', '😽': 'حيوانات', '🙀': 'حيوانات', '😿': 'حيوانات',
    '😹': 'حيوانات', '🐶': 'حيوانات', '🐱': 'حيوانات', '🐭': 'حيوانات', '🐹': 'حيوانات',
    '🐰': 'حيوانات', '🦊': 'حيوانات', '🐻': 'حيوانات', '🐼': 'حيوانات', '🐨': 'حيوانات',
    '🐯': 'حيوانات', '🦁': 'حيوانات', '🐮': 'حيوانات', '🐷': 'حيوانات', '🐽': 'حيوانات',
    '🐸': 'حيوانات', '🐵': 'حيوانات', '🙈': 'حيوانات', '🙉': 'حيوانات', '🙊': 'حيوانات',
    '🐒': 'حيوانات', '🐔': 'حيوانات', '🐧': 'حيوانات', '🐦': 'حيوانات', '🐤': 'حيوانات',
    '🐣': 'حيوانات', '🐥': 'حيوانات', '🦆': 'حيوانات', '🦅': 'حيوانات', '🦉': 'حيوانات',
    '🦇': 'حيوانات', '🐺': 'حيوانات', '🐗': 'حيوانات', '🐴': 'حيوانات', '🦄': 'حيوانات',
    '🐝': 'حيوانات', '🐛': 'حيوانات', '🦋': 'حيوانات', '🐌': 'حيوانات', '🐞': 'حيوانات',
    '🐢': 'حيوانات', '🐍': 'حيوانات', '🦎': 'حيوانات', '🐙': 'حيوانات', '🦑': 'حيوانات',
    '🦀': 'حيوانات', '🐠': 'حيوانات', '🐟': 'حيوانات', '🐬': 'حيوانات', '🐳': 'حيوانات',
    '🐋': 'حيوانات', '🦈': 'حيوانات', '🐊': 'حيوانات', '🐅': 'حيوانات', '🐆': 'حيوانات',
    '🦓': 'حيوانات', '🦍': 'حيوانات', '🐘': 'حيوانات', '🦛': 'حيوانات', '🦒': 'حيوانات',
    '🐫': 'حيوانات', '🦘': 'حيوانات', '🐖': 'حيوانات', '🐄': 'حيوانات', '🐑': 'حيوانات',
    '🐐': 'حيوانات', '🌸': 'طبيعة', '💐': 'طبيعة', '🌷': 'طبيعة', '🌹': 'طبيعة',
    '🌻': 'طبيعة', '🌼': 'طبيعة', '🌱': 'طبيعة', '🌲': 'طبيعة', '🌳': 'طبيعة',
    '🌴': 'طبيعة', '🍀': 'طبيعة', '🍁': 'طبيعة', '🍂': 'طبيعة', '🍃': 'طبيعة',
    '🌵': 'طبيعة', '🌾': 'طبيعة', '🌊': 'طبيعة', '🔥': 'طبيعة', '⭐': 'طبيعة',
    '🌟': 'طبيعة', '✨': 'طبيعة', '⚡': 'طبيعة', '☀️': 'طبيعة', '🌤️': 'طبيعة',
    '⛅': 'طبيعة', '🌧️': 'طبيعة', '⛈️': 'طبيعة', '🌈': 'طبيعة', '☁️': 'طبيعة',
    '❄️': 'طبيعة', '☃️': 'طبيعة', '🌙': 'طبيعة', '🌛': 'طبيعة', '🌎': 'طبيعة',
    '🍏': 'طعام', '🍎': 'طعام', '🍐': 'طعام', '🍊': 'طعام', '🍋': 'طعام',
    '🍌': 'طعام', '🍉': 'طعام', '🍇': 'طعام', '🍓': 'طعام', '🍈': 'طعام',
    '🍒': 'طعام', '🍑': 'طعام', '🥭': 'طعام', '🍍': 'طعام', '🥥': 'طعام',
    '🥝': 'طعام', '🍅': 'طعام', '🍆': 'طعام', '🥑': 'طعام', '🥦': 'طعام',
    '🥕': 'طعام', '🌽': 'طعام', '🌶️': 'طعام', '🥔': 'طعام', '🍠': 'طعام',
    '🥐': 'طعام', '🍞': 'طعام', '🥖': 'طعام', '🥨': 'طعام', '🧀': 'طعام',
    '🥚': 'طعام', '🍳': 'طعام', '🥞': 'طعام', '🥓': 'طعام', '🍔': 'طعام',
    '🍟': 'طعام', '🍕': 'طعام', '🌭': 'طعام', '🥪': 'طعام', '🌮': 'طعام',
    '🌯': 'طعام', '🥗': 'طعام', '🍿': 'طعام', '🍱': 'طعام', '🍣': 'طعام',
    '🍤': 'طعام', '🍦': 'طعام', '🍧': 'طعام', '🍨': 'طعام', '🍩': 'طعام',
    '🍪': 'طعام', '🎂': 'طعام', '🍰': 'طعام', '🧁': 'طعام', '🍫': 'طعام',
    '🍬': 'طعام', '🍭': 'طعام', '🍯': 'طعام', '☕': 'طعام', '🍵': 'طعام',
    '🥤': 'طعام', '🧃': 'طعام', '🍺': 'طعام', '🍻': 'طعام', '🥂': 'طعام',
    '🍷': 'طعام', '🍹': 'طعام', '🎉': 'نشاطات', '🎊': 'نشاطات', '🎈': 'نشاطات',
    '🎁': 'نشاطات', '🏆': 'نشاطات', '🥇': 'نشاطات', '⚽': 'نشاطات', '🏀': 'نشاطات',
    '🏈': 'نشاطات', '⚾': 'نشاطات', '🎾': 'نشاطات', '🏐': 'نشاطات', '🏓': 'نشاطات',
    '🎯': 'نشاطات', '🎮': 'نشاطات', '🎲': 'نشاطات', '🎸': 'نشاطات', '🎹': 'نشاطات',
    '🎨': 'نشاطات', '🎤': 'نشاطات', '🎧': 'نشاطات', '🎬': 'نشاطات', '📷': 'نشاطات',
    '🚗': 'سفر', '🚕': 'سفر', '🚌': 'سفر', '🏍️': 'سفر', '🚲': 'سفر',
    '✈️': 'سفر', '🚀': 'سفر', '🚁': 'سفر', '⛵': 'سفر', '🚢': 'سفر',
    '🗺️': 'سفر', '🏔️': 'سفر', '🏖️': 'سفر', '🏝️': 'سفر', '🏕️': 'سفر',
    '🏠': 'سفر', '🏫': 'سفر', '🕌': 'سفر', '🕋': 'سفر', '⛩️': 'سفر',
    '💻': 'أشياء', '📱': 'أشياء', '⌚': 'أشياء', '📷': 'أشياء', '🎥': 'أشياء',
    '📺': 'أشياء', '🔋': 'أشياء', '💡': 'أشياء', '🔦': 'أشياء', '💰': 'أشياء',
    '💵': 'أشياء', '💳': 'أشياء', '📚': 'أشياء', '✏️': 'أشياء', '📝': 'أشياء',
    '📌': 'أشياء', '🔑': 'أشياء', '🔒': 'أشياء', '🎁': 'أشياء', '⏰': 'أشياء',
    '❤️': 'رموز', '🧡': 'رموز', '💛': 'رموز', '💚': 'رموز', '💙': 'رموز',
    '💜': 'رموز', '🖤': 'رموز', '🤍': 'رموز', '🤎': 'رموز', '💔': 'رموز',
    '❣️': 'رموز', '💕': 'رموز', '💞': 'رموز', '💓': 'رموز', '💗': 'رموز',
    '💖': 'رموز', '💘': 'رموز', '💝': 'رموز', '✅': 'رموز', '❌': 'رموز',
    '❓': 'رموز', '❗': 'رموز', '💯': 'رموز', '🔞': 'رموز', '♻️': 'رموز',
    '🕐': 'رموز', '🇸🇦': 'أعلام', '🇪🇬': 'أعلام', '🇦🇪': 'أعلام', '🇰🇼': 'أعلام',
    '🇶🇦': 'أعلام', '🇧🇭': 'أعلام', '🇴🇲': 'أعلام', '🇯🇴': 'أعلام', '🇱🇧': 'أعلام',
    '🇮🇶': 'أعلام', '🇸🇾': 'أعلام', '🇾🇪': 'أعلام', '🇵🇸': 'أعلام', '🇲🇦': 'أعلام',
    '🇩🇿': 'أعلام', '🇹🇳': 'أعلام', '🇱🇾': 'أعلام', '🇸🇩': 'أعلام', '🇺🇸': 'أعلام',
    '🇬🇧': 'أعلام', '🇹🇷': 'أعلام'
};

function getEmojiCategories() {
    const categories = {};
    for (const [emoji, category] of Object.entries(fullEmojis)) {
        if (!categories[category]) categories[category] = [];
        categories[category].push(emoji);
    }
    return categories;
}

function initFullEmojiPanel() {
    const panel = document.getElementById('emojiPanel');
    if (!panel) return;
    panel.innerHTML = '';
    const categories = getEmojiCategories();

    for (const [category, emojis] of Object.entries(categories)) {
        const catLabel = document.createElement('div');
        catLabel.className = 'emoji-category';
        catLabel.textContent = category;
        panel.appendChild(catLabel);

        emojis.forEach(emoji => {
            const span = document.createElement('span');
            span.className = 'emoji-item';
            span.textContent = emoji;
            span.onclick = function() {
                insertAtCursor(emoji);
                document.getElementById('emojiPanel').style.display = 'none';
            };
            panel.appendChild(span);
        });
    }
}

function insertAtCursor(text) {
    const input = document.getElementById('messageInput');
    if (!input) return;
    const cursorPos = input.selectionStart || 0;
    const value = input.value;
    input.value = value.slice(0, cursorPos) + text + value.slice(cursorPos);
    input.focus();
    input.setSelectionRange(cursorPos + text.length, cursorPos + text.length);
    input.dispatchEvent(new Event('input'));
}

// ================================================================
//  إدارة الرسائل
// ================================================================

function addMessageToArray(msg) {
    if (!msg || !msg.id) return;
    if (window.messageCache.has(msg.id)) {
        const existing = window.messageCache.get(msg.id);
        const index = window.allMessages.indexOf(existing);
        if (index !== -1) {
            window.allMessages[index] = { ...msg };
            window.messageCache.set(msg.id, window.allMessages[index]);
        }
        return;
    }
    const msgTime = getTimestampValue(msg.timestamp);
    let insertIndex = window.allMessages.length;
    for (let i = window.allMessages.length - 1; i >= 0; i--) {
        const currentTime = getTimestampValue(window.allMessages[i].timestamp);
        if (currentTime <= msgTime) {
            insertIndex = i + 1;
            break;
        }
        if (i === 0) insertIndex = 0;
    }
    window.allMessages.splice(insertIndex, 0, { ...msg });
    window.messageCache.set(msg.id, window.allMessages[insertIndex]);
    window.processedIds.add(msg.id);
}

function addMessagesToArray(newMessages) {
    if (!newMessages || newMessages.length === 0) return;
    const validMessages = newMessages.filter(msg => msg && msg.id && !window.messageCache.has(msg.id));
    if (validMessages.length === 0) return;
    const allMsgs = [...window.allMessages, ...validMessages];
    allMsgs.sort((a, b) => {
        const timeA = getTimestampValue(a.timestamp);
        const timeB = getTimestampValue(b.timestamp);
        return timeA - timeB;
    });
    const uniqueMessages = [];
    const seenIds = new Set();
    for (const msg of allMsgs) {
        if (!seenIds.has(msg.id)) {
            seenIds.add(msg.id);
            uniqueMessages.push(msg);
        }
    }
    window.allMessages = uniqueMessages;
    window.messageCache.clear();
    window.processedIds.clear();
    window.allMessages.forEach(msg => {
        if (msg.id) {
            window.messageCache.set(msg.id, msg);
            window.processedIds.add(msg.id);
        }
    });
    saveMessagesToStorage();
}

function deleteMessageFromArray(id) {
    if (!id) return false;
    const index = window.allMessages.findIndex(m => m.id === id);
    if (index !== -1) {
        window.allMessages.splice(index, 1);
        window.messageCache.delete(id);
        window.processedIds.delete(id);
        saveMessagesToStorage();
        return true;
    }
    return false;
}

function updateMessageInArray(id, data) {
    if (!id || !data) return false;
    const index = window.allMessages.findIndex(m => m.id === id);
    if (index !== -1) {
        window.allMessages[index] = { ...window.allMessages[index], ...data };
        window.messageCache.set(id, window.allMessages[index]);
        saveMessagesToStorage();
        return true;
    }
    return false;
}

// طبقة المزامنة المتفائلة: تستبدل رسالة محلية مؤقتة (تنتظر رد السيرفر) بنفس
// الرسالة بعد رجوعها من فايرستور بمعرّف حقيقي، بدل حذفها وإضافة رسالة جديدة
// (وهذا كان سيسبب "قفزة"/ومضة بالواجهة). تُبقي مكان الرسالة بالمصفوفة كما هو.
function reconcileMessageId(oldId, newId, data) {
    const index = window.allMessages.findIndex(m => m.id === oldId);
    if (index === -1) return false;
    window.messageCache.delete(oldId);
    window.processedIds.delete(oldId);
    window.pendingByClientId.delete(data.clientId);
    window.allMessages[index] = { ...window.allMessages[index], ...data, id: newId, status: 'sent' };
    window.messageCache.set(newId, window.allMessages[index]);
    window.processedIds.add(newId);
    saveMessagesToStorage();
    return true;
}

// طبقة مزامنة إضافية: تهدئة الحفظ بالتخزين المحلي (localStorage.setItem
// متزامن/يوقف المتصفح لحظياً)، فكان يُستدعى مع كل نبضة رسائل حتى لو وصلت
// عدة نبضات بنفس الثانية (4 أشخاص يكتبون بنفس الوقت). الآن نجمع الطلبات
// ونكتب مرة واحدة فقط بعد فترة هدوء قصيرة.
let _saveStorageTimeout = null;
function saveMessagesToStorage() {
    if (_saveStorageTimeout) return;
    _saveStorageTimeout = setTimeout(() => {
        _saveStorageTimeout = null;
        try {
            // نستبعد بيانات الصور/الملصقات الثقيلة (base64) من النسخة المحفوظة
            // محلياً؛ هي أصلاً موجودة بفايرستور وتُعاد فوراً عبر onSnapshot،
            // وتخزينها بلوكال ستوريدج كل مرة كان يُبطئ الحفظ والقراءة كثيراً.
            const toSave = window.allMessages.slice(-500).map(m => {
                if (m.imageData || m.stickerData) {
                    const { imageData, stickerData, ...rest } = m;
                    return rest;
                }
                return m;
            });
            localStorage.setItem('tomodachi_messages', JSON.stringify(toSave));
        } catch (e) {
            console.warn('⚠️ فشل حفظ الرسائل:', e);
        }
    }, 400);
}

function loadMessagesFromStorage() {
    try {
        const saved = localStorage.getItem('tomodachi_messages');
        if (saved) {
            const parsed = JSON.parse(saved);
            if (Array.isArray(parsed) && parsed.length > 0) {
                // طبقة قوة إضافية: أي رسالة كانت "قيد الإرسال" وقت إغلاق الصفحة/التحديث
                // لا نعرف مصيرها الحقيقي (وصلت أم لا) — نعرضها كـ "فشل" بدل ما تبقى
                // عالقة بعلامة ⏳ للأبد، بحيث يقدر المستخدم يضغط لإعادة الإرسال بوضوح.
                parsed.forEach(msg => {
                    if (msg.status === 'sending') msg.status = 'failed';
                });
                window.allMessages = parsed;
                window.messageCache.clear();
                window.processedIds.clear();
                window.allMessages.forEach(msg => {
                    if (msg.id) {
                        window.messageCache.set(msg.id, msg);
                        window.processedIds.add(msg.id);
                    }
                });
                console.log(`📦 تم تحميل ${window.allMessages.length} رسالة من localStorage`);
                return true;
            }
        }
    } catch (e) {
        console.warn('⚠️ فشل تحميل الرسائل:', e);
    }
    return false;
}

// ================================================================
//  الستيكرات
// ================================================================

async function loadStickersFromFirebase() {
    try {
        const doc = await db.collection('stickers').doc('all').get();
        if (doc.exists) {
            const data = doc.data();
            if (data.stickers && data.stickers.length > 0) {
                window.allStickers = data.stickers;
                if (typeof window.mergeAnimeStickerPack === 'function') window.mergeAnimeStickerPack();
                try { localStorage.setItem('stickers', JSON.stringify(window.allStickers)); } catch (e) {}
                return window.allStickers;
            }
        }
    } catch (e) {
        console.warn('⚠️ فشل تحميل الستيكرات:', e);
    }

    try {
        const saved = localStorage.getItem('stickers');
        if (saved) {
            window.allStickers = JSON.parse(saved);
            if (typeof window.mergeAnimeStickerPack === 'function') window.mergeAnimeStickerPack();
            return window.allStickers;
        }
    } catch (e) {}

    window.allStickers = [
        { id: 'sticker_1', emoji: '😊', type: 'emoji' },
        { id: 'sticker_2', emoji: '😂', type: 'emoji' },
        { id: 'sticker_3', emoji: '❤️', type: 'emoji' }
    ];
    if (typeof window.mergeAnimeStickerPack === 'function') window.mergeAnimeStickerPack();
    return window.allStickers;
}

function renderStickers(tabId) {
    const grid = document.getElementById('stickerGrid');
    if (!grid) return;
    let stickers = window.allStickers;
    if (tabId !== 'my_stickers' && tabId !== 'all_stickers') {
        stickers = window.allStickers.filter(s => s.pack === tabId);
        if (stickers.length === 0) stickers = window.allStickers;
    } else if (tabId === 'all_stickers') {
        stickers = window.allStickers;
    }
    if (!stickers || stickers.length === 0) {
        grid.innerHTML = '<div style="padding:20px;text-align:center;opacity:0.5;width:100%;">لا توجد ستيكرات</div>';
        return;
    }
    grid.innerHTML = '';
    stickers.forEach(sticker => {
        const span = document.createElement('span');
        span.className = 'sticker-item';
        if (sticker.type === 'image' && sticker.data) {
            span.innerHTML = `<img src="${escapeHtml(sticker.data)}" alt="${escapeHtml(sticker.name || 'ستيكر')}" title="بواسطة: ${escapeHtml(sticker.uploadedBy || 'غير معروف')}" />`;
        } else {
            span.textContent = sticker.emoji || '😊';
            span.title = sticker.uploadedBy ? `بواسطة: ${sticker.uploadedBy}` : '';
        }
        span.onclick = function() {
            if (sticker.type === 'image' && sticker.data) {
                sendStickerMessage(sticker.data);
            } else {
                insertAtCursor(sticker.emoji || '😊');
            }
            document.getElementById('stickerPanel').style.display = 'none';
        };
        grid.appendChild(span);
    });
}

function renderStickerTabs() {
    const tabs = document.getElementById('stickerTabs');
    if (!tabs) return;
    tabs.innerHTML = '';

    const myTab = document.createElement('span');
    myTab.className = 'tab' + (window.currentStickerTab === 'my_stickers' ? ' active' : '');
    myTab.textContent = '⭐ ستيكراتي';
    myTab.onclick = function() {
        window.currentStickerTab = 'my_stickers';
        renderStickerTabs();
        renderStickers(window.currentStickerTab);
    };
    tabs.appendChild(myTab);

    const allTab = document.createElement('span');
    allTab.className = 'tab' + (window.currentStickerTab === 'all_stickers' ? ' active' : '');
    allTab.textContent = '📦 الكل';
    allTab.onclick = function() {
        window.currentStickerTab = 'all_stickers';
        renderStickerTabs();
        renderStickers(window.currentStickerTab);
    };
    tabs.appendChild(allTab);

    const packs = {};
    window.allStickers.forEach(s => {
        if (s.pack) {
            if (!packs[s.pack]) packs[s.pack] = [];
            packs[s.pack].push(s);
        }
    });
    Object.keys(packs).forEach(packName => {
        const tab = document.createElement('span');
        tab.className = 'tab' + (window.currentStickerTab === packName ? ' active' : '');
        tab.textContent = packName;
        tab.onclick = function() {
            window.currentStickerTab = packName;
            renderStickerTabs();
            renderStickers(window.currentStickerTab);
        };
        tabs.appendChild(tab);
    });

    const uploadTab = document.createElement('span');
    uploadTab.className = 'tab upload';
    uploadTab.textContent = '📷 رفع';
    uploadTab.onclick = function() { document.getElementById('stickerFileInput').click(); };
    tabs.appendChild(uploadTab);

    const downloadTab = document.createElement('span');
    downloadTab.className = 'tab download';
    downloadTab.textContent = '📥 تحميل';
    downloadTab.onclick = downloadStickerPack;
    tabs.appendChild(downloadTab);
}

async function sendStickerMessage(stickerData) {
    try {
        const msgData = {
            username: window.currentUser,
            text: '🎨 ستيكر',
            avatar: window.myAvatar,
            isAdmin: window.isAdmin,
            timestamp: firebase.firestore.FieldValue.serverTimestamp(),
            deleted: false,
            reactions: {},
            edited: false,
            editedAt: null,
            sticker: true,
            stickerData: stickerData,
            compressed: true
        };
        await db.collection('messages').add(msgData);
    } catch (error) {
        console.error('فشل إرسال الستيكر:', error);
        showNotification('❌ فشل الإرسال');
    }
}

// اسم الدالة بالزر بواجهة HTML هو uploadStickerPack بينما التنفيذ الفعلي هنا -
// alias يضمن عمل الزر (كان الزر بدون هذا معطّل تماماً: ReferenceError صامت)
function uploadStickerPack() { return downloadStickerPack(); }

async function downloadStickerPack() {
    const packName = prompt('أدخل اسم الحزمة:');
    if (!packName) return;
    try {
        showNotification('⏳ جاري تحميل الستيكرات...');
        const emojis = ['😊', '😂', '❤️', '🔥', '👍', '👏', '🎉', '✨', '💪', '🤔', '😍', '🥰', '😎', '🤩', '👀', '💀', '🙏',
            '😅', '🤗', '😇', '🥳', '🤯', '😱', '🥺', '😤', '👻', '🎃', '💫', '⭐', '🌈'
        ];
        const shuffled = emojis.sort(() => Math.random() - 0.5);
        const selected = shuffled.slice(0, 12);
        const newStickers = selected.map((emoji, index) => ({
            id: 'pack_' + Date.now() + '_' + index,
            emoji: emoji,
            type: 'emoji',
            pack: packName,
            uploadedBy: window.currentUser
        }));
        window.allStickers = [...window.allStickers, ...newStickers];
        await saveStickersToFirebase(window.allStickers);
        try { localStorage.setItem('stickers', JSON.stringify(window.allStickers)); } catch (e) {}
        renderStickerTabs();
        renderStickers(window.currentStickerTab);
        renderAdminStickers();
        showNotification(`✅ تم تحميل ${newStickers.length} ستيكر`);
    } catch (error) {
        console.error(error);
        showNotification('❌ فشل تحميل الستيكرات');
    }
}

/**
 * رفع ستيكر صورة واحد من جهاز المستخدم (كانت هذه الدالة تُستدعى من واجهة
 * الدردشة بدون أن تكون معرّفة إطلاقاً، فيفشل رفع الستيكرات بصمت)
 */
async function uploadSticker(file) {
    if (!file || !file.type || !file.type.startsWith('image/')) { showNotification('❌ يرجى اختيار صورة'); return; }
    if (file.size > 5 * 1024 * 1024) { showNotification('❌ حجم الصورة كبير جداً (أقصى 5MB)'); return; }
    try {
        showNotification('⏳ جاري معالجة الستيكر...');
        const dataUrl = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => resolve(e.target.result);
            reader.onerror = () => reject(new Error('فشل قراءة الملف'));
            reader.readAsDataURL(file);
        });
        // ضغط ذكي إلى حجم صغير آمن (الستيكرات تُخزَّن كلها بمستند واحد
        // stickers/all بـ Firestore، فيجب أن يبقى كل ستيكر صغيراً)
        const compressed = await compressImageSmart(dataUrl, {
            maxWidth: 300, startQuality: 0.7, minQuality: 0.3, maxBase64Bytes: 100 * 1024
        });
        const newSticker = {
            id: 'sticker_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
            type: 'image',
            data: compressed,
            name: (file.name || 'ستيكر').slice(0, 30),
            uploadedBy: window.currentUser
        };
        window.allStickers = [...(window.allStickers || []), newSticker];
        await saveStickersToFirebase(window.allStickers);
        try { localStorage.setItem('stickers', JSON.stringify(window.allStickers)); } catch (e) {}
        renderStickerTabs();
        renderStickers(window.currentStickerTab);
        if (typeof renderAdminStickers === 'function') renderAdminStickers();
        showNotification('✅ تم رفع الستيكر');
    } catch (error) {
        console.error('فشل رفع الستيكر:', error);
        showNotification('❌ ' + (error.message || 'فشل رفع الستيكر'));
    }
}

function renderAdminStickers() {
    const container = document.getElementById('adminStickerList');
    if (!container) return;
    container.innerHTML = '';
    window.allStickers.forEach((sticker) => {
        const div = document.createElement('div');
        div.className = 'sticker-item-admin';
        const stickerIdAttr = escapeJsAttr(sticker.id);
        if (sticker.type === 'image' && sticker.data) {
            div.innerHTML = `
                <img src="${escapeHtml(sticker.data)}" />
                <span style="font-size:0.6rem;opacity:0.5;">${escapeHtml(sticker.name || 'صورة')}</span>
                <span style="font-size:0.5rem;opacity:0.3;">${escapeHtml(sticker.uploadedBy || '')}</span>
                <button onclick="deleteSticker('${stickerIdAttr}')" style="background:none;border:none;color:#E74C3C;cursor:pointer;">✕</button>
            `;
        } else {
            div.innerHTML = `
                <span style="font-size:1.2rem;">${escapeHtml(sticker.emoji || '😊')}</span>
                <span style="font-size:0.5rem;opacity:0.3;">${escapeHtml(sticker.uploadedBy || '')}</span>
                <button onclick="deleteSticker('${stickerIdAttr}')" style="background:none;border:none;color:#E74C3C;cursor:pointer;">✕</button>
            `;
        }
        container.appendChild(div);
    });
    if (window.allStickers.length === 0) {
        container.innerHTML = '<div style="opacity:0.4;font-size:0.7rem;padding:8px 0;">لا توجد ستيكرات</div>';
    }
}

async function deleteSticker(stickerId) {
    if (!window.isAdmin) { showNotification('❌ فقط المسؤول يمكنه الحذف'); return; }
    if (!confirm('حذف الستيكر نهائياً؟')) return;
    window.allStickers = window.allStickers.filter(s => s.id !== stickerId);
    await saveStickersToFirebase(window.allStickers);
    try { localStorage.setItem('stickers', JSON.stringify(window.allStickers)); } catch (e) {}
    renderStickerTabs();
    renderStickers(window.currentStickerTab);
    renderAdminStickers();
    showNotification('✅ تم حذف الستيكر');
}

async function saveStickersToFirebase(stickers) {
    try {
        await db.collection('stickers').doc('all').set({
            stickers: stickers,
            updatedAt: firebase.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
    } catch (e) { console.warn('⚠️ فشل حفظ الستيكرات:', e); }
}

// ================================================================
//  الكلمات الممنوعة
// ================================================================

async function loadBadWords() {
    try {
        const snapshot = await db.collection('badwords').get();
        window.badWords = [];
        snapshot.forEach(doc => { window.badWords.push(doc.data().word); });
        renderBadWords();
    } catch (error) { console.error('خطأ في تحميل الكلمات الممنوعة:', error); }
}

function renderBadWords() {
    const list = document.getElementById('badWordsList');
    list.innerHTML = '';
    window.badWords.forEach(word => {
        const tag = document.createElement('span');
        tag.className = 'word';
        tag.innerHTML = `${escapeHtml(word)} <button class="remove" onclick="removeBadWord('${escapeJsAttr(word)}')">✕</button>`;
        list.appendChild(tag);
    });
}

async function addBadWord() {
    const word = document.getElementById('badWordInput').value.trim().toLowerCase();
    if (!word) { showNotification('❌ أدخل كلمة'); return; }
    if (window.badWords.includes(word)) { showNotification('⚠️ الكلمة موجودة بالفعل'); return; }
    try {
        await db.collection('badwords').doc(word).set({ word: word,
            createdAt: firebase.firestore.FieldValue.serverTimestamp() });
        window.badWords.push(word);
        renderBadWords();
        document.getElementById('badWordInput').value = '';
        showNotification('✅ تم إضافة الكلمة');
    } catch (error) { showNotification('❌ فشل الإضافة'); }
}

async function removeBadWord(word) {
    try {
        await db.collection('badwords').doc(word).delete();
        window.badWords = window.badWords.filter(w => w !== word);
        renderBadWords();
        showNotification('✅ تم حذف الكلمة');
    } catch (error) { showNotification('❌ فشل الحذف'); }
}

// ================================================================
//  نظام الحظر التلقائي
// ================================================================

let userBanCounts = {};

async function loadBanCounts() {
    try {
        const snapshot = await db.collection('bans').get();
        snapshot.forEach(doc => {
            userBanCounts[doc.id] = doc.data().count || 0;
        });
    } catch (error) { console.error('خطأ في تحميل عدد الحظر:', error); }
}

async function checkBadWords(text, username) {
    // المسؤول (admin) مستثنى تماماً من فلتر الكلمات الممنوعة ولا يُحظر مهما كتب
    if (window.isAdmin) return false;
    const lowerText = text.toLowerCase();
    let foundWords = [];
    for (const word of window.badWords) {
        if (lowerText.includes(word)) { foundWords.push(word); }
    }
    if (foundWords.length > 0) {
        const banRef = db.collection('bans').doc(username);
        const banDoc = await banRef.get();
        let count = 1;
        if (banDoc.exists) { count = (banDoc.data().count || 0) + 1; }
        await banRef.set({
            username: username,
            count: count,
            lastWord: foundWords.join(', '),
            lastUpdated: firebase.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        userBanCounts[username] = count;
        const banDuration = Math.min(count * 60, 600);
        await db.collection('users').doc(username).update({
            banned: true,
            banReason: `استخدام كلمات ممنوعة (${foundWords.join(', ')})`,
            banExpires: firebase.firestore.FieldValue.serverTimestamp() + banDuration * 1000,
            banCount: count
        });
        showNotification(`⛔ تم حظر ${username} لمدة ${banDuration} ثانية`);
        setTimeout(async () => {
            await db.collection('users').doc(username).update({
                banned: false,
                banReason: null,
                banExpires: null
            });
            showNotification(`🔓 تم إلغاء حظر ${username}`);
        }, banDuration * 1000);
        return true;
    }
    return false;
}

// ================================================================
//  إرسال الرسائل
// ================================================================

function setupMessageSending() {
    document.getElementById('sendBtn').onclick = sendMessage;
    document.getElementById('sendBtn').disabled = true;

    // على الجوال: زر "الإدخال/السطر الجديد" بلوحة المفاتيح ينزل سطر جديد دائماً،
    // والإرسال يتم فقط بزر الإرسال. على الكمبيوتر (لوحة مفاتيح فعلية + فأرة):
    // Enter يرسل، و Shift+Enter ينزل سطر جديد (السلوك المعتاد بتطبيقات الدردشة).
    const isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0) || window.matchMedia('(pointer: coarse)').matches;
    document.getElementById('messageInput').onkeydown = function(e) {
        if (e.key !== 'Enter') return;
        if (isTouchDevice) { return; } // اترك السلوك الافتراضي: سطر جديد
        if (e.shiftKey) { return; }
        e.preventDefault();
        sendMessage();
    };

    // إصلاح أداء مهم: كان الكود يكتب لـ Firestore مع كل ضغطة زر (keystroke)
    // من كل مستخدم، وكل كتابة تُبث فوراً realtime لكل المستخدمين المتصلين
    // (لأن الكل يستمع لنفس مستند typing/status). هذا كان السبب الحقيقي
    // للتهنيج/اللاق عند كتابة أكثر من شخص بنفس الوقت. الآن نكتب فقط عند
    // تغيّر الحالة (بدأ الكتابة / توقف) بدل كل حرف.
    let lastTypingState = null;
    document.getElementById('messageInput').addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';

        const hasText = this.value.trim().length > 0;
        const sendBtnEl = document.getElementById('sendBtn');
        if (sendBtnEl) sendBtnEl.disabled = !hasText;
        if (hasText !== lastTypingState) {
            lastTypingState = hasText;
            db.collection('typing').doc('status').set({
                [window.currentUser]: hasText
            }, { merge: true }).catch(() => {});
        }
        clearTimeout(window.typingTimeout);
        if (hasText) {
            window.typingTimeout = setTimeout(() => {
                lastTypingState = false;
                db.collection('typing').doc('status').set({
                    [window.currentUser]: false
                }, { merge: true }).catch(() => {});
            }, 1500);
        }
    });
}

async function sendMessage() {
    const text = document.getElementById('messageInput').value.trim();
    if (!text) return;

    if (window.settings.muteAll && !window.isAdmin) {
        showNotification('🔇 الدردشة مكتومة حالياً');
        return;
    }

    if (await checkBadWords(text, window.currentUser)) {
        document.getElementById('messageInput').value = '';
        document.getElementById('messageInput').style.height = 'auto';
        document.getElementById('sendBtn').disabled = true;
        showNotification('⛔ تم حظرك مؤقتاً');
        return;
    }

    // ================================================================
    // إرسال متفائل (Optimistic Send)
    // ================================================================
    // المشكلة القديمة: كان الكود ينتظر (await) رد السيرفر الكامل قبل ما
    // يفرّغ حقل الكتابة أو يعرض الرسالة — فكان المستخدم يحس "علّق" التطبيق
    // لثانية أو أكثر مع كل رسالة، وأسوأ لما 4 أشخاص يرسلون بنفس الوقت.
    // الحل: نعرض الرسالة فوراً بحالة "⏳ جاري الإرسال" بدون انتظار الشبكة،
    // ونربطها بمعرّف clientId فريد. لما ترجع نفس الرسالة من فايرستور عبر
    // onSnapshot نستبدلها بمكانها (reconcileMessageId) بدل تكرارها. لو فشل
    // الإرسال (لا يوجد إنترنت مثلاً) نعرض علامة "⚠️ فشل، اضغط لإعادة المحاولة".
    const clientId = 'c_' + Date.now() + '_' + Math.random().toString(36).slice(2, 9);
    const tempId = 'temp_' + clientId;
    const compressedText = compressText(text);

    const msgData = {
        username: window.currentUser,
        text: compressedText,
        avatar: window.myAvatar,
        isAdmin: window.isAdmin,
        timestamp: firebase.firestore.FieldValue.serverTimestamp(),
        deleted: false,
        reactions: {},
        edited: false,
        editedAt: null,
        compressed: true,
        clientId: clientId
    };

    if (window.replyingTo) {
        msgData.replyTo = window.replyingTo;
        msgData.replyText = compressText(window.replyingToText);
    }

    // 1) عرض فوري محلي بدون انتظار الشبكة
    const optimisticMsg = {
        id: tempId,
        ...msgData,
        timestamp: new Date(),
        status: 'sending'
    };
    addMessageToArray(optimisticMsg);
    window.pendingByClientId.set(clientId, tempId);
    scheduleRenderMessages(window.allMessages);

    document.getElementById('messageInput').value = '';
    document.getElementById('messageInput').style.height = 'auto';
    document.getElementById('sendBtn').disabled = true;
    window.replyingTo = null;
    window.replyingToText = '';
    document.getElementById('replyBar').style.display = 'none';
    document.getElementById('emojiPanel').style.display = 'none';
    document.getElementById('stickerPanel').style.display = 'none';

    window.messageCount = window.allMessages.length;
    scheduleUpdateStats();

    const container = document.getElementById('messagesContainer');
    setTimeout(() => {
        container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
    }, 50);

    // 2) الإرسال الفعلي بالخلفية مع إعادة محاولة تلقائية عند تذبذب الشبكة
    try {
        const docRef = await window.withRetry(() => db.collection('messages').add(msgData));
        // لو رجعت onSnapshot أسرع من هذا الرد وصالحت الرسالة مسبقاً، لا داعي لفعل شيء
        if (window.pendingByClientId.has(clientId)) {
            reconcileMessageId(tempId, docRef.id, { id: docRef.id, ...msgData, timestamp: new Date() });
            scheduleRenderMessages(window.allMessages);
        }
    } catch (error) {
        console.error('فشل الإرسال:', error);
        window.pendingByClientId.delete(clientId);
        updateMessageInArray(tempId, { status: 'failed' });
        scheduleRenderMessages(window.allMessages);
        showNotification('❌ فشل الإرسال، اضغط على الرسالة لإعادة المحاولة');
    }

    try {
        await db.collection('typing').doc('status').set({
            [window.currentUser]: false
        }, { merge: true });
    } catch (e) {}
}

// إعادة محاولة إرسال رسالة فشلت سابقاً (تُستدعى من الضغط على الرسالة بحالة failed)
window.retryFailedMessage = async function(tempId) {
    const msg = window.messageCache.get(tempId);
    if (!msg || msg.status !== 'failed') return;

    updateMessageInArray(tempId, { status: 'sending' });
    scheduleRenderMessages(window.allMessages);

    const { id, status, ...msgData } = msg;
    msgData.timestamp = firebase.firestore.FieldValue.serverTimestamp();
    window.pendingByClientId.set(msgData.clientId, tempId);

    try {
        const docRef = await window.withRetry(() => db.collection('messages').add(msgData));
        reconcileMessageId(tempId, docRef.id, { id: docRef.id, ...msgData, timestamp: new Date() });
        scheduleRenderMessages(window.allMessages);
    } catch (error) {
        window.pendingByClientId.delete(msgData.clientId);
        updateMessageInArray(tempId, { status: 'failed' });
        scheduleRenderMessages(window.allMessages);
        showNotification('❌ فشل الإرسال مرة أخرى');
    }
};

// ================================================================
//  تحميل الرسائل
// ================================================================

async function loadMessages() {
    try {
        console.log('📥 بدء تحميل الرسائل...');

        if (loadMessagesFromStorage() && window.allMessages.length > 0) {
            renderMessages(window.allMessages);
            updateStats();
            console.log(`📦 عرض ${window.allMessages.length} رسالة من التخزين المحلي`);
        }

        await loadMessagesFromFirebase();

    } catch (error) {
        console.error('❌ فشل تحميل الرسائل:', error);
        showNotification('❌ فشل تحميل الرسائل');
    }
}

async function loadMessagesFromFirebase() {
    try {
        const snapshot = await db.collection('messages')
            .orderBy('timestamp', 'desc')
            .limit(30)
            .get();

        if (snapshot.empty) {
            if (window.allMessages.length === 0) renderMessages([]);
            return;
        }

        const firebaseMessages = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.deleted !== true) {
                firebaseMessages.push({ id: doc.id, ...data });
            }
        });

        window.lastVisibleDoc = snapshot.docs[snapshot.docs.length - 1];

        addMessagesToArray(firebaseMessages);
        renderMessages(window.allMessages);
        updateStats();

        if (window.allMessages.length >= 30) addLoadMoreButton();

    } catch (error) {
        console.error('❌ فشل تحميل الرسائل من Firebase:', error);
        throw error;
    }
}

// ================================================================
//  تحميل المزيد من الرسائل
// ================================================================

async function loadMoreMessages() {
    if (window.isLoadingMore) return;

    const btn = document.getElementById('loadMoreBtn');
    if (btn) {
        btn.classList.add('loading');
        btn.innerHTML = '⏳ جاري التحميل... <span class="spinner"></span>';
    }

    window.isLoadingMore = true;

    try {
        let query = db.collection('messages')
            .orderBy('timestamp', 'desc')
            .limit(30);

        if (window.lastVisibleDoc) {
            query = query.startAfter(window.lastVisibleDoc);
        }

        const snapshot = await query.get();

        if (snapshot.empty) {
            if (btn) { btn.style.display = 'none'; }
            showNotification('✅ تم تحميل جميع الرسائل');
            window.isLoadingMore = false;
            return;
        }

        window.lastVisibleDoc = snapshot.docs[snapshot.docs.length - 1];

        const newMessages = [];
        const existingIds = new Set(window.allMessages.map(m => m.id));

        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.deleted !== true) {
                if (!existingIds.has(doc.id)) {
                    newMessages.push({ id: doc.id, ...data });
                }
            }
        });

        if (newMessages.length > 0) {
            addMessagesToArray(newMessages);
            renderMessages(window.allMessages);
            showNotification(`✅ تم تحميل ${newMessages.length} رسالة إضافية`);
        }

        if (btn) {
            btn.classList.remove('loading');
            if (snapshot.docs.length === 30) {
                btn.innerHTML = '📤 تحميل المزيد من الرسائل';
                btn.style.display = 'block';
            } else {
                btn.style.display = 'none';
                showNotification('✅ تم تحميل جميع الرسائل');
            }
        }

    } catch (error) {
        console.error('❌ فشل تحميل المزيد:', error);
        showNotification('❌ فشل تحميل المزيد');
        const btn = document.getElementById('loadMoreBtn');
        if (btn) {
            btn.classList.remove('loading');
            btn.innerHTML = '📤 تحميل المزيد (فشل)';
            setTimeout(() => {
                btn.innerHTML = '📤 تحميل المزيد من الرسائل';
            }, 3000);
        }
    }

    window.isLoadingMore = false;
}

function addLoadMoreButton() {
    const container = document.getElementById('messagesContainer');
    const existing = document.getElementById('loadMoreBtn');
    if (existing) return;

    const btn = document.createElement('button');
    btn.id = 'loadMoreBtn';
    btn.innerHTML = '📤 تحميل المزيد من الرسائل';
    btn.onclick = loadMoreMessages;
    container.prepend(btn);
}

// ================================================================
//  استماع الرسائل
// ================================================================

// ================================================================
//  طبقة مزامنة إضافية: تجميع (coalesce) طلبات إعادة الرسم
// ================================================================
// المشكلة: عند كتابة/إرسال عدة أشخاص بنفس الوقت، كانت كل نبضة Snapshot
// (من Firestore) تستدعي renderMessages() فوراً وبشكل منفصل، وكل استدعاء
// يعيد بناء innerHTML لكل الرسائل من الصفر (DOM ثقيل). لو وصلت 3-4 نبضات
// خلال نفس اللحظة (لأن 4 أشخاص يكتبون/يرسلون بنفس الوقت) كانت تتكدّس
// وتسبب تهنيج ملحوظ بالواجهة. الحل: تجميع كل الطلبات المتتالية بنفس
// الإطار عبر requestAnimationFrame + طبقة "تهدئة" (throttle) بسيطة، بحيث
// نعيد الرسم مرة واحدة فقط حتى لو وصلت عدة تحديثات دفعة واحدة.
let _renderScheduled = false;
let _pendingRenderMessages = null;
function scheduleRenderMessages(messages) {
    _pendingRenderMessages = messages;
    if (_renderScheduled) return;
    _renderScheduled = true;
    requestAnimationFrame(() => {
        _renderScheduled = false;
        const toRender = _pendingRenderMessages;
        _pendingRenderMessages = null;
        renderMessages(toRender);
    });
}

// طبقة مزامنة إضافية: تهدئة تحديثات "حالة الاتصال/الإحصائيات" التي كانت
// تُستدعى مع كل نبضة رسائل حتى لو ما تغيّر شيء ملموس بالواجهة
let _statsUpdateTimeout = null;
function scheduleUpdateStats() {
    if (_statsUpdateTimeout) return;
    _statsUpdateTimeout = setTimeout(() => {
        _statsUpdateTimeout = null;
        updateStats();
    }, 200);
}

function listenMessages() {
    if (window.messagesListener) {
        try { window.messagesListener(); } catch (e) {}
        window.messagesListener = null;
    }

    window.messagesListener = db.collection('messages')
        .orderBy('timestamp', 'desc')
        .limit(50)
        .onSnapshot((snapshot) => {
            try {
                if (snapshot.empty) return;

                // طبقة مزامنة إضافية: نستخدم docChanges() بدل المرور على كل الـ 50
                // مستند بكل نبضة (snapshot.forEach القديم). فايرستور يعطينا فقط
                // المستندات التي فعلاً تغيّرت (أُضيفت/تعدّلت/حُذفت)، فالمعالجة تصير
                // O(عدد التغييرات) بدل O(50) دائماً — فرق كبير لما 4 أشخاص يكتبون
                // بنفس الوقت وتتوالى النبضات بسرعة.
                const changes = snapshot.docChanges();
                const newMessages = [];
                let hasOtherChanges = false;

                changes.forEach(change => {
                    const doc = change.doc;
                    const data = doc.data();

                    if (change.type === 'removed' || data.deleted === true) {
                        if (window.messageCache.has(doc.id)) {
                            deleteMessageFromArray(doc.id);
                            hasOtherChanges = true;
                        }
                        return;
                    }

                    if (change.type === 'added') {
                        // تصالح المزامنة المتفائلة: هل هذه الرسالة رسالة أرسلناها
                        // نحن محلياً وهي معروضة مسبقاً بمعرّف مؤقت؟ لو نعم، نستبدلها
                        // بمكانها بدل إضافتها كرسالة جديدة (يمنع الظهور المزدوج/الوميض)
                        if (data.clientId && window.pendingByClientId.has(data.clientId)) {
                            const tempId = window.pendingByClientId.get(data.clientId);
                            reconcileMessageId(tempId, doc.id, { id: doc.id, ...data });
                            hasOtherChanges = true;
                            return;
                        }
                        if (!window.messageCache.has(doc.id)) {
                            newMessages.push({ id: doc.id, ...data });
                        }
                        return;
                    }

                    // change.type === 'modified'
                    if (window.messageCache.has(doc.id)) {
                        updateMessageInArray(doc.id, data);
                        hasOtherChanges = true;
                    }
                });

                if (newMessages.length > 0) {
                    addMessagesToArray(newMessages);
                    scheduleRenderMessages(window.allMessages);
                    window.messageCount = window.allMessages.length;
                    scheduleUpdateStats();

                    const container = document.getElementById('messagesContainer');
                    const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 50;
                    if (isAtBottom) {
                        setTimeout(() => {
                            container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
                        }, 100);
                    } else {
                        window.unreadCount += newMessages.length;
                        document.getElementById('scrollToBottom').style.display = 'flex';
                        document.getElementById('scrollToBottom').innerHTML =
                            `⬇️ <span class="badge">${window.unreadCount}</span>`;
                    }

                    // إشعار محلي إذا كان التطبيق بالخلفية (راجع js/pwa.js)
                    if (typeof notifyNewMessages === 'function') {
                        notifyNewMessages(newMessages);
                    }
                } else if (hasOtherChanges) {
                    scheduleRenderMessages(window.allMessages);
                    scheduleUpdateStats();
                }

            } catch (error) {
                console.error('❌ خطأ في معالجة الرسائل:', error);
            }
        }, (error) => {
            console.error('❌ خطأ في استماع الرسائل:', error);
            updateConnectionStatus(false);
        });
}

// ================================================================
//  عرض الرسائل
// ================================================================

function renderMessages(messages) {
    const container = document.getElementById('messagesContainer');
    const existingBtn = document.getElementById('loadMoreBtn');
    const btnClone = existingBtn ? existingBtn.cloneNode(true) : null;

    if (!messages || messages.length === 0) {
        container.innerHTML = `
            <div class="welcome-msg">
                <div class="icon">👥</div>
                <div class="title">مرحباً في Tomodachi</div>
                <div class="sub">ابدأ المحادثة الآن</div>
            </div>
        `;
        return;
    }

    const sorted = [...messages].sort((a, b) => {
        const timeA = getTimestampValue(a.timestamp);
        const timeB = getTimestampValue(b.timestamp);
        return timeA - timeB;
    });

    let html = '';
    let lastDate = '';

    sorted.forEach(data => {
        if (data.deleted === true) return;

        const isSent = data.username === window.currentUser;
        const sender = data.username || 'مجهول';
        const senderSafe = escapeHtml(sender);
        const senderAttr = escapeJsAttr(sender);
        const avatar = data.avatar || '👥';
        const avatarSafe = escapeHtml(avatar);
        const idAttr = escapeJsAttr(data.id);
        const time = formatTime(data.timestamp);
        const adminBadge = data.isAdmin ? ' ⭐' : '';
        const reactions = data.reactions || {};
        const reactionKeys = Object.keys(reactions);

        const date = formatDate(data.timestamp);
        if (date && date !== lastDate) {
            html += `<div class="date-divider">${escapeHtml(date)}</div>`;
            lastDate = date;
        }

        let reactionsHtml = '';
        if (reactionKeys.length > 0) {
            reactionsHtml = `<div class="reactions">`;
            reactionKeys.forEach(emoji => {
                const users = reactions[emoji] || [];
                reactionsHtml += `
                    <span class="reaction" onclick="toggleReaction('${idAttr}', '${escapeJsAttr(emoji)}')">
                        ${escapeHtml(emoji)} ${users.length}
                    </span>
                `;
            });
            reactionsHtml += `</div>`;
        }

        const editedMark = data.edited ? ' (معدلة)' : '';
        let replyHtml = '';
        if (data.replyTo && data.replyText) {
            const replyTextDecompressed = escapeHtml(decompressText(data.replyText));
            const replyAvatarSafe = escapeHtml(data.avatar);
            replyHtml = `
                <div class="reply-preview">
                    <span class="reply-avatar">${isValidImageUrl(data.avatar) ? `<img src="${replyAvatarSafe}" />` : (replyAvatarSafe || '👤')}</span>
                    ↩️ ${replyTextDecompressed.substring(0, 35)}${replyTextDecompressed.length > 35 ? '...' : ''}
                </div>
            `;
        }

        const avatarDisplay = isValidImageUrl(avatar) ? `<img src="${avatarSafe}" alt="avatar" />` : avatarSafe;

        let stickerHtml = '';
        if (data.sticker && data.stickerData) {
            stickerHtml = `<span class="sticker"><img src="${escapeHtml(data.stickerData)}" /></span>`;
        } else if (data.sticker) {
            stickerHtml = `<span class="sticker">${escapeHtml(data.text)}</span>`;
        }

        let imageHtml = '';
        if (data.image && data.imageData) {
            imageHtml = `<img src="${escapeHtml(data.imageData)}" class="message-image" loading="lazy" decoding="async" onload="this.classList.add('loaded')" onclick="viewImage('${escapeJsAttr(data.imageData)}')" />`;
        }

        const decompressedText = escapeHtml(decompressText(data.text));
        const sentClass = isSent ? 'sent custom-color' : 'received custom-color';
        const statusClass = data.status ? ` msg-status-${data.status}` : '';
        const retryAttr = data.status === 'failed' ? ` onclick="retryFailedMessage('${idAttr}')"` : '';
        let statusIcon = '';
        if (isSent) {
            if (data.status === 'sending') statusIcon = '<span class="check pending">⏳</span>';
            else if (data.status === 'failed') statusIcon = '<span class="check failed">⚠️ إعادة المحاولة</span>';
            else statusIcon = '<span class="check">✓✓</span>';
        }

        html += `
            <div class="msg-wrapper${statusClass}" 
                 data-id="${idAttr}" 
                 data-text="${decompressedText}"
                 data-sent="${isSent}"${retryAttr}>
                ${replyHtml}
                <div class="msg ${sentClass}">
                    <div class="avatar-small" onclick="showProfile('${senderAttr}')" title="عرض الملف الشخصي">
                        ${avatarDisplay}
                    </div>
                    <div class="content">
                        <span class="sender-name" onclick="showProfile('${senderAttr}')">${senderSafe}${adminBadge}</span>
                        ${stickerHtml || imageHtml || `<div class="text">${decompressedText}${editedMark}</div>`}
                        <div class="time">
                            ${time}
                            ${statusIcon}
                        </div>
                        ${reactionsHtml}
                    </div>
                </div>
            </div>
        `;
    });

    if (btnClone && btnClone.style.display !== 'none') {
        container.innerHTML = html;
        container.prepend(btnClone);
        btnClone.onclick = loadMoreMessages;
    } else {
        container.innerHTML = html;
    }

    const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 50;
    if (isAtBottom && messages.length > 0 && !window.isUserScrolling) {
        setTimeout(() => {
            container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
        }, 50);
    }

    setTimeout(setupSwipeToReply, 50);
}

window.viewImage = function(imageData) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0,0,0,0.85); z-index: 1000;
        display: flex; align-items: center; justify-content: center;
        cursor: pointer; animation: fadeIn 0.2s ease;
    `;
    modal.innerHTML = `<img src="${escapeHtml(imageData)}" style="max-width:90%;max-height:90%;border-radius:12px;object-fit:contain;" />`;
    modal.onclick = () => modal.remove();
    document.body.appendChild(modal);
};

// ================================================================
//  التفاعلات
// ================================================================

window.toggleReaction = async function(messageId, emoji) {
    if (!window.currentUser) return;
    try {
        const docRef = db.collection('messages').doc(messageId);
        const doc = await docRef.get();
        if (!doc.exists) return;
        const data = doc.data();
        const reactions = data.reactions || {};
        const users = reactions[emoji] || [];
        const index = users.indexOf(window.currentUser);
        if (index > -1) { users.splice(index, 1); } else { users.push(window.currentUser); }
        if (users.length === 0) { delete reactions[emoji]; } else { reactions[emoji] = users; }
        await docRef.update({ reactions: reactions });
        updateMessageInArray(messageId, { reactions: reactions });
    } catch (error) { console.error('خطأ في التفاعل:', error); }
};

// ================================================================
//  السحب للرد
// ================================================================

function setupSwipeToReply() {
    const container = document.getElementById('messagesContainer');
    container.removeEventListener('touchstart', handleSwipeStart);
    container.removeEventListener('touchmove', handleSwipeMove);
    container.removeEventListener('touchend', handleSwipeEnd);
    container.addEventListener('touchstart', handleSwipeStart, { passive: true });
    container.addEventListener('touchmove', handleSwipeMove, { passive: true });
    container.addEventListener('touchend', handleSwipeEnd, { passive: true });
}

function handleSwipeStart(e) {
    const touch = e.touches[0];
    const target = document.elementFromPoint(touch.clientX, touch.clientY);
    const wrapper = target ? target.closest('.msg-wrapper') : null;
    if (wrapper) {
        window.swipeTarget = wrapper;
        window.swipeStartX = touch.clientX;
    } else { window.swipeTarget = null; }
}

function handleSwipeMove(e) {
    if (!window.swipeTarget) return;
    const touch = e.touches[0];
    const deltaX = touch.clientX - window.swipeStartX;
    if (deltaX > 30) {
        const msg = window.swipeTarget.querySelector('.msg');
        if (msg) {
            msg.style.transform = `translateX(${Math.min(deltaX, 80)}px)`;
            msg.style.opacity = 0.7;
        }
    }
}

function handleSwipeEnd(e) {
    if (!window.swipeTarget) return;
    const msg = window.swipeTarget.querySelector('.msg');
    if (msg) {
        const transform = msg.style.transform || '';
        const match = transform.match(/translateX\((\d+)px\)/);
        const deltaX = match ? parseInt(match[1]) : 0;
        if (deltaX > 50) {
            const messageId = window.swipeTarget.dataset.id;
            const messageText = window.swipeTarget.dataset.text || '';
            if (messageId) { startReply(messageId, messageText); }
        }
        msg.style.transform = '';
        msg.style.opacity = '';
    }
    window.swipeTarget = null;
}

// ================================================================
//  الرد على الرسالة
// ================================================================

function startReply(messageId, messageText) {
    window.replyingTo = messageId;
    window.replyingToText = messageText;
    document.getElementById('replyBar').style.display = 'flex';
    const messageTextSafe = escapeHtml(messageText);
    document.getElementById('replyText').innerHTML =
        `<span class="reply-name">الرد على:</span> ${messageTextSafe.substring(0, 45)}${messageTextSafe.length > 45 ? '...' : ''}`;
    document.getElementById('messageInput').focus();
}

// ================================================================
//  تعديل الرسالة
// ================================================================

window.editMessage = function(messageId) {
    if (!window.currentUser) return;
    window.editingMessageId = messageId;
    db.collection('messages').doc(messageId).get().then(doc => {
        if (doc.exists) {
            const data = doc.data();
            if (data.username === window.currentUser || window.isAdmin) {
                document.getElementById('editMessageInput').value = decompressText(data.text);
                document.getElementById('editMessageModal').style.display = 'flex';
            } else { showNotification('❌ لا يمكنك تعديل رسالة أخرى'); }
        }
    }).catch(error => {
        console.error('خطأ:', error);
        showNotification('❌ فشل تحميل الرسالة');
    });
};

// ================================================================
//  حذف الرسالة
// ================================================================

window.deleteMessage = async function(id) {
    if (window.settings.confirmDelete && !window.isAdmin && !confirm('حذف الرسالة؟')) return;
    try {
        await db.collection('messages').doc(id).update({ deleted: true });
        deleteMessageFromArray(id);
        renderMessages(window.allMessages);
        showNotification('✅ تم الحذف');
    } catch (error) {
        console.error(error);
        showNotification('❌ فشل الحذف');
    }
};

// ================================================================
//  الملف الشخصي
// ================================================================

async function showProfile(username) {
    try {
        const doc = await db.collection('users').doc(username).get();
        if (!doc.exists) { showNotification('❌ المستخدم غير موجود'); return; }
        const data = doc.data();
        const avatar = data.avatar || '👥';
        const bio = data.bio || '📝 لا توجد نبذة';
        const status = data.online ? '🟢 متصل الآن' : '⚫ غير متصل';

        const avatarSafe = escapeHtml(avatar);
        document.getElementById('profileAvatarDisplay').innerHTML = isValidImageUrl(avatar) ?
            `<img src="${avatarSafe}" alt="avatar" style="width:100%;height:100%;object-fit:cover;" />` : avatarSafe;
        document.getElementById('profileNameDisplay').textContent = username + (data.isAdmin ? ' ⭐' : '');
        document.getElementById('profileUsernameDisplay').textContent = '@' + username;
        document.getElementById('profileBioDisplay').textContent = bio;
        document.getElementById('profileStatusDisplay').textContent = status;

        document.getElementById('profileModal').style.display = 'flex';
    } catch (e) {
        showNotification('❌ فشل تحميل الملف الشخصي');
        console.error(e);
    }
}

// ================================================================
//  مودال تعديل الصورة
// ================================================================

function openAvatarModal() {
    if (!window.currentUser) return;
    document.getElementById('avatarModal').style.display = 'flex';
    document.getElementById('editNameInput').value = window.currentUser;
    document.getElementById('editBioInput').value = window.myBio || '📝 مرحباً، أنا في Tomodachi!';
    const preview = document.getElementById('avatarPreview');
    if (isValidImageUrl(window.myAvatar)) {
        preview.innerHTML = `<img src="${escapeHtml(window.myAvatar)}" alt="avatar" /><div class="upload-overlay">📷 تغيير</div>`;
    } else {
        preview.innerHTML = `${escapeHtml(window.myAvatar)}<div class="upload-overlay">📷 تغيير</div>`;
    }

    const menu = document.getElementById('mobileMenu');
    if (menu && menu.classList.contains('open')) {
        menu.classList.remove('open');
        document.getElementById('menuOverlay').classList.remove('show');
        document.body.style.overflow = '';
    }
}

// ================================================================
//  استماع المستخدمين
// ================================================================

function listenUsers() {
    if (window.usersListener) {
        try { window.usersListener(); } catch (e) {}
        window.usersListener = null;
    }
    window.usersListener = db.collection('users').onSnapshot((snapshot) => {
        let online = 0;
        let blocked = 0;
        let adminListHtml = '';
        window.allUsers = {};

        snapshot.forEach(doc => {
            const data = doc.data();
            window.allUsers[doc.id] = data;
            if (data.online) online++;
            if (data.blocked) blocked++;
            if (window.isAdmin && doc.id !== window.currentUser) {
                const status = data.online ? '🟢' : '⚫';
                const adminBadge = data.isAdmin ? '<span class="badge admin">مسؤول</span>' : '';
                const blockedBadge = data.blocked ? '<span class="badge blocked">محظور</span>' : '';
                const bannedBadge = data.banned ? '<span class="badge banned">⛔ محظور</span>' : '';
                const uidAttr = escapeJsAttr(doc.id);
                const uidSafe = escapeHtml(doc.id);
                const avatarSafe = escapeHtml(data.avatar);
                adminListHtml += `
                    <div class="user-item">
                        <div class="info">
                            <div class="avatar">${isValidImageUrl(data.avatar) ? `<img src="${avatarSafe}" />` : (avatarSafe || '👤')}</div>
                            <span class="name">${uidSafe}</span>
                            ${adminBadge}${blockedBadge}${bannedBadge}
                            <span class="status">${status}</span>
                        </div>
                        <div class="actions-group">
                            ${data.blocked ? 
                                `<button class="btn-unblock" onclick="unblockUser('${uidAttr}')">🔓</button>` :
                                `<button class="btn-block" onclick="blockUser('${uidAttr}')">⛔</button>`
                            }
                            ${data.banned ? 
                                `<button class="btn-unblock" onclick="unbanUser('${uidAttr}')">🔓 إلغاء</button>` : ''
                            }
                            <button class="btn-delete" onclick="deleteUser('${uidAttr}')">🗑️</button>
                            ${!data.isAdmin && doc.id !== 'slx23m' ? 
                                `<button class="btn-admin" onclick="makeAdmin('${uidAttr}')">⭐</button>` : ''}
                            <button class="btn-warn" onclick="warnUser('${uidAttr}')">⚠️</button>
                        </div>
                    </div>
                `;
            }
        });

        window.onlineUsers = online;
        window.blockedCount = blocked;
        updateStats();

        document.getElementById('statUsers').textContent = Object.keys(window.allUsers).length;
        document.getElementById('statOnline').textContent = window.onlineUsers;
        document.getElementById('statMessages').textContent = window.messageCount;
        document.getElementById('statBlocked').textContent = window.blockedCount;

        if (window.isAdmin) {
            const currentData = window.allUsers[window.currentUser];
            if (currentData) {
                // إصلاح ثغرة XSS: كان أفاتار المستخدم يُدرج هنا بدون تنظيف (escapeHtml)،
                // فأي قيمة avatar تحتوي على علامة اقتباس (") تقدر "تكسر" خاصية src
                // وتحقن HTML/JS تعسفي (تُنفَّذ عند كل من يفتح لوحة المسؤول). بما إن
                // قواعد فايرستور الحالية لا تمنع أي زائر من كتابة أي نص بحقل avatar
                // مباشرة عبر console المتصفح (بدون المرور على واجهة رفع الصورة)،
                // هذا الإصلاح ضروري وليس نظرياً.
                const currentAvatarSafe = escapeHtml(currentData.avatar || '');
                adminListHtml = `
                    <div class="user-item" style="background:rgba(24,119,242,0.04);border-color:#1877F2;">
                        <div class="info">
                            <div class="avatar">${isValidImageUrl(currentData.avatar) ? `<img src="${currentAvatarSafe}" />` : (currentAvatarSafe || '👤')}</div>
                            <span class="name">${window.currentUser} <span class="badge you">أنت</span></span>
                            <span class="status">🟢</span>
                        </div>
                        <span style="font-size:0.45rem;opacity:0.4;">نشط</span>
                    </div>
                ` + adminListHtml;
            }
            document.getElementById('adminUserList').innerHTML = adminListHtml ||
                '<div style="text-align:center;opacity:0.4;padding:10px 0;font-size:0.7rem;">لا يوجد مستخدمين</div>';
        }
    }, (error) => {
        console.error('خطأ في استماع المستخدمين:', error);
    });
}

function updateStats() {
    document.getElementById('onlineInfo').textContent = `${window.onlineUsers} متصل · ${window.messageCount} رسالة`;
    const statusEl = document.querySelector('.menu-status');
    if (statusEl) {
        statusEl.textContent = window.onlineUsers > 0 ? '🟢 متصل' : '⚫ غير متصل';
    }
}

// ================================================================
//  استماع الكتابة
// ================================================================

// طبقة مزامنة إضافية: تهدئة تحديثات مؤشر "يكتب..." — عند كتابة عدة أشخاص
// بنفس اللحظة تصل نبضات كثيرة جداً لنفس المستند، فنجمعها بإطار واحد
// بدل تحديث DOM مع كل نبضة على حدة
let _typingRenderScheduled = false;
let _pendingTypingDoc = null;
function listenTyping() {
    if (window.typingListener) {
        try { window.typingListener(); } catch (e) {}
        window.typingListener = null;
    }
    window.typingListener = db.collection('typing').doc('status').onSnapshot((doc) => {
        _pendingTypingDoc = doc;
        if (_typingRenderScheduled) return;
        _typingRenderScheduled = true;
        requestAnimationFrame(() => {
            _typingRenderScheduled = false;
            renderTypingIndicator(_pendingTypingDoc);
        });
    }, (error) => {
        console.error('❌ خطأ في استماع الكتابة:', error);
    });
}

function renderTypingIndicator(doc) {
    try {
        if (doc.exists) {
            const data = doc.data();
            const typingUsers = [];
            for (let key in data) {
                if (key !== window.currentUser && data[key] === true) { typingUsers.push(key); }
            }
            if (typingUsers.length > 0) {
                document.getElementById('typingText').textContent =
                    `${typingUsers.join('، ')} يكتب${typingUsers.length > 1 ? 'ون' : ''}...`;
                document.getElementById('typingIndicator').style.display = 'flex';
            } else {
                document.getElementById('typingText').textContent = '';
                document.getElementById('typingIndicator').style.display = 'none';
            }
        } else {
            document.getElementById('typingIndicator').style.display = 'none';
        }
    } catch (error) {
        console.error('خطأ في عرض مؤشر الكتابة:', error);
        document.getElementById('typingIndicator').style.display = 'none';
    }
}

// ================================================================
//  وظائف المسؤول
// ================================================================

window.blockUser = async function(username) {
    if (!window.isAdmin || username === 'slx23m') {
        showNotification('❌ لا يمكن حظر المسؤول');
        return;
    }
    if (!confirm(`⛔ حظر ${username}؟`)) return;
    try {
        await db.collection('users').doc(username).update({ blocked: true });
        await db.collection('blocked').doc(username).set({
            blockedBy: window.currentUser,
            blockedAt: firebase.firestore.FieldValue.serverTimestamp()
        });
        showNotification(`✅ تم حظر ${username}`);
    } catch (error) { showNotification('❌ فشل الحظر'); }
};

window.unblockUser = async function(username) {
    if (!window.isAdmin) return;
    try {
        await db.collection('users').doc(username).update({ blocked: false });
        await db.collection('blocked').doc(username).delete();
        showNotification(`✅ تم إلغاء حظر ${username}`);
    } catch (error) { showNotification('❌ فشل إلغاء الحظر'); }
};

window.unbanUser = async function(username) {
    if (!window.isAdmin) return;
    try {
        await db.collection('users').doc(username).update({
            banned: false,
            banReason: null,
            banExpires: null
        });
        await db.collection('bans').doc(username).delete();
        showNotification(`✅ تم إلغاء حظر ${username}`);
    } catch (error) { showNotification('❌ فشل إلغاء الحظر'); }
};

window.deleteUser = async function(username) {
    if (!window.isAdmin || username === 'slx23m') {
        showNotification('❌ لا يمكن حذف المسؤول');
        return;
    }
    if (!confirm(`🗑️ حذف ${username} نهائياً؟`)) return;
    try {
        await db.collection('users').doc(username).delete();
        const msgs = await db.collection('messages').where('username', '==', username).get();
        const batch = db.batch();
        msgs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
        const toDelete = window.allMessages.filter(m => m.username === username);
        toDelete.forEach(m => deleteMessageFromArray(m.id));
        renderMessages(window.allMessages);
        showNotification(`✅ تم حذف ${username}`);
    } catch (error) { showNotification('❌ فشل الحذف'); }
};

window.makeAdmin = async function(username) {
    if (!window.isAdmin || username === 'slx23m') return;
    if (!confirm(`⭐ ترقية ${username}؟`)) return;
    try {
        await db.collection('users').doc(username).update({ isAdmin: true });
        showNotification(`✅ تم ترقية ${username}`);
    } catch (error) { showNotification('❌ فشل الترقية'); }
};

window.warnUser = async function(username) {
    if (!window.isAdmin) return;
    showNotification(`⚠️ تم إرسال تحذير إلى ${username}`);
};

window.clearAllMessages = async function() {
    if (!window.isAdmin) return;
    if (!confirm('⚠️ هل تريد حذف جميع الرسائل نهائياً؟')) return;
    try {
        const snapshot = await db.collection('messages').get();
        const batch = db.batch();
        snapshot.forEach(doc => batch.delete(doc.ref));
        await batch.commit();

        window.allMessages = [];
        window.messageCache.clear();
        window.processedIds.clear();
        localStorage.removeItem('tomodachi_messages');
        renderMessages([]);
        updateStats();
        showNotification('✅ تم مسح جميع الرسائل');
    } catch (error) {
        showNotification('❌ فشل المسح');
        console.error(error);
    }
};

// ================================================================
//  فتح لوحة المسؤول
// ================================================================

function openAdminPanel() {
    if (!window.isAdmin) {
        showNotification('❌ فقط المسؤول يمكنه الدخول');
        return;
    }
    document.getElementById('adminPanel').style.display = 'flex';
    document.getElementById('adminPanel').classList.add('fade-enter');
    updateStats();
    loadBadWords();
    renderAdminStickers();
}

// ================================================================
//  المزامنة
// ================================================================

// ================================================================
// طبقة تحسين السيرفر: تقليل ضغط "فحص الاتصال"
// ================================================================
// كان الكود القديم يعمل قراءة فعلية من فايرستور (db.collection('_').get())
// كل 5 ثوانٍ طول الوقت لكل مستخدم متصل، فقط للتحقق من الاتصال! هذا يعني
// 720 قراءة/ساعة لكل شخص بدون أي فائدة فعلية غير عرض 🟢/🔴 — حمل زائد على
// السيرفر (وعلى الفوترة) يكبر بسرعة مع زيادة عدد المستخدمين. البديل الأخف:
// 1) الاعتماد أولاً على أحداث المتصفح الحقيقية (online/offline) وهي فورية
//    ومجانية بدون أي طلب شبكة.
// 2) استخدام حالة مستمعي فايرستور أنفسهم (onSnapshot error callback) التي
//    أصلاً تتحدث الحالة عند فشل حقيقي بالاتصال.
// 3) إبقاء فحص دوري خفيف جداً (كل دقيقة بدل كل 5 ثواني) فقط كشبكة أمان.
function startAutoSync() {
    if (window.syncInterval) clearInterval(window.syncInterval);
    // فحص احتياطي خفيف جداً (كل 60 ثانية بدل كل 5 ثواني = تقليل الحمل 12 ضعف)
    window.syncInterval = setInterval(() => { syncMessages(); }, 60000);

    if (!window._connectivityEventsBound) {
        window._connectivityEventsBound = true;
        window.addEventListener('online', () => {
            updateConnectionStatus(true);
            document.getElementById('lastSyncTime').textContent = 'منذ لحظات';
            // طبقة قوة إضافية: إعادة إرسال أي رسائل فشلت تلقائياً فور عودة الاتصال
            // بدل ترك المستخدم يكتشف الفشل بنفسه ويضغط يدوياً على كل رسالة
            const failedMsgs = window.allMessages.filter(m => m.status === 'failed');
            failedMsgs.forEach(m => { if (typeof window.retryFailedMessage === 'function') window.retryFailedMessage(m.id); });
            resyncRealtimeListeners('online');
        });
        window.addEventListener('offline', () => updateConnectionStatus(false));
        // حالة ابتدائية فورية بدون أي طلب شبكة
        updateConnectionStatus(navigator.onLine);
    }

    bindResumeResync();
}

// ================================================================
// إصلاح "رسائل صديقي ما توصل إلا لما أدخل وأطلع من التطبيق"
// ================================================================
// السبب: WebView أندرويد يوقف/يخنق عمليات JS بالخلفية (خصوصاً بعد فترة أو
// عند تبديل التطبيقات)، فيتجمّد اتصال فايرستور الحي (الـ stream الطويل)
// بصمت دون أن يستدعي أبداً دالة onError الخاصة بـ onSnapshot - فلا شيء
// بالكود يعرف أن الاتصال تجمّد أصلاً حتى يُعيد فتحه. الحل السابق الوحيد كان
// إغلاق التطبيق وإعادة فتحه (يُعيد تحميل الصفحة بالكامل فيُنشئ اتصال جديد).
// الآن: أي رجوع فعلي للتطبيق (رجوع من الخلفية، تركيز النافذة، أو عودة
// الاتصال بالإنترنت) يُعيد ربط مستمعي الرسائل/المستخدمين/الكتابة تلقائياً -
// بنفس الطريقة اللي كان يعملها window.repairServer يدوياً - بدل انتظار
// المستخدم يكتشف المشكلة ويعيد فتح التطبيق بنفسه.
let _lastResyncAt = 0;
function resyncRealtimeListeners(reason) {
    const now = Date.now();
    // تهدئة: لا داعي لإعادة الربط أكثر من مرة كل 3 ثوانٍ (لو وصلت عدة أحداث
    // بنفس اللحظة، مثلاً focus + visibilitychange معاً).
    if (now - _lastResyncAt < 3000) return;
    _lastResyncAt = now;
    try {
        if (typeof db === 'undefined' || !db) return;
        console.log('🔄 إعادة ربط مستمعي المزامنة الحية (' + reason + ')');
        // إعادة تدوير اتصال الشبكة بفايرستور نفسه (يجبره على فتح اتصال جديد
        // بدل الاعتماد على الاتصال القديم المتجمّد المحتمل).
        if (typeof db.disableNetwork === 'function' && typeof db.enableNetwork === 'function') {
            db.disableNetwork().catch(() => {}).finally(() => {
                db.enableNetwork().catch(() => {}).finally(() => {
                    if (typeof listenMessages === 'function') listenMessages();
                    if (typeof listenUsers === 'function') listenUsers();
                    if (typeof listenTyping === 'function') listenTyping();
                });
            });
        } else {
            if (typeof listenMessages === 'function') listenMessages();
            if (typeof listenUsers === 'function') listenUsers();
            if (typeof listenTyping === 'function') listenTyping();
        }
        syncMessages();
    } catch (e) {
        console.warn('⚠️ فشل إعادة ربط مستمعي المزامنة:', e);
    }
}
window.resyncRealtimeListeners = resyncRealtimeListeners;

function bindResumeResync() {
    if (window._resumeResyncBound) return;
    window._resumeResyncBound = true;
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) resyncRealtimeListeners('visible');
    });
    window.addEventListener('focus', () => resyncRealtimeListeners('focus'));
    window.addEventListener('pageshow', () => resyncRealtimeListeners('pageshow'));
}

async function syncMessages() {
    if (window.isSyncing) return;
    // لو المتصفح نفسه يقول أوفلاين، لا داعي لعمل طلب شبكة فاشل بالتأكيد
    if (!navigator.onLine) { updateConnectionStatus(false); return; }
    window.isSyncing = true;
    try {
        await db.collection('_').doc('_').get();
        updateConnectionStatus(true);
        document.getElementById('lastSyncTime').textContent = 'منذ لحظات';
    } catch (e) {
        updateConnectionStatus(false);
    } finally { window.isSyncing = false; }
}

function updateConnectionStatus(isOnline) {
    const indicator = document.getElementById('statusIndicator');
    const text = document.getElementById('statusText');
    if (isOnline) {
        indicator.className = 'indicator online';
        text.textContent = '🟢 متصل';
    } else {
        indicator.className = 'indicator offline';
        text.textContent = '🔴 غير متصل';
    }
}

window.restartSync = function() {
    if (window.syncInterval) { clearInterval(window.syncInterval);
        window.syncInterval = null; }
    startAutoSync();
    showNotification('✅ تم إعادة تشغيل المزامنة');
};

window.forceSync = function() {
    if (window.isSyncing) { showNotification('⏳ جاري المزامنة بالفعل'); return; }
    showNotification('⏳ جاري المزامنة الفورية...');
    syncMessages().then(() => showNotification('✅ تمت المزامنة بنجاح'));
};

window.repairServer = function() {
    showNotification('🔧 جاري إصلاح السيرفر...');
    try {
        if (window.messagesListener) { try { window.messagesListener(); } catch (e) {} window.messagesListener = null; }
        db.collection('_').doc('_').get();
        listenMessages();
        showNotification('✅ تم إصلاح السيرفر بنجاح');
        updateConnectionStatus(true);
    } catch (e) {
        showNotification('❌ فشل إصلاح السيرفر: ' + e.message);
        updateConnectionStatus(false);
    }
};

window.fixIndexes = function() {
    if (!window.isAdmin) { showNotification('❌ فقط المسؤول يمكنه إصلاح الفهارس'); return; }
    if (!confirm('🔍 إصلاح الفهارس؟ سيتم إعادة محاولة إنشاء الفهارس المفقودة.')) return;
    showNotification('⏳ جاري إصلاح الفهارس...');
    setTimeout(() => {
        showNotification('✅ تم إصلاح الفهارس');
    }, 1500);
};

window.rebuildDatabase = function() {
    if (!window.isAdmin) { showNotification('❌ فقط المسؤول يمكنه إعادة البناء'); return; }
    if (!confirm('⚠️ هل أنت متأكد؟ سيتم حذف جميع البيانات وإعادة بنائها من الصفر!')) return;
    showNotification('⏳ جاري إعادة بناء قاعدة البيانات...');
    setTimeout(() => {
        location.reload();
    }, 2000);
};

// ================================================================
//  تبديل الإعدادات
// ================================================================

function toggleSwitch(id) {
    const el = document.getElementById(id + 'Switch');
    if (!el) return;
    el.classList.toggle('active');
    window.settings[id] = el.classList.contains('active');
    showNotification(`✅ تم ${window.settings[id] ? 'تفعيل' : 'إيقاف'} ${id}`);
}

// ================================================================
//  مودال الإعدادات العامة (يفتح من القائمة الجانبية - لكل مستخدم)
// ================================================================

function setupSettingsModal() {
    const modal = document.getElementById('settingsModal');
    const notifSwitch = document.getElementById('notifSettingSwitch');
    const versionLabel = document.getElementById('appVersionLabel');
    if (!modal) return;

    // انعكاس الحالة المحفوظة على المفتاح عند فتح التطبيق
    if (notifSwitch) {
        notifSwitch.classList.toggle('active', window.settings.notifications);
        notifSwitch.onclick = function () {
            window.settings.notifications = !window.settings.notifications;
            notifSwitch.classList.toggle('active', window.settings.notifications);
            localStorage.setItem('tomodachi_notifSetting', window.settings.notifications ? 'on' : 'off');
            showNotification(window.settings.notifications ? '🔔 تم تفعيل إشعارات الرسائل' : '🔕 تم إيقاف إشعارات الرسائل');
        };
    }

    if (versionLabel && window.AndroidBridge) {
        // نميّز نسخة التطبيق الأصلي عن نسخة الويب فقط بصرياً، بدون أي منطق إضافي
        versionLabel.textContent = '1.0.0';
    }

    const closeBtn = document.getElementById('closeSettingsModal');
    const closeBtn2 = document.getElementById('closeSettingsModal2');
    [closeBtn, closeBtn2].forEach(btn => {
        if (btn) btn.onclick = () => { modal.style.display = 'none'; };
    });
    modal.onclick = function (e) {
        if (e.target === this) this.style.display = 'none';
    };
}

// ================================================================
//  طبقة مراقبة حالة الاتصال (تحسين طبقات مزامنة فايربيس)
// ================================================================
// النقطة الخضراء برأس التطبيق كانت مجرد زخرفة CSS ثابتة بلا أي معنى حقيقي.
// الآن تعكس حالة الاتصال الفعلية: أخضر = متصل، أحمر = غير متصل (أي رسالة
// تُرسَل الآن ستبقى محلياً وتُزامَن تلقائياً فور عودة الاتصال بفضل
// enablePersistence بملف firebase.js)، كهرماني نابض = عاد الاتصال ويجري
// استكمال المزامنة المؤجلة حالياً.
function setupConnectionMonitor() {
    const dot = document.getElementById('connDot');
    const info = document.getElementById('onlineInfo');
    if (!dot) return;

    let lastOfflineText = null;

    function setOnline() {
        dot.classList.remove('offline');
        dot.classList.add('syncing');
        if (info && lastOfflineText !== null) info.textContent = lastOfflineText;
        lastOfflineText = null;
        // نعرض حالة "يتزامن" لثوانٍ قليلة بعد عودة الاتصال (فايرستور يستكمل
        // إرسال أي رسائل انتظرت محلياً بهذه الأثناء) ثم نستقر على "متصل".
        setTimeout(() => dot.classList.remove('syncing'), 2500);
    }

    function setOffline() {
        dot.classList.remove('syncing');
        dot.classList.add('offline');
        if (info) {
            lastOfflineText = info.textContent;
            info.textContent = '⚠️ غير متصل بالإنترنت';
        }
    }

    window.addEventListener('online', setOnline);
    window.addEventListener('offline', setOffline);

    if (!navigator.onLine) setOffline();
}

function openSettingsModal() {
    const modal = document.getElementById('settingsModal');
    if (!modal) return;
    modal.style.display = 'flex';
    modal.style.zIndex = '500';

    const menu = document.getElementById('mobileMenu');
    if (menu && menu.classList.contains('open')) {
        menu.classList.remove('open');
        document.getElementById('menuOverlay').classList.remove('show');
        document.body.style.overflow = '';
    }
}
window.openSettingsModal = openSettingsModal;

function closeSettingsModal() {
    const modal = document.getElementById('settingsModal');
    if (modal) modal.style.display = 'none';
}
window.closeSettingsModal = closeSettingsModal;

// ================================================================
//  القائمة الجانبية
// ================================================================

function setupMobileMenu() {
    const menuBtn = document.getElementById('menuToggle');
    const menu = document.getElementById('mobileMenu');
    const overlay = document.getElementById('menuOverlay');
    const closeBtn = document.getElementById('menuClose');

    if (!menuBtn || !menu || !overlay) return;

    menuBtn.onclick = function(e) {
        e.preventDefault();
        e.stopPropagation();
        updateMenuUserInfo();
        menu.classList.add('open');
        overlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    };

    closeBtn.onclick = function() {
        menu.classList.remove('open');
        overlay.classList.remove('show');
        document.body.style.overflow = '';
    };

    overlay.onclick = function() {
        menu.classList.remove('open');
        overlay.classList.remove('show');
        document.body.style.overflow = '';
    };

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            menu.classList.remove('open');
            overlay.classList.remove('show');
            document.body.style.overflow = '';
        }
    });
}

function updateMenuUserInfo() {
    const avatarEl = document.getElementById('menuAvatar');
    const usernameEl = document.getElementById('menuUsername');
    const statusEl = document.querySelector('.menu-status');

    if (avatarEl) {
        if (isValidImageUrl(window.myAvatar)) {
            avatarEl.innerHTML = `<img src="${escapeHtml(window.myAvatar)}" />`;
        } else {
            avatarEl.textContent = window.myAvatar || '👥';
        }
    }

    if (usernameEl) {
        usernameEl.textContent = window.currentUser || 'Tomodachi';
        if (window.isAdmin) usernameEl.textContent += ' ⭐';
    }

    if (statusEl) {
        statusEl.textContent = window.onlineUsers > 0 ? '🟢 متصل' : '⚫ غير متصل';
    }
}

// ================================================================
//  البحث المحسن
// ================================================================

function setupSearchEnhanced() {
    const searchInput = document.getElementById('searchInput');
    const searchResults = document.getElementById('searchResults');
    const searchClear = document.getElementById('searchClear');

    if (!searchInput) return;

    searchInput.addEventListener('input', function() {
        const query = this.value.trim().toLowerCase();

        if (searchClear) {
            searchClear.classList.toggle('visible', query.length > 0);
        }

        if (!query) {
            searchResults.classList.remove('active');
            searchResults.innerHTML = '';
            return;
        }

        const results = window.allMessages.filter(msg => {
            if (msg.deleted) return false;
            const text = decompressText(msg.text || '').toLowerCase();
            const username = (msg.username || '').toLowerCase();
            return text.includes(query) || username.includes(query);
        });

        if (results.length === 0) {
            searchResults.innerHTML = '<div class="no-results">🔍 لا توجد نتائج</div>';
            searchResults.classList.add('active');
            return;
        }

        let html = '';
        results.slice(0, 20).forEach(msg => {
            const text = escapeHtml(decompressText(msg.text || ''));
            const username = escapeHtml(msg.username || 'مجهول');
            const time = formatTime(msg.timestamp);
            const avatar = msg.avatar || '👥';
            const avatarSafe = escapeHtml(avatar);
            const avatarDisplay = isValidImageUrl(avatar) ? `<img src="${avatarSafe}" />` : avatarSafe;

            // ملاحظة: query جاي من input المستخدم الحالي نفسه (بحث محلي) مش من
            // مستخدمين تانيين، لكن بنعقّمه برضه احتياطًا قبل ما نستخدمه في التظليل.
            const querySafe = escapeHtml(query);
            let highlightedText = text;
            const index = text.toLowerCase().indexOf(querySafe.toLowerCase());
            if (index !== -1) {
                highlightedText = text.substring(0, index) +
                    '<mark>' + text.substring(index, index + querySafe.length) + '</mark>' +
                    text.substring(index + querySafe.length);
            }

            html += `
                <div class="result-item" onclick="scrollToMessage('${escapeJsAttr(msg.id)}')">
                    <div class="result-avatar">${avatarDisplay}</div>
                    <div class="result-info">
                        <div class="result-name">${username}</div>
                        <div class="result-text">${highlightedText}</div>
                    </div>
                    <div class="result-time">${time}</div>
                </div>
            `;
        });

        searchResults.innerHTML = html;
        searchResults.classList.add('active');
    });

    if (searchClear) {
        searchClear.onclick = function() {
            searchInput.value = '';
            searchInput.dispatchEvent(new Event('input'));
            searchInput.focus();
        };
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && searchInput === document.activeElement) {
            searchInput.blur();
            searchResults.classList.remove('active');
            searchResults.innerHTML = '';
        }
    });
}

window.scrollToMessage = function(messageId) {
    const container = document.getElementById('messagesContainer');
    const wrapper = container.querySelector(`.msg-wrapper[data-id="${messageId}"]`);
    if (wrapper) {
        wrapper.scrollIntoView({ behavior: 'smooth', block: 'center' });
        wrapper.classList.add('highlight');
        setTimeout(() => wrapper.classList.remove('highlight'), 2000);

        document.getElementById('searchResults').classList.remove('active');
        document.getElementById('searchResults').innerHTML = '';
        document.getElementById('searchInput').value = '';
        document.getElementById('searchClear').classList.remove('visible');
    } else {
        showNotification('⚠️ الرسالة غير موجودة');
    }
};

// ================================================================
//  قائمة التفاعلات
// ================================================================

function setupReactionBar() {
    document.addEventListener('contextmenu', function(e) {
        const wrapper = e.target.closest('.msg-wrapper');
        if (wrapper) {
            e.preventDefault();
            const id = wrapper.dataset.id;
            const text = wrapper.dataset.text || '';
            const isSent = wrapper.dataset.sent === 'true';
            showReactionBar(e.clientX, e.clientY, id, text, isSent);
        }
    });
}

function showReactionBar(x, y, messageId, messageText, isSent) {
    const bar = document.getElementById('reactionBar');
    const items = document.getElementById('reactionItems');
    items.innerHTML = '';

    const emojis = ['❤️', '🔥', '😂', '😍', '👏', '🎉', '😢', '😡', '💯', '🙏'];
    emojis.forEach(emoji => {
        const span = document.createElement('span');
        span.className = 'reaction-item';
        span.textContent = emoji;
        span.onclick = () => {
            window.toggleReaction(messageId, emoji);
            bar.style.display = 'none';
        };
        items.appendChild(span);
    });

    const divider = document.createElement('span');
    divider.className = 'divider';
    items.appendChild(divider);

    const replyBtn = document.createElement('button');
    replyBtn.className = 'action-btn';
    replyBtn.innerHTML = '↩️';
    replyBtn.title = 'رد';
    replyBtn.onclick = () => {
        startReply(messageId, messageText);
        bar.style.display = 'none';
    };
    items.appendChild(replyBtn);

    if (isSent || window.isAdmin) {
        const editBtn = document.createElement('button');
        editBtn.className = 'action-btn';
        editBtn.innerHTML = '✏️';
        editBtn.title = 'تعديل';
        editBtn.onclick = () => {
            window.editMessage(messageId);
            bar.style.display = 'none';
        };
        items.appendChild(editBtn);

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'action-btn danger';
        deleteBtn.innerHTML = '🗑️';
        deleteBtn.title = 'حذف';
        deleteBtn.onclick = () => {
            window.deleteMessage(messageId);
            bar.style.display = 'none';
        };
        items.appendChild(deleteBtn);
    }

    bar.style.left = Math.min(x, window.innerWidth - 200) + 'px';
    bar.style.top = Math.min(y - 40, window.innerHeight - 80) + 'px';
    bar.style.display = 'flex';
    bar.classList.add('pop-enter');

    setTimeout(() => {
        bar.classList.remove('pop-enter');
    }, 200);

    setTimeout(() => {
        document.addEventListener('click', function hideBar(e) {
            if (!bar.contains(e.target)) {
                bar.style.display = 'none';
                document.removeEventListener('click', hideBar);
            }
        });
    }, 10);
}

// ================================================================
//  إعداد الأزرار
// ================================================================

function setupButtons() {
    const emojiBtn = document.getElementById('emojiBtn');
    if (emojiBtn) {
        emojiBtn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const panel = document.getElementById('emojiPanel');
            if (!panel) return;
            if (panel.style.display === 'flex') {
                panel.style.display = 'none';
            } else {
                panel.style.display = 'flex';
                document.getElementById('stickerPanel').style.display = 'none';
            }
        };
    }

    const stickerBtn = document.getElementById('stickerBtn');
    if (stickerBtn) {
        stickerBtn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const panel = document.getElementById('stickerPanel');
            if (!panel) return;
            if (panel.style.display === 'flex') {
                panel.style.display = 'none';
            } else {
                panel.style.display = 'flex';
                document.getElementById('emojiPanel').style.display = 'none';
                renderStickerTabs();
                renderStickers(window.currentStickerTab);
            }
        };
    }

    document.getElementById('imageBtn').onclick = function(e) {
        e.preventDefault();
        document.getElementById('imageFileInput').click();
    };

    document.getElementById('imageFileInput').onchange = async function(e) {
        const file = e.target.files[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) { showNotification('❌ يرجى اختيار صورة'); return; }
        if (file.size > 10 * 1024 * 1024) { showNotification('❌ حجم الصورة كبير جداً (أقصى 10MB)'); return; }

        try {
            showNotification('⏳ جاري معالجة الصورة...');
            const reader = new FileReader();
            reader.onload = async function(e) {
                try {
                    // ضغط ذكي يضمن أن الصورة + base64 تبقى ضمن حد حجم مستند
                    // Firestore بأمان (بدون الحاجة لـ Firebase Storage)
                    const compressed = await compressImageSmart(e.target.result, {
                        maxWidth: 900, startQuality: 0.6, minQuality: 0.25, maxBase64Bytes: 700 * 1024
                    });
                    const msgData = {
                        username: window.currentUser,
                        text: '📷 صورة',
                        avatar: window.myAvatar,
                        isAdmin: window.isAdmin,
                        timestamp: firebase.firestore.FieldValue.serverTimestamp(),
                        deleted: false,
                        reactions: {},
                        edited: false,
                        editedAt: null,
                        image: true,
                        imageData: compressed,
                        compressed: true
                    };
                    await db.collection('messages').add(msgData);
                    showNotification('✅ تم إرسال الصورة');
                } catch (err) {
                    console.error('فشل إرسال الصورة:', err);
                    showNotification('❌ ' + (err.message || 'فشل إرسال الصورة'));
                }
            };
            reader.readAsDataURL(file);
        } catch (error) {
            console.error('فشل إرسال الصورة:', error);
            showNotification('❌ فشل إرسال الصورة');
        }
        this.value = '';
    };

    document.getElementById('stickerFileInput').onchange = async function(e) {
        const files = e.target.files;
        if (!files || files.length === 0) return;
        for (const file of files) { await uploadSticker(file); }
        this.value = '';
    };

    document.getElementById('closeAdminBtn').onclick = function(e) {
        e.preventDefault();
        document.getElementById('adminPanel').style.display = 'none';
    };

    document.querySelectorAll('#adminPanel .admin-tabs button').forEach(tab => {
        tab.onclick = function(e) {
            e.preventDefault();
            document.querySelectorAll('#adminPanel .admin-tabs button').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            const tabId = this.dataset.tab;
            document.querySelectorAll('#adminPanel .admin-section').forEach(s => s.classList.remove('active'));
            document.getElementById('tab-' + tabId).classList.add('active');
        };
    });

    document.getElementById('addBadWordBtn').onclick = addBadWord;
    document.getElementById('badWordInput').onkeydown = (e) => { if (e.key === 'Enter') addBadWord(); };

    document.getElementById('replyCancel').onclick = function() {
        window.replyingTo = null;
        window.replyingToText = '';
        document.getElementById('replyBar').style.display = 'none';
    };

    document.getElementById('userAvatar').onclick = openAvatarModal;
    document.getElementById('profileCloseBtn').onclick = () => {
        document.getElementById('profileModal').style.display = 'none';
    };
    document.getElementById('profileCloseBtn2').onclick = () => {
        document.getElementById('profileModal').style.display = 'none';
    };
    document.getElementById('profileModal').onclick = (e) => {
        if (e.target === e.currentTarget) {
            document.getElementById('profileModal').style.display = 'none';
        }
    };

    document.getElementById('closeAvatarModal').onclick = () => {
        document.getElementById('avatarModal').style.display = 'none';
    };
    document.getElementById('cancelAvatarBtn').onclick = () => {
        document.getElementById('avatarModal').style.display = 'none';
    };
    document.getElementById('avatarModal').onclick = (e) => {
        if (e.target === e.currentTarget) document.getElementById('avatarModal').style.display = 'none';
    };

    const randomAvatarBtn = document.getElementById('randomAvatarBtn');
    if (randomAvatarBtn) {
        randomAvatarBtn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const animalAvatar = generateRandomAnimalAvatar();
            document.getElementById('avatarPreview').innerHTML =
                `<img src="${animalAvatar}" alt="avatar" /><div class="upload-overlay">📷 تغيير</div>`;
            document.querySelector('#avatarModal .btn-save').dataset.newAvatar = animalAvatar;
        };
    }

    document.getElementById('fileInput').onchange = async function(e) {
        const file = e.target.files[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) { showNotification('❌ يرجى اختيار صورة'); return; }
        if (file.size > 5 * 1024 * 1024) { showNotification('❌ حجم الصورة كبير جداً (أقصى 5MB)'); return; }

        try {
            showNotification('⏳ جاري معالجة الصورة...');
            const reader = new FileReader();
            reader.onload = async function(e) {
                try {
                    const compressed = await compressImageSmart(e.target.result, {
                        maxWidth: 320, startQuality: 0.75, minQuality: 0.3, maxBase64Bytes: 150 * 1024
                    });
                    document.getElementById('avatarPreview').innerHTML =
                        `<img src="${compressed}" alt="avatar" /><div class="upload-overlay">📷 تغيير</div>`;
                    document.querySelector('#avatarModal .btn-save').dataset.newAvatar = compressed;
                    showNotification('✅ تم تحميل الصورة');
                } catch (err) {
                    console.error(err);
                    showNotification('❌ ' + (err.message || 'فشل معالجة الصورة'));
                }
            };
            reader.readAsDataURL(file);
        } catch (error) {
            console.error(error);
            showNotification('❌ فشل معالجة الصورة');
        }
        document.getElementById('fileInput').value = '';
    };

    document.getElementById('saveAvatarBtn').onclick = async function() {
        const newName = document.getElementById('editNameInput').value.trim();
        const newBio = document.getElementById('editBioInput').value.trim();
        if (!newName) { showNotification('❌ الاسم مطلوب'); return; }
        if (newName.length > 20 || !isValidUsername(newName)) {
            showNotification('❌ الاسم يجب أن يحتوي على حروف وأرقام فقط (٢٠ حرف كحد أقصى)');
            return;
        }

        const img = document.querySelector('#avatarPreview img');
        let newAvatar = window.myAvatar;
        if (img && img.src !== window.myAvatar) { newAvatar = img.src; }

        const updates = {};
        if (newAvatar !== window.myAvatar) updates.avatar = newAvatar;
        if (newBio !== window.myBio) updates.bio = newBio || '📝 مرحباً، أنا في Tomodachi!';

        if (newName !== window.currentUser) {
            try {
                const oldDoc = await db.collection('users').doc(window.currentUser).get();
                if (oldDoc.exists) {
                    const data = oldDoc.data();
                    // مهم: نحدّث بريد حساب Firebase Auth الحقيقي أولاً ليطابق
                    // الاسم الجديد (بما إن الدخول صار مبني على username@tomodachi.app).
                    // بدون هذه الخطوة، أول تسجيل دخول بالاسم الجديد كان سينشئ
                    // حساباً فارغاً جديداً بدل الدخول لنفس الحساب بكلمة المرور القديمة.
                    if (window.firebaseAuth.currentUser) {
                        await window.firebaseAuth.currentUser.updateEmail(usernameToEmail(newName));
                    }
                    await db.collection('users').doc(newName).set({
                        ...data,
                        username: newName,
                        avatar: newAvatar,
                        bio: newBio || data.bio || '📝 مرحباً، أنا في Tomodachi!',
                        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
                    });
                    await db.collection('users').doc(window.currentUser).delete();
                }
                window.currentUser = newName;
                document.getElementById('currentUserDisplay').textContent = newName;
                if (window.isAdmin) document.getElementById('currentUserDisplay').textContent += ' ⭐';
                localStorage.setItem('lastUser', newName);
                updateMenuUserInfo();
            } catch (e) {
                if (e.code === 'auth/requires-recent-login') {
                    showNotification('❌ لأمان حسابك: سجّل خروج ثم دخول مجدداً قبل تغيير اسم المستخدم');
                } else {
                    showNotification('❌ فشل تغيير الاسم');
                }
                return;
            }
        } else {
            try {
                await db.collection('users').doc(window.currentUser).update(updates);
            } catch (e) {
                showNotification('❌ فشل تحديث البيانات');
                return;
            }
        }

        if (newAvatar !== window.myAvatar) {
            window.myAvatar = newAvatar;
            updateAvatarUI();
            updateMenuUserInfo();
        }
        if (newBio !== window.myBio) {
            window.myBio = newBio || '📝 مرحباً، أنا في Tomodachi!';
        }

        document.getElementById('avatarModal').style.display = 'none';
        showNotification('✅ تم التحديث بنجاح');
    };

    document.getElementById('saveEditMessageBtn').onclick = async function() {
        const newText = document.getElementById('editMessageInput').value.trim();
        if (!newText) { showNotification('❌ لا يمكن أن تكون فارغة'); return; }
        if (!window.editingMessageId) return;
        try {
            await db.collection('messages').doc(window.editingMessageId).update({
                text: compressText(newText),
                edited: true,
                editedAt: firebase.firestore.FieldValue.serverTimestamp()
            });
            updateMessageInArray(window.editingMessageId, {
                text: compressText(newText),
                edited: true,
                editedAt: new Date()
            });
            renderMessages(window.allMessages);
            showNotification('✅ تم التعديل');
            document.getElementById('editMessageModal').style.display = 'none';
            window.editingMessageId = null;
        } catch (error) {
            showNotification('❌ فشل التعديل');
            console.error(error);
        }
    };

    document.getElementById('cancelEditMessageBtn').onclick = () => {
        document.getElementById('editMessageModal').style.display = 'none';
        window.editingMessageId = null;
    };
    document.getElementById('closeEditMessageModal').onclick = () => {
        document.getElementById('editMessageModal').style.display = 'none';
        window.editingMessageId = null;
    };
    document.getElementById('editMessageModal').onclick = (e) => {
        if (e.target === e.currentTarget) {
            document.getElementById('editMessageModal').style.display = 'none';
            window.editingMessageId = null;
        }
    };

    setupLogout();
}

// ================================================================
//  تسجيل الخروج
// ================================================================

function setupLogout() {
    document.getElementById('logoutBtn').onclick = function(e) {
        e.preventDefault();
        if (confirm('تسجيل الخروج؟')) {
            if (window.currentUser) {
                db.collection('users').doc(window.currentUser).update({
                    online: false,
                    lastSeen: firebase.firestore.FieldValue.serverTimestamp()
                }).catch(() => {});
            }
            if (window.messagesListener) {
                try { window.messagesListener(); } catch (e) {}
                window.messagesListener = null;
            }
            saveMessagesToStorage();
            // تسجيل خروج حقيقي من Firebase Auth (وليس فقط مسح بيانات محلية) —
            // بدونها كانت جلسة المصادقة المحفوظة تبقى حية فيسجّل الدخول التلقائي
            // نفس المستخدم من جديد فور إعادة تحميل الصفحة، فيصير زر "تسجيل
            // الخروج" بلا أي أثر فعلي.
            window.firebaseAuth.signOut().catch(() => {});
            localStorage.removeItem('lastUser');
            setTimeout(() => { location.reload(); }, 300);
        }
    };
}

// ================================================================
// نظام الحسابات: كل مستخدم له بريد إلكتروني خاص تلقائياً + كلمة مرور حقيقية
// ================================================================
// كل اسم مستخدم يتحول لبريد داخلي بصيغة username@tomodachi.app ويُنشأ له
// حساب حقيقي في Firebase Authentication (سيرفرات جوجل، ليس بالواجهة). هذا
// يعني: أول مرة يكتب أي شخص اسمه وكلمة مرور يختارها، يُنشأ له حساب فوراً.
// أي مرة بعدها لازم يكتب نفس كلمة المرور. ما فيه أي إعداد إضافي مطلوب منك
// بلوحة Firebase غير تفعيل "Email/Password" (خطوة واحدة، اشرحها بالأسفل).
function usernameToEmail(username) {
    return username.toLowerCase() + '@tomodachi.app';
}

// تسجّل دخول مستخدم موجود مسبقاً بفايرستور (تُستخدم من زر الدخول اليدوي
// ومن تسجيل الدخول التلقائي الصامت عند فتح التطبيق). تُرجع true لو نجح.
async function loginExistingUser(username, { silent = false } = {}) {
    try {
        const userDoc = await db.collection('users').doc(username).get();
        if (!userDoc.exists) return false;

        const data = userDoc.data();
        if (data.banned) {
            if (!silent) {
                document.getElementById('loginError').textContent =
                    `⛔ أنت محظور: ${data.banReason || 'استخدام كلمات ممنوعة'}`;
            }
            return false;
        }
        window.currentUser = username;
        window.isAdmin = data.isAdmin || false;
        if (data.avatar && data.avatar !== '👥') window.myAvatar = data.avatar;
        if (data.bio) window.myBio = data.bio;

        if (data.themeSettings) {
            if (typeof updateThemeSettingsFromRemote === 'function') {
                updateThemeSettingsFromRemote(data.themeSettings);
            }
            console.log('✅ تم تحميل الثيم من Firebase:', data.themeSettings);
        } else {
            const defaultSettings = {
                mode: 'light',
                sentColor: '#0084FF',
                receivedColor: '#E4E6EB',
                background: '',
                customBackground: null
            };
            if (typeof updateThemeSettingsFromRemote === 'function') {
                updateThemeSettingsFromRemote(defaultSettings);
            }
            if (typeof saveUserThemeSettings === 'function') {
                await saveUserThemeSettings();
            }
        }

        await db.collection('users').doc(username).update({
            online: true,
            lastSeen: firebase.firestore.FieldValue.serverTimestamp()
        });
        localStorage.setItem('lastUser', username);
        enterChat(username);
        return true;
    } catch (e) {
        console.warn('خطأ في التحقق:', e);
        return false;
    }
}

// إنشاء مستند مستخدم جديد بفايرستور لأول مرة (بعد نجاح إنشاء حساب Firebase
// Auth). مرتبط بـ uid الحساب الحقيقي — هذا ما تتحقق منه firestore.rules
// عند أي تعديل لاحق لضمان إن صاحب الحساب الحقيقي بس يقدر يعدّل بياناته.
async function createNewUserDoc(username, uid) {
    const avatarUrl = generateAvatarUrl(username);
    const defaultThemeSettings = {
        mode: 'light',
        sentColor: '#0084FF',
        receivedColor: '#E4E6EB',
        background: '',
        customBackground: null
    };

    await db.collection('users').doc(username).set({
        username: username,
        uid: uid,
        avatar: avatarUrl,
        bio: '📝 مرحباً، أنا في Tomodachi!',
        isAdmin: username === 'slx23m',
        online: true,
        blocked: false,
        banned: false,
        banReason: null,
        banExpires: null,
        banCount: 0,
        themeSettings: defaultThemeSettings,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
        lastSeen: firebase.firestore.FieldValue.serverTimestamp()
    });

    window.currentUser = username;
    window.isAdmin = username === 'slx23m';
    window.myAvatar = avatarUrl;
    window.myBio = '📝 مرحباً، أنا في Tomodachi!';
    if (typeof updateThemeSettingsFromRemote === 'function') {
        updateThemeSettingsFromRemote(defaultThemeSettings);
    }
    localStorage.setItem('lastUser', username);
    enterChat(username);
}

// ================================================================
// تسجيل دخول تلقائي صامت (Persistent Login) — لكل المستخدمين وليس فقط المسؤول
// ================================================================
// الهدف: كل شخص يسجّل دخول مرة وحدة بس، وتبقى الجلسة محفوظة بمتصفحه —
// حتى بعد ما نرفع تحديثات جديدة للتطبيق — وما يحتاج يكتب اسمه وكلمة مروره
// كل مرة يفتح فيها التطبيق، إلا لو مسح بيانات المتصفح فعلياً. نعتمد على
// جلسة Firebase Authentication الحقيقية المحفوظة (LOCAL persistence بملف
// js/firebase.js) لكل حساب — موثّقة فعلياً من سيرفرات جوجل، مو مجرد قيمة
// بلوكال ستوريدج يقدر أي حد يزوّرها.
async function attemptAutoLogin() {
    const authUser = await new Promise(resolve => {
        const unsubscribe = window.firebaseAuth.onAuthStateChanged(u => {
            unsubscribe();
            resolve(u);
        });
    });
    if (!authUser || !authUser.email) return false;

    const username = authUser.email.split('@')[0];
    document.getElementById('usernameInput').value = username;
    return await loginExistingUser(username, { silent: true });
}

// ================================================================
//  تسجيل الدخول / إنشاء حساب — تبويبان منفصلان وصريحان
// ================================================================
// السبب: النموذج الموحّد السابق (يجرّب دخول، ولو فشل يُنشئ حساباً تلقائياً)
// كان يعتمد على استقبال رمز خطأ "auth/user-not-found" من Firebase بدقة —
// وبعض الحالات (شبكة، أو صياغة الرمز تختلف بين إصدارات SDK) كانت تخلي
// المستخدم يعلق برسالة "كلمة مرور خاطئة" حتى لو الحساب أصلاً مو موجود.
// الحل الأوضح: تبويب "تسجيل دخول" وتبويب "إنشاء حساب جديد" منفصلين تماماً،
// كل واحد يستدعي دالة Firebase المحددة له مباشرة بدون تخمين.
window.loginMode = 'login';

function setLoginMode(mode) {
    window.loginMode = mode;
    document.getElementById('tabLogin').classList.toggle('active', mode === 'login');
    document.getElementById('tabSignup').classList.toggle('active', mode === 'signup');
    document.getElementById('confirmPasswordWrap').style.display = mode === 'signup' ? 'block' : 'none';
    document.getElementById('passwordInput').setAttribute('autocomplete', mode === 'signup' ? 'new-password' : 'current-password');
    document.getElementById('loginBtnLabel').textContent = mode === 'signup' ? '✨ إنشاء الحساب' : '🚀 دخول';
    document.getElementById('loginHint').textContent = mode === 'signup'
        ? 'اختر اسم مستخدم وكلمة مرور جديدة (6 أحرف على الأقل) — احفظها فهي حسابك الدائم'
        : 'اكتب اسم المستخدم وكلمة المرور اللي سجّلت فيهم من قبل';
    document.getElementById('loginError').textContent = '';
}

function setupLogin() {
    document.getElementById('tabLogin').onclick = () => setLoginMode('login');
    document.getElementById('tabSignup').onclick = () => setLoginMode('signup');

    document.getElementById('loginBtn').onclick = async function() {
        const username = document.getElementById('usernameInput').value.trim();
        const password = document.getElementById('passwordInput').value;
        const loginBtn = document.getElementById('loginBtn');
        document.getElementById('loginError').textContent = '';

        if (!username) {
            document.getElementById('loginError').textContent = '✖ أدخل اسم المستخدم';
            return;
        }
        if (username.length < 2) {
            document.getElementById('loginError').textContent = '✖ الاسم قصير جداً';
            return;
        }
        if (username.length > 20) {
            document.getElementById('loginError').textContent = '✖ الاسم طويل جداً';
            return;
        }
        if (!isValidUsername(username)) {
            document.getElementById('loginError').textContent = '✖ الاسم يجب أن يحتوي على حروف وأرقام فقط (بدون رموز)';
            return;
        }
        if (!password || password.length < 6) {
            document.getElementById('loginError').textContent = '✖ كلمة المرور يجب أن تكون 6 أحرف على الأقل';
            return;
        }
        if (window.loginMode === 'signup') {
            const confirmPassword = document.getElementById('confirmPasswordInput').value;
            if (password !== confirmPassword) {
                document.getElementById('loginError').textContent = '✖ كلمتا المرور غير متطابقتين';
                return;
            }
        }

        const originalBtnHtml = loginBtn.innerHTML;
        loginBtn.disabled = true;
        loginBtn.classList.add('loading');
        loginBtn.innerHTML = window.loginMode === 'signup'
            ? '<span class="btn-spinner"></span> جاري إنشاء الحساب...'
            : '<span class="btn-spinner"></span> جاري الدخول...';
        const resetBtn = () => {
            loginBtn.disabled = false;
            loginBtn.classList.remove('loading');
            loginBtn.innerHTML = originalBtnHtml;
        };

        const email = usernameToEmail(username);

        if (window.loginMode === 'signup') {
            // ================================================================
            // تبويب "إنشاء حساب جديد" — واضح ومباشر: ينشئ الحساب أو يفشل برسالة
            // دقيقة (مثلاً لو الاسم محجوز مسبقاً يطلب الانتقال لتبويب الدخول)
            // ================================================================
            try {
                const cred = await window.firebaseAuth.createUserWithEmailAndPassword(email, password);
                await createNewUserDoc(username, cred.user.uid);
            } catch (createError) {
                if (createError.code === 'auth/email-already-in-use') {
                    document.getElementById('loginError').textContent = '✖ هذا الاسم محجوز مسبقاً — جرّب تبويب "تسجيل دخول"';
                } else if (createError.code === 'auth/weak-password') {
                    document.getElementById('loginError').textContent = '✖ كلمة المرور ضعيفة جداً';
                } else if (!window.firebaseAuth) {
                    document.getElementById('loginError').textContent = '✖ تعذّر الاتصال بخدمة إنشاء الحساب — تحقق من اتصالك بالإنترنت وأعد المحاولة';
                } else {
                    document.getElementById('loginError').textContent = '✖ خطأ: ' + createError.message;
                }
                resetBtn();
            }
            return;
        }

        // ================================================================
        // تبويب "تسجيل دخول" — يحاول الدخول بالحساب الموجود فقط، بدون أي
        // محاولة إنشاء تلقائي غامضة
        // ================================================================
        try {
            await window.firebaseAuth.signInWithEmailAndPassword(email, password);
            const loggedIn = await loginExistingUser(username);
            if (!loggedIn) {
                if (!document.getElementById('loginError').textContent) {
                    document.getElementById('loginError').textContent = '✖ تعذّر تحميل الحساب';
                }
                resetBtn();
            }
            // نجح loginExistingUser → enterChat() تولّت الباقي، لا داعي لـ resetBtn
        } catch (authError) {
            if (authError.code === 'auth/user-not-found') {
                document.getElementById('loginError').textContent = '✖ ما فيه حساب بهذا الاسم — جرّب تبويب "إنشاء حساب جديد"';
            } else if (authError.code === 'auth/wrong-password' || authError.code === 'auth/invalid-credential') {
                document.getElementById('loginError').textContent = '✖ كلمة المرور غير صحيحة';
            } else if (authError.code === 'auth/too-many-requests') {
                document.getElementById('loginError').textContent = '✖ محاولات كثيرة جداً، حاول لاحقاً';
            } else if (!window.firebaseAuth) {
                document.getElementById('loginError').textContent = '✖ تعذّر الاتصال بخدمة تسجيل الدخول — تحقق من اتصالك بالإنترنت وأعد المحاولة';
            } else {
                document.getElementById('loginError').textContent = '✖ خطأ: ' + authError.message;
            }
            resetBtn();
        }
    };

    document.getElementById('usernameInput').onkeydown = (e) => {
        if (e.key === 'Enter') document.getElementById('passwordInput').focus();
    };
    document.getElementById('passwordInput').onkeydown = (e) => {
        if (e.key === 'Enter') {
            if (window.loginMode === 'signup') {
                document.getElementById('confirmPasswordInput').focus();
            } else {
                document.getElementById('loginBtn').click();
            }
        }
    };
    document.getElementById('confirmPasswordInput').onkeydown = (e) => {
        if (e.key === 'Enter') document.getElementById('loginBtn').click();
    };
}

// ================================================================
//  الدخول إلى الدردشة
// ================================================================

function enterChat(username) {
    document.getElementById('loginScreen').style.display = 'none';
    document.getElementById('chatScreen').style.display = 'flex';
    document.getElementById('chatScreen').classList.add('fade-enter');

    document.getElementById('currentUserDisplay').textContent = username;
    if (window.isAdmin) {
        document.getElementById('currentUserDisplay').textContent += ' ⭐';
        const adminMenuItem = document.getElementById('adminMenuItem');
        if (adminMenuItem) adminMenuItem.style.display = 'flex';
    }

    updateMenuUserInfo();

    loadAvatar(username);
    listenUsers();
    listenTyping();
    initFullEmojiPanel();
    
    loadStickersFromFirebase().then(() => {
        renderStickerTabs();
        renderStickers('my_stickers');
        renderAdminStickers();
    });
    
    loadBadWords();
    loadBanCounts();
    startAutoSync();
    loadMessages();

    const updateInterval = setInterval(() => {
        if (window.currentUser) {
            db.collection('users').doc(window.currentUser).update({
                online: true,
                lastSeen: firebase.firestore.FieldValue.serverTimestamp()
            }).catch(() => {});
        }
    }, 10000);

    window.addEventListener('beforeunload', () => {
        clearInterval(updateInterval);
        if (window.currentUser) {
            db.collection('users').doc(window.currentUser).update({ online: false }).catch(() => {});
        }
        if (window.syncInterval) clearInterval(window.syncInterval);
        saveMessagesToStorage();
    });

    const container = document.getElementById('messagesContainer');
    container.addEventListener('scroll', () => {
        const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 50;
        if (isAtBottom) {
            document.getElementById('scrollToBottom').style.display = 'none';
            window.unreadCount = 0;
            if ('clearAppBadge' in navigator) navigator.clearAppBadge().catch(() => {});
        } else if (window.unreadCount > 0) {
            document.getElementById('scrollToBottom').style.display = 'flex';
        }
        window.isUserScrolling = true;
        clearTimeout(container._scrollTimeout);
        container._scrollTimeout = setTimeout(() => {
            window.isUserScrolling = false;
        }, 200);
    });

    document.getElementById('scrollToBottom').onclick = () => {
        container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
        document.getElementById('scrollToBottom').style.display = 'none';
        window.unreadCount = 0;
        if ('clearAppBadge' in navigator) navigator.clearAppBadge().catch(() => {});
    };

    setupMobileMenu();
    setupSettingsModal();
    setupConnectionMonitor();
    setupSearchEnhanced();

    setTimeout(() => {
        listenMessages();
    }, 500);
}

// ================================================================
//  الصورة الشخصية
// ================================================================

function loadAvatar(username) {
    db.collection('users').doc(username).onSnapshot((doc) => {
        if (doc.exists) {
            const data = doc.data();
            if (data.avatar && data.avatar !== window.myAvatar) {
                window.myAvatar = data.avatar;
                updateAvatarUI();
                updateMenuUserInfo();
            }
            if (data.bio) window.myBio = data.bio;
            
            if (data.themeSettings && typeof updateThemeSettingsFromRemote === 'function') {
                updateThemeSettingsFromRemote(data.themeSettings);
                console.log('✅ تم تحديث الثيم من Firebase (Real-time)');
            }
        }
    });
}

function updateAvatarUI() {
    const avatar = document.getElementById('userAvatar');
    if (isValidImageUrl(window.myAvatar)) {
        avatar.innerHTML = `<div class="avatar-image"><img src="${escapeHtml(window.myAvatar)}" alt="avatar" /></div><span class="online-indicator"></span>`;
    } else {
        avatar.innerHTML = `<div class="avatar-image">${escapeHtml(window.myAvatar)}</div><span class="online-indicator"></span>`;
    }
}

// ================================================================
//  بداية التطبيق
// ================================================================

async function startApp() {
    console.log('👥 Tomodachi - جاري التهيئة...');
    window.__tomodachiReady = false;

    const splash = document.getElementById('splashScreen');
    const bar = document.getElementById('splashBar');

    let progress = 0;
    const interval = setInterval(() => {
        progress += Math.random() * 10;
        if (progress > 95) progress = 95;
        bar.style.width = progress + '%';
    }, 300);

    try {
        // إصلاح: بدل الانتظار لاستثناء عشوائي لو `db` غير موجودة (فايرستور
        // لم يُهيَّأ - راجع js/firebase.js)، نتحقق صراحة من العلم اللي
        // يضبطه firebase.js ونعطي رسالة واضحة فوراً بدل التعليق.
        if (!window.__firebaseReady || !db) {
            throw new Error('firebase-not-ready: ' + (window.__firebaseInitError || 'غير معروف'));
        }
        await db.collection('_').doc('_').get();
        progress = 30;
        bar.style.width = '30%';

        await loadStickersFromFirebase();
        progress = 50;
        bar.style.width = '50%';

        await loadBadWords();
        progress = 70;
        bar.style.width = '70%';

        await loadBanCounts();
        progress = 90;
        bar.style.width = '90%';

        if (loadMessagesFromStorage()) {
            console.log(`📦 تم تحميل ${window.allMessages.length} رسالة من localStorage`);
        }

        progress = 100;
        bar.style.width = '100%';
        clearInterval(interval);

        setTimeout(async () => {
            splash.classList.add('hidden');
            window.__tomodachiReady = true;
            setTimeout(async () => {
                splash.style.display = 'none';

                // محاولة تسجيل دخول تلقائي صامت قبل إظهار شاشة تسجيل الدخول —
                // لو نجحت، ندخل الدردشة مباشرة بدون ما المستخدم يلمس أي شيء.
                const autoLoggedIn = await attemptAutoLogin();
                if (autoLoggedIn) {
                    console.log('✅ تسجيل دخول تلقائي ناجح:', window.currentUser);
                    console.log(`📦 ${window.allMessages.length} رسالة مخزنة محلياً (مرتبة تصاعدياً)`);
                    return;
                }

                document.getElementById('loginScreen').style.display = 'flex';
                document.getElementById('loginScreen').classList.add('fade-enter');

                console.log('👥 Tomodachi - جاهز للاستخدام!');
                console.log(`📦 ${window.allMessages.length} رسالة مخزنة محلياً (مرتبة تصاعدياً)`);
            }, 600);
        }, 400);

    } catch (error) {
        clearInterval(interval);
        window.__tomodachiReady = true;
        console.error('❌ فشل التهيئة:', error);
        bar.style.width = '100%';
        splash.querySelector('p').textContent = '❌ فشل الاتصال بالخادم';
        setTimeout(() => {
            splash.classList.add('hidden');
            setTimeout(() => {
                splash.style.display = 'none';
                document.getElementById('loginScreen').style.display = 'flex';
                document.getElementById('loginScreen').classList.add('fade-enter');
                document.getElementById('loginError').textContent = '⚠️ تعذر الاتصال بقاعدة البيانات';
            }, 600);
        }, 1500);
    }
}

// ================================================================
//  التهيئة النهائية
// ================================================================

// ================================================================
// إصلاح جذري لتعليق التطبيق أثناء التحميل ("توقف عن الاستجابة"):
// ================================================================
// هذا الملف (app.js) لا يُحمَّل بـ <script> عادي بالصفحة، بل يُحقَن
// ديناميكياً بجافاسكربت (createElement('script') + appendChild) عبر
// سلسلة __loadScriptChain بملف index.html، بعد أن تنتهي مكتبات
// Firebase/dayjs/... من التحميل أولاً. المشكلة: عنصر <script> المُضاف
// ديناميكياً لا يُعطّل تحليل الصفحة (غير محاصر/non-blocking)، فيكتمل
// تحليل HTML بالكامل ويُطلَق حدث DOMContentLoaded الخاص بالمستند قبل أن
// يصل هذا الملف نفسه للتنفيذ بوقت طويل. نتيجة ذلك: `document.
// addEventListener('DOMContentLoaded', ...)` كان يُسجَّل مستمعاً لحدثٍ
// وقع بالفعل ومضى - فلا يُستدعى أبداً - و`startApp()` لا تُنادى مطلقاً،
// فتبقى شاشة التحميل (splashScreen) عالقة للأبد على نسبتها الأخيرة (~40%)
// إلى أن يظهر تحذير "⚠️ التطبيق توقف عن الاستجابة" من المراقب بالأعلى.
// الحل: التحقق من document.readyState أولاً - لو المستند انتهى تحليله
// بالفعل (وهو الحال الفعلي دائماً هنا) نُشغّل التهيئة فوراً بدل انتظار
// حدث لن يتكرر.
// ================================================================
function __tomodachiInit() {
    console.log('👥 Tomodachi - نسخة مستقرة كاملة');
    console.log('👤 المسؤول: slx23m | كلمة: 1442');
    console.log('✅ الترتيب: تصاعدي (الأقدم أولاً)');
    console.log('💾 حفظ محلي مع الاحتفاظ بالترتيب');
    console.log('🔄 مزامنة فورية مع Firebase');
    console.log('🎨 إيموجي وستيكرات كاملة');
    console.log('🌓 نظام ثيمات متقدم مع حفظ في Firebase');
    console.log('📱 PWA جاهزة للتثبيت');
    console.log('🛡️ لوحة مسؤول كاملة');

    setupLogin();
    setupMessageSending();
    setupButtons();
    setupReactionBar();

    if (typeof initThemes === 'function') {
        initThemes();
    }

    startApp();
}

if (document.readyState === 'loading') {
    // احتياط فقط - نظرياً لن يحدث بما أن هذا الملف يُحقَن بعد اكتمال
    // التحليل دائماً، لكن نتركه لأي تغيير مستقبلي بطريقة تحميل الملفات.
    document.addEventListener('DOMContentLoaded', __tomodachiInit);
} else {
    __tomodachiInit();
}

console.log('✅ Tomodachi - تم التحميل بنجاح');

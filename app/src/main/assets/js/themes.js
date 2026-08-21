// ============================================================
//  Tomodachi - نظام الثيمات المتقدم (المصحح والمحسّن)
//  حل مشكلة تجمد الواجهة (UI Freeze)
// ============================================================

// ============================================================
//  1. إعدادات الثيمات
// ============================================================

const THEME_PRESETS = {
    sentColors: [
        '#0084FF', '#1877F2', '#42B72A', '#F39C12', '#8B5CF6',
        '#E74C3C', '#1ABC9C', '#FF6B6B', '#4ECDC4', '#45B7D1',
        '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F',
        '#BB8FCE', '#85C1E9', '#82E0AA', '#F1948A', '#F8C471'
    ],
    receivedColors: [
        // رمادية/محايدة (الأصلية)
        '#E4E6EB', '#D4D6DA', '#C4C6CA', '#F0F2F5', '#E8EAED',
        '#F5F5F5', '#E8E8E8', '#F0E6D3', '#E6D5B8', '#D5C4A1',
        // أزرق: من الفاتح جداً إلى المتوسط
        '#DCEFFF', '#BEE3FF', '#9AD0FF', '#6FB8FF', '#3D9BFF',
        // أخضر: من الفاتح جداً إلى المتوسط
        '#D6EFE0', '#B8E8C8', '#93DBAE', '#66CC91', '#3DBB78',
        // أحمر/وردي: من الفاتح جداً إلى المتوسط
        '#FCEAEA', '#F9CFCF', '#F5AEAE', '#EF8888', '#E85D5D',
        // ألوان إضافية متنوعة (بنفسجي، أصفر، فيروزي)
        '#F3DCEB', '#E0DBFA', '#FFF3D6', '#DFF7F1', '#D9F0F5',
        // غامقة (لوضع الثيم الداكن)
        '#2F3542', '#3A3F52', '#26313F', '#1F2A38', '#2C2C3A'
    ],
    backgrounds: [
        { name: 'افتراضي', url: '' },
        { name: 'غروب', url: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400' },
        { name: 'غابة', url: 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=400' },
        { name: 'بحر', url: 'https://images.unsplash.com/photo-1505228395891-9a51e7e86bf6?w=400' },
        { name: 'نجوم', url: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400' },
        { name: 'ورد', url: 'https://images.unsplash.com/photo-1508615070457-7baeba4009e5?w=400' },
        { name: 'ثلج', url: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400' },
        { name: 'قهوة', url: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400' }
    ]
};

// ============================================================
//  2. إدارة الثيمات
// ============================================================

let userThemeSettings = {
    mode: 'light',
    sentColor: '#0084FF',
    receivedColor: '#E4E6EB',
    background: '',
    customBackground: null
};

// متغيرات التحكم لمنع الحلقات اللانهائية
let _isApplyingTheme = false;
let _themeUpdateQueue = [];
let _rafId = null;

/**
 * تحميل إعدادات الثيم من Firebase
 */
async function loadUserThemeSettings() {
    if (!window.currentUser) {
        console.warn('⚠️ لا يوجد مستخدم حالياً');
        requestAnimationFrame(() => applyThemeSettings());
        return;
    }
    
    try {
        const doc = await db.collection('users').doc(window.currentUser).get();
        if (doc.exists) {
            const data = doc.data();
            if (data.themeSettings) {
                userThemeSettings = {
                    mode: data.themeSettings.mode || 'light',
                    sentColor: data.themeSettings.sentColor || '#0084FF',
                    receivedColor: data.themeSettings.receivedColor || '#E4E6EB',
                    background: data.themeSettings.background || '',
                    customBackground: data.themeSettings.customBackground || null
                };
                console.log('✅ تم تحميل الثيم من Firebase:', userThemeSettings);
            } else {
                console.log('ℹ️ لا توجد إعدادات ثيم، إنشاء إعدادات افتراضية');
                await saveUserThemeSettings();
            }
        }
    } catch (e) {
        console.warn('⚠️ فشل تحميل الثيم من Firebase:', e);
    }
    
    requestAnimationFrame(() => applyThemeSettings());
}

/**
 * حفظ إعدادات الثيم في Firebase
 */
async function saveUserThemeSettings() {
    if (!window.currentUser) {
        console.warn('⚠️ لا يوجد مستخدم لحفظ الإعدادات');
        return false;
    }
    
    try {
        const dataToSave = {
            themeSettings: {
                mode: userThemeSettings.mode || 'light',
                sentColor: userThemeSettings.sentColor || '#0084FF',
                receivedColor: userThemeSettings.receivedColor || '#E4E6EB',
                background: userThemeSettings.background || '',
                customBackground: userThemeSettings.customBackground || null
            },
            themeUpdatedAt: firebase.firestore.FieldValue.serverTimestamp()
        };
        
        await db.collection('users').doc(window.currentUser).set(dataToSave, { merge: true });
        console.log('✅ تم حفظ الثيم في Firebase:', dataToSave.themeSettings);
        return true;
    } catch (e) {
        console.error('❌ فشل حفظ الثيم في Firebase:', e);
        return false;
    }
}

// ============================================================
//  3. تطبيق الثيمات
// ============================================================

/**
 * تطبيق جميع إعدادات الثيم على الواجهة
 */
function applyThemeSettings() {
    if (_isApplyingTheme) {
        console.warn('⚠️ جاري تطبيق الثيم بالفعل، إلغاء الطلب');
        return;
    }
    
    _isApplyingTheme = true;
    
    try {
        console.log('🎨 تطبيق إعدادات الثيم:', userThemeSettings);
        
        requestAnimationFrame(() => {
            applyMode(userThemeSettings.mode);
        });
        
        requestAnimationFrame(() => {
            applyBubbleColors();
        });
        
        requestAnimationFrame(() => {
            applyBackground();
        });
        
        requestAnimationFrame(() => {
            updateThemeUI();
            _isApplyingTheme = false;
        });
        
    } catch (e) {
        console.error('❌ خطأ في تطبيق الثيم:', e);
        _isApplyingTheme = false;
    }
}
window.applyThemeSettings = applyThemeSettings;

/**
 * تطبيق الوضع
 */
let _modeApplyTimeout = null;

function applyMode(mode) {
    if (_modeApplyTimeout) {
        cancelAnimationFrame(_modeApplyTimeout);
        _modeApplyTimeout = null;
    }
    
    _modeApplyTimeout = requestAnimationFrame(() => {
        console.log('🌓 تطبيق الوضع:', mode);
        
        document.body.classList.remove('dark-mode', 'light-mode');
        
        if (mode === 'auto') {
            const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
            document.body.classList.add(isDark ? 'dark-mode' : 'light-mode');
            
            if (!window._systemThemeListener) {
                window._systemThemeListener = true;
                const darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
                darkModeMediaQuery.addEventListener('change', (e) => {
                    if (userThemeSettings.mode === 'auto') {
                        requestAnimationFrame(() => {
                            if (e.matches) {
                                document.body.classList.remove('light-mode');
                                document.body.classList.add('dark-mode');
                            } else {
                                document.body.classList.remove('dark-mode');
                                document.body.classList.add('light-mode');
                            }
                            setTimeout(() => applyBubbleColors(), 50);
                        });
                    }
                });
            }
        } else if (mode === 'dark') {
            document.body.classList.remove('light-mode');
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
            document.body.classList.add('light-mode');
        }
        
        updateModeUI();
        syncNativeStatusBar();
        _modeApplyTimeout = null;
    });
}

/**
 * يزامن لون أيقونات شريط الحالة/التنقل بأندرويد مع الثيم الحالي فعلياً
 * (بدل شريط ملوّن ثابت لا علاقة له بثيم الواجهة) - يعمل فقط داخل التطبيق
 * الأصلي حيث يوجد window.AndroidBridge (راجع MainActivity.NativeBridge)،
 * ولا يفعل شيئاً لو شُغّل الموقع كـ PWA عادية بمتصفح.
 */
function syncNativeStatusBar() {
    if (!window.AndroidBridge || typeof window.AndroidBridge.setStatusBarStyle !== 'function') return;
    try {
        const isLight = document.body.classList.contains('light-mode');
        window.AndroidBridge.setStatusBarStyle(isLight);
    } catch (e) { /* لا يوجد شيء نفعله لو فشل الجسر */ }
}
window.syncNativeStatusBar = syncNativeStatusBar;

/**
 * تحديث واجهة اختيار الوضع
 */
function updateModeUI() {
    requestAnimationFrame(() => {
        document.querySelectorAll('.mode-options .mode-option').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === userThemeSettings.mode);
        });
    });
}

/**
 * تطبيق ألوان الفقاعات
 */
let _colorApplyTimeout = null;

function applyBubbleColors() {
    if (_colorApplyTimeout) {
        cancelAnimationFrame(_colorApplyTimeout);
        _colorApplyTimeout = null;
    }
    
    _colorApplyTimeout = requestAnimationFrame(() => {
        const isDarkMode = document.body.classList.contains('dark-mode');
        
        let sentColor = userThemeSettings.sentColor || '#0084FF';
        let receivedColor = userThemeSettings.receivedColor || '#E4E6EB';
        
        if (isDarkMode) {
            if (receivedColor === '#E4E6EB' || receivedColor === '#F0F2F5' || receivedColor === '#FFFFFF') {
                receivedColor = '#3A3B3C';
            }
        }
        
        const root = document.documentElement;
        root.style.setProperty('--custom-bubble-sent', sentColor);
        root.style.setProperty('--bg-bubble-sent', sentColor);
        root.style.setProperty('--custom-bubble-received', receivedColor);
        root.style.setProperty('--bg-bubble-received', receivedColor);
        
        console.log('🎨 تطبيق ألوان الفقاعات - مرسلة:', sentColor, 'مستلمة:', receivedColor);
        
        updateColorUI();
        _colorApplyTimeout = null;
    });
}

/**
 * تطبيق خلفية الدردشة
 */
let _bgApplyTimeout = null;

function applyBackground() {
    if (_bgApplyTimeout) {
        cancelAnimationFrame(_bgApplyTimeout);
        _bgApplyTimeout = null;
    }
    
    _bgApplyTimeout = requestAnimationFrame(() => {
        const container = document.getElementById('messagesContainer');
        if (!container) {
            _bgApplyTimeout = null;
            return;
        }
        
        const bg = userThemeSettings.customBackground || userThemeSettings.background || '';
        
        if (bg) {
            container.style.backgroundImage = `url(${bg})`;
            container.classList.add('custom-bg');
            console.log('🖼️ تطبيق خلفية مخصصة');
        } else {
            container.style.backgroundImage = '';
            container.classList.remove('custom-bg');
            console.log('🖼️ إزالة الخلفية المخصصة');
        }
        _bgApplyTimeout = null;
    });
}

// ============================================================
//  4. واجهة المودال
// ============================================================

/**
 * فتح مودال إعدادات الثيم
 */
function openThemeSettings() {
    const modal = document.getElementById('themeModal');
    if (!modal) {
        console.warn('⚠️ مودال الثيمات غير موجود');
        return;
    }
    
    console.log('🎨 فتح مودال الثيمات');
    
    requestAnimationFrame(() => {
        updateThemeUI();
        modal.style.display = 'flex';
        modal.style.zIndex = '500';
    });
    
    const menu = document.getElementById('mobileMenu');
    if (menu && menu.classList.contains('open')) {
        menu.classList.remove('open');
        document.getElementById('menuOverlay').classList.remove('show');
        document.body.style.overflow = '';
    }
}
window.openThemeSettings = openThemeSettings;

/**
 * تحديث واجهة المودال
 */
function updateThemeUI() {
    requestAnimationFrame(() => {
        updateModeUI();
        updateColorUI();
        updateBackgroundUI();
    });
}

/**
 * تحديث واجهة اختيار الألوان
 */
function updateColorUI() {
    requestAnimationFrame(() => {
        const sentContainer = document.getElementById('sentColorOptions');
        if (sentContainer) {
            sentContainer.innerHTML = '';
            THEME_PRESETS.sentColors.forEach(color => {
                const item = document.createElement('div');
                item.className = 'color-item' + (color === userThemeSettings.sentColor ? ' active' : '');
                item.style.background = color;
                item.innerHTML = `<span class="check">✓</span>`;
                item.onclick = function(e) {
                    e.stopPropagation();
                    console.log('🎨 اختيار لون مرسل:', color);
                    userThemeSettings.sentColor = color;
                    applyBubbleColors();
                    updateColorUI();
                };
                sentContainer.appendChild(item);
            });
        }
        
        const receivedContainer = document.getElementById('receivedColorOptions');
        if (receivedContainer) {
            receivedContainer.innerHTML = '';
            THEME_PRESETS.receivedColors.forEach(color => {
                const item = document.createElement('div');
                item.className = 'color-item' + (color === userThemeSettings.receivedColor ? ' active' : '');
                item.style.background = color;
                item.innerHTML = `<span class="check">✓</span>`;
                item.onclick = function(e) {
                    e.stopPropagation();
                    console.log('🎨 اختيار لون مستقبل:', color);
                    userThemeSettings.receivedColor = color;
                    applyBubbleColors();
                    updateColorUI();
                };
                receivedContainer.appendChild(item);
            });
        }

        updateLivePreviewColors();
    });
}

/**
 * يحدّث فقاعتي المعاينة الحيّة أعلى لوحة الألوان بآخر لونين مختارين -
 * بدون هذا التحديث كان على المستخدم إغلاق اللوحة وفتح الدردشة الفعلية
 * ليرى نتيجة اختياره، وهو احتكاك غير ضروري لقرار بصري بسيط كهذا.
 */
function updateLivePreviewColors() {
    const sentBubble = document.getElementById('previewBubbleSent');
    const receivedBubble = document.getElementById('previewBubbleReceived');
    const isDarkMode = document.body.classList.contains('dark-mode');
    let receivedColor = userThemeSettings.receivedColor || '#E4E6EB';
    if (isDarkMode && (receivedColor === '#E4E6EB' || receivedColor === '#F0F2F5' || receivedColor === '#FFFFFF')) {
        receivedColor = '#3A3B3C';
    }
    if (sentBubble) sentBubble.style.background = userThemeSettings.sentColor || '#0084FF';
    if (receivedBubble) {
        receivedBubble.style.background = receivedColor;
        // نص داكن تلقائياً فوق الألوان الفاتحة ونص فاتح فوق الغامقة، حتى لا
        // يختفي نص المعاينة عند اختيار لون مستلَم غامق (كان النص أبيض ثابت
        // بغض النظر عن لون الفقاعة المختارة، فيصبح غير مقروء أحياناً)
        receivedBubble.style.color = isColorDark(receivedColor) ? '#fff' : '#050505';
    }
}

function isColorDark(hex) {
    if (!hex || hex[0] !== '#') return false;
    const c = hex.substring(1);
    const rgb = c.length === 3
        ? c.split('').map(ch => parseInt(ch + ch, 16))
        : [c.substring(0, 2), c.substring(2, 4), c.substring(4, 6)].map(h => parseInt(h, 16));
    const brightness = (rgb[0] * 299 + rgb[1] * 587 + rgb[2] * 114) / 1000;
    return brightness < 140;
}

/**
 * تحديث واجهة اختيار الخلفيات
 */
function updateBackgroundUI() {
    requestAnimationFrame(() => {
        const container = document.getElementById('backgroundOptions');
        if (!container) return;
        
        container.innerHTML = '';
        const currentBg = userThemeSettings.customBackground || userThemeSettings.background || '';
        
        THEME_PRESETS.backgrounds.forEach(bg => {
            const item = document.createElement('div');
            item.className = 'bg-item' + (bg.url === currentBg ? ' active' : '');
            
            if (bg.url) {
                item.style.backgroundImage = `url(${bg.url})`;
                item.style.backgroundSize = 'cover';
                item.style.backgroundPosition = 'center';
            } else {
                item.style.background = 'var(--bg-chat)';
                item.style.border = '2px dashed var(--border-color)';
            }
            
            const check = document.createElement('span');
            check.className = 'bg-check';
            check.textContent = '✓';
            item.appendChild(check);
            
            const label = document.createElement('span');
            label.className = 'bg-label';
            label.textContent = bg.name;
            item.appendChild(label);
            
            item.onclick = function(e) {
                e.stopPropagation();
                console.log('🖼️ اختيار خلفية:', bg.name);
                userThemeSettings.background = bg.url;
                userThemeSettings.customBackground = null;
                applyBackground();
                updateBackgroundUI();
            };
            container.appendChild(item);
        });

        const preview = document.getElementById('themeLivePreview');
        if (preview) {
            const bgUrl = userThemeSettings.customBackground || userThemeSettings.background || '';
            preview.style.backgroundImage = bgUrl ? `url(${bgUrl})` : '';
        }
    });
}

/**
 * إعداد رفع الخلفية المخصصة
 */
function setupBackgroundUpload() {
    const input = document.getElementById('bgFileInput');
    if (!input) return;
    
    const newInput = input.cloneNode(true);
    input.parentNode.replaceChild(newInput, input);
    
    newInput.onchange = async function(e) {
        const file = e.target.files[0];
        if (!file) return;
        const uploadBtn = document.getElementById('uploadBgBtn');
        
        if (!file.type.startsWith('image/')) {
            showNotification('❌ يرجى اختيار صورة');
            this.value = '';
            return;
        }
        
        if (file.size > 5 * 1024 * 1024) {
            showNotification('❌ حجم الصورة كبير جداً (أقصى 5MB)');
            this.value = '';
            return;
        }
        
        try {
            if (uploadBtn) uploadBtn.classList.add('is-uploading');
            showNotification('⏳ جاري معالجة الخلفية...');
            const reader = new FileReader();
            reader.onload = function(e) {
                const dataUrl = e.target.result;
                console.log('🖼️ رفع خلفية مخصصة');
                userThemeSettings.customBackground = dataUrl;
                userThemeSettings.background = '';
                applyBackground();
                updateBackgroundUI();
                showNotification('✅ تم تحديث الخلفية');
                if (uploadBtn) uploadBtn.classList.remove('is-uploading');
            };
            reader.readAsDataURL(file);
        } catch (error) {
            console.error('❌ فشل رفع الخلفية:', error);
            showNotification('❌ فشل رفع الخلفية');
            if (uploadBtn) uploadBtn.classList.remove('is-uploading');
        }
        this.value = '';
    };
}

// ============================================================
//  5. دوال الثيم الرئيسية
// ============================================================

/**
 * تبديل الثيم
 */
function toggleTheme() {
    const currentMode = userThemeSettings.mode;
    let newMode;
    if (currentMode === 'light') newMode = 'dark';
    else if (currentMode === 'dark') newMode = 'auto';
    else newMode = 'light';
    setMode(newMode);
}
window.toggleTheme = toggleTheme;

/**
 * تعيين الوضع المحدد
 */
async function setMode(mode) {
    console.log('🌓 تغيير الوضع إلى:', mode);
    userThemeSettings.mode = mode;
    
    await new Promise(resolve => {
        requestAnimationFrame(() => {
            applyMode(mode);
            applyBubbleColors();
            updateThemeUI();
            resolve();
        });
    });
    
    const success = await saveUserThemeSettings();
    if (success) {
        const modeName = mode === 'light' ? 'نهاري' : mode === 'dark' ? 'ليلي' : 'تلقائي';
        showNotification(`🌓 تم تغيير الوضع إلى ${modeName}`);
    } else {
        showNotification('⚠️ فشل حفظ الوضع في Firebase');
    }
}
window.setMode = setMode;

/**
 * إعادة تعيين جميع إعدادات الثيم
 */
async function resetTheme() {
    if (!confirm('هل تريد إعادة تعيين جميع إعدادات الثيم؟')) return;
    
    console.log('🔄 إعادة تعيين الثيم');
    userThemeSettings = {
        mode: 'light',
        sentColor: '#0084FF',
        receivedColor: '#E4E6EB',
        background: '',
        customBackground: null
    };
    
    await new Promise(resolve => {
        requestAnimationFrame(() => {
            applyThemeSettings();
            updateThemeUI();
            resolve();
        });
    });
    
    const success = await saveUserThemeSettings();
    if (success) {
        showNotification('✅ تم إعادة تعيين الثيم');
    } else {
        showNotification('⚠️ فشل إعادة تعيين الثيم في Firebase');
    }
}
window.resetTheme = resetTheme;

// ============================================================
//  6. تهيئة نظام الثيمات
// ============================================================

/**
 * تهيئة نظام الثيمات بالكامل
 */
function initThemes() {
    console.log('🎨 تهيئة نظام الثيمات...');
    setupBackgroundUpload();
    setupThemeButtons();
    setupThemeModal();
    
    if (window.currentUser) {
        loadUserThemeSettings();
    } else {
        applyThemeSettings();
    }
    
    console.log('✅ نظام الثيمات المتقدم تم تهيئته');
}

/**
 * إعداد أزرار الثيم
 */
function setupThemeButtons() {
    const themeToggleLogin = document.getElementById('themeToggleLogin');
    if (themeToggleLogin) {
        const newBtn = themeToggleLogin.cloneNode(true);
        themeToggleLogin.parentNode.replaceChild(newBtn, themeToggleLogin);
        newBtn.onclick = function(e) {
            e.preventDefault();
            toggleTheme();
        };
        console.log('✅ تم تهيئة زر الثيم في شاشة الدخول');
    }
}

/**
 * إعداد مودال الثيمات
 */
function setupThemeModal() {
    // زر الإغلاق (X)
    const closeBtn = document.getElementById('closeThemeModal');
    if (closeBtn) {
        const newCloseBtn = closeBtn.cloneNode(true);
        closeBtn.parentNode.replaceChild(newCloseBtn, closeBtn);
        newCloseBtn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('❌ إغلاق مودال الثيمات');
            document.getElementById('themeModal').style.display = 'none';
        };
        console.log('✅ تم تهيئة زر إغلاق المودال');
    }
    
    // الإغلاق عند النقر خارج المودال
    const modal = document.getElementById('themeModal');
    if (modal) {
        modal.onclick = function(e) {
            if (e.target === this) {
                console.log('❌ إغلاق مودال الثيمات (خارجي)');
                this.style.display = 'none';
            }
        };
    }
    
    // زر حفظ الإعدادات
    const saveBtn = document.getElementById('saveThemeBtn');
    if (saveBtn) {
        const newSaveBtn = saveBtn.cloneNode(true);
        saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
        newSaveBtn.onclick = async function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('💾 بدء حفظ إعدادات الثيم...');
            console.log('📦 الإعدادات الحالية:', userThemeSettings);
            
            this.classList.add('is-loading');
            this.disabled = true;
            
            try {
                await new Promise(resolve => {
                    requestAnimationFrame(() => {
                        applyThemeSettings();
                        resolve();
                    });
                });
                
                const success = await saveUserThemeSettings();
                
                if (success) {
                    showNotification('✅ تم حفظ إعدادات الثيم في Firebase');
                    console.log('✅ تم حفظ الإعدادات بنجاح');
                } else {
                    showNotification('❌ فشل حفظ الإعدادات في Firebase');
                    console.error('❌ فشل حفظ الإعدادات');
                }
            } catch (error) {
                console.error('❌ خطأ أثناء الحفظ:', error);
                showNotification('❌ حدث خطأ أثناء الحفظ');
            } finally {
                this.classList.remove('is-loading');
                this.disabled = false;
                document.getElementById('themeModal').style.display = 'none';
                console.log('✅ تم إغلاق مودال الثيمات بعد الحفظ');
            }
        };
        console.log('✅ تم تهيئة زر حفظ الثيم');
    }
    
    // زر إعادة تعيين
    const resetBtn = document.getElementById('resetThemeBtn');
    if (resetBtn) {
        const newResetBtn = resetBtn.cloneNode(true);
        resetBtn.parentNode.replaceChild(newResetBtn, resetBtn);
        newResetBtn.onclick = async function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            const originalText = this.textContent;
            this.textContent = '⏳ جاري...';
            this.disabled = true;
            
            await resetTheme();
            
            this.textContent = originalText;
            this.disabled = false;
            document.getElementById('themeModal').style.display = 'none';
        };
        console.log('✅ تم تهيئة زر إعادة تعيين الثيم');
    }
    
    // أزرار الوضع
    document.querySelectorAll('.mode-options .mode-option').forEach(btn => {
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);
        newBtn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const mode = this.dataset.mode;
            if (mode) {
                setMode(mode);
            }
        };
    });
    console.log('✅ تم تهيئة أزرار الوضع');

    // شريط القفز السريع بين أقسام لوحة الألوان (الوضع/الألوان/الخلفية) -
    // يمرّر بسلاسة داخل جسم المودال نفسه بدل الصفحة كلها، حتى لا يُفلت
    // التمرير خارج حدود الورقة السفلية على الجوال.
    document.querySelectorAll('.theme-jump-nav button').forEach(btn => {
        btn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const target = document.getElementById(this.dataset.jump);
            if (target) target.scrollIntoView({ block: 'start', behavior: 'smooth' });
        };
    });
    console.log('✅ تم تهيئة شريط القفز السريع للوحة الألوان');
}

// ============================================================
//  7. دوال مساعدة للقائمة الجانبية
// ============================================================

window.openBackgroundSettings = function() {
    openThemeSettings();
    setTimeout(() => {
        const bgSection = document.querySelector('.theme-section:last-child');
        if (bgSection) {
            bgSection.scrollIntoView({ behavior: 'smooth' });
        }
    }, 300);
};

window.openColorPicker = function() {
    openThemeSettings();
    setTimeout(() => {
        const colorSection = document.querySelector('.theme-section:nth-child(2)');
        if (colorSection) {
            colorSection.scrollIntoView({ behavior: 'smooth' });
        }
    }, 300);
};

/**
 * تحديث إعدادات الثيم من بيانات قادمة من Firebase (تسجيل الدخول / مزامنة لحظية)
 * ملاحظة مهمة: لازم نستخدم هذه الدالة بدل الكتابة المباشرة فوق window.userThemeSettings،
 * لأن كل دوال الثيم (applyThemeSettings, saveUserThemeSettings, ...) بتشتغل على
 * المتغير الداخلي userThemeSettings مش على window.userThemeSettings، فأي تعديل مباشر
 * على window.userThemeSettings من ملفات تانية (زي app.js) كان بيتجاهل تمامًا.
 */
function updateThemeSettingsFromRemote(remoteSettings) {
    if (!remoteSettings) return;
    userThemeSettings = {
        mode: remoteSettings.mode || 'light',
        sentColor: remoteSettings.sentColor || '#0084FF',
        receivedColor: remoteSettings.receivedColor || '#E4E6EB',
        background: remoteSettings.background || '',
        customBackground: remoteSettings.customBackground || null
    };
    window.userThemeSettings = userThemeSettings;
    requestAnimationFrame(() => applyThemeSettings());
}
window.updateThemeSettingsFromRemote = updateThemeSettingsFromRemote;

// ============================================================
//  8. تصدير المتغيرات والدوال
// ============================================================

window.userThemeSettings = userThemeSettings;
window.initThemes = initThemes;
window.saveUserThemeSettings = saveUserThemeSettings;
window.loadUserThemeSettings = loadUserThemeSettings;
window.applyThemeSettings = applyThemeSettings;
window.openThemeSettings = openThemeSettings;
window.toggleTheme = toggleTheme;
window.setMode = setMode;
window.resetTheme = resetTheme;

console.log('✅ themes.js تم التحميل بنجاح (المصحح والمحسّن)');

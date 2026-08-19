// ============================================================
// حزمة ستيكرات "أنمي" مجانية افتراضية
// ============================================================
// ملاحظة مهمة: هذه ليست شخصيات أنمي حقيقية أو محمية بحقوق نشر - هي رسومات
// "تشيبي" (Chibi) بسيطة أصلية مرسومة بالكامل بـ SVG (دوائر/أشكال هندسية)
// بنفس أسلوب generateAvatarUrl الموجود أصلاً بملف utils.js، حتى تبقى حرة
// الاستخدام تماماً بدون أي مشكلة ملكية فكرية، وخفيفة جداً بالحجم (SVG نصي
// وليس صور حقيقية) وتُحمَّل فوراً بدون إنترنت.
(function () {
    function chibiFaceSvg({ bg, hair, eyes, mouth, blush = true, extra = '' }) {
        const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160">
  <circle cx="80" cy="86" r="62" fill="${bg}"/>
  <path d="M18 78 Q 20 10 80 10 Q 140 10 142 78 Q 110 55 80 60 Q 50 55 18 78 Z" fill="${hair}"/>
  ${eyes}
  ${blush ? '<ellipse cx="45" cy="100" rx="10" ry="6" fill="#ff9baa" opacity="0.7"/><ellipse cx="115" cy="100" rx="10" ry="6" fill="#ff9baa" opacity="0.7"/>' : ''}
  ${mouth}
  ${extra}
</svg>`.trim();
        return 'data:image/svg+xml,' + encodeURIComponent(svg);
    }

    const roundEyes = '<circle cx="58" cy="88" r="11" fill="#2b2b2b"/><circle cx="102" cy="88" r="11" fill="#2b2b2b"/><circle cx="61" cy="84" r="3.5" fill="#fff"/><circle cx="105" cy="84" r="3.5" fill="#fff"/>';
    const happyEyes = '<path d="M48 88 Q58 78 68 88" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/><path d="M92 88 Q102 78 112 88" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/>';
    const heartEyes = '<text x="58" y="98" font-size="26" text-anchor="middle">💗</text><text x="102" y="98" font-size="26" text-anchor="middle">💗</text>';
    const winkEyes = '<circle cx="58" cy="88" r="11" fill="#2b2b2b"/><circle cx="61" cy="84" r="3.5" fill="#fff"/><path d="M92 88 Q102 82 112 88" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/>';
    const cryEyes = '<circle cx="58" cy="90" r="10" fill="#2b2b2b"/><circle cx="102" cy="90" r="10" fill="#2b2b2b"/><path d="M52 100 Q50 118 44 128" stroke="#7fc1ff" stroke-width="6" fill="none" stroke-linecap="round"/><path d="M108 100 Q110 118 116 128" stroke="#7fc1ff" stroke-width="6" fill="none" stroke-linecap="round"/>';
    const surpriseEyes = '<circle cx="58" cy="88" r="9" fill="#2b2b2b"/><circle cx="102" cy="88" r="9" fill="#2b2b2b"/>';
    const angryEyes = '<path d="M48 82 L68 90" stroke="#2b2b2b" stroke-width="5" stroke-linecap="round"/><path d="M112 82 L92 90" stroke="#2b2b2b" stroke-width="5" stroke-linecap="round"/><path d="M50 96 Q58 90 66 96" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/><path d="M94 96 Q102 90 110 96" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/>';
    const sleepyEyes = '<path d="M48 90 Q58 94 68 90" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/><path d="M92 90 Q102 94 112 90" stroke="#2b2b2b" stroke-width="5" fill="none" stroke-linecap="round"/>';
    const starEyes = '<text x="58" y="98" font-size="24" text-anchor="middle">⭐</text><text x="102" y="98" font-size="24" text-anchor="middle">⭐</text>';

    const smile = '<path d="M62 112 Q80 128 98 112" stroke="#5a3d2b" stroke-width="5" fill="none" stroke-linecap="round"/>';
    const bigLaugh = '<path d="M58 110 Q80 136 102 110 Q80 122 58 110 Z" fill="#7a4a35"/>';
    const small_o = '<ellipse cx="80" cy="116" rx="8" ry="10" fill="#5a3d2b"/>';
    const flat = '<path d="M64 114 L96 114" stroke="#5a3d2b" stroke-width="5" stroke-linecap="round"/>';
    const smirk = '<path d="M62 112 Q84 122 98 108" stroke="#5a3d2b" stroke-width="5" fill="none" stroke-linecap="round"/>';

    const ANIME_STICKERS = [
        { id: 'anime_happy', name: 'سعيد', data: chibiFaceSvg({ bg: '#ffe3ea', hair: '#7a4fe0', eyes: happyEyes, mouth: bigLaugh }) },
        { id: 'anime_love', name: 'حب', data: chibiFaceSvg({ bg: '#fff0e6', hair: '#ff7aa2', eyes: heartEyes, mouth: smile }) },
        { id: 'anime_wink', name: 'غمزة', data: chibiFaceSvg({ bg: '#e6f7ff', hair: '#2f9e6e', eyes: winkEyes, mouth: smirk }) },
        { id: 'anime_cry', name: 'بكاء', data: chibiFaceSvg({ bg: '#eef1ff', hair: '#4a5cc7', eyes: cryEyes, mouth: small_o, blush: false }) },
        { id: 'anime_surprised', name: 'مندهش', data: chibiFaceSvg({ bg: '#fff9e0', hair: '#e08a2f', eyes: surpriseEyes, mouth: small_o }) },
        { id: 'anime_angry', name: 'غاضب', data: chibiFaceSvg({ bg: '#ffe6e6', hair: '#c72f2f', eyes: angryEyes, mouth: flat, blush: false }) },
        { id: 'anime_sleepy', name: 'نعسان', data: chibiFaceSvg({ bg: '#f0eaff', hair: '#8a6bcf', eyes: sleepyEyes, mouth: small_o, extra: '<text x="120" y="55" font-size="22">💤</text>' }) },
        { id: 'anime_star', name: 'إعجاب', data: chibiFaceSvg({ bg: '#e9fff0', hair: '#2fae6b', eyes: starEyes, mouth: bigLaugh }) },
        { id: 'anime_shy', name: 'خجول', data: chibiFaceSvg({ bg: '#ffeef5', hair: '#e6528c', eyes: roundEyes, mouth: small_o }) },
        { id: 'anime_cool', name: 'رايق', data: chibiFaceSvg({ bg: '#eaf5ff', hair: '#333333', eyes: '<rect x="46" y="80" width="26" height="14" rx="4" fill="#2b2b2b"/><rect x="88" y="80" width="26" height="14" rx="4" fill="#2b2b2b"/><rect x="72" y="85" width="16" height="4" fill="#2b2b2b"/>', mouth: smirk, blush: false }) }
    ].map(s => ({
        id: s.id,
        type: 'image',
        data: s.data,
        name: s.name,
        pack: '🎌 أنمي',
        uploadedBy: 'Semo'
    }));

    window.ANIME_STICKER_PACK = ANIME_STICKERS;

    // دمج الحزمة تلقائياً ضمن قائمة الستيكرات المتاحة (بدون تكرار لو
    // تمت إضافتها مسبقاً)، سواء كانت القائمة الحالية جاية من فايرستور أو
    // من التخزين المحلي أو من القائمة الافتراضية القديمة (3 إيموجي فقط).
    window.mergeAnimeStickerPack = function () {
        if (!window.allStickers) window.allStickers = [];
        const existingIds = new Set(window.allStickers.map(s => s.id));
        const missing = ANIME_STICKERS.filter(s => !existingIds.has(s.id));
        if (missing.length > 0) {
            window.allStickers = [...window.allStickers, ...missing];
        }
        return window.allStickers;
    };
})();

// ============================================================
// حزمتا ستيكرات إضافيتان مجانيتان: "ردود أفعال" و "حيوانات كيوت"
// ============================================================
// ملاحظة مهمة (بنفس منطق anime-stickers.js): هذه ليست صوراً منسوخة من أي
// موقع أو تطبيق خارجي (تيليجرام/واتساب/لاين وغيرها) - فتلك الحزم محمية
// بحقوق نشر ولا يجوز تضمينها مباشرة داخل تطبيق آخر. بدلاً من ذلك، هذه
// شارات (badges) دائرية أصلية 100% مرسومة بالكامل بـ SVG نصي (تدرّج لوني +
// حرف/رمز يونيكود قياسي بمنتصفها)، خفيفة جداً بالحجم وتعمل فوراً بدون أي
// اتصال إنترنت، وتبقى حرة الاستخدام تماماً.
(function () {
    function badgeSvg({ from, to, glyph, ring = '#ffffff' }) {
        const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${from}"/>
      <stop offset="1" stop-color="${to}"/>
    </linearGradient>
  </defs>
  <circle cx="80" cy="80" r="72" fill="url(#g)"/>
  <circle cx="80" cy="80" r="72" fill="none" stroke="${ring}" stroke-width="4" opacity="0.55"/>
  <text x="80" y="106" font-size="78" text-anchor="middle">${glyph}</text>
</svg>`.trim();
        return 'data:image/svg+xml,' + encodeURIComponent(svg);
    }

    const REACTION_STICKERS = [
        { id: 'reaction_like', name: 'إعجاب', glyph: '👍', from: '#4facfe', to: '#00f2fe' },
        { id: 'reaction_love', name: 'حب', glyph: '❤️', from: '#ff9a9e', to: '#fecfef' },
        { id: 'reaction_laugh', name: 'ضحك', glyph: '😂', from: '#ffe259', to: '#ffa751' },
        { id: 'reaction_wow', name: 'تفاجئ', glyph: '😮', from: '#f6d365', to: '#fda085' },
        { id: 'reaction_sad', name: 'حزن', glyph: '😢', from: '#a1c4fd', to: '#c2e9fb' },
        { id: 'reaction_angry', name: 'غضب', glyph: '😡', from: '#ff5858', to: '#f857a6' },
        { id: 'reaction_fire', name: 'نار', glyph: '🔥', from: '#f83600', to: '#f9d423' },
        { id: 'reaction_100', name: 'مية مية', glyph: '💯', from: '#fc466b', to: '#3f5efb' },
        { id: 'reaction_clap', name: 'تصفيق', glyph: '👏', from: '#43e97b', to: '#38f9d7' },
        { id: 'reaction_party', name: 'احتفال', glyph: '🎉', from: '#a18cd1', to: '#fbc2eb' },
        { id: 'reaction_sleepy', name: 'نعسان', glyph: '😴', from: '#667eea', to: '#764ba2' },
        { id: 'reaction_deal', name: 'اتفقنا', glyph: '🤝', from: '#f7971e', to: '#ffd200' }
    ].map(s => ({
        id: s.id,
        type: 'image',
        data: badgeSvg({ from: s.from, to: s.to, glyph: s.glyph }),
        name: s.name,
        pack: '✨ ردود أفعال',
        uploadedBy: 'Semo'
    }));

    const ANIMAL_STICKERS = [
        { id: 'animal_cat', name: 'قطة', glyph: '🐱', from: '#ffecd2', to: '#fcb69f' },
        { id: 'animal_dog', name: 'كلب', glyph: '🐶', from: '#fddb92', to: '#d1fdff' },
        { id: 'animal_fox', name: 'ثعلب', glyph: '🦊', from: '#f6d365', to: '#fda085' },
        { id: 'animal_panda', name: 'باندا', glyph: '🐼', from: '#e0eafc', to: '#cfdef3' },
        { id: 'animal_rabbit', name: 'أرنب', glyph: '🐰', from: '#ffdde1', to: '#ee9ca7' },
        { id: 'animal_koala', name: 'كوالا', glyph: '🐨', from: '#d9afd9', to: '#97d9e1' },
        { id: 'animal_lion', name: 'أسد', glyph: '🦁', from: '#f9d423', to: '#ff4e50' },
        { id: 'animal_unicorn', name: 'يونيكورن', glyph: '🦄', from: '#c471f5', to: '#fa71cd' }
    ].map(s => ({
        id: s.id,
        type: 'image',
        data: badgeSvg({ from: s.from, to: s.to, glyph: s.glyph }),
        name: s.name,
        pack: '🐾 حيوانات كيوت',
        uploadedBy: 'Semo'
    }));

    window.EXTRA_STICKER_PACKS = REACTION_STICKERS.concat(ANIMAL_STICKERS);

    // دمج الحزمتين تلقائياً ضمن قائمة الستيكرات المتاحة (بدون تكرار)، بنفس
    // آلية mergeAnimeStickerPack الموجودة أصلاً بملف anime-stickers.js.
    window.mergeExtraStickerPacks = function () {
        if (!window.allStickers) window.allStickers = [];
        const existingIds = new Set(window.allStickers.map(s => s.id));
        const missing = window.EXTRA_STICKER_PACKS.filter(s => !existingIds.has(s.id));
        if (missing.length > 0) {
            window.allStickers = [...window.allStickers, ...missing];
        }
        return window.allStickers;
    };
})();

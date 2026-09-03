package com.tomodachi.app.data

/**
 * ================================================================
 *  الحزم الافتراضية المجانية (أنمي / ردود أفعال / حيوانات كيوت)
 * ================================================================
 * كانت هذه الحزم بالأصل (anime-stickers.js و extra-stickers.js) صوراً
 * SVG نصية مُولَّدة برمجياً (دوائر متدرّجة اللون + رمز يونيكود بمنتصفها،
 * أو وجوه "تشيبي" مرسومة بخطوط SVG). هنا نمثّل نفس الفكرة كبيانات وصفية
 * بسيطة (لون بداية/نهاية التدرج + رمز)، وتُرسَم فعلياً بمكوّن
 * StickerBadge (Compose Canvas) - نفس الناتج البصري تقريباً لكن أخف
 * بكثير من تضمين نصوص SVG كاملة، وبدون أي اعتماد على مكتبة SVG خارجية.
 * تُدمَج هذه الحزمة تلقائياً مع أي ستيكرات مخصَّصة قادمة من فايرستور -
 * تماماً بمعادل mergeAnimeStickerPack()/mergeExtraStickerPacks() الأصليتين.
 */
object StickerPacks {

    private const val ANIME_PACK = "🎌 أنمي"
    private const val REACTION_PACK = "✨ ردود أفعال"
    private const val ANIMAL_PACK = "🐾 حيوانات كيوت"

    val anime: List<Sticker> = listOf(
        Sticker("anime_happy", "vector", emoji = "😊", name = "سعيد", pack = ANIME_PACK, vectorFrom = "#ffe3ea", vectorTo = "#ffb6c9"),
        Sticker("anime_love", "vector", emoji = "😍", name = "حب", pack = ANIME_PACK, vectorFrom = "#fff0e6", vectorTo = "#ff9ab8"),
        Sticker("anime_wink", "vector", emoji = "😉", name = "غمزة", pack = ANIME_PACK, vectorFrom = "#e6f7ff", vectorTo = "#7fe0c0"),
        Sticker("anime_cry", "vector", emoji = "😢", name = "بكاء", pack = ANIME_PACK, vectorFrom = "#eef1ff", vectorTo = "#9fb0f5"),
        Sticker("anime_surprised", "vector", emoji = "😲", name = "مندهش", pack = ANIME_PACK, vectorFrom = "#fff9e0", vectorTo = "#ffd27f"),
        Sticker("anime_angry", "vector", emoji = "😠", name = "غاضب", pack = ANIME_PACK, vectorFrom = "#ffe6e6", vectorTo = "#ff8a8a"),
        Sticker("anime_sleepy", "vector", emoji = "😴", name = "نعسان", pack = ANIME_PACK, vectorFrom = "#f0eaff", vectorTo = "#b79cf0"),
        Sticker("anime_star", "vector", emoji = "🤩", name = "إعجاب", pack = ANIME_PACK, vectorFrom = "#e9fff0", vectorTo = "#7fe0a8"),
        Sticker("anime_shy", "vector", emoji = "☺️", name = "خجول", pack = ANIME_PACK, vectorFrom = "#ffeef5", vectorTo = "#f08bb0"),
        Sticker("anime_cool", "vector", emoji = "😎", name = "رايق", pack = ANIME_PACK, vectorFrom = "#eaf5ff", vectorTo = "#8fbfe0")
    )

    val reactions: List<Sticker> = listOf(
        Sticker("reaction_like", "vector", emoji = "👍", name = "إعجاب", pack = REACTION_PACK, vectorFrom = "#4facfe", vectorTo = "#00f2fe"),
        Sticker("reaction_love", "vector", emoji = "❤️", name = "حب", pack = REACTION_PACK, vectorFrom = "#ff9a9e", vectorTo = "#fecfef"),
        Sticker("reaction_laugh", "vector", emoji = "😂", name = "ضحك", pack = REACTION_PACK, vectorFrom = "#ffe259", vectorTo = "#ffa751"),
        Sticker("reaction_wow", "vector", emoji = "😮", name = "تفاجئ", pack = REACTION_PACK, vectorFrom = "#f6d365", vectorTo = "#fda085"),
        Sticker("reaction_sad", "vector", emoji = "😢", name = "حزن", pack = REACTION_PACK, vectorFrom = "#a1c4fd", vectorTo = "#c2e9fb"),
        Sticker("reaction_angry", "vector", emoji = "😡", name = "غضب", pack = REACTION_PACK, vectorFrom = "#ff5858", vectorTo = "#f857a6"),
        Sticker("reaction_fire", "vector", emoji = "🔥", name = "نار", pack = REACTION_PACK, vectorFrom = "#f83600", vectorTo = "#f9d423"),
        Sticker("reaction_100", "vector", emoji = "💯", name = "مية مية", pack = REACTION_PACK, vectorFrom = "#fc466b", vectorTo = "#3f5efb"),
        Sticker("reaction_clap", "vector", emoji = "👏", name = "تصفيق", pack = REACTION_PACK, vectorFrom = "#43e97b", vectorTo = "#38f9d7"),
        Sticker("reaction_party", "vector", emoji = "🎉", name = "احتفال", pack = REACTION_PACK, vectorFrom = "#a18cd1", vectorTo = "#fbc2eb"),
        Sticker("reaction_sleepy", "vector", emoji = "😴", name = "نعسان", pack = REACTION_PACK, vectorFrom = "#667eea", vectorTo = "#764ba2"),
        Sticker("reaction_deal", "vector", emoji = "🤝", name = "اتفقنا", pack = REACTION_PACK, vectorFrom = "#f7971e", vectorTo = "#ffd200")
    )

    val animals: List<Sticker> = listOf(
        Sticker("animal_cat", "vector", emoji = "🐱", name = "قطة", pack = ANIMAL_PACK, vectorFrom = "#ffecd2", vectorTo = "#fcb69f"),
        Sticker("animal_dog", "vector", emoji = "🐶", name = "كلب", pack = ANIMAL_PACK, vectorFrom = "#fddb92", vectorTo = "#d1fdff"),
        Sticker("animal_fox", "vector", emoji = "🦊", name = "ثعلب", pack = ANIMAL_PACK, vectorFrom = "#f6d365", vectorTo = "#fda085"),
        Sticker("animal_panda", "vector", emoji = "🐼", name = "باندا", pack = ANIMAL_PACK, vectorFrom = "#e0eafc", vectorTo = "#cfdef3"),
        Sticker("animal_rabbit", "vector", emoji = "🐰", name = "أرنب", pack = ANIMAL_PACK, vectorFrom = "#ffdde1", vectorTo = "#ee9ca7"),
        Sticker("animal_koala", "vector", emoji = "🐨", name = "كوالا", pack = ANIMAL_PACK, vectorFrom = "#d9afd9", vectorTo = "#97d9e1"),
        Sticker("animal_lion", "vector", emoji = "🦁", name = "أسد", pack = ANIMAL_PACK, vectorFrom = "#f9d423", vectorTo = "#ff4e50"),
        Sticker("animal_unicorn", "vector", emoji = "🦄", name = "يونيكورن", pack = ANIMAL_PACK, vectorFrom = "#c471f5", vectorTo = "#fa71cd")
    )

    val all: List<Sticker> get() = anime + reactions + animals

    /** يدمج الحزم الافتراضية مع قائمة قادمة من فايرستور بدون تكرار أي id،
     * تماماً كمعادل mergeAnimeStickerPack()/mergeExtraStickerPacks(). */
    fun mergeInto(existing: List<Sticker>): List<Sticker> {
        val existingIds = existing.map { it.id }.toSet()
        val missing = all.filterNot { existingIds.contains(it.id) }
        return existing + missing
    }
}

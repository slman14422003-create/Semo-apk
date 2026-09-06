package com.tomodachi.chat.util

data class EmojiCategory(val id: String, val nameAr: String, val icon: String, val emojis: List<String>)

/**
 * لوحة إيموجي كاملة تغطي كل نطاقات اليونيكود القياسية للإيموجي (وليس نطاقات
 * جزئية مختصرة كما كانت سابقاً)، موزّعة على 10 تصنيفات. كل نطاق هنا يقابل
 * كتلة يونيكود إيموجي رسمية معروفة، بالإضافة إلى الرموز التكميلية
 * (Supplemental Symbols and Pictographs 0x1F900+‎ والامتداد A
 * 0x1FA70+‎) التي لم تكن مشمولة إطلاقاً في النسخة السابقة وتحتوي على مئات
 * الرموز الحديثة (تعابير وجه إضافية، إيماءات يد، أطعمة، حيوانات، أدوات...).
 *
 * الأعلام تُبنى فعلياً من دمج رمزَي "مؤشر إقليمي" (Regional Indicator) بدل
 * تعداد المؤشرات منفردة كما كان سابقاً — تعداد المؤشرات منفردة ينتج حروفاً
 * مربّعة بدل أعلام حقيقية، لذلك استُبدل بقائمة مباشرة من أعلام دول فعلية.
 */
object EmojiData {

    val categories: List<EmojiCategory> = listOf(
        EmojiCategory("faces", "وجوه", "😀", faces),
        EmojiCategory("people", "أشخاص", "🧑", people),
        EmojiCategory("animals", "حيوانات", "🐶", animals),
        EmojiCategory("nature", "طبيعة", "🌿", nature),
        EmojiCategory("food", "طعام", "🍔", food),
        EmojiCategory("activities", "نشاطات", "⚽", activities),
        EmojiCategory("objects", "أشياء", "💡", objects),
        EmojiCategory("travel", "سفر", "✈️", travel),
        EmojiCategory("symbols", "رموز", "❤️", symbols),
        EmojiCategory("flags", "أعلام", "🏁", flags)
    )

    fun search(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return categories.filter { it.nameAr.contains(query) }.flatMap { it.emojis }
    }
}

/** يحوّل مدى نقاط يونيكود (ابتداءً وحتى، شاملَين) إلى قائمة نصوص إيموجي. */
private fun range(startInclusive: Int, endInclusive: Int): List<String> =
    (startInclusive..endInclusive).map { String(Character.toChars(it)) }

// --- وجوه وتعابير: الكتلة الأساسية كاملة + إضافات من الكتلة التكميلية ---
private val faces = range(0x1F600, 0x1F64F) +          // Emoticons block كاملة
    range(0x1F910, 0x1F92F) +                            // وجوه إضافية (مذهول، مرتجف، بمسمار...)
    range(0x1F970, 0x1F97A) +                            // وجوه حديثة (متحمّس، بارد، متوسل...)
    listOf("🥲", "🥸", "🫠", "🫡", "🫢", "🫣", "🫤", "🫥") // إضافات حديثة شائعة

// --- أشخاص وأجزاء جسد وإيماءات يد ---
private val people = range(0x1F466, 0x1F487) +           // أطفال/عائلة/شعر
    range(0x1F9D0, 0x1F9DF) +                            // أشخاص متنوعون (جنيّ، عملاق، ساحر...)
    range(0x1F385, 0x1F385) +                            // بابا نويل
    range(0x1F930, 0x1F93A) +                            // حوامل ورياضيون
    range(0x1F9B0, 0x1F9B9) +                            // شعر/أطراف اصطناعية
    listOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
        "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️",
        "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐",
        "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵", "🦿",
        "👣", "👀", "👁️", "🧠", "🫀", "🫁", "🦷", "🦴", "👅", "👄"
    )

// --- حيوانات وطبيعة (زواحف/بحر/حشرات مضافة على الكتلة الأساسية) ---
private val animals = range(0x1F400, 0x1F43E) +
    range(0x1F980, 0x1F9AE) +                            // كائنات حديثة: أخطبوط، جراد، فلامنجو...
    range(0x1F9A0, 0x1F9AF)

// --- طبيعة ونباتات وطقس ---
private val nature = range(0x1F330, 0x1F343) +
    range(0x2600, 0x26FF) +                              // رموز الطقس والفلك
    range(0x1F300, 0x1F32C) +                            // ظواهر جوية وفلكية
    listOf("🪴", "🌵", "🎍", "🪷", "🪹", "🪺")

// --- طعام وشراب كامل تقريباً ---
private val food = range(0x1F345, 0x1F37F) +
    range(0x1F950, 0x1F96F) +                            // أطعمة حديثة: كرواسون، تاكو، فطيرة...
    listOf("🥭", "🫐", "🥬", "🫑", "🧄", "🧅", "🫒", "🫓", "🧆", "🫕", "🧇")

// --- نشاطات ورياضة وألعاب ---
private val activities = range(0x1F380, 0x1F3FA) +
    range(0x1F93C, 0x1F945) +                            // رياضات إضافية
    listOf("🪀", "🪁", "🎯", "🎳", "🎮", "🧩", "🪄", "🎨", "🎭", "🎟️")

// --- أشياء ومعدّات وأدوات مكتب/منزل ---
private val objects = range(0x1F4A0, 0x1F4FF) +
    range(0x1F5A0, 0x1F5FF) +                            // أدوات وملفات ومكتب
    range(0x1FA70, 0x1FA7F) +                            // أدوات طبية/ملابس حديثة
    range(0x1FA80, 0x1FA8F) +                            // ألعاب/أدوات إضافية
    listOf("🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "💽", "💾", "📀", "🧯", "🪜", "🪞", "🪟")

// --- سفر وأماكن ومركبات ---
private val travel = range(0x1F680, 0x1F6C5) +
    range(0x1F3E0, 0x1F3F0) +                            // مبانٍ ومعالم
    listOf("🛴", "🛵", "🛺", "🚲", "🛹", "🛼", "🚀", "🛸", "⛺", "🏕️", "🗺️")

// --- رموز وقلوب وعلامات ---
private val symbols = listOf(
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🤎", "🖤", "🤍", "💔",
    "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"
) + range(0x1F500, 0x1F53D) +                             // أزرار وأسهم ورموز تحكم
    range(0x2700, 0x27BF) +                              // Dingbats (✅❌✳️ إلخ)
    range(0x2190, 0x21FF) +                              // أسهم
    listOf("♻️", "⚠️", "🚫", "✅", "❌", "❓", "❗", "‼️", "⁉️", "💯", "🔞", "📵", "🔅", "🔆")

// --- أعلام حقيقية (زوج من مؤشرات إقليمية لكل رمز دولة ISO-3166) ---
private fun flagFor(countryCode: String): String {
    val base = 0x1F1E6 - 'A'.code
    return countryCode.uppercase().map { String(Character.toChars(base + it.code)) }.joinToString("")
}

private val flags = listOf(
    "SA", "AE", "EG", "JO", "LB", "SY", "IQ", "KW", "QA", "BH",
    "OM", "YE", "PS", "MA", "DZ", "TN", "LY", "SD",
    "US", "GB", "CA", "FR", "DE", "IT", "ES", "PT",
    "TR", "RU", "CN", "JP", "KR", "IN", "PK", "ID", "MY",
    "BR", "MX", "AR", "AU", "NL", "SE", "CH", "GR"
).map { flagFor(it) } + listOf("🏳️", "🏴", "🏁", "🚩", "🏳️‍🌈", "🏴‍☠️")

val AVATAR_EMOJI_CHOICES = listOf(
    "😀", "😎", "🤖", "👻", "🐱", "🐶", "🦊", "🐼",
    "🐸", "🐵", "🦁", "🐯", "🦄", "🐧", "🐨", "🦋"
)

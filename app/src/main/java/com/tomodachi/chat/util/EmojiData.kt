package com.tomodachi.chat.util

data class EmojiCategory(val id: String, val nameAr: String, val icon: String, val emojis: List<String>)

/**
 * لوحة إيموجي كاملة تغطي كل نطاقات اليونيكود القياسية للإيموجي تقريباً بدل
 * نطاقات جزئية مختصرة — تشمل الآن أيضاً الفجوات التي كانت مفقودة سابقاً
 * (مثل 👀👂👃 ضمن 0x1F440-0x1F465، الساعات 0x1F550-0x1F567، الأبراج
 * الفلكية، مفاتيح الأرقام keycaps، ورموز أكثر من كتلتَي التكميل
 * Supplemental Symbols/Pictographs و Symbols and Pictographs Extended-A
 * بالكامل تقريباً)، بالإضافة لقائمة أعلام موسّعة بشكل كبير (من 44 دولة إلى
 * أكثر من 150). كل قائمة تصنيف تمر بـ`.distinct()` أخيراً لإزالة أي تكرار
 * ناتج عن تداخل النطاقات مع الإضافات اليدوية.
 *
 * ملاحظة صادقة: هذا يغطي كل "الإيموجي الأساسية" (base emoji) في الكتل
 * الرسمية، لكنه لا يولّد تلقائياً كل تدرّجات لون البشرة (🏻🏼🏽🏾🏿) ولا كل
 * تسلسلات ZWJ المركّبة (كالعائلات والمهن بجنسين مختلفين) لأن هذه تسلسلات
 * مُركَّبة وليست رموزاً مستقلة — تغطيتها بالكامل تتطلب لوحة منفصلة أكبر
 * بكثير من لوحة إيموجي عادية.
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

// --- وجوه وتعابير: الكتلة الأساسية كاملة + إضافات من الكتلتين التكميليتين ---
private val faces = (range(0x1F600, 0x1F64F) +          // Emoticons block كاملة
    range(0x1F900, 0x1F90F) +                            // إيماءات/تعابير تكميلية إضافية
    range(0x1F910, 0x1F93A) +                            // وجوه إضافية (مذهول، مرتجف، بمسمار...)
    range(0x1F970, 0x1F97A) +                            // وجوه حديثة (متحمّس، بارد، متوسل...)
    range(0x1FAE0, 0x1FAEF) +                            // وجوه 2021-2023 (يرتجف، يذوب، محدّق...)
    listOf("🥲", "🥸", "🫠", "🫡", "🫢", "🫣", "🫤", "🫥", "☺️", "☹️", "🙂‍↕️", "🙂‍↔️")
).distinct()

// --- أشخاص وأجزاء جسد وإيماءات يد ---
private val people = (range(0x1F440, 0x1F465) +          // فجوة كانت مفقودة: عينان، أذن، أنف...
    range(0x1F466, 0x1F487) +                            // أطفال/عائلة/شعر
    range(0x1F9D0, 0x1F9DF) +                            // أشخاص متنوعون (جنيّ، عملاق، ساحر...)
    range(0x1F385, 0x1F385) +                            // بابا نويل
    range(0x1F930, 0x1F93A) +                            // حوامل ورياضيون
    range(0x1F9B0, 0x1F9B9) +                            // شعر/أطراف اصطناعية
    range(0x1F9BA, 0x1F9CF) +                            // إكسسوارات إتاحة + أشخاص إضافيون
    range(0x1FAC0, 0x1FACF) +                            // أجزاء جسد حديثة (قلب تشريحي، رئتان...)
    range(0x1FAF0, 0x1FAFF) +                            // إيماءات يد حديثة (يد مرتجفة، مصافحة متنوعة...)
    listOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
        "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️",
        "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐",
        "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵", "🦿",
        "👣", "👀", "👁️", "🧠", "🫀", "🫁", "🦷", "🦴", "👅", "👄",
        "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓",
        "👴", "👵", "🙍", "🙎", "🙅", "🙆", "💁", "🙋", "🧏", "🙇",
        "🤦", "🤷", "👮", "🕵️", "💂", "🥷", "👷", "🫅", "🤴", "👸",
        "👳", "👲", "🧕", "🤵", "👰", "🤰", "🫄", "🫃", "🤱", "👼",
        "🎅", "🤶", "🦸", "🦹", "🧙", "🧚", "🧛", "🧜", "🧝", "🧞",
        "🧟", "🧌", "💆", "💇", "🚶", "🧍", "🧎", "🏃", "💃", "🕺",
        "🕴️", "👯", "🧖", "🧗", "🤺", "🏇", "⛷️", "🏂", "🏌️", "🏄",
        "🚣", "🏊", "⛹️", "🏋️", "🚴", "🚵", "🤸", "🤼", "🤽", "🤾",
        "🤹", "🧘", "🛀", "🛌", "👭", "👫", "👬", "💏", "💑", "👪"
    )
).distinct()

// --- حيوانات وطبيعة (زواحف/بحر/حشرات مضافة على الكتلة الأساسية) ---
private val animals = (range(0x1F400, 0x1F43E) +
    range(0x1F980, 0x1F9AE) +                            // كائنات حديثة: أخطبوط، جراد، فلامنجو...
    range(0x1F9A0, 0x1F9AF) +                            // حشرات ونباتات وحيوانات إضافية
    range(0x1FAB0, 0x1FABF)                              // حيوانات 2021-2023: بعوضة، أوزّة، كسلان...
).distinct()

// --- طبيعة ونباتات وطقس ---
private val nature = (range(0x1F330, 0x1F343) +
    range(0x2600, 0x26FF) +                              // رموز الطقس والفلك
    range(0x1F300, 0x1F32C) +                            // ظواهر جوية وفلكية
    range(0x2B00, 0x2BFF) +                              // نجوم وأسهم إضافية (⭐🌟➡️ إلخ)
    range(0x2648, 0x2653) +                               // الأبراج الفلكية الاثنا عشر
    listOf("🪴", "🌵", "🎍", "🪷", "🪹", "🪺")
).distinct()

// --- طعام وشراب كامل تقريباً ---
private val food = (range(0x1F345, 0x1F37F) +
    range(0x1F950, 0x1F96F) +                            // أطعمة حديثة: كرواسون، تاكو، فطيرة...
    range(0x1F9C0, 0x1F9CC) +                            // جبن وأطعمة إضافية
    range(0x1FAD0, 0x1FADF) +                            // أطعمة 2021-2023: زيتون، مانجا، جذر...
    listOf("🥭", "🫐", "🥬", "🫑", "🧄", "🧅", "🫒", "🫓", "🧆", "🫕", "🧇")
).distinct()

// --- نشاطات ورياضة وألعاب ---
private val activities = (range(0x1F380, 0x1F3FA) +
    range(0x1F93C, 0x1F94F) +                            // رياضات وميداليات إضافية
    range(0x1FA80, 0x1FA8F) +                            // ألعاب/أدوات إضافية (يويو، طائرة ورقية...)
    listOf("🪀", "🪁", "🎯", "🎳", "🎮", "🧩", "🪄", "🎨", "🎭", "🎟️", "🪅", "🪩")
).distinct()

// --- أشياء ومعدّات وأدوات مكتب/منزل ---
private val objects = (range(0x1F4A0, 0x1F4FF) +
    range(0x1F550, 0x1F567) +                            // ساعات بكل الأوقات (كانت مفقودة تماماً)
    range(0x1F5A0, 0x1F5FF) +                            // أدوات وملفات ومكتب
    range(0x1F9E0, 0x1F9FF) +                            // دماغ وحقيبة ظهر ورموز إضافية
    range(0x1FA70, 0x1FA7F) +                            // أدوات طبية/ملابس حديثة
    range(0x1FA90, 0x1FAAF) +                            // أدوات منزلية حديثة: مطرقة، حبل، إبريق...
    listOf(
        "🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "💽", "💾", "📀",
        "🧯", "🪜", "🪞", "🪟",
        "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟", "#️⃣", "*️⃣"
    )
).distinct()

// --- سفر وأماكن ومركبات ---
private val travel = (range(0x1F680, 0x1F6FF) +          // نقل وخرائط كاملة (Transport And Map)
    range(0x1F3E0, 0x1F3F0) +                            // مبانٍ ومعالم
    listOf("🛴", "🛵", "🛺", "🚲", "🛹", "🛼", "🚀", "🛸", "⛺", "🏕️", "🗺️")
).distinct()

// --- رموز وقلوب وعلامات ---
private val symbols = (listOf(
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🤎", "🖤", "🤍", "💔",
    "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟",
    "♀️", "♂️", "⚧️", "⚕️"
) + range(0x1F500, 0x1F53D) +                             // أزرار وأسهم ورموز تحكم
    range(0x2700, 0x27BF) +                              // Dingbats (✅❌✳️ إلخ)
    range(0x2190, 0x21FF) +                              // أسهم
    range(0x2600, 0x2604) +                              // رموز فلكية إضافية
    listOf("♻️", "⚠️", "🚫", "✅", "❌", "❓", "❗", "‼️", "⁉️", "💯", "🔞", "📵", "🔅", "🔆", "🈺", "🈹", "㊙️", "㊗️")
).distinct()

// --- أعلام حقيقية (زوج من مؤشرات إقليمية لكل رمز دولة ISO-3166) ---
private fun flagFor(countryCode: String): String {
    val base = 0x1F1E6 - 'A'.code
    return countryCode.uppercase().map { String(Character.toChars(base + it.code)) }.joinToString("")
}

// قائمة موسّعة تغطي معظم دول العالم (بدل 44 دولة فقط سابقاً) — منظّمة حسب
// المنطقة لسهولة المراجعة، تبدأ بالدول العربية كي تظهر أولاً في اللوحة.
private val flagCountryCodes = listOf(
    // الدول العربية
    "SA", "AE", "EG", "JO", "LB", "SY", "IQ", "KW", "QA", "BH",
    "OM", "YE", "PS", "MA", "DZ", "TN", "LY", "SD", "MR", "SO",
    "DJ", "KM",
    // أوروبا
    "GB", "FR", "DE", "IT", "ES", "PT", "NL", "BE", "CH", "AT",
    "SE", "NO", "DK", "FI", "IE", "PL", "CZ", "SK", "HU", "RO",
    "BG", "GR", "TR", "UA", "RU", "BY", "LT", "LV", "EE", "IS",
    "LU", "MT", "CY", "HR", "RS", "SI", "AL", "MK", "BA", "ME",
    "MD", "MC", "AD", "SM", "VA", "LI",
    // أمريكا الشمالية والجنوبية
    "US", "CA", "MX", "BR", "AR", "CL", "CO", "PE", "VE", "EC",
    "BO", "PY", "UY", "CU", "DO", "GT", "HN", "SV", "NI", "CR",
    "PA", "JM", "HT", "TT",
    // آسيا
    "CN", "JP", "KR", "KP", "IN", "PK", "BD", "LK", "NP", "BT",
    "MM", "TH", "VN", "LA", "KH", "MY", "SG", "ID", "PH", "BN",
    "MN", "KZ", "UZ", "TM", "TJ", "KG", "AF", "IR", "IL", "TW",
    "HK", "MO",
    // أفريقيا
    "ZA", "NG", "KE", "ET", "GH", "TZ", "UG", "SN", "CM", "CI",
    "ZW", "ZM", "MZ", "MG", "AO", "BW", "NA", "RW", "ML", "NE",
    "TD", "BF", "BJ", "TG", "GA", "CG", "CD", "GN", "SS", "ER",
    "SC", "MU", "CV", "GM", "SL", "LR", "SZ", "LS", "MW",
    // أوقيانوسيا
    "AU", "NZ", "FJ", "PG", "WS", "TO"
).distinct()

private val flags = flagCountryCodes.map { flagFor(it) } +
    listOf("🏳️", "🏴", "🏁", "🚩", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "🇺🇳", "🇪🇺")

val AVATAR_EMOJI_CHOICES = listOf(
    "😀", "😎", "🤖", "👻", "🐱", "🐶", "🦊", "🐼",
    "🐸", "🐵", "🦁", "🐯", "🦄", "🐧", "🐨", "🦋"
)

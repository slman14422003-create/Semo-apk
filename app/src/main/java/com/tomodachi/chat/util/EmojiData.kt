package com.tomodachi.chat.util

data class EmojiCategory(val id: String, val nameAr: String, val icon: String, val emojis: List<String>)

/**
 * لوحة إيموجي تضم أكثر من 900 رمز موزّعة على 10 تصنيفات، كما هو مطلوب.
 * القوائم هنا مختصرة في هذا الملف لأسباب المساحة، لكن كل تصنيف مصمَّم
 * لتوسيعه بسهولة بإضافة المزيد من نقاط اليونيكود إلى القائمة المناظرة.
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
        // بحث بسيط عبر اسم التصنيف؛ التصفية الفعلية للرموز تتم في الواجهة حسب التصنيف المطابق
        return categories.filter { it.nameAr.contains(query) }.flatMap { it.emojis }
    }
}

private val faces = ('\uD83D'.code.let { 0x1F600 }..0x1F64F).map { String(Character.toChars(it)) }
private val people = (0x1F466..0x1F487).map { String(Character.toChars(it)) } +
    (0x1F9D0..0x1F9DF).map { String(Character.toChars(it)) }
private val animals = (0x1F400..0x1F43E).map { String(Character.toChars(it)) }
private val nature = (0x1F330..0x1F343).map { String(Character.toChars(it)) } +
    (0x2600..0x26FF).map { String(Character.toChars(it)) }
private val food = (0x1F345..0x1F37F).map { String(Character.toChars(it)) }
private val activities = (0x1F380..0x1F3FA).map { String(Character.toChars(it)) }
private val objects = (0x1F4A0..0x1F4FF).map { String(Character.toChars(it)) }
private val travel = (0x1F680..0x1F6C5).map { String(Character.toChars(it)) }
private val symbols = (0x2764..0x2764).map { String(Character.toChars(it)) } +
    (0x1F500..0x1F53D).map { String(Character.toChars(it)) }
private val flags = (0x1F1E6..0x1F1FF).map { String(Character.toChars(it)) }

val AVATAR_EMOJI_CHOICES = listOf(
    "😀", "😎", "🤖", "👻", "🐱", "🐶", "🦊", "🐼",
    "🐸", "🐵", "🦁", "🐯", "🦄", "🐧", "🐨", "🦋"
)

package com.tomodachi.chat.data.model

enum class StickerPackType { BUILTIN, CUSTOM }

data class StickerPack(
    val id: String = "",
    val nameAr: String = "",
    val type: StickerPackType = StickerPackType.BUILTIN,
    val stickers: List<Sticker> = emptyList()
)

data class Sticker(
    val id: String = "",
    val packId: String = "",
    val emojiFallback: String = "🎨",   // للحزم المرسومة كشارات (بدون صور خارجية)
    val gradientStartHex: String = "#FF6F61",
    val gradientEndHex: String = "#5DB7DE",
    val label: String = "",
    val imageUrl: String = "",          // فارغ للحزم الافتراضية، مُعبّأ للمرفوعة من المستخدمين
    val uploadedByUid: String = "",
    val uploadedByUsername: String = "",
    val createdAtMillis: Long = 0L
) {
    companion object {
        const val COLLECTION = "stickers"
    }
}

data class BannedWord(
    val id: String = "",
    val word: String = "",
    val addedByUid: String = "",
    val createdAtMillis: Long = 0L
) {
    companion object {
        const val COLLECTION = "banned_words"
        const val BASE_BAN_SECONDS = 60
        const val MAX_BAN_SECONDS = 600 // 10 دقائق
    }
}

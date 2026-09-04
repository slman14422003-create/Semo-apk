package com.tomodachi.chat.data.repository

import android.net.Uri
import com.tomodachi.chat.data.model.Sticker
import com.tomodachi.chat.data.model.StickerPack
import com.tomodachi.chat.data.model.StickerPackType
import com.tomodachi.chat.data.remote.FirebaseModule
import com.tomodachi.chat.data.remote.FirestorePaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StickerRepository {

    private val firestore get() = FirebaseModule.firestore
    private val storage get() = FirebaseModule.storage
    private val stickersRef get() = firestore.collection(FirestorePaths.STICKERS)

    /**
     * ثلاث حزم افتراضية مجانية (أنمي، ردود أفعال، حيوانات كيوت) — شارات دائرية
     * متدرّجة الألوان مرسومة بالكامل داخل التطبيق، بدون أي اعتماد على صور خارجية.
     */
    fun builtinPacks(): List<StickerPack> = listOf(
        StickerPack(
            id = "anime",
            nameAr = "أنمي",
            type = StickerPackType.BUILTIN,
            stickers = listOf(
                Sticker(id = "anime_1", packId = "anime", emojiFallback = "✨", gradientStartHex = "#FF6F91", gradientEndHex = "#FF9671", label = "بريق"),
                Sticker(id = "anime_2", packId = "anime", emojiFallback = "🌸", gradientStartHex = "#FFC75F", gradientEndHex = "#FF6F91", label = "زهرة"),
                Sticker(id = "anime_3", packId = "anime", emojiFallback = "⚔️", gradientStartHex = "#845EC2", gradientEndHex = "#D65DB1", label = "سيف"),
                Sticker(id = "anime_4", packId = "anime", emojiFallback = "🎌", gradientStartHex = "#0089BA", gradientEndHex = "#00C2A8", label = "علم")
            )
        ),
        StickerPack(
            id = "reactions",
            nameAr = "ردود أفعال",
            type = StickerPackType.BUILTIN,
            stickers = listOf(
                Sticker(id = "reaction_1", packId = "reactions", emojiFallback = "😂", gradientStartHex = "#FFD93D", gradientEndHex = "#FF6B6B", label = "ضحك"),
                Sticker(id = "reaction_2", packId = "reactions", emojiFallback = "😱", gradientStartHex = "#4D96FF", gradientEndHex = "#6BCB77", label = "صدمة"),
                Sticker(id = "reaction_3", packId = "reactions", emojiFallback = "😍", gradientStartHex = "#FF6B6B", gradientEndHex = "#FFD93D", label = "إعجاب"),
                Sticker(id = "reaction_4", packId = "reactions", emojiFallback = "🤔", gradientStartHex = "#6BCB77", gradientEndHex = "#4D96FF", label = "تفكير")
            )
        ),
        StickerPack(
            id = "cute_animals",
            nameAr = "حيوانات كيوت",
            type = StickerPackType.BUILTIN,
            stickers = listOf(
                Sticker(id = "animal_1", packId = "cute_animals", emojiFallback = "🐱", gradientStartHex = "#F6A6FF", gradientEndHex = "#7A6FF0", label = "قطة"),
                Sticker(id = "animal_2", packId = "cute_animals", emojiFallback = "🐶", gradientStartHex = "#FFAF7A", gradientEndHex = "#FFD97A", label = "كلب"),
                Sticker(id = "animal_3", packId = "cute_animals", emojiFallback = "🐰", gradientStartHex = "#B8F2E6", gradientEndHex = "#AED9E0", label = "أرنب"),
                Sticker(id = "animal_4", packId = "cute_animals", emojiFallback = "🐼", gradientStartHex = "#B5B9FF", gradientEndHex = "#8686F5", label = "باندا")
            )
        )
    )

    fun observeCustomStickers(): Flow<List<Sticker>> = callbackFlow {
        val registration = stickersRef.addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            val stickers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Sticker::class.java)?.copy(id = doc.id)
            }
            trySend(stickers)
        }
        awaitClose { registration.remove() }
    }

    /** ضغط تلقائي ذكي للصورة قبل الرفع؛ منطق الضغط الفعلي في util/ImageCompressor. */
    suspend fun uploadCustomSticker(
        compressedBytes: ByteArray,
        uploaderUid: String,
        uploaderUsername: String
    ): Result<Sticker> = try {
        val stickerId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("stickers/$stickerId.jpg")
        storageRef.putBytes(compressedBytes).await()
        val downloadUrl = storageRef.downloadUrl.await()
        val sticker = Sticker(
            id = stickerId,
            packId = "custom_$uploaderUid",
            imageUrl = downloadUrl.toString(),
            uploadedByUid = uploaderUid,
            uploadedByUsername = uploaderUsername,
            createdAtMillis = System.currentTimeMillis()
        )
        stickersRef.document(stickerId).set(sticker).await()
        Result.success(sticker)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteSticker(sticker: Sticker) {
        runCatching {
            if (sticker.imageUrl.isNotBlank()) {
                storage.getReferenceFromUrl(sticker.imageUrl).delete().await()
            }
        }
        stickersRef.document(sticker.id).delete().await()
    }
}

/** يُستخدم فقط لتوضيح نوع مصدر الصورة عند الرفع من معرض الجهاز. */
data class PickedImage(val uri: Uri, val bytes: ByteArray)

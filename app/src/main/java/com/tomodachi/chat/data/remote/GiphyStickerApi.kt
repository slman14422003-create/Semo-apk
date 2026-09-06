package com.tomodachi.chat.data.remote

import com.tomodachi.chat.data.model.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * عميل بسيط لواجهة Giphy Stickers المجانية — بدون أي مكتبة شبكة إضافية
 * (لا Retrofit ولا OkHttp)، فقط HttpURLConnection وOrg.json المدمجتان أصلاً
 * في منصة أندرويد، حتى لا نحتاج لأي تبعية Gradle جديدة عدا coil-gif
 * (المضافة في build.gradle.kts) لعرض صور الستيكرات المتحرّكة (GIF/WebP).
 *
 * المفتاح أدناه هو مفتاح Giphy التجريبي العام الموثَّق رسمياً (يُستخدم في آلاف
 * الأمثلة التعليمية المفتوحة)، صالح للتجربة والتطوير فوراً بلا أي تسجيل، لكنه
 * محدود بمعدل طلبات منخفض. للإنتاج الفعلي: أنشئ تطبيقاً مجانياً على
 * https://developers.giphy.com واستبدل القيمة هنا بمفتاحك الخاص — الحد
 * المجاني هناك يكفي تطبيقاً بحجم متوسط دون أي تكلفة.
 */
object GiphyStickerApi {

    private const val API_KEY = "dc6zaTOxFJmzC"
    private const val BASE_URL = "https://api.giphy.com/v1/stickers"
    private const val CONNECT_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 8000

    suspend fun trending(limit: Int = 30): Result<List<Sticker>> = withContext(Dispatchers.IO) {
        fetch("$BASE_URL/trending?api_key=$API_KEY&limit=$limit&rating=g")
    }

    suspend fun search(query: String, limit: Int = 30): Result<List<Sticker>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext trending(limit)
        val encoded = URLEncoder.encode(query, "UTF-8")
        fetch("$BASE_URL/search?api_key=$API_KEY&q=$encoded&limit=$limit&rating=g")
    }

    private fun fetch(urlString: String): Result<List<Sticker>> = try {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return Result.failure(IllegalStateException("Giphy HTTP $code"))
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val root = JSONObject(body)
        val dataArray = root.optJSONArray("data") ?: return Result.success(emptyList())
        val stickers = buildList {
            for (i in 0 until dataArray.length()) {
                val item = dataArray.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val title = item.optString("title", "ستيكر")
                val images = item.optJSONObject("images") ?: continue
                // نفضّل "fixed_width" لأنها مصغّرة مسبقاً (أخف وأسرع تحميلاً
                // داخل شبكة اللوحة)، ونسقط تدريجياً لخيارات أخرى إن غابت.
                val imageUrl = images.optJSONObject("fixed_width")?.optString("url")
                    ?: images.optJSONObject("downsized")?.optString("url")
                    ?: images.optJSONObject("original")?.optString("url")
                    ?: continue
                if (id.isBlank() || imageUrl.isBlank()) continue
                add(
                    Sticker(
                        id = "giphy_$id",
                        packId = "online",
                        label = title,
                        imageUrl = imageUrl
                    )
                )
            }
        }
        Result.success(stickers)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

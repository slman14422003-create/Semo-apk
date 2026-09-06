package com.tomodachi.chat.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * ميزة جديدة بالكامل: التحقق من التحديثات مباشرة من "GitHub Releases" الخاص
 * بالمستودع، وتنزيل ملف الـ APK وتثبيته من داخل التطبيق نفسه — دون فتح أي
 * متصفح ودون أي اعتماد على متجر تطبيقات. تُستخدَم فقط HttpURLConnection
 * وorg.json (مدمجتان أصلاً في أندرويد) تماماً كأسلوب [GiphyStickerApi]، بلا
 * أي تبعية Gradle إضافية.
 *
 * **مهم**: عدّل قيمتَي [GITHUB_OWNER] و[GITHUB_REPO] أدناه لتطابقا اسم مالك
 * ومستودع GitHub الفعلي الذي يُبنى منه التطبيق عبر GitHub Actions (نفس
 * المستودع الذي يحتوي مجلد .github/workflows في هذا المشروع).
 */
object AppUpdateChecker {

    private const val GITHUB_OWNER = "slman14422003-create"
    private const val GITHUB_REPO = "Semo-apk"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 9000
    private const val READ_TIMEOUT_MS = 9000

    data class ReleaseInfo(
        val tagName: String,
        val displayName: String,
        val notes: String,
        val apkDownloadUrl: String?,
        val apkSizeBytes: Long,
        val publishedAt: String
    )

    sealed class DownloadProgress {
        data class InProgress(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress()
        data class Done(val file: File) : DownloadProgress()
        data class Error(val message: String) : DownloadProgress()
    }

    /** يجلب أحدث إصدار منشور على GitHub Releases، مع معلومات ملاحظات الإصدار ورابط الـ APK. */
    suspend fun fetchLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext Result.failure(IllegalStateException("GitHub HTTP $code"))
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val root = JSONObject(body)
            val tagName = root.optString("tag_name")
            val assets = root.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    // نفضّل ملف الإصدار الموقّع (release) على نسخة التجربة (debug) إن وُجد الاثنان معاً
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val isRelease = !name.contains("debug", ignoreCase = true)
                        if (apkUrl == null || isRelease) {
                            apkUrl = asset.optString("browser_download_url")
                            apkSize = asset.optLong("size", 0L)
                            if (isRelease) break
                        }
                    }
                }
            }
            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    displayName = root.optString("name", tagName),
                    notes = root.optString("body", ""),
                    apkDownloadUrl = apkUrl,
                    apkSizeBytes = apkSize,
                    publishedAt = root.optString("published_at", "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** يقارن رقم الإصدار الحالي المُثبَّت بوسم (tag) الإصدار الأحدث على GitHub. */
    fun isNewerVersion(currentVersionName: String, latestTag: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V")
            .split(".", "-").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val current = parts(currentVersionName)
        val latest = parts(latestTag)
        val maxLen = maxOf(current.size, latest.size)
        for (i in 0 until maxLen) {
            val c = current.getOrElse(i) { 0 }
            val l = latest.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    /**
     * ينزّل ملف الـ APK إلى المجلد الخاص بالتطبيق (لا يحتاج أي صلاحية تخزين)
     * مع إبلاغ تقدّم التنزيل تدريجياً عبر [onProgress]، ثم يعيد الملف النهائي.
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext Result.failure(IllegalStateException("تنزيل فشل: HTTP $code"))
            }
            val totalBytes = connection.contentLengthLong
            val outDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            val outFile = File(outDir, "update_latest.apk")

            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var lastReportedPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else -1
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(percent, downloaded, totalBytes)
                        }
                    }
                }
            }
            connection.disconnect()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** true إن كان بإمكان التطبيق طلب تثبيت حزم من مصادر غير متجر آبل — مطلوب من أندرويد 8+. */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /** يفتح شاشة إعدادات النظام للسماح بتثبيت تطبيقات من هذا المصدر (مطلوبة مرة واحدة فقط). */
    fun openUnknownSourcesSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** يطلق نافذة التثبيت الفعلية عبر FileProvider (بلا حاجة لصلاحيات تخزين تقليدية). */
    fun installApk(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

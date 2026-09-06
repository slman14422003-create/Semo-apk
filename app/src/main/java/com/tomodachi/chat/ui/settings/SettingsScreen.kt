package com.tomodachi.chat.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.data.remote.AppUpdateChecker
import com.tomodachi.chat.ui.chat.MessageBubblePreview
import com.tomodachi.chat.ui.theme.ACCENT_COLOR_CHOICES
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.ui.theme.BubbleShapeStyle
import com.tomodachi.chat.util.parseHexColor
import kotlinx.coroutines.launch

/**
 * شاشة إعدادات مستقلة تماماً عن الملف الشخصي — تجمع كل تفضيلات التطبيق
 * العامة (لا علاقة لها بهوية المستخدم أو بياناته على Firestore) في مكان
 * واحد يشبه إعدادات أي تطبيق تواصل حديث: المظهر (الوضع الداكن/الفاتح، لون
 * العلامة، شكل فقاعات الدردشة، حجم الخط)، وقسم "تحديث التطبيق" الجديد الذي
 * يجلب أحدث إصدار من GitHub Releases ويثبّته مباشرة دون متصفح.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkModePref: String,
    onDarkModePrefChanged: (String) -> Unit,
    accentColorHex: String,
    onAccentColorChanged: (String) -> Unit,
    bubbleShapePref: String,
    onBubbleShapeChanged: (String) -> Unit,
    fontScalePref: Float,
    onFontScaleChanged: (Float) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("الإعدادات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "رجوع", modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionCard {
                SettingsSectionHeader(icon = Icons.Filled.DarkMode, title = "المظهر العام")
                Spacer(Modifier.height(12.dp))
                Text("وضع الإضاءة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                SegmentedChoice(
                    options = listOf("system" to "النظام", "light" to "فاتح", "dark" to "داكن"),
                    selected = darkModePref,
                    onSelected = onDarkModePrefChanged
                )

                Spacer(Modifier.height(18.dp))
                Text("لون العلامة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENT_COLOR_CHOICES.forEach { (hex, label) ->
                        val isSelected = hex == accentColorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hex.isBlank()) Brush.linearGradient(BrandGradient)
                                    else Brush.linearGradient(listOf(parseHexColor(hex), parseHexColor(hex)))
                                )
                                .then(
                                    if (isSelected) Modifier.shadow(4.dp, CircleShape) else Modifier
                                )
                                .clickable { onAccentColorChanged(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SettingsSectionCard {
                SettingsSectionHeader(icon = Icons.Filled.ChatBubble, title = "شكل فقاعات الدردشة")
                Spacer(Modifier.height(12.dp))
                BubbleShapeStyle.entries.forEach { style ->
                    val selected = style.id == bubbleShapePref
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onBubbleShapeChanged(style.id) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MessageBubblePreview(style = style, selected = selected)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            style.labelAr,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        RadioButton(selected = selected, onClick = { onBubbleShapeChanged(style.id) })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SettingsSectionCard {
                SettingsSectionHeader(icon = Icons.Filled.FormatSize, title = "حجم خط الرسائل")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = fontScalePref,
                        onValueChange = onFontScaleChanged,
                        valueRange = 0.85f..1.35f,
                        steps = 4,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )
                    Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "مثال: هذه رسالتك ستظهر بهذا الحجم تقريباً",
                    fontSize = (16 * fontScalePref).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            UpdateSection(
                onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** قسم "تحديث التطبيق" — يجلب آخر إصدار من GitHub Releases ويعرض حالة
 * التنزيل والتثبيت خطوة بخطوة، كل هذا بدون فتح أي متصفح خارجي. */
@Composable
private fun UpdateSection(onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var releaseInfo by remember { mutableStateOf<AppUpdateChecker.ReleaseInfo?>(null) }
    var checkedOnce by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadPercent by remember { mutableStateOf(0) }
    var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }

    val currentVersionName = remember { getAppVersionName(context) }

    fun checkForUpdates() {
        isChecking = true
        scope.launch {
            AppUpdateChecker.fetchLatestRelease()
                .onSuccess { releaseInfo = it; checkedOnce = true }
                .onFailure {
                    checkedOnce = true
                    releaseInfo = null
                    onError("تعذّر التحقق من التحديثات — تحقق من الاتصال بالإنترنت")
                }
            isChecking = false
        }
    }

    fun startDownload(url: String) {
        isDownloading = true
        downloadPercent = 0
        scope.launch {
            AppUpdateChecker.downloadApk(context, url) { percent, _, _ ->
                if (percent >= 0) downloadPercent = percent
            }.onSuccess { file ->
                downloadedFile = file
                isDownloading = false
                if (AppUpdateChecker.canInstallPackages(context)) {
                    AppUpdateChecker.installApk(context, file)
                } else {
                    AppUpdateChecker.openUnknownSourcesSettings(context)
                }
            }.onFailure {
                isDownloading = false
                onError("فشل تنزيل التحديث — حاول مرة أخرى")
            }
        }
    }

    SettingsSectionCard {
        SettingsSectionHeader(icon = Icons.Filled.SystemUpdate, title = "تحديث التطبيق")
        Spacer(Modifier.height(10.dp))
        Text(
            "الإصدار الحالي: $currentVersionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        when {
            isDownloading -> {
                LinearProgressIndicator(
                    progress = { downloadPercent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                )
                Spacer(Modifier.height(6.dp))
                Text("جارٍ تنزيل التحديث… $downloadPercent%", style = MaterialTheme.typography.labelMedium)
            }

            releaseInfo != null && AppUpdateChecker.isNewerVersion(currentVersionName, releaseInfo!!.tagName) -> {
                val info = releaseInfo!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("يتوفّر إصدار جديد: ${info.displayName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        info.notes.take(220),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val url = info.apkDownloadUrl
                        if (url != null) startDownload(url) else onError("لا يوجد ملف APK مرفق بهذا الإصدار")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تنزيل وتثبيت التحديث")
                }
            }

            checkedOnce -> {
                Text("أنت تستخدم أحدث إصدار من التطبيق ✔", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { checkForUpdates() },
                    enabled = !isChecking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("التحقق مرة أخرى")
                }
            }

            else -> {
                Button(
                    onClick = { checkForUpdates() },
                    enabled = !isChecking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("جارٍ التحقق…")
                    } else {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("التحقق من التحديثات")
                    }
                }
            }
        }
    }
}

private fun getAppVersionName(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "1.0.0"
    }
}

@Composable
private fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SegmentedChoice(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.tomodachi.chat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = IgBlue,
    onPrimary = Color.White,
    primaryContainer = LightFieldFill,
    onPrimaryContainer = LightOnSurface,
    secondary = GradientPurple,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightFieldFill,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightDivider,
    error = GradientRed,
    errorContainer = Color(0xFFFFE8E8),
    onErrorContainer = GradientRed
)

private val DarkColors = darkColorScheme(
    primary = IgBlueDark,
    onPrimary = Color.Black,
    primaryContainer = DarkFieldFill,
    onPrimaryContainer = DarkOnSurface,
    secondary = GradientPurple,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkFieldFill,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkDivider,
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFF6B6B)
)

/** لوحة ألوان علامة اختيارية يمكن للمستخدم اختيارها من شاشة الإعدادات
 * الجديدة بدل لون انستقرام الأزرق الثابت — كل قيمة هنا لون "primary" فعلي. */
val ACCENT_COLOR_CHOICES = listOf(
    "" to "الافتراضي", // فارغ = يبقى IgBlue/IgBlueDark الأصلي
    "#0095F6" to "أزرق",
    "#833AB4" to "بنفسجي",
    "#FD1D8D" to "وردي",
    "#25D366" to "أخضر",
    "#FF6F61" to "مرجاني",
    "#FCAF45" to "ذهبي",
    "#2C3E50" to "كحلي"
)

/**
 * وضع داكن/فاتح قابل للتبديل الحر من داخل التطبيق (شاشة الإعدادات)، ولا
 * يعتمد فقط على إعدادات النظام كما يوضّح الباراميتر darkModePref. كما يقبل
 * الآن [accentColorHex] اختيارياً لتخصيص لون العلامة الرئيسي بدل الأزرق
 * الثابت — إن كان فارغاً يُستخدم التدرّج الافتراضي كما كان دوماً.
 */
@Composable
fun TomodachiTheme(
    darkModePref: String = "system", // "system" | "dark" | "light"
    accentColorHex: String = "",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkModePref) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val baseColors = if (useDark) DarkColors else LightColors
    val accent = accentColorHex.takeIf { it.isNotBlank() }?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    }
    val colors = if (accent != null) {
        baseColors.copy(primary = accent, secondary = accent)
    } else {
        baseColors
    }

    // يضبط لون أيقونات شريط الحالة/التنقل السفلي (فاتحة على خلفية داكنة، أو
    // داكنة على خلفية فاتحة) في كل مرة يتغيّر فيها الوضع — سواء تلقائياً من
    // النظام أو يدوياً من شاشة الملف الشخصي. هذا هو ما يجعل شريط الحالة
    // متناسقاً بصرياً مع شريط التطبيق بدل أن يبقى بمظهر النظام الافتراضي.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !useDark
                insetsController.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = TomodachiTypography,
        shapes = TomodachiShapes,
        content = content
    )
}

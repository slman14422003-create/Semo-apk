package com.tomodachi.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

/**
 * وضع داكن/فاتح قابل للتبديل الحر من داخل التطبيق (شاشة الملف الشخصي)،
 * ولا يعتمد فقط على إعدادات النظام كما يوضّح الباراميتر forceDarkMode.
 */
@Composable
fun TomodachiTheme(
    darkModePref: String = "system", // "system" | "dark" | "light"
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkModePref) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = TomodachiTypography,
        shapes = TomodachiShapes,
        content = content
    )
}

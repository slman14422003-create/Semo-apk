package com.tomodachi.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TomodachiPrimaryLight,
    secondary = TomodachiSecondary,
    background = LightBackground,
    surface = LightSurface
)

private val DarkColors = darkColorScheme(
    primary = TomodachiPrimaryDark,
    secondary = TomodachiSecondary,
    background = DarkBackground,
    surface = DarkSurface
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
        content = content
    )
}

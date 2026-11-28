package com.example.miniproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

val MoneeYellow = Color(0xFFFBD24D)
val MoneeYellowSoft = Color(0xFFFFF3C4)
val MoneeCream = Color(0xFFFFFBF2)
val MoneeInk = Color(0xFF2D2D2D)
val MoneeBorder = Color(0xFFF3E7B5)
val MoneeMuted = Color(0xFF7A7466)
val MoneeRed = Color(0xFFE5534B)
val MoneeGreen = Color(0xFF6BAF5B)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class ThemeController(
    val mode: ThemeMode,
    val setMode: (ThemeMode) -> Unit,
)

val LocalThemeController =
    staticCompositionLocalOf { ThemeController(ThemeMode.SYSTEM) {} }

private val MoneeLightColorScheme = lightColorScheme(
    primary = MoneeYellow,
    onPrimary = MoneeInk,
    primaryContainer = MoneeYellowSoft,
    onPrimaryContainer = MoneeInk,
    secondary = MoneeYellow,
    onSecondary = MoneeInk,
    secondaryContainer = MoneeYellowSoft,
    onSecondaryContainer = MoneeInk,
    tertiary = MoneeGreen,
    background = MoneeCream,
    onBackground = MoneeInk,
    surface = Color.White,
    onSurface = MoneeInk,
    surfaceVariant = MoneeYellowSoft,
    onSurfaceVariant = MoneeMuted,
    outline = MoneeBorder,
    outlineVariant = MoneeBorder,
    error = MoneeRed
)

private val MoneeDarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = Color(0xFF5A4B16),
    onPrimaryContainer = Color(0xFFFFF1A8),
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    tertiary = MoneeGreen,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = tertiaryDark,
    onSurfaceVariant = onTertiaryDark,
    outline = outlineDark,
    outlineVariant = outlineDark,
    error = Color(0xFFFF8A82),
)

@Composable
fun MiniProjectTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    MaterialTheme(
        colorScheme = if (useDarkTheme) MoneeDarkColorScheme else MoneeLightColorScheme,
        typography = Typography,
        content = content
    )
}

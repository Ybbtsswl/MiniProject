package com.example.miniproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MoneeYellow = Color(0xFFFBD24D)
val MoneeYellowSoft = Color(0xFFFFF3C4)
val MoneeCream = Color(0xFFFFFBF2)
val MoneeInk = Color(0xFF2D2D2D)
val MoneeBorder = Color(0xFFF3E7B5)
val MoneeMuted = Color(0xFF7A7466)
val MoneeRed = Color(0xFFE5534B)
val MoneeGreen = Color(0xFF6BAF5B)

private val MoneeColorScheme = lightColorScheme(
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

@Composable
fun MiniProjectTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MoneeColorScheme,
        typography = Typography,
        content = content
    )
}
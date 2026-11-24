package com.example.miniproject.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Extra colours that sit on top of the base Monee palette in Theme.kt
val MoneeAmber = Color(0xFFB8860B)        // readable gold for small text on white / cream
val MoneeYellowLight = Color(0xFFFFE866)

val MoneeScreenBrush: Brush = Brush.verticalGradient(
    0f to MoneeCream,
    0.25f to Color.White,
    1f to Color.White
)

// Hero cards (totals, summaries)
val MoneeHeroBrush: Brush = Brush.linearGradient(
    listOf(MoneeYellowSoft, Color(0xFFFFE27A))
)

// Primary buttons / selected chips
val MoneeButtonBrush: Brush = Brush.horizontalGradient(
    listOf(MoneeYellowLight, MoneeYellow)
)

// "Settle" style confirmation buttons
val MoneeGreenBrush: Brush = Brush.horizontalGradient(
    listOf(Color(0xFF8CCB7C), MoneeGreen)
)

val MoneeCardBorder = BorderStroke(1.dp, MoneeBorder)

fun Modifier.moneeBackground(): Modifier = background(MoneeScreenBrush)

fun Modifier.moneeHero(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    clip(shape).background(MoneeHeroBrush)

// ---------- reusable pieces ----------
@Composable
fun moneeCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
fun moneeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MoneeYellow,
    unfocusedBorderColor = MoneeBorder,
    focusedLabelColor = MoneeAmber,
    unfocusedLabelColor = MoneeMuted,
    cursorColor = MoneeAmber,
    focusedTextColor = MoneeInk,
    unfocusedTextColor = MoneeInk
)

@Composable
fun MoneeGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    brush: Brush = MoneeButtonBrush,
    contentColor: Color = MoneeInk,
    shape: Shape = RoundedCornerShape(50.dp),
    content: @Composable RowScope.() -> Unit
) {
    val look = if (enabled) Modifier.background(brush, shape)
    else Modifier.background(MoneeBorder.copy(alpha = 0.6f), shape)

    Row(
        modifier = modifier
            .clip(shape)
            .then(look)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (if (enabled) contentColor else MoneeMuted)
        ) {
            content()
        }
    }
}

@Composable
fun MoneeChip(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable RowScope.() -> Unit
) {
    val look = if (selected) Modifier.background(MoneeButtonBrush, shape)
    else Modifier
        .background(MaterialTheme.colorScheme.surface, shape)
        .border(1.dp, MoneeBorder, shape)

    Row(
        modifier = modifier
            .clip(shape)
            .then(look)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (if (selected) MoneeInk else MoneeMuted)
        ) {
            content()
        }
    }
}

@Composable
fun moneeTopInsets(): WindowInsets =
    WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
// ---------- flat style used by Split Bill / Add Transaction ----------
val MoneePageBg = Color(0xFFF5F7FC)
val MoneeFieldBg = Color(0xFFEBF0FA)

fun Modifier.moneeFlatBackground(): Modifier = background(MoneePageBg)

/** White rounded card that holds one section of a form. */
@Composable
fun MoneeSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        content = content
    )
}

/** Borderless, light-blue filled text field (yellow outline when focused). */
@Composable
fun moneeFilledFieldColors(container: Color = MoneeFieldBg) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = container,
    unfocusedContainerColor = container,
    focusedBorderColor = MoneeYellow,
    unfocusedBorderColor = Color.Transparent,
    focusedLabelColor = MoneeAmber,
    unfocusedLabelColor = MoneeMuted,
    cursorColor = MoneeAmber,
    focusedTextColor = MoneeInk,
    unfocusedTextColor = MoneeInk,
    focusedPlaceholderColor = MoneeMuted.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = MoneeMuted.copy(alpha = 0.6f)
)

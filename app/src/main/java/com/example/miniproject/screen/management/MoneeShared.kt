/*
 * ============================================================================
 * Monee — Shared UI Theme, Formatting Helpers & Components
 * File: MoneeShared.kt
 * ============================================================================
 * Place all 4 files (MoneeShared.kt, BudgetManagement.kt, SavingsManagement.kt,
 * FinancialReport.kt) inside the same package: com.monee.app
 * ============================================================================
 */
package com.example.miniproject.screen.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.MoneeYellowLight
import com.example.miniproject.ui.theme.MoneeYellowSoft
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ============================================================================
// 1. DESIGN TOKENS & YELLOW/GOLD THEME PALETTE
// ============================================================================
object MoneeColors {
    val BrandYellow = MoneeYellowLight
    val BrandGold = MoneeYellow
    val BrandDeep = Color(0xFFE2B72F)
    val BrandDarkGold = Color(0xFFA16207)
    val BrandSoft = MoneeYellowSoft
    val BrandLight = Color(0xFFFFFBEB)
    val BrandBadgeBg = MoneeYellowSoft

    val SurfaceBg = Color(0xFFF5F7FA)
    val CardWhite = Color(0xFFFFFFFF)
    val InkDark = Color(0xFF0F172A)
    val Slate700 = Color(0xFF334155)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)

    val WarningOrange = Color(0xFFF97316)
    val WarningBg = Color(0xFFFFF7ED)
    val WarningBorder = Color(0xFFFED7AA)
    val WarningText = Color(0xFF9A3412)

    val DangerRed = Color(0xFFEF4444)
    val DangerBg = Color(0xFFFEF2F2)
    val DangerBorder = Color(0xFFFECACA)
    val DangerText = Color(0xFF991B1B)

    val InfoBlue = Color(0xFF3B82F6)
    val InfoBg = Color(0xFFEFF6FF)
    val InfoBorder = Color(0xFFBFDBFE)
    val InfoText = Color(0xFF1E3A8A)
}

val CategoryColorsMap: Map<String, Color> =
    mapOf(
        "Food & Dining" to Color(0xFFEAB308),
        "Transportation" to Color(0xFF3B82F6),
        "Bills & Utilities" to Color(0xFFCA8A04),
        "Entertainment" to Color(0xFFEF4444),
        "Shopping" to Color(0xFF8B5CF6),
        "Education" to Color(0xFFFACC15),
        "Healthcare" to Color(0xFFEC4899),
    )

// ============================================================================
// 2. SHARED FORMATTING HELPERS
// ============================================================================
fun formatRM(amount: Double): String {
    val nf =
        NumberFormat.getNumberInstance(Locale("en", "MY")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    return "RM ${nf.format(amount)}"
}

fun formatShortDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    } catch (_: Exception) {
        isoDate
    }
}

fun formatShortDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}

fun formatDateTime(dt: LocalDateTime): String {
    return dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH))
}

// ============================================================================
// 3. SHARED REUSABLE UI COMPONENTS
// ============================================================================
@Composable
fun KpiCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MoneeColors.CardWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MoneeColors.Slate100),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                fontSize = 11.sp,
                color = MoneeColors.Slate400,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
fun InsightCard(title: String, body: String, bg: Color, border: Color, titleColor: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(3.dp))
            Text(body, fontSize = 11.sp, color = MoneeColors.Slate700, lineHeight = 16.sp)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 13.sp, color = MoneeColors.Slate700) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MoneeColors.DangerRed,
                        contentColor = Color.White,
                    ),
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MoneeColors.Slate700) }
        },
    )
}

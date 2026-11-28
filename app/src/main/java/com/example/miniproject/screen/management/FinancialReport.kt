/*
 * ============================================================================
 * Module 3: Financial Analytics & Reports (FR-FAR-001 to FR-FAR-007)
 * File: FinancialReport.kt
 * ============================================================================
 */
package com.example.miniproject.screen.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.launch

// ============================================================================
// 1. FINANCIAL REPORT DATA MODELS (MATCHING UML CLASS DIAGRAM)
// ============================================================================
enum class AnalyticsPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

/**
 * UML Entity: Financial_Report +String reportId, +String userId, +String reportType, +date
 * startDate, +date endDate, +double totalIncome, +double totalExpenses, +double totalSavings,
 * +datetime generatedAt, +String? file_path = null
 */
data class FinancialReport(
    val reportId: String,
    val userId: String,
    val reportType: String, // "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalIncome: Double,
    val totalExpenses: Double,
    val totalSavings: Double,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val file_path: String? = null,
)

data class CategoryComparison(val category: String, val budget: Double, val actual: Double)

data class AnalyticsDataset(
    val periodLabel: String,
    val income: Double,
    val expenses: Double,
    val savings: Double,
    val budgetUtil: Double,
    val changePct: String,
    val categories: List<CategoryComparison>,
    val barLabels: List<String>,
    val barIncome: List<Double>,
    val barExpense: List<Double>,
    val trendLabels: List<String>,
    val trendExpense: List<Double>,
    val trendSavings: List<Double>,
)

// ============================================================================
// 2. FINANCIAL REPORT VIEWMODEL
// ============================================================================
// ============================================================================
// 3. ANALYTICS & REPORTS SCREEN + CHARTS + REPORT MODAL
// ============================================================================
@Composable
fun AnalyticsReportsScreen(
    vm: FinancialReportViewModel,
    budgets: List<Budget>,
    savingsGoals: List<SavingGoal>,
    showMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching {
                            val report = requireNotNull(vm.generatedReport)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                java.io.File(requireNotNull(report.file_path)).inputStream().use {
                                    input ->
                                    requireNotNull(context.contentResolver.openOutputStream(uri))
                                        .use { output -> input.copyTo(output) }
                                }
                            }
                            vm.repository
                                .updateFilePath(report.userId, report.reportId, uri.toString())
                                .getOrThrow()
                        }
                        .onSuccess { showMessage("Report downloaded.") }
                        .onFailure { showMessage(it.message ?: "Unable to download report.") }
                }
        }
    val data = vm.getAnalyticsDataset(budgets)
    val sortedCats = remember(data) { data.categories.sortedByDescending { it.actual } }
    val topCat = sortedCats.firstOrNull() ?: CategoryComparison("Food & Dining", 0.0, 0.0)
    val overCats = remember(data) { data.categories.filter { it.actual > it.budget } }
    val savingsRate = if (data.income > 0) (data.savings / data.income) * 100.0 else 0.0

    var monthMenuExpanded by remember { mutableStateOf(false) }
    var showGeneratedReportModal by remember { mutableStateOf(false) }
    var pdfExportedBanner by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    color = MoneeColors.CardWhite,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MoneeColors.Slate100),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MoneeColors.Slate100)
                                    .padding(4.dp)
                        ) {
                            AnalyticsPeriod.values().forEach { p ->
                                val active = vm.analyticsPeriod == p
                                Box(
                                    modifier =
                                        Modifier.weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(
                                                if (active) MoneeColors.CardWhite
                                                else Color.Transparent
                                            )
                                            .clickable { vm.selectPeriod(p) }
                                            .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        p.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            if (active) MoneeColors.BrandDarkGold
                                            else MoneeColors.Slate500,
                                    )
                                }
                            }
                        }

                        Box {
                            val monthLabel = "Month: ${vm.selectedMonth}"
                            OutlinedTextField(
                                value = monthLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { monthMenuExpanded = true }) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth().clickable { monthMenuExpanded = true },
                            )
                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false },
                            ) {
                                (0L..11L)
                                    .map {
                                        java.time.YearMonth.now().minusMonths(it).let { month ->
                                            month.toString() to month.toString()
                                        }
                                    }
                                    .forEach { (code, title) ->
                                        DropdownMenuItem(
                                            text = { Text("Month: $title") },
                                            onClick = {
                                                vm.updateMonthSelection(code)
                                                monthMenuExpanded = false
                                            },
                                        )
                                    }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vm.reportStartDate,
                                onValueChange = { vm.updateCustomDateRange(it, vm.reportEndDate) },
                                label = { Text("Start Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = vm.reportEndDate,
                                onValueChange = {
                                    vm.updateCustomDateRange(vm.reportStartDate, it)
                                },
                                label = { Text("End Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        vm.createFinancialReport(budgets)
                                            .onSuccess {
                                                pdfExportedBanner = null
                                                showGeneratedReportModal = true
                                            }
                                            .onFailure { err ->
                                                showMessage(err.message ?: "Invalid date range.")
                                            }
                                    } catch (error: Exception) {
                                        showMessage(error.message ?: "Unable to save changes.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MoneeColors.BrandYellow,
                                    contentColor = MoneeColors.InkDark,
                                ),
                        ) {
                            Text("Generate Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            "Total Income",
                            formatRM(data.income),
                            MoneeColors.InkDark,
                            Modifier.weight(1f),
                        )
                        KpiCard(
                            "Total Expenses",
                            formatRM(data.expenses),
                            MoneeColors.DangerRed,
                            Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            "Net Savings",
                            formatRM(data.savings),
                            MoneeColors.BrandDarkGold,
                            Modifier.weight(1f),
                        )
                        KpiCard(
                            "Budget Util.",
                            String.format(Locale.US, "%.1f%%", data.budgetUtil),
                            if (data.budgetUtil >= 100) MoneeColors.DangerRed
                            else MoneeColors.InkDark,
                            Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MoneeColors.CardWhite,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MoneeColors.Slate100),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Expense Distribution by Category",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.InkDark,
                                )
                                Text(
                                    "Highest: ${topCat.category} (${formatRM(topCat.actual)})",
                                    fontSize = 11.sp,
                                    color = MoneeColors.Slate400,
                                )
                            }
                            Surface(color = MoneeColors.WarningBg, shape = RoundedCornerShape(50)) {
                                Text(
                                    "${data.changePct} vs prev",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.WarningText,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ComposeDonutChart(categories = data.categories)
                    }
                }
            }

            item {
                Surface(
                    color = MoneeColors.CardWhite,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MoneeColors.Slate100),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Income vs Expenses Comparison",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeColors.InkDark,
                        )
                        Spacer(Modifier.height(12.dp))
                        ComposeBarChart(
                            labels = data.barLabels,
                            income = data.barIncome,
                            expenses = data.barExpense,
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MoneeColors.CardWhite,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MoneeColors.Slate100),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Spending & Savings Trends Over Time",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeColors.InkDark,
                        )
                        Spacer(Modifier.height(12.dp))
                        ComposeTrendLineChart(
                            labels = data.trendLabels,
                            expenses = data.trendExpense,
                            savings = data.trendSavings,
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MoneeColors.CardWhite,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MoneeColors.Slate100),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Budget Allocation vs Actual Spending",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeColors.InkDark,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(
                                "Category",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate400,
                                modifier = Modifier.weight(1.4f),
                            )
                            Text(
                                "Budget",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate400,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Actual",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate400,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Var.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate400,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                        HorizontalDivider(color = MoneeColors.Slate100)
                        data.categories.forEach { c ->
                            val diff = c.budget - c.actual
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                Text(
                                    c.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MoneeColors.Slate700,
                                    modifier = Modifier.weight(1.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    String.format(Locale.US, "%.0f", c.budget),
                                    fontSize = 11.sp,
                                    color = MoneeColors.Slate500,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    String.format(Locale.US, "%.0f", c.actual),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.InkDark,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${if (diff < 0) "-" else "+"}${abs(diff).toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (diff < 0) MoneeColors.DangerRed
                                        else MoneeColors.BrandDarkGold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.8f),
                                )
                            }
                            HorizontalDivider(color = MoneeColors.Slate100.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "FINANCIAL INSIGHTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.Slate500,
                    )
                    if (overCats.isNotEmpty()) {
                        InsightCard(
                            title = "Overspending Detected",
                            body =
                                overCats.joinToString("; ") {
                                    "${it.category} exceeded budget by ${formatRM(it.actual - it.budget)}"
                                },
                            bg = MoneeColors.DangerBg,
                            border = MoneeColors.DangerBorder,
                            titleColor = MoneeColors.DangerText,
                        )
                    }
                    InsightCard(
                        title =
                            "Healthy Savings Rate (${String.format(Locale.US, "%.1f%%", savingsRate)})",
                        body =
                            "You retained ${formatRM(data.savings)} from your ${formatRM(data.income)} income this period.",
                        bg = MoneeColors.BrandLight,
                        border = MoneeColors.BrandSoft,
                        titleColor = MoneeColors.InkDark,
                    )
                    InsightCard(
                        title = "Actionable Suggestion",
                        body =
                            "${topCat.category} is your top expense (${formatRM(topCat.actual)}). Trimming 10% could add ${formatRM(topCat.actual * 0.1)} to your Emergency Fund!",
                        bg = MoneeColors.InfoBg,
                        border = MoneeColors.InfoBorder,
                        titleColor = MoneeColors.InfoText,
                    )
                }
            }
        }
    }

    if (showGeneratedReportModal) {
        val activeReport = vm.generatedReport
        if (activeReport != null) {
            GeneratedReportPopupModal(
                report = activeReport,
                data = data,
                savingsGoals = savingsGoals,
                savingsRate = savingsRate,
                topCat = topCat,
                overCats = overCats,
                exportedBanner = pdfExportedBanner,
                onDismiss = { showGeneratedReportModal = false },
                onExportPdf = {
                    scope.launch {
                        try {
                            vm.exportFinancialReportToPdf(context, budgets, savingsGoals)
                                .onSuccess { msg ->
                                    pdfExportedBanner = msg
                                    downloadLauncher.launch(
                                        "Monee_Report_${vm.generatedReport?.reportId}.pdf"
                                    )
                                }
                                .onFailure { err ->
                                    showMessage("PDF export failed: ${err.message}")
                                }
                        } catch (error: Exception) {
                            showMessage(error.message ?: "Unable to save changes.")
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun GeneratedReportPopupModal(
    report: FinancialReport,
    data: AnalyticsDataset,
    savingsGoals: List<SavingGoal>,
    savingsRate: Double,
    topCat: CategoryComparison,
    overCats: List<CategoryComparison>,
    exportedBanner: String?,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val totalCatSpend = data.categories.sumOf { it.actual }.coerceAtLeast(1.0)
    val sortedCats = data.categories.sortedByDescending { it.actual }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MoneeColors.CardWhite,
            modifier = Modifier.fillMaxWidth(0.93f).fillMaxHeight(0.88f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Generated Financial Report",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MoneeColors.InkDark,
                        )
                        Text(
                            "Review before exporting",
                            fontSize = 11.sp,
                            color = MoneeColors.Slate400,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier =
                            Modifier.size(34.dp).clip(CircleShape).background(MoneeColors.Slate100),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Report",
                            tint = MoneeColors.Slate700,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                HorizontalDivider(color = MoneeColors.Slate100)

                Column(
                    modifier =
                        Modifier.weight(1f)
                            .background(MoneeColors.SurfaceBg)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MoneeColors.BrandSoft,
                                            MoneeColors.BrandYellow,
                                            MoneeColors.BrandGold,
                                        )
                                    )
                                )
                                .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Monee Financial Report",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoneeColors.InkDark,
                                )
                                Surface(
                                    color = MoneeColors.InkDark.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        report.reportType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneeColors.InkDark,
                                        modifier =
                                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                "${formatShortDate(report.startDate)} – ${formatShortDate(report.endDate)}",
                                fontSize = 11.sp,
                                color = MoneeColors.InkDark,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Report ID: ${report.reportId} · Generated: ${formatDateTime(report.generatedAt)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MoneeColors.InkDark.copy(alpha = 0.8f),
                            )
                        }
                    }

                    if (exportedBanner != null) {
                        Surface(
                            color = MoneeColors.BrandLight,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MoneeColors.BrandGold),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MoneeColors.BrandDarkGold,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        exportedBanner,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneeColors.InkDark,
                                    )
                                    report.file_path?.let { path ->
                                        Text(
                                            path,
                                            fontSize = 9.sp,
                                            color = MoneeColors.Slate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            "Total Income",
                            formatRM(report.totalIncome),
                            MoneeColors.InkDark,
                            Modifier.weight(1f),
                        )
                        KpiCard(
                            "Total Expenses",
                            formatRM(report.totalExpenses),
                            MoneeColors.DangerRed,
                            Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            "Total Savings",
                            formatRM(report.totalSavings),
                            MoneeColors.BrandDarkGold,
                            Modifier.weight(1f),
                        )
                        KpiCard(
                            "Savings Rate",
                            String.format(Locale.US, "%.1f%%", savingsRate),
                            MoneeColors.InkDark,
                            Modifier.weight(1f),
                        )
                    }

                    Surface(color = MoneeColors.CardWhite, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Expense Breakdown by Category",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark,
                            )
                            sortedCats.forEach { c ->
                                val pct = (c.actual / totalCatSpend) * 100.0
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            c.category,
                                            fontSize = 11.sp,
                                            color = MoneeColors.Slate700,
                                        )
                                        Text(
                                            "${formatRM(c.actual)} (${String.format(Locale.US, "%.1f%%", pct)})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat() },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(50)),
                                        color =
                                            CategoryColorsMap[c.category] ?: MoneeColors.BrandGold,
                                        trackColor = MoneeColors.Slate100,
                                    )
                                }
                            }
                        }
                    }

                    Surface(color = MoneeColors.CardWhite, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Savings Goals Progress",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark,
                            )
                            savingsGoals.forEach { g ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(g.goalName, fontSize = 11.sp, color = MoneeColors.Slate700)
                                    Text(
                                        "${g.progressPercentage.toInt()}% (${g.status})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            if (g.status == "EXPIRED") MoneeColors.DangerRed
                                            else MoneeColors.BrandDarkGold,
                                    )
                                }
                            }
                        }
                    }

                    InsightCard(
                        title = "Financial Insights & Recommendations",
                        body =
                            buildString {
                                if (overCats.isNotEmpty()) {
                                    append(
                                        "• Overspending: ${overCats.joinToString { "${it.category} (+${formatRM(it.actual - it.budget)})" }}\n"
                                    )
                                }
                                append(
                                    "• Top Category: ${topCat.category} (${formatRM(topCat.actual)}).\n"
                                )
                                append(
                                    "• Overall: ${String.format(Locale.US, "%.1f%%", savingsRate)} savings rate."
                                )
                            },
                        bg = MoneeColors.BrandLight,
                        border = MoneeColors.BrandSoft,
                        titleColor = MoneeColors.InkDark,
                    )
                }

                HorizontalDivider(color = MoneeColors.Slate100)
                Box(
                    modifier =
                        Modifier.fillMaxWidth().background(MoneeColors.CardWhite).padding(14.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MoneeColors.BrandYellow,
                                contentColor = MoneeColors.InkDark,
                            ),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export to PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. COMPOSE CANVAS CHARTS
// ============================================================================
@Composable
fun ComposeDonutChart(categories: List<CategoryComparison>) {
    val total = categories.sumOf { it.actual }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            var startAngle = -90f
            val strokeWidth = 28.dp.toPx()
            categories.forEach { c ->
                val sweep = ((c.actual / total) * 360.0).toFloat()
                drawArc(
                    color = CategoryColorsMap[c.category] ?: MoneeColors.BrandGold,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            categories.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(10.dp)
                                .clip(CircleShape)
                                .background(CategoryColorsMap[c.category] ?: MoneeColors.BrandGold)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        c.category,
                        fontSize = 11.sp,
                        color = MoneeColors.Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${((c.actual / total) * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.InkDark,
                    )
                }
            }
        }
    }
}

@Composable
fun ComposeBarChart(labels: List<String>, income: List<Double>, expenses: List<Double>) {
    val maxVal = max(income.maxOrNull() ?: 1.0, expenses.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val groupCount = labels.size.coerceAtLeast(1)
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.28f).coerceAtMost(28.dp.toPx())

            labels.indices.forEach { i ->
                val centerX = groupWidth * i + groupWidth / 2f
                val incH = ((income.getOrElse(i) { 0.0 } / maxVal) * (size.height - 12f)).toFloat()
                val expH =
                    ((expenses.getOrElse(i) { 0.0 } / maxVal) * (size.height - 12f)).toFloat()

                drawRoundRect(
                    color = MoneeColors.BrandGold,
                    topLeft = Offset(centerX - barWidth - 3f, size.height - incH),
                    size = Size(barWidth, incH),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawRoundRect(
                    color = MoneeColors.DangerRed,
                    topLeft = Offset(centerX + 3f, size.height - expH),
                    size = Size(barWidth, expH),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { lbl ->
                Text(
                    lbl,
                    fontSize = 10.sp,
                    color = MoneeColors.Slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).background(MoneeColors.BrandGold, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Income (RM)", fontSize = 10.sp, color = MoneeColors.Slate500)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).background(MoneeColors.DangerRed, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Expenses (RM)", fontSize = 10.sp, color = MoneeColors.Slate500)
            }
        }
    }
}

@Composable
fun ComposeTrendLineChart(labels: List<String>, expenses: List<Double>, savings: List<Double>) {
    val maxVal = max(expenses.maxOrNull() ?: 1.0, savings.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val count = labels.size.coerceAtLeast(2)
            val stepX = size.width / (count - 1)

            fun buildPath(values: List<Double>): Path =
                Path().apply {
                    values.forEachIndexed { idx, v ->
                        val x = idx * stepX
                        val y = size.height - ((v / maxVal) * (size.height - 16f)).toFloat() - 8f
                        if (idx == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

            drawPath(
                buildPath(expenses),
                color = MoneeColors.WarningOrange,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                buildPath(savings),
                color = MoneeColors.BrandDeep,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            expenses.forEachIndexed { idx, v ->
                val x = idx * stepX
                val y = size.height - ((v / maxVal) * (size.height - 16f)).toFloat() - 8f
                drawCircle(
                    color = MoneeColors.WarningOrange,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y),
                )
            }
            savings.forEachIndexed { idx, v ->
                val x = idx * stepX
                val y = size.height - ((v / maxVal) * (size.height - 16f)).toFloat() - 8f
                drawCircle(
                    color = MoneeColors.BrandDeep,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { lbl -> Text(lbl, fontSize = 10.sp, color = MoneeColors.Slate500) }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(MoneeColors.WarningOrange, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Expenses", fontSize = 10.sp, color = MoneeColors.Slate500)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(MoneeColors.BrandDeep, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Savings", fontSize = 10.sp, color = MoneeColors.Slate500)
            }
        }
    }
}

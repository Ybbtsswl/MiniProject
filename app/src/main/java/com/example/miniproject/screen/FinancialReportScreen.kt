package com.example.miniproject.screen

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Expense
import com.example.miniproject.data.FinancialReport
import com.example.miniproject.data.Income
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.repository.FinancialReportRepository
import com.example.miniproject.repository.IncomeRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Screen colors and display helpers.
private val ReportGold = Color(0xFFFFCE45)
private val ReportInk = Color(0xFF282828)
private val ReportMuted = Color(0xFF92908D)
private val ReportGreen = Color(0xFF009E78)

private fun reportMoney(value: Double) = String.format(Locale.US, "RM %,.2f", value)

private fun reportDate(value: String) = runCatching { LocalDate.parse(value.take(10)) }.getOrNull()

private data class CashBucket(val label: String, val income: Double, val expense: Double)

@Composable
fun FinancialReportScreen(userId: String?, onBack: () -> Unit) {
    // Repositories and state for transactions, saved reports, and loading errors.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenseRepository = remember { ExpenseRepository() }
    val incomeRepository = remember { IncomeRepository() }
    val reportRepository = remember { FinancialReportRepository() }
    var history by remember(userId) { mutableStateOf<List<FinancialReport>>(emptyList()) }
    var reportError by remember { mutableStateOf<String?>(null) }
    var expenses by remember(userId) { mutableStateOf<List<Expense>>(emptyList()) }
    var incomes by remember(userId) { mutableStateOf<List<Income>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    // Fetch this user's transactions and saved report history.
    LaunchedEffect(userId, reload) {
        loading = true
        error = null
        if (userId == null) error = "Sign in to view your financial report."
        else {
            val expenseResult = expenseRepository.getExpenses(userId)
            val incomeResult = incomeRepository.getIncome(userId)
            if (expenseResult.isFailure || incomeResult.isFailure)
                error = "Unable to load your transactions. Please retry."
            else {
                expenses = expenseResult.getOrThrow()
                incomes = incomeResult.getOrThrow()
                runCatching { reportRepository.getReports(userId).getOrThrow() }
                    .onSuccess { history = it }
                    .onFailure {
                        reportError = "Unable to load saved reports. Use Refresh to retry."
                    }
            }
        }
        loading = false
    }
    // Date selection defaults to today. Other modes expand it into a date range.
    var mode by rememberSaveable { mutableStateOf("DAILY") }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var customStart by rememberSaveable {
        mutableStateOf(LocalDate.now().withDayOfMonth(1).toString())
    }
    var customEnd by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showDates by remember { mutableStateOf(false) }
    val day = LocalDate.parse(anchor)
    val start =
        when (mode) {
            "DAILY" -> day
            "WEEKLY" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "YEARLY" -> day.withDayOfYear(1)
            "CUSTOM" -> LocalDate.parse(customStart)
            else -> day.withDayOfMonth(1)
        }
    val end =
        when (mode) {
            "DAILY" -> start
            "WEEKLY" -> start.plusDays(6)
            "YEARLY" -> start.plusYears(1).minusDays(1)
            "CUSTOM" -> LocalDate.parse(customEnd)
            else -> start.plusMonths(1).minusDays(1)
        }
    val periodLabel =
        when (mode) {
            "MONTHLY" -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            "YEARLY" -> start.year.toString()
            else -> "$start – $end"
        }
    // Calculate the on-screen totals using only transactions within the period.
    val filteredExpenses =
        expenses.filter { reportDate(it.date)?.let { d -> d in start..end } == true }
    val filteredIncome =
        incomes.filter { reportDate(it.incomeDate)?.let { d -> d in start..end } == true }
    val income = filteredIncome.sumOf { it.amount }
    val spent = filteredExpenses.sumOf { it.amount }
    val net = income - spent
    val categories =
        filteredExpenses
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    // Split the period into days, weeks, or months for the cash-flow bars.
    val buckets = buildList {
        var cursor = start
        var index = 1
        while (!cursor.isAfter(end)) {
            val next =
                when (mode) {
                    "WEEKLY" -> cursor.plusDays(1)
                    "YEARLY" -> cursor.plusMonths(1)
                    else -> cursor.plusDays(7)
                }
            val until = minOf(next.minusDays(1), end)
            val from = cursor
            val label =
                when (mode) {
                    "YEARLY" -> cursor.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
                    "WEEKLY" -> cursor.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US))
                    else ->
                        "Week $index (${cursor.dayOfMonth} ${cursor.month.name.take(3)} – ${until.dayOfMonth} ${until.month.name.take(3)})"
                }
            add(
                CashBucket(
                    label,
                    filteredIncome
                        .filter { reportDate(it.incomeDate)?.let { d -> d in from..until } == true }
                        .sumOf { it.amount },
                    filteredExpenses
                        .filter { reportDate(it.date)?.let { d -> d in from..until } == true }
                        .sumOf { it.amount },
                )
            )
            cursor = next
            index++
        }
    }
    // Build a simple explanation from the selected period's actual results.
    val insight =
        when {
            filteredExpenses.isEmpty() && filteredIncome.isEmpty() ->
                "No transactions in this period. Add income and expenses to start tracking your finances."
            net < 0 ->
                "Expenses exceed income by ${reportMoney(-net)}. Review your largest spending category before planning new purchases."
            categories.isNotEmpty() ->
                "Your largest expense category is ${categories.first().first}. Reducing it by 10% would leave an extra ${reportMoney(categories.first().second * .1)} in your balance."
            else ->
                "You have recorded ${reportMoney(income)} in income with no expenses in this period. Keep recording transactions for a complete picture."
        }
    // PDF export state survives the Android file-picker interaction.
    var exporting by remember { mutableStateOf(false) }
    var exportLines by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var pendingReportId by rememberSaveable { mutableStateOf("") }
    var pendingFilename by rememberSaveable { mutableStateOf("") }
    // Write the PDF to the chosen destination, then record its filename in Supabase.
    val exporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    exporting = true
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            val pdf = PdfDocument()
                            try {
                                val paint =
                                    Paint().apply {
                                        textSize = 12f
                                        color = android.graphics.Color.DKGRAY
                                    }
                                // Wrap long labels so every exported row stays within the page.
                                val wrapped =
                                    exportLines.flatMap { line ->
                                        buildList {
                                            var remaining = line
                                            while (remaining.length > 75) {
                                                val split =
                                                    remaining.lastIndexOf(' ', 75).takeIf { it > 0 }
                                                        ?: 75
                                                add(remaining.take(split))
                                                remaining = remaining.drop(split).trimStart()
                                            }
                                            add(remaining)
                                        }
                                    }
                                wrapped.chunked(45).forEachIndexed { index, lines ->
                                    val page =
                                        pdf.startPage(
                                            PdfDocument.PageInfo.Builder(595, 842, index + 1)
                                                .create()
                                        )
                                    lines.forEachIndexed { row, line ->
                                        page.canvas.drawText(line, 36f, 48f + row * 16f, paint)
                                    }
                                    pdf.finishPage(page)
                                }
                                context.contentResolver.openOutputStream(uri)?.use {
                                    pdf.writeTo(it)
                                } ?: error("Unable to open destination")
                            } finally {
                                pdf.close()
                            }
                        }
                    }
                    if (result.isSuccess && userId != null) {
                        runCatching {
                                reportRepository
                                    .updateFilePath(
                                        userId,
                                        pendingReportId,
                                        "local-download:$pendingFilename",
                                    )
                                    .getOrThrow()
                            }
                            .onFailure {
                                reportError =
                                    "PDF downloaded, but its filename could not sync. The report results are saved; download again from history to retry."
                            }
                        runCatching { reportRepository.getReports(userId).getOrThrow() }
                            .onSuccess { history = it }
                    }
                    exporting = false
                    Toast.makeText(
                            context,
                            if (result.isSuccess) "Financial report downloaded"
                            else "Unable to export report. Download again from saved reports.",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }
        }
    // Export the saved server snapshot, including when downloading from history.
    fun download(report: FinancialReport) {
        pendingReportId = report.reportId
        pendingFilename = "financial-report-${report.startDate}-${report.endDate}.pdf"
        exportLines =
            arrayListOf(
                "FINANCIAL REPORT",
                "${report.startDate} to ${report.endDate}",
                "Type: ${report.reportType}",
                "Generated: ${report.generatedAt}",
                "",
                "Income: ${reportMoney(report.totalIncome)}",
                "Expenses: ${reportMoney(report.totalExpenses)}",
                "Net savings: ${reportMoney(report.totalSavings)}",
                "",
                "Net savings = income minus expenses.",
                "This is a saved snapshot of transactions in the selected period.",
            )
        exporter.launch(pendingFilename)
    }
    // Scrollable screen: period, totals, charts, insight, export, and history.
    Surface(color = Color(0xFFFCFCFC), contentColor = ReportInk) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(44.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        "Analytics & Reports",
                        Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFF2F2F2)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY", "CUSTOM").forEach { option ->
                        TextButton(
                            onClick = {
                                if (option == "CUSTOM") showDates = true else mode = option
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                            colors =
                                ButtonDefaults.textButtonColors(
                                    containerColor =
                                        if (mode == option) ReportInk else Color.Transparent,
                                    contentColor = if (mode == option) Color.White else ReportMuted,
                                ),
                        ) {
                            Text(option, fontSize = 10.sp)
                        }
                    }
                }
            }
            item {
                ReportCard {
                    Text("REPORT PERIOD", fontSize = 10.sp, color = ReportMuted)
                    Text(periodLabel, fontWeight = FontWeight.Bold, color = Color(0xFFB87825))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = {
                                anchor =
                                    when (mode) {
                                        "DAILY" -> day.minusDays(1)
                                        "WEEKLY" -> day.minusWeeks(1)
                                        "YEARLY" -> day.minusYears(1)
                                        else -> day.minusMonths(1)
                                    }.toString()
                            },
                            enabled = mode != "CUSTOM",
                        ) {
                            Text("‹ Previous")
                        }
                        TextButton(onClick = { showDates = true }) { Text("Change") }
                        TextButton(
                            onClick = {
                                anchor =
                                    when (mode) {
                                        "DAILY" -> day.plusDays(1)
                                        "WEEKLY" -> day.plusWeeks(1)
                                        "YEARLY" -> day.plusYears(1)
                                        else -> day.plusMonths(1)
                                    }.toString()
                            },
                            enabled = mode != "CUSTOM",
                        ) {
                            Text("Next ›")
                        }
                    }
                    Text(
                        "Default: today. Change dates before exporting, or use the selected period below.",
                        fontSize = 11.sp,
                        color = ReportMuted,
                    )
                    TextButton(onClick = { reload++ }, enabled = !loading && !exporting) {
                        Text("Refresh")
                    }
                }
            }
            if (loading)
                item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = ReportGold) }
            else if (error != null)
                item {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { reload++ }) { Text("Retry") }
                }
            else {
                item {
                    ReportCard(emphasized = true) {
                        Text("NET COMPARISON", fontSize = 10.sp, color = ReportMuted)
                        Text("$periodLabel Summary", fontWeight = FontWeight.Bold)
                        Text(
                            if (net >= 0) "● Healthy Surplus" else "● Spending Deficit",
                            color = if (net >= 0) ReportGreen else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportMetric(
                                "INCOME",
                                reportMoney(income),
                                "Recorded income",
                                Modifier.weight(1f),
                            )
                            ReportMetric(
                                "EXPENSES",
                                reportMoney(spent),
                                "Recorded spending",
                                Modifier.weight(1f),
                            )
                            ReportMetric(
                                "NET SAVINGS",
                                reportMoney(net),
                                if (income > 0)
                                    String.format(Locale.US, "%.1f%% rate", net / income * 100)
                                else "Rate: N/A",
                                Modifier.weight(1f),
                                true,
                            )
                        }
                        Text(
                            "Net savings = income minus expenses; excludes savings goal deposits.",
                            fontSize = 10.sp,
                            color = ReportMuted,
                        )
                    }
                }
                item {
                    Text("CASH FLOW OVERVIEW", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "● Income (yellow)   ● Expense (dark)",
                        color = ReportMuted,
                        fontSize = 11.sp,
                    )
                }
                val maxCash =
                    buckets.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
                items(buckets, key = { it.label }) { bucket ->
                    ReportCard {
                        Text(bucket.label, fontSize = 12.sp)
                        Text(
                            "${reportMoney(bucket.income)} / ${reportMoney(bucket.expense)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        ReportBar(bucket.income / maxCash, ReportGold)
                        ReportBar(bucket.expense / maxCash, ReportInk)
                    }
                }
                item {
                    Text(
                        "Average weekly surplus: ${reportMoney(net / ((ChronoUnit.DAYS.between(start, end) + 1) / 7.0))} / week",
                        fontSize = 12.sp,
                    )
                }
                item {
                    Text(
                        "SPENDING PATTERNS & TRENDS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "Total outflow: ${reportMoney(spent)}",
                        fontSize = 11.sp,
                        color = ReportMuted,
                    )
                    if (categories.isEmpty())
                        Text(
                            "No expenses recorded for this period.",
                            fontSize = 12.sp,
                            color = ReportMuted,
                        )
                }
                items(categories, key = { it.first }) { (category, amount) ->
                    ReportCard {
                        Text(category, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${reportMoney(amount)} (${if (spent > 0) (amount / spent * 100).toInt() else 0}%)",
                            fontSize = 12.sp,
                        )
                        ReportBar(if (spent > 0) amount / spent else 0.0, ReportGold)
                    }
                }
                item {
                    ReportCard(emphasized = true, yellow = true) {
                        Text(
                            "☀  SMART FINANCIAL INSIGHT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(insight, fontSize = 13.sp)
                    }
                }
                item {
                    Button(
                        onClick = {
                            if (userId != null) {
                                exporting = true
                                reportError = null
                                scope.launch {
                                    runCatching {
                                            reportRepository
                                                .createReport(
                                                    userId,
                                                    mode,
                                                    start.toString(),
                                                    end.toString(),
                                                )
                                                .getOrThrow()
                                        }
                                        .onSuccess { report ->
                                            history = listOf(report) + history
                                            download(report)
                                        }
                                        .onFailure {
                                            reportError =
                                                "Unable to save the report in Supabase. Check your connection and retry."
                                        }
                                    exporting = false
                                }
                            }
                        },
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = ReportGold,
                                contentColor = ReportInk,
                            ),
                    ) {
                        Text(
                            if (exporting) "EXPORTING…" else "EXPORT FINANCIAL REPORT (PDF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        "Save a summary of the selected reporting period.",
                        fontSize = 10.sp,
                        color = ReportMuted,
                    )
                    reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                item { Text("Saved reports", fontWeight = FontWeight.Bold) }
                items(history, key = { it.reportId }) { report ->
                    ReportCard {
                        Text(
                            "${report.startDate} – ${report.endDate}",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Income ${reportMoney(report.totalIncome)} · Expenses ${reportMoney(report.totalExpenses)}",
                            fontSize = 12.sp,
                        )
                        Text("Net savings ${reportMoney(report.totalSavings)}", fontSize = 12.sp)
                        Text(
                            "Generated ${report.generatedAt.take(10)}",
                            fontSize = 10.sp,
                            color = ReportMuted,
                        )
                        TextButton(onClick = { download(report) }, enabled = !exporting) {
                            Text("Download saved result (PDF)")
                        }
                    }
                }
            }
        }
    }
    // Custom date dialog: accept a valid inclusive range of up to 366 days.
    if (showDates) {
        var from by remember { mutableStateOf(start.toString()) }
        var to by remember { mutableStateOf(end.toString()) }
        var dateError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDates = false },
            title = { Text("Custom report period") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        from,
                        { from = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        to,
                        { to = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        singleLine = true,
                    )
                    if (dateError)
                        Text(
                            "Enter valid dates, with the end after the start (maximum 366 days).",
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val a = runCatching { LocalDate.parse(from) }.getOrNull()
                        val b = runCatching { LocalDate.parse(to) }.getOrNull()
                        if (a == null || b == null || ChronoUnit.DAYS.between(a, b) !in 0..365)
                            dateError = true
                        else {
                            customStart = a.toString()
                            customEnd = b.toString()
                            mode = "CUSTOM"
                            showDates = false
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = { TextButton(onClick = { showDates = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReportCard(
    emphasized: Boolean = false,
    yellow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (emphasized) 4.dp else 1.dp,
        border = BorderStroke(1.dp, if (emphasized) ReportInk else Color(0xFFECECEC)),
        color = if (yellow) Color(0xFFFFFAE8) else Color.White,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun ReportMetric(
    label: String,
    amount: String,
    detail: String,
    modifier: Modifier,
    yellow: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (yellow) Color(0xFFFFFAE8) else Color(0xFFF8F8F8))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, fontSize = 9.sp, color = ReportMuted)
        Text(amount, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(detail, fontSize = 9.sp, color = if (yellow) Color(0xFFB87825) else ReportMuted)
    }
}

@Composable
private fun ReportBar(ratio: Double, color: Color) {
    LinearProgressIndicator(
        progress = { ratio.toFloat().coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(5.dp),
        color = color,
        trackColor = Color(0xFFF0F0F0),
    )
}

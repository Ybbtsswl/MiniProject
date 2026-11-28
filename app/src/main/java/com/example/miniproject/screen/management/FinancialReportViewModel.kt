package com.example.miniproject.screen.management

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.miniproject.SupabaseClient
import com.example.miniproject.repository.*
import io.github.jan.supabase.auth.auth
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

class FinancialReportViewModel : ViewModel() {
    private val initialMonth = YearMonth.now()

    private fun userId() =
        requireNotNull(SupabaseClient.instance.auth.currentUserOrNull()?.id) { "Please sign in." }

    val currentUserId
        get() = userId()

    var currentUserName by mutableStateOf("")
    val repository = FinancialReportRepository()
    var analyticsPeriod by mutableStateOf(AnalyticsPeriod.MONTHLY)
    var selectedMonth by mutableStateOf(initialMonth.toString())
    var reportStartDate by mutableStateOf(initialMonth.atDay(1).toString())
    var reportEndDate by mutableStateOf(initialMonth.atEndOfMonth().toString())
    var customRangeApplied by mutableStateOf(false)
    var generatedReport by mutableStateOf<FinancialReport?>(null)
    private var incomes by mutableStateOf<List<com.example.miniproject.data.Income>>(emptyList())
    private var expenses by mutableStateOf<List<com.example.miniproject.data.Expense>>(emptyList())

    suspend fun load() {
        incomes = IncomeRepository().getIncome(userId()).getOrThrow()
        expenses = ExpenseRepository().getExpenses(userId()).getOrThrow()
    }

    fun updateMonthSelection(value: String) {
        selectedMonth = value
        val month = YearMonth.parse(value)
        reportStartDate = month.atDay(1).toString()
        reportEndDate = month.atEndOfMonth().toString()
        customRangeApplied = false
    }

    fun updateCustomDateRange(start: String, end: String): Result<String> {
        reportStartDate = start
        reportEndDate = end
        customRangeApplied = true
        return Result.success("Dates updated")
    }

    fun selectPeriod(value: AnalyticsPeriod) {
        analyticsPeriod = value
        val today = LocalDate.now()
        val start =
            when (value) {
                AnalyticsPeriod.WEEKLY -> today.minusDays((today.dayOfWeek.value - 1).toLong())
                AnalyticsPeriod.YEARLY -> today.withDayOfYear(1)
                else -> today.withDayOfMonth(1)
            }
        val end =
            when (value) {
                AnalyticsPeriod.WEEKLY -> start.plusDays(6)
                AnalyticsPeriod.YEARLY -> start.plusYears(1).minusDays(1)
                else -> start.plusMonths(1).minusDays(1)
            }
        reportStartDate = start.toString()
        reportEndDate = end.toString()
        customRangeApplied = false
    }

    fun getAnalyticsDataset(budgets: List<Budget>): AnalyticsDataset {
        val start = runCatching { LocalDate.parse(reportStartDate) }.getOrDefault(LocalDate.now())
        val end = runCatching { LocalDate.parse(reportEndDate) }.getOrDefault(start)
        val ins = incomes.filter { it.incomeDate.take(10) in start.toString()..end.toString() }
        val exps = expenses.filter { it.date.take(10) in start.toString()..end.toString() }
        val income = ins.sumOf { it.amount }
        val spent = exps.sumOf { it.amount }
        val cats =
            exps
                .groupBy { it.category.ifBlank { "Other" } }
                .map { (category, rows) ->
                    CategoryComparison(
                        category,
                        budgets
                            .filter {
                                it.category == category &&
                                    it.startDate <= end &&
                                    it.endDate >= start
                            }
                            .sumOf { it.amountLimit },
                        rows.sumOf { it.amount },
                    )
                }
        val dates =
            (0..5).map {
                start.plusDays(
                    java.time.temporal.ChronoUnit.DAYS.between(start, end).coerceAtLeast(0) * it / 6
                )
            }
        val inc =
            dates.mapIndexed { index, date ->
                ins.filter {
                        it.incomeDate.take(10) >= date.toString() &&
                            (index == 5 || it.incomeDate.take(10) < dates[index + 1].toString())
                    }
                    .sumOf { it.amount }
            }
        val exp =
            dates.mapIndexed { index, date ->
                exps
                    .filter {
                        it.date.take(10) >= date.toString() &&
                            (index == 5 || it.date.take(10) < dates[index + 1].toString())
                    }
                    .sumOf { it.amount }
            }
        val labels = dates.map { it.toString().substring(5) }
        val limit =
            budgets.filter { it.startDate <= end && it.endDate >= start }.sumOf { it.amountLimit }
        return AnalyticsDataset(
            "$start – $end",
            income,
            spent,
            income - spent,
            if (limit > 0) spent / limit * 100 else 0.0,
            "N/A",
            cats,
            labels,
            inc,
            exp,
            labels,
            exp,
            inc.zip(exp) { a, b -> a - b },
        )
    }

    suspend fun createFinancialReport(budgets: List<Budget>): Result<FinancialReport> =
        runCatching {
            val start = LocalDate.parse(reportStartDate)
            val end = LocalDate.parse(reportEndDate)
            require(!end.isBefore(start)) { "End date must follow start date." }
            val saved =
                repository
                    .createReport(
                        userId(),
                        if (customRangeApplied) "CUSTOM" else analyticsPeriod.name,
                        start.toString(),
                        end.toString(),
                    )
                    .getOrThrow()
            FinancialReport(
                    saved.reportId,
                    saved.userId,
                    saved.reportType,
                    start,
                    end,
                    saved.totalIncome,
                    saved.totalExpenses,
                    saved.totalSavings,
                )
                .also { generatedReport = it }
        }

    suspend fun exportFinancialReportToPdf(
        context: Context,
        budgets: List<Budget>,
        savingsGoals: List<SavingGoal>,
    ): Result<String> {
        return try {
            val report = generatedReport ?: createFinancialReport(budgets).getOrThrow()
            val data = getAnalyticsDataset(budgets)
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint =
                Paint().apply {
                    color = android.graphics.Color.rgb(161, 98, 7)
                    textSize = 20f
                    isFakeBoldText = true
                }
            val headerPaint =
                Paint().apply {
                    color = android.graphics.Color.rgb(15, 23, 42)
                    textSize = 13f
                    isFakeBoldText = true
                }
            val bodyPaint =
                Paint().apply {
                    color = android.graphics.Color.rgb(51, 65, 85)
                    textSize = 11f
                }

            var y = 50f
            canvas.drawText("Monee — Financial Report (${report.reportId})", 40f, y, titlePaint)
            y += 22f
            canvas.drawText(
                "${report.reportType} | User: $currentUserName (${report.userId})",
                40f,
                y,
                bodyPaint,
            )
            y += 18f
            canvas.drawText(
                "Date Range: ${formatShortDate(report.startDate)} to ${formatShortDate(report.endDate)}",
                40f,
                y,
                bodyPaint,
            )
            y += 16f
            canvas.drawText(
                "Generated At: ${formatDateTime(report.generatedAt)}",
                40f,
                y,
                bodyPaint,
            )

            y += 30f
            canvas.drawText("Financial Summary", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("Total Income: ${formatRM(report.totalIncome)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Total Expenses: ${formatRM(report.totalExpenses)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Total Savings: ${formatRM(report.totalSavings)}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText(
                "Budget Utilization: ${String.format(Locale.US, "%.1f%%", data.budgetUtil)}",
                40f,
                y,
                bodyPaint,
            )

            y += 32f
            canvas.drawText("Budget Allocation vs. Actual Spending", 40f, y, headerPaint)
            y += 20f
            data.categories.forEach { cat ->
                val variance = cat.budget - cat.actual
                val status =
                    if (variance < 0) "Over ${formatRM(abs(variance))}"
                    else "Saved ${formatRM(variance)}"
                canvas.drawText(
                    "${cat.category}: Budget ${formatRM(cat.budget)} | Actual ${formatRM(cat.actual)} ($status)",
                    40f,
                    y,
                    bodyPaint,
                )
                y += 18f
            }

            y += 24f
            canvas.drawText("Savings Goals Progress", 40f, y, headerPaint)
            y += 20f
            savingsGoals.forEach { g ->
                canvas.drawText(
                    "${g.goalName}: ${formatRM(g.currentAmount)} / ${formatRM(g.targetAmount)} (${g.progressPercentage.toInt()}% - ${g.status})",
                    40f,
                    y,
                    bodyPaint,
                )
                y += 18f
            }

            document.finishPage(page)

            val dir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val fileName = "Monee_Financial_Report_${report.reportType}_${report.reportId}.pdf"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            repository.updateFilePath(userId(), report.reportId, file.absolutePath).getOrThrow()
            generatedReport = report.copy(file_path = file.absolutePath)
            Result.success("PDF saved: ${file.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

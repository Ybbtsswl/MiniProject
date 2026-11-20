package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.miniproject.data.Budget
import com.example.miniproject.data.BudgetCategory
import com.example.miniproject.repository.BudgetRepository
import com.example.miniproject.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BudgetBackground = Color(0xFFFAFAF8)
private val BudgetText = Color(0xFF202635)
private val BudgetMuted = Color(0xFF7D828D)
private val BudgetYellow = Color(0xFFFFBF2F)
private val BudgetGreen = Color(0xFF17A673)
private val BudgetRed = Color(0xFFF24B50)

@Composable
fun BudgetScreen(
    userId: String?,
    onBack: () -> Unit,
    onManageExpenses: () -> Unit
) {
    val budgetRepository = remember { BudgetRepository() }
    val expenseRepository = remember { ExpenseRepository() }
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val startDate = remember { month.atDay(1).toString() }
    val endDate = remember { month.atEndOfMonth().toString() }
    val monthLabel = remember { month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)) }
    var budget by remember { mutableStateOf<Budget?>(null) }
    var categorySpent by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun refreshBudgetData() {
        if (userId == null) return
        val loaded =
            budgetRepository.getActiveBudget(userId, LocalDate.now().toString()).getOrNull()
        val expenses = expenseRepository.getExpenses(userId).getOrDefault(emptyList())
            .filter { it.date in startDate..endDate }
        val spent = expenses.sumOf { it.amount }
        categorySpent = expenses.groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
        budget = loaded
        if (loaded != null && loaded.totalSpent != spent) {
            budgetRepository.updateTotalSpent(loaded, spent)
            budget = loaded.copy(totalSpent = spent)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(userId) { refreshBudgetData() }
    DisposableEffect(lifecycleOwner, userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { refreshBudgetData() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentBudget = budget ?: Budget(
        userId = userId.orEmpty(), budgetName = monthLabel, amountLimit = 0.0,
        totalSpent = categorySpent.values.sum(), startDate = startDate, endDate = endDate
    )
    val spent = currentBudget.totalSpent
    val safeToSpend = (currentBudget.amountLimit - spent).coerceAtLeast(0.0)
    val percent = if (currentBudget.amountLimit > 0) (spent / currentBudget.amountLimit).toFloat()
        .coerceIn(0f, 1f) else 0f
    val categories = listOf(
        BudgetCategory(
            "Food & Dining",
            "🍔",
            categorySpent["Food & Dining"] ?: 0.0,
            currentBudget.amountLimit * .30
        ),
        BudgetCategory(
            "Housing & Utilities",
            "🏠",
            categorySpent["Housing & Utilities"] ?: 0.0,
            currentBudget.amountLimit * .32
        ),
        BudgetCategory(
            "Transportation",
            "🚗",
            categorySpent["Transportation"] ?: 0.0,
            currentBudget.amountLimit * .16
        ),
        BudgetCategory(
            "Entertainment",
            "🎬",
            categorySpent["Entertainment"] ?: 0.0,
            currentBudget.amountLimit * .10
        )
    )

    Scaffold(containerColor = BudgetBackground) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        "Back",
                        tint = BudgetText
                    )
                }
                Text(
                    "Budget Management",
                    color = BudgetText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Budget Hub 🎯", color = BudgetText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Keep your spending balanced", color = BudgetMuted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AssistChip(
                    onClick = {},
                    label = { Text(monthLabel, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            null,
                            Modifier.size(14.dp)
                        )
                    })
            }
            Spacer(Modifier.height(10.dp))
            BudgetSummaryCard(
                currentBudget,
                spent,
                safeToSpend,
                percent,
                onSetBudget = { showBudgetDialog = true }) {
                val updated = currentBudget.copy(amountLimit = spent + 650.0)
                scope.launch {
                    saveBudget(updated, budgetRepository) {
                        budget = it ?: updated; errorMessage =
                        if (it == null) "Unable to save budget" else null
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val alert = categories.firstOrNull { it.remaining < 0 || it.progress >= .9f }
            if (alert != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF7F6))
                        .border(1.dp, Color(0xFFFFD1D1), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        null,
                        tint = BudgetText,
                        modifier = Modifier.size(18.dp)
                    ); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Limit Alert: ${alert.name}",
                            color = BudgetText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ); Text(
                        "${currency(alert.spent)} spent of ${currency(alert.limit)}",
                        color = BudgetRed,
                        fontSize = 10.sp
                    )
                    }
                    TextButton(onClick = { showBudgetDialog = true }) {
                        Text(
                            "+ Adjust",
                            color = Color(0xFF996D00),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Category Budgets",
                        color = BudgetText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ); Text(
                    "${categories.size} key categories active",
                    color = BudgetMuted,
                    fontSize = 10.sp
                )
                }
                TextButton(
                    onClick = onManageExpenses,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                    modifier = Modifier.background(Color(0xFFFFF4D5), RoundedCornerShape(12.dp))
                ) {
                    Text("Manage", color = Color(0xFFAA7900), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            categories.forEach { CategoryBudgetCard(it) }
            errorMessage?.let {
                Text(
                    it,
                    color = BudgetRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
    if (showBudgetDialog) {
        SetBudgetDialog(currentBudget.amountLimit, { showBudgetDialog = false }) { value ->
            val updated = currentBudget.copy(amountLimit = value)
            showBudgetDialog = false
            scope.launch {
                saveBudget(updated, budgetRepository) {
                    budget = it ?: updated; errorMessage =
                    if (it == null) "Unable to save budget" else null
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    budget: Budget,
    spent: Double,
    safeToSpend: Double,
    percent: Float,
    onSetBudget: () -> Unit,
    onRecalculate: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Total Monthly Budget ⓘ",
                color = BudgetMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "↗ ${(percent * 100).toInt()}% spent",
                color = Color(0xFF9A7100),
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xFFFFF4CF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            currency(budget.amountLimit),
            color = BudgetText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp)
        )
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox(
                "Spent so far",
                currency(spent),
                BudgetRed,
                Modifier.weight(1f)
            ); StatBox("Safe to spend", currency(safeToSpend), BudgetGreen, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${LocalDate.now().dayOfMonth} of ${LocalDate.now().lengthOfMonth()} days passed",
                color = BudgetMuted,
                fontSize = 10.sp
            ); Text(
            currency(
                safeToSpend / (LocalDate.now()
                    .lengthOfMonth() - LocalDate.now().dayOfMonth).coerceAtLeast(1)
            ), color = BudgetText, fontSize = 10.sp, fontWeight = FontWeight.Bold
        )
        }
        LinearProgressIndicator(
            progress = {
                (LocalDate.now().dayOfMonth.toFloat() / LocalDate.now()
                    .lengthOfMonth()).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(5.dp)
                .clip(CircleShape),
            color = BudgetYellow,
            trackColor = Color(0xFFEDEFF2)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRecalculate) {
                Icon(
                    Icons.Outlined.Edit,
                    null,
                    Modifier.size(16.dp)
                ); Spacer(Modifier.width(4.dp)); Text(
                "Recalculate Limit",
                color = BudgetText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            }
            Button(
                onClick = onSetBudget,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BudgetYellow,
                    contentColor = BudgetText
                ),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
            ) { Text("+ Set Budget", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

private suspend fun saveBudget(
    budget: Budget,
    repository: BudgetRepository,
    onSaved: (Budget?) -> Unit
) {
    val result =
        if (budget.budgetId.isBlank()) repository.createBudget(budget) else repository.updateBudget(
            budget
        )
    onSaved(result.getOrNull())
}

@Composable
private fun StatBox(label: String, amount: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .padding(9.dp)
    ) {
        Text("●  $label", color = color, fontSize = 10.sp); Text(
        amount,
        color = BudgetText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
    }
}

@Composable
private fun CategoryBudgetCard(category: BudgetCategory) {
    val over = category.remaining < 0; Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (over) Color(0xFFFFD2D2) else Color(0xFFECECEF),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                category.icon,
                fontSize = 17.sp,
                modifier = Modifier
                    .size(29.dp)
                    .background(Color(0xFFFFF3D4), CircleShape),
                textAlign = TextAlign.Center
            ); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) {
            Text(
                category.name,
                color = BudgetText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ); Text(
            if (over) "- ${currency(-category.remaining)} over" else "${currency(category.remaining)} left",
            color = if (over) BudgetRed else BudgetGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        }; Column(horizontalAlignment = Alignment.End) {
            Text(
                currency(category.spent),
                color = if (over) BudgetRed else BudgetText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ); Text("of ${currency(category.limit)}", color = BudgetMuted, fontSize = 9.sp)
        }
        }; LinearProgressIndicator(
        progress = { category.progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(5.dp)
            .clip(CircleShape),
        color = if (over) BudgetRed else BudgetYellow,
        trackColor = Color(0xFFEDEFF2)
    )
    }
}

@Composable
private fun SetBudgetDialog(current: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf(current.toInt().toString()) }; AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set monthly budget") },
        text = {
            OutlinedTextField(
                value,
                { value = it.filter(Char::isDigit) },
                label = { Text("Amount (RM)") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                value.toDoubleOrNull()?.takeIf { it >= 0 }?.let(onSave)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun currency(amount: Double): String = "RM " + NumberFormat.getNumberInstance(Locale.US)
    .apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(amount)

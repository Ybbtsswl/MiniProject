package com.example.miniproject.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.miniproject.data.Income
import com.example.miniproject.data.IncomeCategory
import com.example.miniproject.repository.IncomeRepository
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeButtonBrush
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.moneeFieldColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun IncomeScreen(
    userId: String,
    onBack: () -> Unit,
    onAddIncome: () -> Unit,
    onSeeAll: (String) -> Unit = {},
    showAllHistory: Boolean = false
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now().toString()) }
    var selectedTab by remember { mutableStateOf("All") }
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var categories by remember { mutableStateOf<List<IncomeCategory>>(emptyList()) }
    var selectedIncome by remember { mutableStateOf<Income?>(null) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    val incomeRepository = remember { IncomeRepository() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, selectedMonth, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            incomes = incomeRepository.getIncome(userId).getOrDefault(emptyList())
            categories = incomeRepository.getIncomeCategories().getOrDefault(emptyList())
        }
    }

    val displayMonth = runCatching {
        LocalDate.parse("$selectedMonth-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }.getOrDefault(selectedMonth)
    val monthIncomes = if (showAllHistory) incomes
    else incomes.filter { it.incomeDate.startsWith(selectedMonth) }
    val monthlyTotal = monthIncomes.sumOf { it.amount }
    val categoryName: (Income) -> String = { income ->
        categories.firstOrNull { it.incomeCategoryId == income.categoryId }?.name
            ?: income.categoryId?.takeIf { it.isNotBlank() }?.take(8)
            ?: "Other"
    }
    val categoryTotals = monthIncomes.groupBy(categoryName)
        .mapValues { (_, values) -> values.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    val tabNames = listOf("All") + categoryTotals.map { it.first }
    val filteredIncomes = if (selectedTab == "All") monthIncomes
    else monthIncomes.filter { categoryName(it) == selectedTab }

    Box(modifier = Modifier.fillMaxSize().moneeBackground()) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MoneeInk)
                }
                Text("Income", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", fontSize = 24.sp, color = MoneeMuted, modifier = Modifier.clickable {
                    selectedMonth = YearMonth.parse(selectedMonth).minusMonths(1).toString()
                })
                Text(displayMonth, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MoneeInk)
                Text("›", fontSize = 24.sp, color = MoneeMuted, modifier = Modifier.clickable {
                    selectedMonth = YearMonth.parse(selectedMonth).plusMonths(1).toString()
                })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().moneeHero(RoundedCornerShape(20.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL INCOME THIS MONTH", fontSize = 12.sp, color = MoneeMuted, fontWeight = FontWeight.Medium)
                    Text(formatIncomeAmount(monthlyTotal), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MoneeInk, modifier = Modifier.padding(top = 8.dp))
                    Text(incomeChangeText(incomes, selectedMonth), fontSize = 12.sp, color = MoneeGreen, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Summary by category", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
            Text("Shown transactions · ${formatIncomeAmount(monthlyTotal)}", fontSize = 11.sp, color = MoneeMuted, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.height(10.dp))
            categoryTotals.take(3).forEach { (name, amount) -> IncomeCategoryBar(name, amount, monthlyTotal) }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showAllHistory) "All income history" else "Income history",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk
                )
                if (!showAllHistory) {
                    Text(
                        "See all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneeAmber,
                        modifier = Modifier.clickable { onSeeAll(selectedMonth) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tabNames) { tab ->
                    Surface(onClick = { selectedTab = tab }, shape = RoundedCornerShape(18.dp), color = if (selectedTab == tab) MoneeYellow else Color.White) {
                        Text(tab, color = MoneeInk, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (filteredIncomes.isEmpty()) {
                    item { Text("No income records for this month", color = MoneeMuted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)) }
                } else {
                    val grouped = filteredIncomes.groupBy { incomeDisplayDate(it.incomeDate) }
                    grouped.forEach { (date, itemsList) ->
                        item {
                            Text(
                                text = date,
                                fontSize = 12.sp,
                                color = MoneeMuted,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(itemsList) { income ->
                            IncomeItemRow(income, categoryName(income)) { selectedIncome = income }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                .navigationBarsPadding().padding(top = 24.dp, bottom = 16.dp)
        ) {
            MoneeGradientButton(
                onClick = onAddIncome,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
            ) { Text("+ Add Income", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }

    selectedIncome?.let { income ->
        IncomeDetailDialog(
            income = income,
            categoryName = categoryName(income),
            onDismiss = { selectedIncome = null },
            onEdit = {
                selectedIncome = null
                editingIncome = income
            },
            onDelete = {
                selectedIncome = null
                scope.launch {
                    incomeRepository.deleteIncome(income.incomeId, userId)
                        .onSuccess {
                            incomes = incomes.filterNot { it.incomeId == income.incomeId }
                            Toast.makeText(context, "Income deleted", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            Toast.makeText(context, it.message ?: "Unable to delete income", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }

    editingIncome?.let { income ->
        EditIncomeDialog(
            income = income,
            categories = categories,
            onDismiss = { editingIncome = null },
            onSave = { updated ->
                editingIncome = null
                scope.launch {
                    incomeRepository.updateIncome(updated)
                        .onSuccess { saved ->
                            incomes = incomes.map { if (it.incomeId == saved.incomeId) saved else it }
                            Toast.makeText(context, "Income updated", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            Toast.makeText(context, it.message ?: "Unable to update income", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }
}

@Composable
private fun IncomeCategoryBar(label: String, amount: Double, total: Double) {
    val percentage = if (total > 0) (amount / total * 100).toInt().coerceIn(5, 100) else 0
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MoneeInk)
            Text(formatIncomeAmount(amount), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MoneeInk)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Box(modifier = Modifier.fillMaxWidth(percentage / 100f).fillMaxHeight().background(MoneeButtonBrush, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun IncomeItemRow(income: Income, categoryName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, color = MoneeInk, fontWeight = FontWeight.SemiBold)
                Text(income.description.ifBlank { "Income" }, color = MoneeMuted, fontSize = 12.sp)
            }
            Text("+${formatIncomeAmount(income.amount)}", color = MoneeInk, fontWeight = FontWeight.Bold)
        }
    }
}

private fun incomeDisplayDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        }
    } catch (_: Exception) {
        dateString
    }
}

@Composable
private fun IncomeDetailDialog(
    income: Income,
    categoryName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Income Details", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(formatIncomeAmount(income.amount), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MoneeAmber)
                Text("Category: $categoryName")
                Text("Description: ${income.description.ifBlank { "–" }}")
                Text("Date: ${income.incomeDate}")
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("Edit") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun EditIncomeDialog(
    income: Income,
    categories: List<IncomeCategory>,
    onDismiss: () -> Unit,
    onSave: (Income) -> Unit
) {
    var amount by remember { mutableStateOf(income.amount.toString()) }
    var description by remember { mutableStateOf(income.description) }
    var date by remember { mutableStateOf(income.incomeDate) }
    var selectedCategoryId by remember { mutableStateOf(income.categoryId) }
    val valid = amount.toDoubleOrNull()?.let { it > 0 } == true &&
        date.isNotBlank() && selectedCategoryId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Income", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    leadingIcon = { Text("RM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = moneeFieldColors()
                )
                Text("Category", fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { category ->
                        MoneeChip(
                            selected = selectedCategoryId == category.incomeCategoryId,
                            onClick = { selectedCategoryId = category.incomeCategoryId }
                        ) { Text("${category.icon ?: "•"} ${category.name}") }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    colors = moneeFieldColors()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = moneeFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(income.copy(
                    amount = amount.toDouble(),
                    categoryId = selectedCategoryId,
                    incomeDate = date,
                    description = description.trim()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun incomeChangeText(incomes: List<Income>, month: String): String {
    val current = incomes.filter { it.incomeDate.startsWith(month) }.sumOf { it.amount }
    val previousMonth = runCatching { YearMonth.parse(month).minusMonths(1).toString() }.getOrDefault(month)
    val previous = incomes.filter { it.incomeDate.startsWith(previousMonth) }.sumOf { it.amount }
    if (previous <= 0.0) return "No income last month"
    val change = (current - previous) / previous * 100
    return if (change >= 0) "↑ ${String.format(Locale.US, "%.1f", change)}% more than last month"
    else "↓ ${String.format(Locale.US, "%.1f", abs(change))}% less than last month"
}

internal fun formatIncomeAmount(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${format.format(amount)}"
}

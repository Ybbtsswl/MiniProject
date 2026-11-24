package com.example.miniproject.screen

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.miniproject.data.Category
import com.example.miniproject.data.Income
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.repository.IncomeRepository
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeHero
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun IncomeScreen(userId: String, onBack: () -> Unit, onAddIncome: () -> Unit) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now().toString()) }
    var selectedTab by remember { mutableStateOf("All") }
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val incomeRepository = remember { IncomeRepository() }
    val categoryRepository = remember { ExpenseRepository() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(userId, selectedMonth, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            incomes = incomeRepository.getIncome(userId).getOrDefault(emptyList())
            categories = categoryRepository.getCategories(userId).getOrDefault(emptyList())
        }
    }

    val displayMonth = runCatching {
        LocalDate.parse("$selectedMonth-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }.getOrDefault(selectedMonth)
    val monthIncomes = incomes.filter { it.incomeDate.startsWith(selectedMonth) }
    val monthlyTotal = monthIncomes.sumOf { it.amount }
    val categoryName: (Income) -> String = { income ->
        categories.firstOrNull { it.categoryId == income.categoryId }?.name
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
                Text("Income history", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
                Text("See all", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MoneeAmber)
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
                    items(filteredIncomes) { income -> IncomeItemRow(income, categoryName(income)) }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                .navigationBarsPadding().padding(top = 24.dp, bottom = 16.dp)
        ) {
            Button(
                onClick = onAddIncome,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoneeAmber, contentColor = MoneeInk)
            ) { Text("+ Add Income", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
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
            Box(modifier = Modifier.fillMaxWidth(percentage / 100f).fillMaxHeight().background(MoneeAmber, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun IncomeItemRow(income: Income, categoryName: String) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(categoryName, color = MoneeInk, fontWeight = FontWeight.SemiBold)
            Text(income.description.ifBlank { "Income" }, color = MoneeMuted, fontSize = 12.sp)
            Text(income.incomeDate, color = MoneeMuted, fontSize = 11.sp)
        }
        Text("+${formatIncomeAmount(income.amount)}", color = MoneeInk, fontWeight = FontWeight.Bold)
    }
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

private fun formatIncomeAmount(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${format.format(amount)}"
}

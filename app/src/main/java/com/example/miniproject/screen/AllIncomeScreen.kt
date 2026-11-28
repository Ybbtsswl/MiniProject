package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.miniproject.data.Income
import com.example.miniproject.data.IncomeCategory
import com.example.miniproject.repository.IncomeRepository
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.moneeBackground
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun AllIncomeScreen(
    userId: String,
    initialMonth: String,
    onBack: () -> Unit,
    onAddIncome: () -> Unit
) {
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var categories by remember { mutableStateOf<List<IncomeCategory>>(emptyList()) }
    var selectedMonth by remember(initialMonth) { mutableStateOf(initialMonth) }
    val repository = remember { IncomeRepository() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(userId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            incomes = repository.getIncome(userId).getOrDefault(emptyList())
            categories = repository.getIncomeCategories().getOrDefault(emptyList())
        }
    }

    val categoryName: (Income) -> String = { income ->
        categories.firstOrNull { it.incomeCategoryId == income.categoryId }?.name ?: "Other"
    }
    val monthIncomes = incomes.filter { it.incomeDate.startsWith(selectedMonth) }
    val displayMonth = runCatching {
        LocalDate.parse("$selectedMonth-01")
            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }.getOrDefault(selectedMonth)
    val grouped = monthIncomes.groupBy { allIncomeDisplayDate(it.incomeDate) }

    Box(Modifier.fillMaxSize().moneeBackground()) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MoneeInk)
                }
                Text("All Income", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(18.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (monthIncomes.isEmpty()) {
                    item { Text("No income records for this month", color = MoneeMuted, modifier = Modifier.padding(vertical = 40.dp)) }
                } else {
                    grouped.forEach { (date, dateIncomes) ->
                        item {
                            Text(date, fontSize = 12.sp, color = MoneeMuted, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(dateIncomes) { income ->
                            AllIncomeRow(income, categoryName(income))
                        }
                    }
                }
            }

            Button(
                onClick = onAddIncome,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD84D), contentColor = MoneeInk)
            ) { Text("+ Add Income", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun AllIncomeRow(income: Income, categoryName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(categoryName, color = MoneeInk, fontWeight = FontWeight.SemiBold)
                Text(income.description.ifBlank { "Income" }, color = MoneeMuted, fontSize = 12.sp)
            }
            Text("+${formatIncomeAmount(income.amount)}", color = MoneeInk, fontWeight = FontWeight.Bold)
        }
    }
}

private fun allIncomeDisplayDate(dateString: String): String = try {
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

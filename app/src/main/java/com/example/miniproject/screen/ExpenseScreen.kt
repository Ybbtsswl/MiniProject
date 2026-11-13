package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ScreenBackground = Color(0xFFFFFFFF)
private val CardSoft = Color(0xFFFFF9E6)
private val TextPrimary = Color(0xFF2D2D2D)
private val TextSecondary = Color(0xFF888888)
private val BorderSoft = Color(0xFFF5F5F5)

@Composable
fun ExpenseScreen(
    onAddClick: () -> Unit,
    onExpenseClick: (Expense) -> Unit = {},
    userId: String? = null,
    viewModel: ExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))) }

    LaunchedEffect(userId, selectedMonth) {
        userId?.let {
            viewModel.loadExpensesByMonth(it, selectedMonth)
        }
    }

    val expenses = uiState.expenses
    val displayMonth = try {
        LocalDate.parse("$selectedMonth-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (e: Exception) {
        selectedMonth
    }
    val monthlyTotal = expenses.sumOf { it.amount }
    val monthChange = -12.4

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBackground
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "Expenses",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 20.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        val prev = LocalDate.parse("$selectedMonth-01").minusMonths(1)
                        selectedMonth = prev.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }
                )
                Text(
                    text = displayMonth,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        val next = LocalDate.parse("$selectedMonth-01").plusMonths(1)
                        selectedMonth = next.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSoft)
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL EXPENSES THIS MONTH",
                        fontSize = 12.sp,
                        color = Color(0xFF9A8B48),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatCurrency(monthlyTotal),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(text = "↓ ", fontSize = 12.sp, color = MoneeGreen)
                        Text(
                            text = "${monthChange * -1}% less than last month",
                            fontSize = 12.sp,
                            color = MoneeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD447))
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading data",
                            color = MoneeRed,
                            fontSize = 14.sp
                        )
                    }
                }
                expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses yet.\nTap + to add your first expense!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Summary by category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap an item to view details",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val categoryTotals = expenses
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { exp -> exp.amount } }
                        .toList()
                        .sortedByDescending { (_, amount) -> amount }

                    categoryTotals.take(3).forEach { (cat, amt) ->
                        CategoryBar(cat, amt, monthlyTotal)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expense History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val tabs = listOf("All", "Food", "Shopping", "Transport")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        tabs.forEach { tab ->
                            TabItem(
                                label = tab,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredExpenses = when (selectedTab) {
                        "Food" -> expenses.filter {
                            it.category.equals("Food", ignoreCase = true) ||
                                    it.category.equals("Food & drinks", ignoreCase = true)
                        }
                        "Shopping" -> expenses.filter {
                            it.category.equals("Shopping", ignoreCase = true)
                        }
                        "Transport" -> expenses.filter {
                            it.category.equals("Transport", ignoreCase = true)
                        }
                        else -> expenses
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        if (filteredExpenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No transactions in this category",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val grouped = filteredExpenses.groupBy { getDisplayDate(it.date) }
                            grouped.forEach { (date, itemsList) ->
                                item {
                                    Text(
                                        text = date,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(itemsList) { exp ->
                                    ExpenseItemRow(
                                        exp = exp,
                                        onItemClick = { onExpenseClick(exp) },
                                        onDelete = {
                                            exp.id.let { id ->
                                                viewModel.deleteExpense(id) { success, _ ->
                                                    if (success) {
                                                        userId?.let { viewModel.loadExpensesByMonth(it, selectedMonth) }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFE866),
                    contentColor = TextPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "+ Add Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(label: String, amount: Double, total: Double) {
    val percentage = if (total > 0) (amount / total * 100).toInt().coerceIn(5, 100) else 0

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = formatCurrency(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BorderSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFFFD447).copy(alpha = 0.85f))
            )
        }
    }
}


@Composable
private fun TabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        fontSize = 13.sp,
        color = if (selected) TextPrimary else TextSecondary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CardSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun ExpenseItemRow(
    exp: Expense,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(text = categoryEmoji(exp.category), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            exp.description?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = "${exp.category} · ${exp.date}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "- ${formatCurrency(exp.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MoneeRed
            )
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MoneeRed),
                contentPadding = PaddingValues(start = 8.dp, end = 4.dp)
            ) {
                Text("Delete", fontSize = 11.sp)
            }
        }
    }
}

private fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "food", "food & drinks" -> "🍜"
    "transport" -> "🚌"
    "shopping" -> "🛍️"
    "groceries" -> "🛒"
    "bills" -> "📄"
    "entertainment" -> "🎬"
    else -> "💰"
}

private fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

private fun getDisplayDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        when {
            date.isEqual(today) -> "Today"
            date.isEqual(yesterday) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        }
    } catch (e: Exception) {
        dateStr
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Expense Screen")
@Composable
fun ExpenseScreenPreview() {
    MiniProjectTheme {
        ExpenseScreen(
            onAddClick = {},
            onExpenseClick = {},
            userId = null
        )
    }
}

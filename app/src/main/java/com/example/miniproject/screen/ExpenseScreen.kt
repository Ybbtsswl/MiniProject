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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeRed
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private val ScreenBackground = Color(0xFFFFFFFF)
private val CardSoft = Color(0xFFFFF9E6)
private val TextPrimary = Color(0xFF2D2D2D)
private val TextSecondary = Color(0xFF888888)
private val BorderSoft = Color(0xFFF5F5F5)

@Composable
fun ExpenseScreen(
    onAddClick: () -> Unit,
    onSplitBillClick: () -> Unit = {},
    userId: String? = null
) {
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("All") }

    LaunchedEffect(userId) {
        expenses = sampleExpenses()
    }

    val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
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

            // Top Bar
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

            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", fontSize = 20.sp, color = TextSecondary)
                Text(
                    text = currentMonth,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text("›", fontSize = 20.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Card — Softer yellow
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
                            text = "${monthChange * -1}% less than September",
                            fontSize = 12.sp,
                            color = MoneeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Summary
            Text(
                text = "Summary by category",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Shown transactions · ${formatCurrency(expenses.sumOf { it.amount })}",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            CategoryBar("Transport", 120.00, monthlyTotal)
            CategoryBar("Groceries", 86.00, monthlyTotal)
            CategoryBar("Food & drinks", 73.50, monthlyTotal)

            Spacer(modifier = Modifier.height(28.dp))

            // Expense History Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expense history",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    color = Color(0xFFD4B848),
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
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

            // Expense List
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
                val grouped = filteredExpenses.groupBy { getDisplayDate(it.date) }
                grouped.forEach { (date, items) ->
                    item {
                        Text(
                            text = date,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(items) { exp ->
                        ExpenseItemRow(exp)
                    }
                }
            }
        }

        // Add Expense FAB — Softer yellow
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
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun ExpenseItemRow(exp: Expense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
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
            Text(
                text = exp.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "${exp.category} · 12:00 PM",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "- ${formatCurrency(exp.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MoneeRed
            )
            if (exp.isFromSplit == true) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CardSoft,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Split",
                        fontSize = 9.sp,
                        color = Color(0xFF9A8B48),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "food", "food & drinks" -> "🍜"
    "transport" -> "🚌"
    "shopping" -> "🛍️"
    "groceries" -> "🛒"
    "bills" -> "🧾"
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
            date.isEqual(today) -> "Today, ${date.format(DateTimeFormatter.ofPattern("d MMMM"))}"
            date.isEqual(yesterday) -> "Yesterday, ${date.format(DateTimeFormatter.ofPattern("d MMMM"))}"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun sampleExpenses(): List<Expense> {
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    return listOf(
        Expense(id = "1", userId = "demo", amount = 28.50, category = "Food & drinks", description = "Lunch at Kopitiam", date = today, isFromSplit = false),
        Expense(id = "2", userId = "demo", amount= 86.00, category = "Groceries", description = "Weekly groceries", date = today, isFromSplit = false),
        Expense(id = "3", userId = "demo", amount = 45.00, category = "Food & drinks", description = "Dinner with friends", date = yesterday, isFromSplit = true),
        Expense(id = "4", userId = "demo", amount = 120.00, category = "Transport", description = "Petrol refill", date = yesterday, isFromSplit = false)
    )
}

@Preview(showBackground = true, name = "Expense Screen")
@Composable
fun ExpenseScreenPreview() {
    MiniProjectTheme {
        ExpenseScreen(
            onAddClick = {},
            onSplitBillClick = {}
        )
    }
}

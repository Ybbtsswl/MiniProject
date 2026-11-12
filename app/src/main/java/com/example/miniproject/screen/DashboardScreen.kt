package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.ExpenseRepository
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val DashboardBackground = Color(0xFFF8F8FF)
private val CardColor = Color.White
private val Yellow = Color(0xFFFFD447)
private val PaleYellow = Color(0xFFFFF4C9)
private val DarkText = Color(0xFF172033)
private val MutedText = Color(0xFF686D79)
private val BlueTint = Color(0xFFF1F5FD)
private val Red = Color(0xFFD92935)

@Composable
fun DashboardScreen(
    userId: String? = null,
    onAdd: () -> Unit = {},
    onExpense: () -> Unit = {},
    onSplitBill: () -> Unit = {},
    onAddSplitBill: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    val repository = remember { ExpenseRepository() }

    LaunchedEffect(userId) {
        expenses = if (userId == null) {
            emptyList()
        } else {
            repository.getExpenses(userId).getOrDefault(emptyList())
        }
    }

    Scaffold(
        containerColor = DashboardBackground,
        bottomBar = {
            DashboardBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAdd = onAdd
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardContent(
                    expenses = expenses,
                    onExpense = onExpense,
                    onSplitBill = onSplitBill
                )
                1 -> ScreenPlaceholder("Analytics")
                2 -> SplitBillScreen(onBillClick = {}, onCreateSplitBill = onAddSplitBill, onBack = {}, showTitle = true)
                3 -> ProfileScreen(userId = userId, onSignOut = onSignOut)
            }
        }
    }
}

@Composable
private fun DashboardContent(
    expenses: List<Expense>,
    onExpense: () -> Unit,
    onSplitBill: () -> Unit
) {
    val currentMonth = LocalDate.now().toString().substring(0, 7)
    val monthlyExpenses = expenses
        .filter { it.date.startsWith(currentMonth) }
        .sumOf { it.amount }
    val income = 0.0
    val balance = income - monthlyExpenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 18.dp)
    ) {
        DashboardHeader()
        Spacer(modifier = Modifier.height(22.dp))
        BalanceCard(balance = balance)
        Spacer(modifier = Modifier.height(18.dp))
        MonthlyOverview(income = income, expenses = monthlyExpenses)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Quick Actions",
            color = DarkText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        QuickActionGrid(onExpense = onExpense, onSplitBill = onSplitBill)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "monee",
            color = DarkText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "•",
            color = DarkText,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(text = "Home", color = Color(0xFF3D414A), fontSize = 16.sp)
        Spacer(modifier = Modifier.width(52.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(PaleYellow)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(text = "🙂", fontSize = 16.sp) }
    }
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "Hello, User! 👋",
            color = DarkText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Here's your financial overview",
            color = MutedText,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun BalanceCard(balance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardColor)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Total Balance  ◉", color = MutedText, fontSize = 12.sp)
            Text(
                text = formatCurrency(balance),
                color = DarkText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(PaleYellow)
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text(
                text = "↗ +12.4%",
                color = Color(0xFF655100),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthlyOverview(income: Double, expenses: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardColor)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "This Month", color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlueTint)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text(text = "April 2025", color = MutedText, fontSize = 11.sp) }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthlyMetric("Income", formatCurrency(income), Color(0xFF9D7900), Modifier.weight(1f))
            MonthlyMetric("Expenses", formatCurrency(expenses), Red, Modifier.weight(1f), amountColor = Red)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SpendingChart(expenses = expenses)
    }
}

@Composable
private fun MonthlyMetric(
    label: String, amount: String, dotColor: Color,
    modifier: Modifier = Modifier, amountColor: Color = DarkText
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BlueTint)
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(text = "  $label", color = MutedText, fontSize = 11.sp)
        }
        Text(
            text = amount,
            color = amountColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SpendingChart(expenses: Double) {
    val values = if (expenses == 0.0) List(7) { 0 } else listOf(32, 66, 38, 70, 54, 36, 24)
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth().height(92.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(v.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            when (i) {
                                0, 2, 5, 6 -> Color(0xFFFFDE86)
                                1, 4 -> Color(0xFFE7BD36)
                                else -> Yellow
                            }
                        )
                )
                Text(labels[i], color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val f = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${f.format(amount)}"
}

@Composable
private fun QuickActionGrid(onExpense: () -> Unit, onSplitBill: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("✓", "Income", Modifier.weight(1f)) {}
            QuickAction("📄", "Expenses", Modifier.weight(1f), onClick = onExpense)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("📈", "Analytics", Modifier.weight(1f)) {}
            QuickAction("📋", "Split Bill", Modifier.weight(1f), onClick = onSplitBill)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("🐷", "Savings", Modifier.weight(1f)) {}
            QuickAction("⊘", "Budget", Modifier.weight(1f)) {}
        }
    }
}

@Composable
private fun QuickAction(
    icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(50.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CardColor, contentColor = DarkText)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(PaleYellow),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = Color(0xFF8D6E00), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = DarkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DashboardBottomBar(
    selectedTab: Int, onTabSelected: (Int) -> Unit, onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("⌂", "Home", selectedTab == 0) { onTabSelected(0) }
        BottomItem("⌁", "Analytics", selectedTab == 1) { onTabSelected(1) }

        Button(
            onClick = onAdd,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Color(0xFF6B5500)
            )
        ) { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light) }

        BottomItem("↗", "Split Bill", selectedTab == 2) { onTabSelected(2) }
        BottomItem("♙", "Profile", selectedTab == 3) { onTabSelected(3) }
    }
}

@Composable
private fun BottomItem(
    icon: String, label: String, selected: Boolean, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (selected) DarkText else MutedText
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(label, fontSize = 10.sp)
        }
    }
}

@Composable
fun ScreenPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = DarkText, fontSize = 20.sp)
    }
}

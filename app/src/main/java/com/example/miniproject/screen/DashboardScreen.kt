package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeButtonBrush
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.MoneeYellowSoft
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeHero
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardColor = Color.White

@Composable
fun DashboardScreen(
    userId: String? = null,
    onAdd: () -> Unit = {},
    onExpense: () -> Unit = {},
    onSplitBill: () -> Unit = {},
    onAddSplitBill: () -> Unit = {},
    onBudget: () -> Unit = {},
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

    Box(modifier = Modifier.fillMaxSize().moneeBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            // Status bar + camera cut-out: the Scaffold pads for both ...
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
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
                    // ... and tells the tabs inside it (Split Bill / Profile have their own
                    // TopAppBar) that those insets are already handled, so nothing is added twice
                    .consumeWindowInsets(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardContent(
                        expenses = expenses,
                        onExpense = onExpense,
                        onSplitBill = onSplitBill,
                        onBudget = onBudget
                    )
                    1 -> ScreenPlaceholder("Analytics")
                    2 -> SplitBillScreen(
                        userId = userId ?: "",
                        viewModel = viewModel(),
                        onBillClick = { },
                        onCreateClick = onAddSplitBill,
                        onBack = { selectedTab = 0 }
                    )
                    3 -> ProfileScreen(userId = userId, onSignOut = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    expenses: List<Expense>,
    onExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onBudget: () -> Unit
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
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        DashboardHeader()
        Spacer(modifier = Modifier.height(22.dp))
        BalanceCard(balance = balance)
        Spacer(modifier = Modifier.height(18.dp))
        MonthlyOverview(income = income, expenses = monthlyExpenses)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Quick Actions",
            color = MoneeInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        QuickActionGrid(onExpense = onExpense, onSplitBill = onSplitBill, onBudget = onBudget)
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
            color = MoneeInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "•",
            color = MoneeAmber,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(text = "Home", color = MoneeMuted, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(52.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MoneeYellowSoft)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(text = "🙂", fontSize = 16.sp) }
    }
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "Hello, User! 👋",
            color = MoneeInk,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Here's your financial overview",
            color = MoneeMuted,
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
            .moneeHero(RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Total Balance  ◉", color = MoneeMuted, fontSize = 12.sp)
            Text(
                text = formatCurrency(balance),
                color = MoneeInk,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text(
                text = "↗ +12.4%",
                color = MoneeInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthlyOverview(income: Double, expenses: Double) {
    val shape = RoundedCornerShape(22.dp)
    val monthLabel = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardColor)
            .border(1.dp, MoneeBorder, shape)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "This Month", color = MoneeInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MoneeYellowSoft)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text(text = monthLabel, color = MoneeMuted, fontSize = 11.sp) }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthlyMetric("Income", formatCurrency(income), MoneeAmber, Modifier.weight(1f))
            MonthlyMetric("Expenses", formatCurrency(expenses), MoneeRed, Modifier.weight(1f), amountColor = MoneeRed)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SpendingChart(expenses = expenses)
    }
}

@Composable
private fun MonthlyMetric(
    label: String, amount: String, dotColor: Color,
    modifier: Modifier = Modifier, amountColor: Color = MoneeInk
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MoneeYellowSoft.copy(alpha = 0.5f))
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(text = "  $label", color = MoneeMuted, fontSize = 11.sp)
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
            val highlighted = i == 1 || i == 3 || i == 4
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(v.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(MoneeButtonBrush, alpha = if (highlighted) 1f else 0.55f)
                )
                Text(labels[i], color = MoneeMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
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
private fun QuickActionGrid(
    onExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onBudget: () -> Unit
) {
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
            QuickAction("⊘", "Budget", Modifier.weight(1f), onClick = onBudget)
        }
    }
}

@Composable
private fun QuickAction(
    icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50.dp)
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(CardColor)
            .border(1.dp, MoneeBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MoneeYellowSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = MoneeAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = MoneeInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DashboardBottomBar(
    selectedTab: Int, onTabSelected: (Int) -> Unit, onAdd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(CardColor)) {
        HorizontalDivider(color = MoneeBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()          // keep the bar clear of the gesture / nav bar
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("⌂", "Home", selectedTab == 0) { onTabSelected(0) }
            BottomItem("⌁", "Analytics", selectedTab == 1) { onTabSelected(1) }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MoneeButtonBrush)
                    .clickable(role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = MoneeInk, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }

            BottomItem("↗", "Split Bill", selectedTab == 2) { onTabSelected(2) }
            BottomItem("♙", "Profile", selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
private fun BottomItem(
    icon: String, label: String, selected: Boolean, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MoneeYellowSoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp, color = if (selected) MoneeInk else MoneeMuted)
        Text(
            label,
            fontSize = 10.sp,
            color = if (selected) MoneeInk else MoneeMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
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
        Text(text, color = MoneeInk, fontSize = 20.sp)
    }
}

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Expense
import com.example.miniproject.data.Income
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.repository.IncomeRepository
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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val CardColor = Color.White
private val IncomeOrange = Color(0xFFFF8A1F)

@Composable
fun DashboardScreen(
    userId: String? = null,
    onAdd: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onExpense: () -> Unit = {},
    onSplitBill: () -> Unit = {},
    onAddSplitBill: () -> Unit = {},
    onBillClick: (String) -> Unit = {},
    onBudget: () -> Unit = {},
    onSavings: () -> Unit = {},
    onReports: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    val repository = remember { ExpenseRepository() }
    val incomeRepository = remember { IncomeRepository() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var userName by remember { mutableStateOf<String?>(null) }

    // Logged-in user's name from the profiles table (name / full_name / username)
    LaunchedEffect(userId) {
        userName = if (userId == null) null else withContext(Dispatchers.IO) {
            try {
                val row = SupabaseClient.instance.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<JsonObject>()
                listOf("name", "full_name", "username").firstNotNullOfOrNull { key ->
                    (row?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    LaunchedEffect(userId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            expenses = if (userId == null) {
                emptyList()
            } else {
                repository.getExpenses(userId).getOrDefault(emptyList())
            }
            incomes = if (userId == null) {
                emptyList()
            } else {
                incomeRepository.getIncome(userId).getOrDefault(emptyList())
            }
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
                        userName = userName,
                        expenses = expenses,
                        incomes = incomes,
                        onAddIncome = onAddIncome,
                        onExpense = onExpense,
                        onSplitBill = onSplitBill,
                        onBudget = onBudget,
                        onSavings = onSavings,
                        onReports = onReports
                    )
                    1 -> FinancialReportScreen(userId = userId, onBack = { selectedTab = 0 })
                    2 -> SplitBillScreen(
                        userId = userId ?: "",
                        viewModel = viewModel(),
                        onBillClick = onBillClick,
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
    userName: String?,
    expenses: List<Expense>,
    incomes: List<Income>,
    onAddIncome: () -> Unit,
    onExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onBudget: () -> Unit,
    onSavings: () -> Unit,
    onReports: () -> Unit
) {
    val currentMonth = LocalDate.now().toString().substring(0, 7)
    val monthlyExpenses = expenses
        .filter { it.date.startsWith(currentMonth) }
        .sumOf { it.amount }
    val income = incomes
        .filter { it.incomeDate.startsWith(currentMonth) }
        .sumOf { it.amount }
    val balance = income - monthlyExpenses

    // Real change vs last month's balance (income - expenses)
    val prevMonth = LocalDate.now().minusMonths(1).toString().substring(0, 7)
    val prevIncome = incomes.filter { it.incomeDate.startsWith(prevMonth) }.sumOf { it.amount }
    val prevExpenses = expenses.filter { it.date.startsWith(prevMonth) }.sumOf { it.amount }
    val prevBalance = prevIncome - prevExpenses
    val balanceChange: Double? =
        if (prevBalance != 0.0) (balance - prevBalance) / abs(prevBalance) * 100 else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        DashboardHeader(userName = userName)
        Spacer(modifier = Modifier.height(22.dp))
        BalanceCard(balance = balance, change = balanceChange)
        Spacer(modifier = Modifier.height(18.dp))
        MonthlyOverview(income = income, expenses = monthlyExpenses, allExpenses = expenses, allIncomes = incomes)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Quick Actions",
            color = MoneeInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        QuickActionGrid(
            onIncome = onAddIncome,
            onExpense = onExpense,
            onSplitBill = onSplitBill,
            onBudget = onBudget,
            onSavings = onSavings,
            onReports = onReports
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DashboardHeader(userName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Monee",
            color = MoneeInk,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Spacer(modifier = Modifier.width(2.dp))
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
            text = "Hello, ${userName ?: "there"}! 👋",
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
private fun BalanceCard(balance: Double, change: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .moneeHero(RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Total Balance", color = MoneeMuted, fontSize = 12.sp)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        change == null -> "–"
                        change >= 0 -> "↗ " + String.format(Locale.US, "%+.1f%%", change)
                        else -> "↘ " + String.format(Locale.US, "%.1f%%", change)
                    },
                    color = MoneeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (change == null) "no last month data" else "vs last month",
                    color = MoneeMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun MonthlyOverview(
    income: Double, expenses: Double,
    allExpenses: List<Expense>, allIncomes: List<Income>
) {
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
            MonthlyMetric("Income", formatCurrency(income), IncomeOrange, Modifier.weight(1f))
            MonthlyMetric("Expenses", formatCurrency(expenses), MoneeYellow, Modifier.weight(1f), amountColor = MoneeRed)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SpendingChart(expenses = allExpenses, incomes = allIncomes)
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
private fun SpendingChart(expenses: List<Expense>, incomes: List<Income>) {
    // Real data: income and expenses for each of the last 7 days (today is the last bar).
    // Each bar is stacked: expenses (yellow) at the bottom, income (orange) on top.
    val today = remember { LocalDate.now() }
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val expenseTotals = days.map { d ->
        val key = d.toString()
        expenses.filter { it.date.startsWith(key) }.sumOf { it.amount }
    }
    val incomeTotals = days.map { d ->
        val key = d.toString()
        incomes.filter { it.incomeDate.startsWith(key) }.sumOf { it.amount }
    }
    val maxTotal = days.indices.maxOfOrNull { expenseTotals[it] + incomeTotals[it] } ?: 0.0
    val maxBar = 72f

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Last 7 days", color = MoneeMuted, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(IncomeOrange))
            Text(text = "  Income    ", color = MoneeMuted, fontSize = 11.sp)
            Box(Modifier.size(8.dp).clip(CircleShape).background(MoneeYellow))
            Text(text = "  Expenses", color = MoneeMuted, fontSize = 11.sp)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { i, day ->
            val exp = expenseTotals[i]
            val inc = incomeTotals[i]
            val isToday = i == days.lastIndex
            val expH = if (maxTotal > 0.0 && exp > 0.0) maxOf(4f, (exp / maxTotal).toFloat() * maxBar) else 0f
            val incH = if (maxTotal > 0.0 && inc > 0.0) maxOf(4f, (inc / maxTotal).toFloat() * maxBar) else 0f
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier
                        .width(30.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                ) {
                    if (expH == 0f && incH == 0f) {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(MoneeBorder))
                    }
                    if (incH > 0f) {
                        Box(Modifier.fillMaxWidth().height(incH.dp).background(IncomeOrange))
                    }
                    if (expH > 0f) {
                        Box(Modifier.fillMaxWidth().height(expH.dp).background(MoneeYellow))
                    }
                }
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    color = if (isToday) MoneeInk else MoneeMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 5.dp)
                )
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
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onSplitBill: () -> Unit,
    onBudget: () -> Unit,
    onSavings: () -> Unit,
    onReports: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("✓", "Income", Modifier.weight(1f), onClick = onIncome)
            QuickAction("📄", "Expenses", Modifier.weight(1f), onClick = onExpense)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("📈", "Analytics", Modifier.weight(1f), onClick = onReports)
            QuickAction("📋", "Split Bill", Modifier.weight(1f), onClick = onSplitBill)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("🐷", "Savings", Modifier.weight(1f), onClick = onSavings)
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

package com.example.miniproject.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.MoneeTab
import com.example.miniproject.screen.management.*
import com.example.miniproject.ui.theme.moneeBackground
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// Each NavGraph destination opens one management page with its own standard app bar.
@Composable
fun BudgetScreen(userId: String?, onBack: () -> Unit, onManageExpenses: () -> Unit) {
    FinanceManagementScreen(userId, MoneeTab.BUDGET, onBack)
}

@Composable
fun SavingsScreen(userId: String?, onBack: () -> Unit) {
    FinanceManagementScreen(userId, MoneeTab.SAVINGS, onBack)
}

@Composable
fun FinancialReportScreen(
    userId: String?,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onAdd: () -> Unit = {},
    onSplitBill: () -> Unit = {},
    onProfile: () -> Unit = {},
) {
    FinanceManagementScreen(
        userId,
        MoneeTab.ANALYTICS,
        onBack,
        onHome,
        onAdd,
        onSplitBill,
        onProfile,
    )
}

@Composable
private fun FinanceManagementScreen(
    userId: String?,
    initialTab: MoneeTab,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onAdd: () -> Unit = {},
    onSplitBill: () -> Unit = {},
    onProfile: () -> Unit = {},
) {
    val budget: BudgetViewModel = viewModel(key = "management-budget-$userId")
    val savings: SavingsViewModel = viewModel(key = "management-savings-$userId")
    val reports: FinancialReportViewModel = viewModel(key = "management-reports-$userId")
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message: (String) -> Unit = { text ->
        scope.launch { snackbar.showSnackbar(text) }
        Unit
    }
    LaunchedEffect(userId, refresh) {
        error = null
        runCatching {
                requireNotNull(userId) { "Please sign in to manage your finances." }
                // Fetch independent Supabase data in parallel while the page stays visible.
                coroutineScope {
                    awaitAll(
                        async { budget.load() },
                        async { savings.load() },
                        async { reports.load() },
                    )
                }
                reports.currentUserName = savings.currentUserName
            }
            .onFailure { error = it.message ?: "Unable to load your account." }
        error?.let { message ->
            if (
                snackbar.showSnackbar(message, actionLabel = "Retry") ==
                    SnackbarResult.ActionPerformed
            ) {
                refresh++
            }
        }
    }
    Box(Modifier.fillMaxSize().moneeBackground()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (initialTab == MoneeTab.ANALYTICS) {
                    DashboardBottomBar(
                        selectedTab = 1,
                        onTabSelected = { tab ->
                            when (tab) {
                                0 -> onHome()
                                2 -> onSplitBill()
                                3 -> onProfile()
                            }
                        },
                        onAdd = onAdd,
                    )
                }
            },
            topBar = {
                Row(
                    modifier =
                        Modifier.statusBarsPadding().fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MoneeColors.InkDark,
                        )
                    }
                    Text(
                        text =
                            when (initialTab) {
                                MoneeTab.BUDGET -> "Budget Management"
                                MoneeTab.SAVINGS -> "Savings Management"
                                MoneeTab.ANALYTICS -> "Analytics & Reports"
                            },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneeColors.InkDark,
                    )
                    Spacer(Modifier.width(48.dp))
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                when (initialTab) {
                    MoneeTab.BUDGET -> BudgetManagementScreen(budget, message)
                    MoneeTab.SAVINGS -> SavingsManagementScreen(savings, message)
                    MoneeTab.ANALYTICS ->
                        AnalyticsReportsScreen(
                            reports,
                            budget.budgets,
                            savings.savingsGoals,
                            message,
                        )
                }
            }
        }
    }
}

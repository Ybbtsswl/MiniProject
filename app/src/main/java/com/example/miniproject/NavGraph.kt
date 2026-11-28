package com.example.miniproject

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.miniproject.repository.AuthRepository
import com.example.miniproject.screen.*
import com.example.miniproject.screen.auth.AuthScreen
import com.example.miniproject.screen.auth.RegistrationOtpScreen
import com.example.miniproject.screen.auth.ForgotPasswordScreen
import com.example.miniproject.screen.onboarding.OnboardingRoute
import com.example.miniproject.viewmodel.SplitBillViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import android.net.Uri


sealed class Screen(val route: String) {
    object Startup : Screen("startup")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth/{isRegister}") {
        fun createRoute(isRegister: Boolean) = "auth/$isRegister"
    }
    object RegistrationOtp : Screen("registration_otp/{email}/{name}") {
        fun createRoute(email: String, name: String) =
            "registration_otp/${Uri.encode(email)}/${Uri.encode(name)}"
    }
    object ForgotPassword : Screen("forgot_password")
    object Dashboard : Screen("dashboard")
    object Income : Screen("income")
    object Expense : Screen("expense")
    object AllExpenses : Screen("all_expenses")
    object Budget : Screen("budget")
    object Savings : Screen("savings")
    object Reports : Screen("reports")
    object AddTransaction : Screen("add_transaction/{userId}?income={income}") {
        fun createRoute(userId: String, income: Boolean = false) =
            "add_transaction/$userId?income=$income"
    }
    object SplitBill : Screen("split_bill/{userId}") {
        fun createRoute(userId: String) = "split_bill/$userId"
    }
    object AllSplitBills : Screen("all_split_bills/{userId}") {
        fun createRoute(userId: String) = "all_split_bills/$userId"
    }
    object AddSplitBill : Screen("add_split_bill/{userId}") {
        fun createRoute(userId: String) = "add_split_bill/$userId"
    }
    object SplitBillDetail : Screen("split_bill_detail/{billId}") {
        fun createRoute(billId: String) = "split_bill_detail/$billId"
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Startup.route
) {
    val splitBillViewModel: SplitBillViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Startup.route) {
            // Supabase Auth persists the session on the device. Check it before
            // showing onboarding so returning users go straight to the dashboard.
            LaunchedEffect(Unit) {
                val destination = if (supabaseClient.auth.currentSessionOrNull() != null) {
                    Screen.Dashboard.route
                } else {
                    // A logged-out user should always see the login/register
                    // screen, including after an app restart.
                    Screen.Auth.createRoute(isRegister = false)
                }

                navController.navigate(destination) {
                    popUpTo(Screen.Startup.route) { inclusive = true }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(Screen.Onboarding.route) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Screen.Auth.createRoute(isRegister = true)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Auth.route,
            arguments = listOf(
                navArgument("isRegister") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val isRegister = backStackEntry.arguments?.getBoolean("isRegister") ?: false
            val authRepository = remember { AuthRepository() }
            AuthScreen(
                isRegister = isRegister,
                onModeChange = { newMode ->
                    navController.navigate(Screen.Auth.createRoute(newMode)) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onRegistrationOtpRequired = { email, name ->
                    navController.navigate(Screen.RegistrationOtp.createRoute(email, name)) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onAuthenticate = { request ->
                    authRepository.authenticate(request)
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authRepository = remember { AuthRepository() }
            ForgotPasswordScreen(
                onRequestOtp = { email -> authRepository.requestPasswordRecovery(email) },
                onVerifyOtp = { email, token -> authRepository.verifyRecoveryOtp(email, token) },
                onUpdatePassword = { password -> authRepository.updateRecoveredPassword(password) },
                onCompleted = {
                    navController.navigate(Screen.Auth.createRoute(false)) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RegistrationOtp.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = Uri.decode(backStackEntry.arguments?.getString("email").orEmpty())
            val name = Uri.decode(backStackEntry.arguments?.getString("name").orEmpty())
            val authRepository = remember { AuthRepository() }

            RegistrationOtpScreen(
                email = email,
                onVerify = { token -> authRepository.verifyRegistrationOtp(email, token, name) },
                onResend = { authRepository.resendRegistrationOtp(email) },
                onVerified = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.RegistrationOtp.route) { inclusive = true }
                    }
                },
                onBack = { navController.navigate(Screen.Auth.createRoute(true)) {
                    popUpTo(Screen.RegistrationOtp.route) { inclusive = true }
                } }
            )
        }

        composable(Screen.Dashboard.route) {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            val scope = rememberCoroutineScope()

            DashboardScreen(
                userId = userId,
                onAdd = {
                    userId?.let { navController.navigate(Screen.AddTransaction.createRoute(it)) }
                },
                onAddIncome = {
                    navController.navigate(Screen.Income.route)
                },
                onExpense = {
                    navController.navigate(Screen.Expense.route)
                },
                onSplitBill = {
                    userId?.let { navController.navigate(Screen.SplitBill.createRoute(it)) }
                },
                onBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onSavings = { navController.navigate(Screen.Savings.route) },
                onReports = { navController.navigate(Screen.Reports.route) },
                onAddSplitBill = {
                    userId?.let { navController.navigate(Screen.AddSplitBill.createRoute(it)) }
                },
                onBillClick = { billId ->
                    navController.navigate(Screen.SplitBillDetail.createRoute(billId))
                }
            ) {
                scope.launch {
                    supabaseClient.auth.signOut()
                    navController.navigate(Screen.Auth.createRoute(false)) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Expense.route) {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            ExpenseScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onAddClick = {
                    userId?.let { navController.navigate(Screen.AddTransaction.createRoute(it)) }
                },
                onSeeAllClick = {
                    navController.navigate(Screen.AllExpenses.route)
                }
            )
        }

        composable(Screen.Income.route) {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            if (userId != null) {
                IncomeScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onAddIncome = {
                        navController.navigate(Screen.AddTransaction.createRoute(userId, income = true))
                    }
                )
            }
        }

        composable(Screen.AllExpenses.route) {
            AllExpensesScreen(
                userId = supabaseClient.auth.currentUserOrNull()?.id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                userId = supabaseClient.auth.currentUserOrNull()?.id,
                onBack = { navController.popBackStack() },
                onManageExpenses = { navController.navigate(Screen.Expense.route) }
            )
        }

        composable(Screen.Savings.route) {
            SavingsScreen(
                userId = supabaseClient.auth.currentUserOrNull()?.id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            FinancialReportScreen(
                userId = supabaseClient.auth.currentUserOrNull()?.id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("income") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            AddTransactionScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                initialIsExpense = backStackEntry.arguments?.getBoolean("income") != true
            )
        }

        composable(
            route = Screen.AddSplitBill.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!

            AddSplitBillScreen(
                userId = userId,
                viewModel = splitBillViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    splitBillViewModel.loadBills(userId)   // so the new bill shows in the list
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.SplitBill.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!

            LaunchedEffect(userId) {
                splitBillViewModel.loadGroups(userId)
            }

            SplitBillScreen(
                userId = userId,
                viewModel = splitBillViewModel,
                onBillClick = { billId ->
                    navController.navigate(Screen.SplitBillDetail.createRoute(billId))
                },
                onCreateClick = {
                    navController.navigate(Screen.AddSplitBill.createRoute(userId))
                },
                onBack = { navController.popBackStack() },
                onSeeAllClick = {
                    navController.navigate(Screen.AllSplitBills.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.AllSplitBills.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!

            AllSplitBillsScreen(
                userId = userId,
                viewModel = splitBillViewModel,
                onBillClick = { billId ->
                    navController.navigate(Screen.SplitBillDetail.createRoute(billId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SplitBillDetail.route,
            arguments = listOf(
                navArgument("billId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId")!!
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            val scope = rememberCoroutineScope()
            val uiState by splitBillViewModel.uiState.collectAsState()

            var detailData by remember { mutableStateOf<SplitBillViewModel.BillDetailData?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var loadError by remember { mutableStateOf<String?>(null) }

            fun reload(refreshList: Boolean) {
                scope.launch {
                    splitBillViewModel.loadBillDetail(billId)
                        .onSuccess { detailData = it; loadError = null }
                        .onFailure { loadError = it.message ?: "Failed to load bill" }
                    isLoading = false
                    if (refreshList && userId != null) splitBillViewModel.loadBills(userId)
                }
            }

            LaunchedEffect(billId) { reload(refreshList = false) }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                detailData != null -> {
                    val data = detailData!!
                    SplitBillDetailScreen(
                        splitBill = data.bill,
                        members = data.members,
                        items = data.items,
                        allocations = data.allocations,
                        currentUserId = userId ?: "",
                        errorMessage = uiState.error,
                        onBack = { navController.popBackStack() },
                        onSettleMember = { memberId ->
                            splitBillViewModel.settleMember(billId, memberId, userId) {
                                reload(refreshList = true)
                            }
                        },
                        onSettleBill = {
                            splitBillViewModel.settleWholeBill(billId) {
                                reload(refreshList = true)
                            }
                        }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(loadError ?: "Bill not found")
                    }
                }
            }
        }
    }
}

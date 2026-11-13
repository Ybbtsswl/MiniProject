package com.example.miniproject

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.miniproject.repository.AuthRepository
import com.example.miniproject.screen.*
import com.example.miniproject.screen.auth.AuthScreen
import com.example.miniproject.screen.onboarding.OnboardingRoute
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.example.miniproject.data.Expense
import com.example.miniproject.viewmodel.ExpenseViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth/{isRegister}") {
        fun createRoute(isRegister: Boolean) = "auth/$isRegister"
    }
    object Dashboard : Screen("dashboard")
    object Expense : Screen("expense")
    object AddTransaction : Screen("add_transaction/{userId}") {
        fun createRoute(userId: String) = "add_transaction/$userId"
    }
    object SplitBill : Screen("split_bill")
    object AddSplitBill : Screen("add_split_bill")
    object BillDetails : Screen("bill_details")
    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
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
                onAuthenticate = { request ->
                    authRepository.authenticate(request)
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val userId = SupabaseClient.instance.auth.currentUserOrNull()?.id
            val scope = rememberCoroutineScope()
            DashboardScreen(
                userId = userId,
                onAdd = {
                    userId?.let { navController.navigate(Screen.AddTransaction.createRoute(it)) }
                },
                onExpense = {
                    navController.navigate(Screen.Expense.route)
                },
                onSplitBill = {
                    navController.navigate(Screen.SplitBill.route)
                }
            ) {
                scope.launch {
                    SupabaseClient.instance.auth.signOut()
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Expense.route) {
            val userId = SupabaseClient.instance.auth.currentUserOrNull()?.id
            ExpenseScreen(
                userId = userId,
                onAddClick = {
                    userId?.let { navController.navigate(Screen.AddTransaction.createRoute(it)) }
                },
                onExpenseClick = { expense ->
                    // ✅ ONLY pass the ID — NO object through savedStateHandle!
                    if (expense.id.isNotEmpty()) {
                        navController.navigate(Screen.ExpenseDetail.createRoute(expense.id))
                    }
                }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            AddTransactionScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val viewModel: ExpenseViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val expense = uiState.expenses.find { it.id == expenseId }

            when {
                expense == null && uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD447))
                    }
                }
                expense != null -> {
                    ExpenseDetailScreen(
                        expense = expense,
                        onBack = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expense not found", color = Color.Black)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }


        composable(Screen.SplitBill.route) {
            SplitBillScreen(
                onCreateSplitBill = {
                    navController.navigate(Screen.AddSplitBill.route)
                },
                onBack = {
                    navController.popBackStack()
                },
                onBillClick = {
                    navController.navigate(Screen.BillDetails.route)
                }
            )
        }

        composable(Screen.AddSplitBill.route) {
            AddSplitBillScreen(
                onBack = { navController.popBackStack() },
                onCreateSplit = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BillDetails.route) {
            BillDetailsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSettle = {
                    navController.popBackStack()
                }
            )
        }
    }
}

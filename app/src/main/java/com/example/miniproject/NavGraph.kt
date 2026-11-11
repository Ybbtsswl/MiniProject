package com.example.miniproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.AuthRepository
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.screen.DashboardScreen
import com.example.miniproject.screen.ExpenseScreen
import com.example.miniproject.screen.AddTransactionScreen
import com.example.miniproject.screen.auth.AuthScreen
import com.example.miniproject.screen.onboarding.OnboardingRoute
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth/{isRegister}") {
        fun createRoute(isRegister: Boolean) = "auth/$isRegister"
    }
    object Dashboard : Screen("dashboard")
    object Expense : Screen("expense")
    object AddTransaction : Screen("add_transaction") // unified screen
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
                onExpense = {
                    navController.navigate(Screen.Expense.route)
                },
                onSignOut = {
                    scope.launch {
                        SupabaseClient.instance.auth.signOut()
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Expense.route) {
            ExpenseScreen(
                onAddClick = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onSplitBillClick = {
                    // TODO: navigate to Split Bill screen later
                }
            )
        }

        composable(Screen.AddTransaction.route) {
            val userId = SupabaseClient.instance.auth.currentUserOrNull()?.id
            val scope = rememberCoroutineScope()
            val expenseRepository = remember { ExpenseRepository() }

            AddTransactionScreen(
                onSaveExpense = { amount, category, description, date ->
                    if (userId != null) {
                        scope.launch {
                            expenseRepository.addExpense(
                                Expense(
                                    userId = userId,
                                    amount = amount,
                                    category = category,
                                    description = description,
                                    date = date
                                )
                            ).onSuccess {
                                navController.popBackStack()
                            }
                        }
                    }
                },
                onSaveIncome = { amount, category, description, date, _ ->
                    // TODO: implement IncomeRepository later
                    // For now, just go back
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

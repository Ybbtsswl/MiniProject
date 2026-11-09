package com.example.miniproject

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.miniproject.screen.AddExpenseScreen
import com.example.miniproject.screen.DashboardScreen
import com.example.miniproject.screen.ExpenseScreen
import com.example.miniproject.screen.auth.AuthScreen
import com.example.miniproject.screen.auth.WelcomeScreen
import com.example.miniproject.screen.onboarding.OnboardingRoute

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth/{isRegister}") {
        fun createRoute(isRegister: Boolean) = "auth/$isRegister"
    }
    object Welcome : Screen("welcome")
    object Dashboard : Screen("dashboard")
    object AddExpense : Screen("add_expense")
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
            AuthScreen(
                isRegister = isRegister,
                onModeChange = { newMode ->
                    navController.navigate(Screen.Auth.createRoute(newMode)) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onSuccess = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onSignOut = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onEnterDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddExpense = {
                    navController.navigate(Screen.AddExpense.route)
                }
            )
        }

        composable("add_expense") {
            AddExpenseScreen(
                onSave = { amount, category, description, date ->

                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

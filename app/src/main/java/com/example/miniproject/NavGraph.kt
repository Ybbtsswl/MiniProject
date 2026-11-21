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
import com.example.miniproject.screen.onboarding.OnboardingRoute
import com.example.miniproject.viewmodel.SplitBillViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String) {
    object Startup : Screen("startup")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth/{isRegister}") {
        fun createRoute(isRegister: Boolean) = "auth/$isRegister"
    }
    object Dashboard : Screen("dashboard")
    object Expense : Screen("expense")
    object Budget : Screen("budget")
    object AddTransaction : Screen("add_transaction/{userId}") {
        fun createRoute(userId: String) = "add_transaction/$userId"
    }
    object SplitBill : Screen("split_bill/{userId}") {
        fun createRoute(userId: String) = "split_bill/$userId"
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
                    Screen.Onboarding.route
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
                onAuthenticate = { request ->
                    authRepository.authenticate(request)
                }
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
                onExpense = {
                    navController.navigate(Screen.Expense.route)
                },
                onSplitBill = {
                    userId?.let { navController.navigate(Screen.SplitBill.createRoute(it)) }
                },
                onBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onAddSplitBill = {
                    userId?.let { navController.navigate(Screen.AddSplitBill.createRoute(it)) }
                }
            ) {
                scope.launch {
                    supabaseClient.auth.signOut()
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Expense.route) {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            ExpenseScreen(
                userId = userId,
                onAddClick = {
                    userId?.let { navController.navigate(Screen.AddTransaction.createRoute(it)) }
                }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                userId = supabaseClient.auth.currentUserOrNull()?.id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            AddTransactionScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddSplitBill.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            val splitBillViewModel: SplitBillViewModel = viewModel()
            val scope = rememberCoroutineScope()

            AddSplitBillScreen(
                userId = userId,
                viewModel = splitBillViewModel,
                onBack = { navController.popBackStack() },
                onCreateSplit = { bill, members, items, allocations ->
                    splitBillViewModel.createSplitBill(
                        bill = bill,
                        members = members,
                        items = items,
                        allocations = allocations,
                        onSuccess = { navController.popBackStack() },
                        onError = { /* handle error */ }
                    )
                },
                onSaveGroup = { group ->
                    kotlinx.coroutines.runBlocking {
                        splitBillViewModel.createGroup(group)
                    }
                },

                onLoadGroup = { groupId ->
                    kotlinx.coroutines.runBlocking {
                        splitBillViewModel.loadGroupMembers(groupId)
                    }
                },

                onLookupUser = { lookupUserId ->
                    try {
                        val userJson = supabaseClient.postgrest["profiles"]
                            .select { filter { eq("id", lookupUserId) }; limit(1) }
                            .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                        if (userJson != null) {
                            val name = userJson["username"]?.toString()
                                ?.removeSurrounding("\"")
                                ?: userJson["name"]?.toString()
                                    ?.removeSurrounding("\"")
                            if (!name.isNullOrEmpty()) {
                                Result.success(name)
                            } else {
                                Result.failure(Exception("Name not found"))
                            }
                        } else {
                            Result.failure(Exception("User not found"))
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
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
            val splitBillViewModel: SplitBillViewModel = viewModel()

            LaunchedEffect(userId) {
                splitBillViewModel.loadGroups(userId)
            }

            val groupsState by splitBillViewModel.uiState.collectAsState()

            SplitBillScreen(
                userId = userId,
                viewModel = splitBillViewModel,
                onBillClick = { billId ->
                    navController.navigate(Screen.SplitBillDetail.createRoute(billId))
                },
                onCreateClick = {
                    navController.navigate(Screen.AddSplitBill.createRoute(userId))
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
            val splitBillViewModel: SplitBillViewModel = viewModel()
            val scope = rememberCoroutineScope()

            var detailData by remember { mutableStateOf<SplitBillViewModel.BillDetailData?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(billId) {
                scope.launch {
                    val result = splitBillViewModel.loadBillDetail(billId)
                    detailData = result.getOrNull()
                    isLoading = false
                }
            }

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
                        onBack = { navController.popBackStack() },
                        onSettleAllocation = { allocationId ->
                            scope.launch {
                                if (userId != null) {
                                    splitBillViewModel.settleAllocation(
                                        splitAllocationId = allocationId,
                                        billId = billId,
                                        userId = userId,
                                        selectedMonth = java.time.LocalDate.now()
                                    )
                                    // Refresh data
                                    detailData = splitBillViewModel.loadBillDetail(billId).getOrNull()
                                }
                            }
                        },
                        onSettleBill = {
                            scope.launch {
                                if (userId != null) {
                                    splitBillViewModel.settleBill(
                                        billId = billId,
                                        userId = userId,
                                        selectedMonth = java.time.LocalDate.now()
                                    )
                                    navController.popBackStack()
                                }
                            }
                        }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("Bill not found")
                    }
                }
            }
        }
    }
}

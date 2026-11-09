package com.example.miniproject.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("📊", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("💵", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Income") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("💰", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Expenses") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("📈", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Budget") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Text("🎯", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Savings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Text("👤", style = MaterialTheme.typography.titleLarge) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardContent()
                1 -> IncomeScreen()
                2 -> ExpenseScreen(
                    onAddClick = onAddExpense,
                    onSplitBillClick = { }
                )
                3 -> BudgetScreen()
                4 -> SavingsGoalScreen()
                5 -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun DashboardContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "📊 Dashboard\nWelcome back!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable fun IncomeScreen() = ScreenPlaceholder("💵 Income")
@Composable fun BudgetScreen() = ScreenPlaceholder("📈 Budget")
@Composable fun SavingsGoalScreen() = ScreenPlaceholder("🎯 Savings Goal")
@Composable fun ProfileScreen() = ScreenPlaceholder("👤 Profile")

@Composable
fun ScreenPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

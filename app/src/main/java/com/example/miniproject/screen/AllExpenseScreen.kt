package com.example.miniproject.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeFieldColors
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.util.Locale

/**
 * Every expense record (all months), newest first, with a search box.
 * Read-only: edit and delete stay on the main Expenses screen.
 */
@Composable
fun AllExpensesScreen(
    userId: String?,
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(userId) {
        userId?.let { viewModel.loadExpenses(it) }   // loads every month
    }

    val expenses = uiState.expenses
    val results = remember(expenses, query) {
        val q = query.trim()
        val matches = if (q.isEmpty()) {
            expenses
        } else {
            expenses.filter { e ->
                e.description.contains(q, ignoreCase = true) ||
                        e.category.contains(q, ignoreCase = true) ||
                        e.date.contains(q) ||
                        String.format(Locale.US, "%.2f", e.amount).contains(q)
            }
        }
        matches.sortedByDescending { it.date }   // ISO dates sort correctly as text
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .moneeBackground()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MoneeInk
                    )
                }
                Text(
                    text = "All Expenses",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search description, category, date or amount") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MoneeMuted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MoneeMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = moneeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MoneeYellow)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading data",
                            color = MoneeRed,
                            fontSize = 14.sp
                        )
                    }
                }

                expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No expenses yet.", color = MoneeMuted, fontSize = 14.sp)
                    }
                }

                else -> {
                    Text(
                        text = "${results.size} record${if (results.size == 1) "" else "s"} · " +
                                formatExpenseAmount(results.sumOf { it.amount }),
                        fontSize = 12.sp,
                        color = MoneeMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses match \"${query.trim()}\"",
                                color = MoneeMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(results) { exp ->
                                ExpenseItemRow(exp = exp)
                            }
                        }
                    }
                }
            }
        }
    }
}

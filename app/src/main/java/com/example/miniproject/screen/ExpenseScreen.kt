package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.repository.ExpenseRepository

@Composable
fun ExpenseScreen(
    onAddClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    userId: String? = null
) {
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    val repository = remember { ExpenseRepository() }

    LaunchedEffect(userId) {
        expenses = if (userId == null) {
            emptyList()
        } else {
            repository.getExpenses(userId).getOrDefault(emptyList())
        }
    }

    val total = expenses.sumOf { it.amount }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Hello, User! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "Here's your financial overview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Balance-style card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Total Spent", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "RM ${String.format("%.2f", total)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "${expenses.size} transactions this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAction("➕", "Add Expense", onAddClick, Modifier.weight(1f))
                QuickAction("🧾", "Split Bill", onSplitBillClick, Modifier.weight(1f))
            }

            Text(
                "Recent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )

            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses yet\nTap \"Add Expense\" to get started!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(expenses) { exp ->
                        ExpenseItem(exp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ExpenseItem(exp: Expense) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(categoryEmoji(exp.category), fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exp.category,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${exp.description} · ${exp.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- RM ${String.format("%.2f", exp.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = MoneeRed,
                    style = MaterialTheme.typography.titleMedium
                )
                if (exp.isFromSplit) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Split",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "food" -> "🍜"
    "transport" -> "🚌"
    "shopping" -> "🛍️"
    "bills" -> "🧾"
    "entertainment" -> "🎬"
    else -> "💰"
}

@Composable
@Preview(showBackground = true, name = "Expense Screen")
fun ExpenseScreenPreview() {
    MiniProjectTheme {
        ExpenseScreen(
            onAddClick = {},
            onSplitBillClick = {}
        )
    }
}

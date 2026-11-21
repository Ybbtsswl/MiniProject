package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Category
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeButtonBrush
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeCream
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.MoneeYellowSoft
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeCardColors
import com.example.miniproject.ui.theme.moneeFieldColors
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExpenseScreen(
    onAddClick: () -> Unit,
    onSeeAllClick: () -> Unit = {},
    userId: String? = null,
    viewModel: ExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadCategories(it)
        }
    }

    LaunchedEffect(userId, selectedMonth) {
        userId?.let {
            viewModel.loadExpensesByMonth(it, selectedMonth)
        }
    }

    val expenses = uiState.expenses
    val displayMonth = try {
        LocalDate.parse("$selectedMonth-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (e: Exception) {
        selectedMonth
    }
    val monthlyTotal = expenses.sumOf { it.amount }
    val monthChange = -12.4

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    color = MoneeInk,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "Expenses",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 20.sp,
                    color = MoneeMuted,
                    modifier = Modifier.clickable {
                        val prev = LocalDate.parse("$selectedMonth-01").minusMonths(1)
                        selectedMonth = prev.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }
                )
                Text(
                    text = displayMonth,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MoneeInk
                )
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = MoneeMuted,
                    modifier = Modifier.clickable {
                        val next = LocalDate.parse("$selectedMonth-01").plusMonths(1)
                        selectedMonth = next.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .moneeHero(RoundedCornerShape(20.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL EXPENSES THIS MONTH",
                        fontSize = 12.sp,
                        color = MoneeMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatExpenseAmount(monthlyTotal),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeInk,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(text = "↓ ", fontSize = 12.sp, color = MoneeGreen)
                        Text(
                            text = "${monthChange * -1}% less than last month",
                            fontSize = 12.sp,
                            color = MoneeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        Text(
                            text = "No expenses yet.\nTap + to add your first expense!",
                            color = MoneeMuted,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Summary by category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneeInk
                    )
                    Text(
                        text = "Tap an item to view details",
                        fontSize = 11.sp,
                        color = MoneeMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val categoryTotals = expenses
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { exp -> exp.amount } }
                        .toList()
                        .sortedByDescending { (_, amount) -> amount }

                    categoryTotals.take(3).forEach { (cat, amt) ->
                        CategoryBar(cat, amt, monthlyTotal)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expense History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MoneeInk
                        )
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MoneeAmber,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSeeAllClick)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val tabNames = listOf("All") + categories.map { it.name }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tabNames) { tab ->
                            TabItem(
                                label = tab,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredExpenses = when (selectedTab) {
                        "All" -> expenses
                        else -> expenses.filter {
                            it.category.equals(selectedTab, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        if (filteredExpenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No transactions in this category",
                                        color = MoneeMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val grouped = filteredExpenses.groupBy { expenseDisplayDate(it.date) }
                            grouped.forEach { (date, itemsList) ->
                                item {
                                    Text(
                                        text = date,
                                        fontSize = 12.sp,
                                        color = MoneeMuted,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 0.dp)
                                    )
                                }
                                items(itemsList) { exp ->
                                    ExpenseItemRow(
                                        exp = exp,
                                        onItemClick = { selectedExpense = exp }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add button with a soft white fade so list items don't run into it
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.White))
                )
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            MoneeGradientButton(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = "+ Add Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    selectedExpense?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            onDismiss = { selectedExpense = null },
            onEdit = {
                selectedExpense = null
                editingExpense = expense
            },
            onDelete = {
                selectedExpense = null
                expense.expenseId.let { id ->
                    viewModel.deleteExpense(id) { success, _, _ ->
                        if (success) {
                            userId?.let { viewModel.loadExpensesByMonth(it, selectedMonth) }
                        }
                    }
                }
            }
        )
    }

    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            categories = categories,
            onDismiss = { editingExpense = null },
            onSave = { updatedExpense ->
                editingExpense = null
                viewModel.updateExpense(updatedExpense) { success, _ ->
                    if (success) {
                        userId?.let { viewModel.loadExpensesByMonth(it, selectedMonth) }
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryBar(label: String, amount: Double, total: Double) {
    val percentage = if (total > 0) (amount / total * 100).toInt().coerceIn(5, 100) else 0

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MoneeInk
            )
            Text(
                text = formatExpenseAmount(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MoneeInk
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MoneeButtonBrush)
            )
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    MoneeChip(selected = selected, onClick = onClick) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// internal: also used by AllExpensesScreen. Pass onItemClick = null for a non-clickable row.
@Composable
internal fun ExpenseItemRow(
    exp: Expense,
    onItemClick: (() -> Unit)? = null
) {
    val displayText = exp.description.ifEmpty {
        exp.category
    }

    val textSize = if (exp.description.isEmpty()) 15.sp else 14.sp
    val textWeight = if (exp.description.isEmpty()) FontWeight.SemiBold else FontWeight.Medium

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onItemClick != null) Modifier.clickable { onItemClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = moneeCardColors(),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MoneeYellowSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(text = expenseCategoryEmoji(exp.category), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayText,
                    fontSize = textSize,
                    fontWeight = textWeight,
                    color = MoneeInk
                )
                Text(
                    text = "${exp.category} · ${exp.date}",
                    fontSize = 11.sp,
                    color = MoneeMuted,
                    modifier = if (exp.description.isEmpty()) Modifier.padding(top = 2.dp) else Modifier
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "- ${formatExpenseAmount(exp.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeRed
                )
            }
        }
    }
}

@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MoneeCream,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneeInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MoneeMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .moneeHero(RoundedCornerShape(16.dp))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatExpenseAmount(expense.amount),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeRed
                        )
                        Text(
                            text = expense.category,
                            fontSize = 15.sp,
                            color = MoneeMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                DetailRow(
                    label = "Description",
                    value = expense.description.ifEmpty { "—" }
                )
                DetailRow(label = "Date", value = expense.date)
                if (expense.isFromSplit) {
                    DetailRow(label = "Source", value = "From Split Bill")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MoneeGradientButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneeRed.copy(alpha = 0.12f),
                            contentColor = MoneeRed
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MoneeMuted
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MoneeInk
        )
    }
}

@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var description by remember { mutableStateOf(expense.description ?: "") }
    var selectedCategoryId by remember { mutableStateOf(expense.categoryId) }
    var selectedDate by remember { mutableStateOf(expense.date) }

    val isFormValid by remember {
        derivedStateOf {
            amount.toDoubleOrNull() != null &&
                    amount.toDoubleOrNull()!! > 0 &&
                    selectedCategoryId.isNotEmpty() &&
                    selectedDate.isNotEmpty()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MoneeCream,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneeInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MoneeMuted
                        )
                    }
                }

                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MoneeMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Text("RM", fontWeight = FontWeight.SemiBold) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = moneeFieldColors()
                    )
                }

                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MoneeMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryId == cat.categoryId
                            MoneeChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = cat.categoryId }
                            ) {
                                Text("${cat.icon ?: "💰"} ${cat.name}", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MoneeMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = MoneeMuted)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = moneeFieldColors()
                    )
                }

                Column {
                    Text(
                        text = "Description (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MoneeMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("What was this for?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = moneeFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneeYellowSoft,
                            contentColor = MoneeInk
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                    MoneeGradientButton(
                        onClick = {
                            val updatedAmount = amount.toDoubleOrNull() ?: 0.0
                            val selectedCat = categories.find { it.categoryId == selectedCategoryId }
                            val updatedExpense = expense.copy(
                                amount = updatedAmount,
                                categoryId = selectedCategoryId,
                                category = selectedCat?.name ?: "",
                                date = selectedDate,
                                description = description.trim().ifEmpty { "" }
                            )
                            onSave(updatedExpense)
                        },
                        enabled = isFormValid,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

internal fun expenseCategoryEmoji(categoryName: String?): String {
    if (categoryName == null) return "💰"
    return when (categoryName.lowercase()) {
        "food", "food & drinks" -> "🍜"
        "transport" -> "🚌"
        "shopping" -> "🛍️"
        "groceries" -> "🛒"
        "bills" -> "📄"
        "entertainment" -> "🎬"
        else -> "💰"
    }
}

internal fun formatExpenseAmount(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

internal fun expenseDisplayDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        when {
            date.isEqual(today) -> "Today"
            date.isEqual(yesterday) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        }
    } catch (e: Exception) {
        dateStr
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Expense Screen")
@Composable
fun ExpenseScreenPreview() {
    MiniProjectTheme {
        ExpenseScreen(
            onAddClick = {},
            userId = null
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditExpenseDialogPreview() {
    MiniProjectTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .moneeBackground(),
            contentAlignment = Alignment.Center
        ) {
            EditExpenseDialog(
                expense = Expense(
                    expenseId = "1",
                    userId = "user1",
                    amount = 15.50,
                    categoryId = "CAT001",
                    category = "Food",
                    date = "2026-11-17",
                    description = "Lunch at restaurant"
                ),
                categories = listOf(
                    Category("CAT001", "", "Food", "🍜"),
                    Category("CAT002", "", "Transport", "🚌"),
                    Category("CAT003", "", "Shopping", "🛍️")
                ),
                onDismiss = {},
                onSave = {}
            )
        }
    }
}

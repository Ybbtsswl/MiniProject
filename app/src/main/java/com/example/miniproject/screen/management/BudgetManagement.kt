/*
 * ============================================================================
 * Module 1: Budget Management (FR-BGT-001 to FR-BGT-006)
 * File: BudgetManagement.kt
 * ============================================================================
 */
package com.example.miniproject.screen.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.miniproject.data.Category
import com.example.miniproject.screen.AddCategoryDialog
import com.example.miniproject.screen.CategorySelector
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

// ============================================================================
// 1. BUDGET DATA MODELS (MATCHING UML CLASS DIAGRAM)
// ============================================================================
data class BudgetExpense(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String, // YYYY-MM-DD
)

/**
 * UML Entity: Budget +String budgetId, +String userId, +String budgetName, +double amountLimit,
 * +double totalSpent, +date startDate, +date endDate, +String status, +datetime createdAt
 */
data class Budget(
    val budgetId: String,
    val userId: String,
    val budgetName: String,
    val amountLimit: Double,
    val totalSpent: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String = "ACTIVE", // "ACTIVE" | "WARNING" | "EXCEEDED"
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val category: String = "Food & Dining",
    val expenses: List<BudgetExpense> = emptyList(),
) {
    val remaining: Double
        get() = amountLimit - totalSpent

    val usagePct: Double
        get() = if (amountLimit > 0.0) (totalSpent / amountLimit) * 100.0 else 0.0
}

data class BudgetAlert(
    val id: String,
    val budgetId: String,
    val isDanger: Boolean,
    val message: String,
)

fun computeBudgetStatus(spent: Double, limit: Double): String {
    if (limit <= 0.0) return "ACTIVE"
    val pct = (spent / limit) * 100.0
    return when {
        pct >= 100.0 -> "EXCEEDED"
        pct >= 80.0 -> "WARNING"
        else -> "ACTIVE"
    }
}

// ============================================================================
// 2. BUDGET VIEWMODEL
// ============================================================================
// ============================================================================
// 3. BUDGET MANAGEMENT SCREEN & DIALOGS
// ============================================================================
@Composable
fun BudgetManagementScreen(
    vm: BudgetViewModel,
    categories: List<Category>,
    showMessage: (String) -> Unit,
    onAddBudget: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var deletingBudget by remember { mutableStateOf<Budget?>(null) }
    var detailsBudgetId by remember { mutableStateOf<String?>(null) }

    val totalAllocated = vm.budgets.sumOf { it.amountLimit }
    val totalSpent = vm.budgets.sumOf { it.totalSpent }
    val totalRemaining = totalAllocated - totalSpent
    val overallPct = if (totalAllocated > 0.0) (totalSpent / totalAllocated) * 100.0 else 0.0

    val allAlerts = vm.getActiveBudgetAlerts()
    val visibleAlerts = allAlerts.filterNot { it.id in vm.dismissedAlertIds }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Alert Banners
            if (visibleAlerts.isNotEmpty()) {
                items(visibleAlerts, key = { it.id }) { alert ->
                    Surface(
                        color = if (alert.isDanger) MoneeColors.DangerBg else MoneeColors.WarningBg,
                        shape = RoundedCornerShape(16.dp),
                        border =
                            BorderStroke(
                                1.dp,
                                if (alert.isDanger) MoneeColors.DangerBorder
                                else MoneeColors.WarningBorder,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint =
                                    if (alert.isDanger) MoneeColors.DangerRed
                                    else MoneeColors.WarningOrange,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (alert.isDanger) MoneeColors.DangerText
                                        else MoneeColors.WarningText,
                                )
                                Text(
                                    text = "View Details",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color =
                                        if (alert.isDanger) MoneeColors.DangerRed
                                        else MoneeColors.WarningOrange,
                                    modifier =
                                        Modifier.padding(top = 2.dp).clickable {
                                            detailsBudgetId = alert.budgetId
                                        },
                                )
                            }
                            IconButton(
                                onClick = { vm.dismissAlert(alert.id) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Overview Card (Yellow Gradient)
            item {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MoneeColors.BrandSoft,
                                        MoneeColors.BrandYellow,
                                        MoneeColors.BrandGold,
                                    )
                                )
                            )
                            .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "TOTAL MONTHLY BUDGET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark.copy(alpha = 0.75f),
                            )
                            Surface(
                                color = MoneeColors.InkDark.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%% Used", overallPct),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.InkDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatRM(totalAllocated),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MoneeColors.InkDark,
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (overallPct / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier =
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                            color =
                                when {
                                    overallPct >= 100.0 -> MoneeColors.DangerRed
                                    overallPct >= 80.0 -> MoneeColors.WarningOrange
                                    else -> MoneeColors.InkDark
                                },
                            trackColor = MoneeColors.InkDark.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MoneeColors.InkDark.copy(alpha = 0.12f))
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "Total Spent",
                                    fontSize = 10.sp,
                                    color = MoneeColors.InkDark.copy(alpha = 0.75f),
                                )
                                Text(
                                    formatRM(totalSpent),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoneeColors.InkDark,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Remaining Balance",
                                    fontSize = 10.sp,
                                    color = MoneeColors.InkDark.copy(alpha = 0.75f),
                                )
                                Text(
                                    formatRM(totalRemaining),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color =
                                        if (totalRemaining < 0) MoneeColors.DangerText
                                        else MoneeColors.InkDark,
                                )
                            }
                        }
                    }
                }
            }

            // Budget list heading. The primary action stays fixed at the bottom.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "YOUR BUDGETS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeColors.Slate500,
                        )
                        if (vm.dismissedAlertIds.isNotEmpty() && allAlerts.isNotEmpty()) {
                            Text(
                                "Alerts (${allAlerts.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MoneeColors.WarningOrange,
                                modifier = Modifier.clickable { vm.restoreAlerts() },
                            )
                        }
                    }
                }
            }

            // Budget Cards List
            if (vm.budgets.isEmpty()) {
                item {
                    Surface(
                        color = MoneeColors.CardWhite,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "No Budgets Yet",
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate700,
                            )
                            Text(
                                "Tap + Add Budget to create your first monthly budget.",
                                fontSize = 12.sp,
                                color = MoneeColors.Slate500,
                            )
                        }
                    }
                }
            } else {
                items(vm.budgets, key = { it.budgetId }) { budget ->
                    BudgetCardItem(
                        budget = budget,
                        onViewDetails = { detailsBudgetId = budget.budgetId },
                        onEdit = { editingBudget = budget },
                        onDelete = { deletingBudget = budget },
                    )
                }
            }
        }

        // Keep the main action reachable while the budget list scrolls behind it.
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, MoneeColors.CardWhite))
                    )
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = 16.dp)
        ) {
            MoneeGradientButton(
                onClick = onAddBudget,
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
            ) {
                Text("+ Add Budget", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    editingBudget?.let { b ->
        BudgetFormDialog(
            title = "Edit Budget",
            confirmText = "Save Changes",
            initial = b,
            categories = categories,
            onDismiss = { editingBudget = null },
            onSave = { name, amount, cat, start, end ->
                scope.launch {
                    try {
                        vm.updateBudget(b.budgetId, name, amount, cat, start, end)
                            .onSuccess {
                                editingBudget = null
                                showMessage(it)
                            }
                            .onFailure { showMessage(it.message ?: "Invalid input") }
                    } catch (error: Exception) {
                        showMessage(error.message ?: "Unable to save changes.")
                    }
                }
            },
        )
    }

    deletingBudget?.let { b ->
        ConfirmDeleteDialog(
            title = "Delete Budget",
            message = "Remove \"${b.budgetName}\" from your active budgets?",
            onDismiss = { deletingBudget = null },
            onConfirm = {
                scope.launch {
                    try {
                        vm.deleteBudget(b.budgetId)
                        deletingBudget = null
                        showMessage("Budget removed.")
                    } catch (error: Exception) {
                        showMessage(error.message ?: "Unable to save changes.")
                    }
                }
            },
        )
    }

    detailsBudgetId?.let { id ->
        val liveBudget = vm.budgets.find { it.budgetId == id }
        if (liveBudget != null) {
            BudgetDetailsDialog(
                budget = liveBudget,
                onDismiss = { detailsBudgetId = null },
                onAddExpense = { amount, date, desc ->
                    scope.launch {
                        try {
                            vm.addExpenseToBudget(liveBudget.budgetId, amount, date, desc)
                                .onSuccess { showMessage(it) }
                                .onFailure { showMessage(it.message ?: "Error adding expense") }
                        } catch (error: Exception) {
                            showMessage(error.message ?: "Unable to save changes.")
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun BudgetCardItem(
    budget: Budget,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val pct = budget.usagePct
    val barColor =
        when {
            pct >= 100.0 -> MoneeColors.DangerRed
            pct >= 80.0 -> MoneeColors.WarningOrange
            else -> MoneeColors.BrandGold
        }
    val badgeBg =
        when {
            pct >= 100.0 -> MoneeColors.DangerBg
            pct >= 80.0 -> MoneeColors.WarningBg
            else -> MoneeColors.BrandBadgeBg
        }
    val badgeText =
        when {
            pct >= 100.0 -> MoneeColors.DangerRed
            pct >= 80.0 -> MoneeColors.WarningText
            else -> MoneeColors.BrandDarkGold
        }

    Surface(
        color = MoneeColors.CardWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MoneeColors.Slate100),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        budget.budgetName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.InkDark,
                    )
                    Text(
                        "${budget.category} · ${budget.expenses.size} txn · ${budget.status}",
                        fontSize = 11.sp,
                        color = MoneeColors.Slate400,
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        "${pct.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MoneeColors.Slate100,
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Allocated", fontSize = 10.sp, color = MoneeColors.Slate400)
                    Text(
                        formatRM(budget.amountLimit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.InkDark,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spent", fontSize = 10.sp, color = MoneeColors.Slate400)
                    Text(
                        formatRM(budget.totalSpent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.InkDark,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (budget.remaining < 0) "Exceeded" else "Remaining",
                        fontSize = 10.sp,
                        color = MoneeColors.Slate400,
                    )
                    Text(
                        formatRM(abs(budget.remaining)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (budget.remaining < 0) MoneeColors.DangerRed
                            else MoneeColors.BrandDarkGold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MoneeColors.Slate100)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MoneeColors.BrandBadgeBg,
                            contentColor = MoneeColors.BrandDarkGold,
                        ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("View Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MoneeColors.Slate100,
                            contentColor = MoneeColors.Slate700,
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }
                IconButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MoneeColors.DangerBg),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MoneeColors.DangerRed,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Full-page create flow. Editing remains a compact dialog from an existing budget card. */
@Composable
fun AddBudgetScreen(
    vm: BudgetViewModel,
    userId: String?,
    categories: List<Category>,
    isLoadingCategories: Boolean,
    categoryViewModel: ExpenseViewModel,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var startDate by remember {
        mutableStateOf(LocalDate.now().withDayOfMonth(1).toString())
    }
    var endDate by remember {
        mutableStateOf(java.time.YearMonth.now().atEndOfMonth().toString())
    }
    var isSaving by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectionError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var endDateError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var isSavingCategory by remember { mutableStateOf(false) }
    var addCategoryError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null) {
            selectedCategoryId = categories.firstOrNull()?.categoryId
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Create a monthly spending limit and track it against your expenses.",
                color = MoneeColors.Slate500,
                fontSize = 13.sp,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Budget name") },
                placeholder = { Text("Example: Monthly food budget") },
                isError = nameError != null,
                supportingText = nameError?.let { message -> { Text(message) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    amountError = null
                },
                label = { Text("Budget amount (RM)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = amountError?.let { message -> { Text(message) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val selectedName =
                    categories.firstOrNull { it.categoryId == selectedCategoryId }?.name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Category", fontWeight = FontWeight.SemiBold)
                    selectedName?.let {
                        Text(
                            "Selected: $it",
                            color = MoneeColors.BrandDarkGold,
                            fontSize = 12.sp,
                        )
                    }
                }
                CategorySelector(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    isLoading = isLoadingCategories,
                    onSelect = {
                        selectedCategoryId = it.categoryId
                        selectionError = null
                    },
                    onAddCategory = {
                        addCategoryError = null
                        showAddCategory = true
                    },
                )
                selectionError?.let {
                    Text(it, color = MoneeColors.DangerRed, fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {
                        startDate = it
                        startDateError = null
                        endDateError = null
                    },
                    label = { Text("Start date") },
                    isError = startDateError != null,
                    supportingText = { Text(startDateError ?: "YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = {
                        endDate = it
                        endDateError = null
                    },
                    label = { Text("End date") },
                    isError = endDateError != null,
                    supportingText = { Text(endDateError ?: "YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            saveError?.let { Text(it, color = MoneeColors.DangerRed, fontSize = 13.sp) }
        }

        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, MoneeColors.CardWhite))
                    )
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = 16.dp)
        ) {
            MoneeGradientButton(
                enabled = !isSaving,
                onClick = {
                    val selected = categories.firstOrNull { it.categoryId == selectedCategoryId }
                    val parsedAmount = amount.toDoubleOrNull()
                    val parsedStart = runCatching { LocalDate.parse(startDate) }.getOrNull()
                    val parsedEnd = runCatching { LocalDate.parse(endDate) }.getOrNull()

                    nameError = if (name.isBlank()) "Enter a budget name." else null
                    amountError =
                        if (parsedAmount == null || parsedAmount <= 0) {
                            "Enter a positive budget amount."
                        } else {
                            null
                        }
                    selectionError = if (selected == null) "Choose a category." else null
                    startDateError =
                        if (parsedStart == null) "Enter a valid start date." else null
                    endDateError =
                        when {
                            parsedEnd == null -> "Enter a valid end date."
                            parsedStart != null && parsedEnd.isBefore(parsedStart) ->
                                "End date must be on or after the start date."
                            else -> null
                        }

                    if (
                        nameError == null && amountError == null && selectionError == null &&
                            startDateError == null && endDateError == null
                    ) {
                            isSaving = true
                            saveError = null
                            scope.launch {
                                vm.addBudget(name, parsedAmount!!, selected!!.name, startDate, endDate)
                                    .onSuccess {
                                        isSaving = false
                                        showMessage(it)
                                        onBack()
                                    }
                                    .onFailure {
                                        isSaving = false
                                        saveError = it.message ?: "Unable to save budget."
                                    }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
            ) {
                Text(
                    if (isSaving) "Saving..." else "Add Budget",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            isSaving = isSavingCategory,
            error = addCategoryError,
            onDismiss = { if (!isSavingCategory) showAddCategory = false },
            onSave = { categoryName, icon ->
                val ownerId = userId
                if (ownerId == null) {
                    addCategoryError = "Please sign in before adding a category."
                } else {
                    isSavingCategory = true
                    addCategoryError = null
                    categoryViewModel.addCategory(ownerId, categoryName, icon) { result ->
                        isSavingCategory = false
                        result
                            .onSuccess {
                                selectedCategoryId = it.categoryId
                                showAddCategory = false
                            }
                            .onFailure {
                                addCategoryError = it.message ?: "Unable to add category."
                            }
                    }
                }
            },
        )
    }
}

@Composable
fun BudgetFormDialog(
    title: String,
    confirmText: String,
    initial: Budget?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.budgetName ?: "") }
    var amountStr by remember { mutableStateOf(initial?.amountLimit?.toString() ?: "") }
    var category by remember {
        mutableStateOf(initial?.category ?: categories.firstOrNull()?.name.orEmpty())
    }
    var startDate by remember {
        mutableStateOf(
            initial?.startDate?.toString() ?: LocalDate.now().withDayOfMonth(1).toString()
        )
    }
    var endDate by remember {
        mutableStateOf(
            initial?.endDate?.toString() ?: java.time.YearMonth.now().atEndOfMonth().toString()
        )
    }
    var catExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MoneeColors.CardWhite) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MoneeColors.InkDark,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Allocated Amount (RM) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Spending Category *") },
                        trailingIcon = {
                            IconButton(onClick = { catExpanded = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { catExpanded = true },
                    )
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false },
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.icon ?: "•"} ${item.name}") },
                                onClick = {
                                    category = item.name
                                    catExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel", color = MoneeColors.Slate700)
                    }
                    Button(
                        onClick = {
                            onSave(
                                name,
                                amountStr.toDoubleOrNull() ?: 0.0,
                                category,
                                startDate,
                                endDate,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MoneeColors.BrandYellow,
                                contentColor = MoneeColors.InkDark,
                            ),
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetDetailsDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onAddExpense: (Double, String, String) -> Unit,
) {
    var showExpenseForm by remember { mutableStateOf(false) }
    var expAmt by remember { mutableStateOf("") }
    var expDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var expDesc by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MoneeColors.CardWhite,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.84f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            budget.budgetName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MoneeColors.InkDark,
                        )
                        Text(
                            "${formatShortDate(budget.startDate)} – ${formatShortDate(budget.endDate)}",
                            fontSize = 11.sp,
                            color = MoneeColors.Slate400,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(
                    color = MoneeColors.Slate100,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(color = MoneeColors.SurfaceBg, shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    when (budget.status) {
                                        "EXCEEDED" -> "Exceeded Limit"
                                        "WARNING" -> "Approaching Limit"
                                        else -> "Within Budget"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (budget.usagePct >= 100.0) MoneeColors.DangerRed
                                        else MoneeColors.BrandDarkGold,
                                )
                                Text(
                                    "${String.format(Locale.US, "%.1f", budget.usagePct)}% Used",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (budget.usagePct / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                color =
                                    if (budget.usagePct >= 100.0) MoneeColors.DangerRed
                                    else MoneeColors.BrandGold,
                                trackColor = MoneeColors.Slate200,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Budget: ${formatRM(budget.amountLimit)}", fontSize = 11.sp)
                                Text(
                                    "Spent: ${formatRM(budget.totalSpent)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (showExpenseForm) {
                        Surface(
                            color = MoneeColors.BrandLight,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MoneeColors.BrandSoft),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "Record Expense",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.BrandDarkGold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = expAmt,
                                        onValueChange = { expAmt = it },
                                        label = { Text("Amount (RM)") },
                                        keyboardOptions =
                                            KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = expDate,
                                        onValueChange = { expDate = it },
                                        label = { Text("Date") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                OutlinedTextField(
                                    value = expDesc,
                                    onValueChange = { expDesc = it },
                                    label = { Text("Description *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showExpenseForm = false },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Cancel", color = MoneeColors.Slate700)
                                    }
                                    Button(
                                        onClick = {
                                            onAddExpense(
                                                expAmt.toDoubleOrNull() ?: 0.0,
                                                expDate,
                                                expDesc,
                                            )
                                            expAmt = ""
                                            expDesc = ""
                                            showExpenseForm = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = MoneeColors.BrandYellow,
                                                contentColor = MoneeColors.InkDark,
                                            ),
                                    ) {
                                        Text("Add Expense", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showExpenseForm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MoneeColors.BrandYellow,
                                    contentColor = MoneeColors.InkDark,
                                ),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add Expense", fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "EXPENSE HISTORY (${budget.expenses.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.Slate400,
                    )
                    budget.expenses.forEach { ex ->
                        Surface(color = MoneeColors.SurfaceBg, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ex.description,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneeColors.InkDark,
                                    )
                                    Text(
                                        formatShortDate(ex.date),
                                        fontSize = 10.sp,
                                        color = MoneeColors.Slate400,
                                    )
                                }
                                Text(
                                    "-${formatRM(ex.amount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.InkDark,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

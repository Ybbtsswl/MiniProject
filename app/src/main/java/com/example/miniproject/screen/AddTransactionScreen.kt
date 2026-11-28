package com.example.miniproject.screen

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Expense
import com.example.miniproject.data.Income
import com.example.miniproject.data.IncomeCategory
import com.example.miniproject.repository.IncomeRepository
import com.example.miniproject.receipt.ReceiptData
import com.example.miniproject.receipt.ReceiptScanButton
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeButtonBrush
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.MoneeYellowSoft
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeFieldColors
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    userId: String? = null,
    onBack: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    initialIsExpense: Boolean = true,
    viewModel: ExpenseViewModel = viewModel()
) {
    var isExpense by remember { mutableStateOf(initialIsExpense) }
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedIncomeCategoryId by remember { mutableStateOf<String?>(null) }
    var incomeCategories by remember { mutableStateOf<List<IncomeCategory>>(emptyList()) }
    var isLoadingIncomeCategories by remember { mutableStateOf(true) }
    var incomeCategoryError by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var displayDate by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")))
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var repeatIncome by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf("Monthly") }
    var scanNotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var isSavingCategory by remember { mutableStateOf(false) }
    var categorySaveError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val incomeRepository = remember { IncomeRepository() }
    val saveScope = rememberCoroutineScope()

    // Load categories from ViewModel
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoadingCategories by viewModel.isLoadingCategories.collectAsStateWithLifecycle()

    // ✅ Load categories when screen opens
    LaunchedEffect(userId) {
        viewModel.loadCategories(userId)
    }

    LaunchedEffect(Unit) {
        incomeRepository.getIncomeCategories()
            .onSuccess { incomeCategories = it }
            .onFailure { incomeCategoryError = it.message ?: "Failed to load income categories" }
        isLoadingIncomeCategories = false
    }

    // Pre-fill the form from a scanned receipt. Nothing is saved: the user reviews and edits first.
    fun applyReceipt(r: ReceiptData) {
        scanError = null

        // The receipt's own total is what was actually paid; rebuild it only if it could not be read
        val total = r.total
            ?: r.items.takeIf { it.isNotEmpty() }?.let { found ->
                (r.subtotal ?: found.sumOf { it.lineTotal }) +
                        (r.sstAmount ?: 0.0) + (r.serviceChargeAmount ?: 0.0)
            }
        total?.let { amount = String.format(Locale.US, "%.2f", it) }

        r.merchant?.let { if (description.isBlank()) description = it }

        r.date?.let { iso ->
            try {
                displayDate = LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            } catch (e: Exception) {
                // keep the current date if the scanned one cannot be used
            }
        }

        scanNotes = r.warnings + "Check the amount and date, then pick a category before saving."
        Toast.makeText(context, "Receipt scanned. Please review the details", Toast.LENGTH_SHORT).show()
    }

    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.fillMaxSize().moneeBackground(),
        color = Color.Transparent
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Income / Expense toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(scheme.surface)
                    .border(1.dp, MoneeBorder, RoundedCornerShape(50.dp))
                    .padding(4.dp)
            ) {
                Row {
                    listOf("Income", "Expense").forEach { label ->
                        val selected = if (label == "Expense") isExpense else !isExpense
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50.dp))
                                .then(
                                    if (selected) Modifier.background(MoneeButtonBrush)
                                    else Modifier.background(Color.Transparent)
                                )
                                .clickable {
                                    isExpense = label == "Expense"
                                    selectedCategoryId = null
                                    selectedIncomeCategoryId = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MoneeInk else MoneeMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .moneeHero(RoundedCornerShape(20.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isExpense) "ENTER EXPENSE AMOUNT" else "ENTER INCOME AMOUNT",
                        fontSize = 12.sp,
                        color = MoneeMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RM ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeInk
                        )
                        TextField(
                            value = amount,
                            onValueChange = {
                                if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it
                            },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MoneeAmber,
                                focusedTextColor = MoneeInk,
                                unfocusedTextColor = MoneeInk,
                                focusedPlaceholderColor = MoneeMuted.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = MoneeMuted.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.widthIn(min = 120.dp)
                        )
                    }
                    Text(
                        text = "Malaysian Ringgit (MYR)",
                        fontSize = 11.sp,
                        color = MoneeMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val selectedLabel = if (isExpense)
                    categories.find { it.categoryId == selectedCategoryId }?.name
                else incomeCategories.find {
                    it.incomeCategoryId == selectedIncomeCategoryId
                }?.name

                if (selectedLabel != null) {
                    Text(
                        text = "Selected: $selectedLabel",
                        fontSize = 12.sp,
                        color = MoneeAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isExpense) {
                CategorySelector(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    isLoading = isLoadingCategories,
                    onSelect = { selectedCategoryId = it.categoryId },
                    onAddCategory = {
                        categorySaveError = null
                        showAddCategory = true
                    },
                )
            } else {
                when {
                    isLoadingIncomeCategories -> {
                        Text("Loading income categories...", fontSize = 13.sp, color = MoneeMuted)
                    }
                    incomeCategories.isEmpty() -> {
                        Text(
                            incomeCategoryError ?: "No income categories found",
                            fontSize = 13.sp,
                            color = MoneeMuted
                        )
                    }
                    else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(incomeCategories) { category ->
                            IncomeCategoryChip(
                                label = category.name,
                                icon = category.icon,
                                selected = selectedIncomeCategoryId == category.incomeCategoryId,
                                onClick = {
                                    selectedIncomeCategoryId = category.incomeCategoryId
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("What was this for?", color = MoneeMuted.copy(alpha = 0.5f)) },
                shape = fieldShape,
                colors = moneeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MoneeAmber)
                    },
                    shape = fieldShape,
                    colors = moneeFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (!isExpense) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { repeatIncome = !repeatIncome }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Repeat this income",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically log this amount monthly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MoneeMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = repeatIncome,
                        onCheckedChange = { repeatIncome = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MoneeYellow,
                            checkedBorderColor = MoneeYellow,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MoneeBorder,
                            uncheckedBorderColor = MoneeBorder
                        )
                    )
                }
                if (repeatIncome) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Payment schedule", fontSize = 12.sp, color = MoneeMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Monthly", "Weekly").forEach { frequency ->
                            MoneeChip(
                                selected = recurringFrequency == frequency,
                                onClick = { recurringFrequency = frequency }
                            ) { Text(frequency, fontSize = 12.sp) }
                        }
                    }
                }
            }

            if (isExpense) {
                Spacer(modifier = Modifier.height(24.dp))

                // Real receipt scanning: fills in amount, description and date for review
                ReceiptScanButton(
                    onResult = { applyReceipt(it) },
                    onError = { scanError = it }
                )

                scanError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = scheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (scanNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MoneeYellowSoft),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MoneeBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Please review", fontWeight = FontWeight.SemiBold, color = MoneeInk)
                            scanNotes.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MoneeInk)
                            }
                            TextButton(onClick = { scanNotes = emptyList() }) {
                                Text("Dismiss", color = MoneeAmber)
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            MoneeGradientButton(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    when {
                        value == null || value <= 0.0 -> error = "Enter a valid amount"
                        isExpense && selectedCategoryId == null -> error = "Pick a category"
                        !isExpense && selectedIncomeCategoryId == null -> error = "Pick a category"
                        userId == null -> error = "User not logged in"
                        else -> {
                            error = null
                            val formattedDate = try {
                                val localDate = LocalDate.parse(
                                    displayDate,
                                    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                                )
                                localDate.toString()
                            } catch (e: Exception) {
                                displayDate
                            }

                            if (isExpense) {
                                val selectedCat = categories.find { it.categoryId == selectedCategoryId }

                                val newExpense = Expense(
                                    userId = userId,
                                    amount = value,
                                    categoryId = selectedCategoryId ?: "",
                                    category = selectedCat?.name ?: "",
                                    description = description.trim().ifEmpty { "" },
                                    date = formattedDate
                                )
                                viewModel.addExpense(newExpense) { success, message ->
                                    if (success) {
                                        Toast.makeText(context, "Expense saved", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess()
                                    } else {
                                        error = message
                                    }
                                }
                            } else {
                                val newIncome = Income(
                                    userId = userId,
                                    amount = value,
                                    categoryId = selectedIncomeCategoryId,
                                    incomeDate = formattedDate,
                                    description = description.trim().ifEmpty { "" }
                                )
                                saveScope.launch {
                                    val incomeToSave = if (repeatIncome) {
                                        incomeRepository.addRecurringIncome(
                                            userId = userId,
                                            categoryId = selectedIncomeCategoryId,
                                            amount = value,
                                            startDate = formattedDate,
                                            frequency = recurringFrequency
                                        ).fold(
                                            onSuccess = { recurring ->
                                                Result.success(newIncome.copy(recurringIncomeId = recurring.recurringIncomeId))
                                            },
                                            onFailure = { Result.failure(it) }
                                        )
                                    } else {
                                        Result.success(newIncome)
                                    }

                                    incomeToSave.fold(
                                        onSuccess = { income -> incomeRepository.addIncome(income) },
                                        onFailure = { Result.failure<Income>(it) }
                                    )
                                        .onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (repeatIncome) "Recurring income saved" else "Income saved",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onSaveSuccess()
                                        }
                                        .onFailure {
                                            error = it.message ?: "Failed to save income"
                                        }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isExpense) "✓ Save Expense" else "✓ Save Income",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(
                    displayDate,
                    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                )
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                LocalDate.now()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selectedLocalDate = Instant
                            .ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        displayDate = selectedLocalDate.format(
                            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.SemiBold, color = MoneeAmber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MoneeMuted)
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MoneeYellow,
                    selectedDayContentColor = MoneeInk,
                    todayDateBorderColor = MoneeYellow,
                    titleContentColor = MoneeInk,
                    headlineContentColor = MoneeInk
                )
            )
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            isSaving = isSavingCategory,
            error = categorySaveError,
            onDismiss = { if (!isSavingCategory) showAddCategory = false },
            onSave = { name, icon ->
                val ownerId = userId
                if (ownerId == null) {
                    categorySaveError = "Please sign in before adding a category."
                } else {
                    isSavingCategory = true
                    categorySaveError = null
                    viewModel.addCategory(ownerId, name, icon) { result ->
                        isSavingCategory = false
                        result
                            .onSuccess { saved ->
                                selectedCategoryId = saved.categoryId
                                showAddCategory = false
                            }
                            .onFailure {
                                categorySaveError = it.message ?: "Unable to add category."
                            }
                    }
                }
            },
        )
    }
}

@Composable
private fun CategoryChip(
    category: com.example.miniproject.data.Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = category.icon ?: "•"
    MoneeChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = category.name,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun IncomeCategoryChip(
    label: String,
    icon: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    MoneeChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = icon ?: "•", fontSize = 16.sp)
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddTransactionScreenPreview() {
    MiniProjectTheme {
        AddTransactionScreen(
            userId = null,
            onBack = {},
            onSaveSuccess = {}
        )
    }
}

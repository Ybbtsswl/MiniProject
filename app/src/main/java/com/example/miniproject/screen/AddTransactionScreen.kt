package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val expenseCategories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Other")
private val incomeCategories = listOf("Salary", "Invest", "Gift", "Customize +")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    userId: String? = null,
    onBack: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: ExpenseViewModel = viewModel()
) {
    var isExpense by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var displayDate by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")))
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var repeatIncome by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(12.dp)
    val cardBg = Color(0xFFFFF9E6)

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFF3F3F3))
                    .padding(4.dp)
            ) {
                Row {
                    listOf("Income", "Expense").forEach { label ->
                        val selected = if (label == "Expense") isExpense else !isExpense
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (selected) scheme.primary else Color.Transparent)
                                .clickable {
                                    isExpense = label == "Expense"
                                    selectedCategory = ""
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) scheme.onPrimary else Color(0xFF666666)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isExpense) "ENTER EXPENSE AMOUNT" else "ENTER INCOME AMOUNT",
                        fontSize = 12.sp,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RM ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground
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
                                cursorColor = scheme.primary
                            ),
                            modifier = Modifier.widthIn(min = 120.dp)
                        )
                    }
                    Text(
                        text = "Malaysian Ringgit (MYR)",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant,
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
                if (selectedCategory.isNotEmpty()) {
                    Text(
                        text = "Selected: $selectedCategory",
                        fontSize = 12.sp,
                        color = scheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val categories = if (isExpense) expenseCategories else incomeCategories
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories) { name ->
                    CategoryChip(
                        label = name,
                        selected = selectedCategory == name,
                        onClick = { selectedCategory = name },
                        isExpense = isExpense
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = scheme.onSurfaceVariant) },
                placeholder = { Text("What was this for?", color = scheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                shape = fieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.surface,
                    unfocusedContainerColor = scheme.surface,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = Color(0xFFE8E8E8),
                    focusedLabelColor = scheme.onSurfaceVariant,
                    cursorColor = scheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = scheme.onSurfaceVariant)
                    },
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = Color(0xFFE8E8E8),
                        cursorColor = scheme.onSurface
                    ),
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
                            color = scheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = repeatIncome,
                        onCheckedChange = { repeatIncome = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.primary,
                            checkedTrackColor = scheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (isExpense) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Receipt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Optional",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text("📷", fontSize = 20.sp)
                            Text("Take photo", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text("📁", fontSize = 20.sp)
                            Text("Upload file", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text(
                    text = "JPG, PNG or PDF · up to 10 MB",
                    fontSize = 10.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            error?.let {
                Text(
                    text = it,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    when {
                        value == null || value <= 0.0 -> error = "Enter a valid amount"
                        selectedCategory.isBlank() -> error = "Pick a category"
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
                                val newExpense = Expense(
                                    userId = userId,
                                    amount = value,
                                    category = selectedCategory,
                                    description = description.trim(),
                                    date = formattedDate
                                )
                                viewModel.addExpense(newExpense) { success, message ->
                                    if (success) {
                                        viewModel.refresh()
                                        onSaveSuccess()
                                    } else {
                                        error = message
                                    }
                                }
                            } else {
                                onSaveSuccess()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
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
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = scheme.primary,
                    selectedDayContentColor = scheme.onPrimary,
                    todayDateBorderColor = scheme.primary,
                    titleContentColor = scheme.onBackground,
                    headlineContentColor = scheme.onBackground
                )
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isExpense: Boolean
) {
    val icon = when (label) {
        "Food" -> "🍽️"
        "Transport" -> "🚗"
        "Shopping" -> "🛍️"
        "Bills" -> "📄"
        "Entertainment" -> "🎬"
        "Other" -> "⋮"
        "Salary" -> "💰"
        "Invest" -> "📈"
        "Gift" -> "🎁"
        "Customize +" -> "⚙️"
        else -> "•"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (!selected) BorderStroke(1.dp, Color(0xFFE8E8E8)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
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

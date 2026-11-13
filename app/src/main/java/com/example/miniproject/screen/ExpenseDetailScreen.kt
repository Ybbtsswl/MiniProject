package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

private val CardBg = Color(0xFFFFF9E6)

@Composable
fun ExpenseDetailScreen(
    expense: Expense,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var category by remember { mutableStateOf(expense.category) }
    var description by remember { mutableStateOf(expense.description) }
    var date by remember { mutableStateOf(expense.date) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // Top Bar
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (isEditing) "Edit Expense" else "Expense Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (!isEditing) {
                    TextButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Edit", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier,
                            textStyle = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Text(
                            text = formatCurrency(expense.amount),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeRed
                        )
                    }
                    Text(
                        text = if (isEditing) category else expense.category,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fields — View Mode
            if (!isEditing) {
                DetailRow(label = "Description", value = expense.description)
                DetailRow(label = "Category", value = expense.category)
                DetailRow(label = "Date", value = expense.date)
            }
            // Fields — Edit Mode
            else {
                description?.let { it1 ->
                    OutlinedTextField(
                        value = it1,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isEditing = false
                            amount = expense.amount.toString()
                            category = expense.category
                            description = expense.description
                            date = expense.date
                            error = null
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val value = amount.toDoubleOrNull()
                            when {
                                value == null || value <= 0.0 -> error = "Enter a valid amount"
                                category.isBlank() -> error = "Enter a category"
                                else -> {
                                    error = null
                                    isSaving = true
                                    val updatedExpense = expense.copy(
                                        amount = value,
                                        category = category,
                                        description = description,
                                        date = date
                                    )
                                    viewModel.updateExpense(updatedExpense) { success, msg ->
                                        isSaving = false
                                        if (success) {
                                            onSaveSuccess()
                                        } else {
                                            error = msg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("✓ Save Changes")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (value != null) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExpenseDetailScreenPreview() {
    MiniProjectTheme {
        ExpenseDetailScreen(
            expense = Expense(
                userId = "1",
                amount = 15.50,
                category = "Food",
                description = "Lunch",
                date = "2026-11-05"
            ),
            onBack = {},
            onSaveSuccess = {}
        )
    }
}

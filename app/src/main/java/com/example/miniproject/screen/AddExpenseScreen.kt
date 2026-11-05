package com.example.miniproject.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Expense
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.viewmodel.ExpenseViewModel
import io.github.jan.supabase.auth.auth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp


class AddExpenseScreen : Fragment() {
    private val vm: ExpenseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MiniProjectTheme {
                AddExpenseContent(
                    onSave = { amount, category, description, date ->
                        saveExpense(amount, category, description, date)
                    },
                    onCancel = { findNavController().popBackStack() }
                )
            }
        }
    }

    private fun saveExpense(amount: Double, category: String, description: String, date: String) {
        val userId = SupabaseClient.instance.auth.currentSessionOrNull()?.user?.id
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        vm.saveExpense(
            Expense(
                userId = userId,
                amount = amount,
                category = category,
                description = description,
                date = date
            ),
            userId
        ){ success ->
            if (success) {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
            }
            vm.clearMessage()
        }

        findNavController().popBackStack()
    }
}

private val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    onSave: (amount: Double, category: String, description: String, date: String) -> Unit,
    onCancel: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = Manual, 1 = Scan Receipt
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surface,
        unfocusedContainerColor = scheme.surface,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline,
        focusedLabelColor = scheme.onSurface,
        cursorColor = scheme.onSurface
    )

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Top bar: back arrow + centered title
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Add Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // Manual / Scan Receipt toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(4.dp)) {
                    listOf("Manual", "Scan Receipt").forEachIndexed { index, label ->
                        val selected = tab == index
                        Surface(
                            onClick = { tab = index },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) scheme.primaryContainer else scheme.surface,
                            border = if (selected) BorderStroke(1.dp, scheme.primary) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (tab == 1) {
                // Placeholder for the scan flow
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outline),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧾", fontSize = 44.sp)
                            Text("Scan receipt coming soon", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("RM ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { name ->
                        FilterChip(
                            selected = category == name,
                            onClick = { category = name },
                            label = { Text(name) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = scheme.surface,
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = category == name,
                                borderColor = scheme.outline,
                                selectedBorderColor = scheme.primary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Box {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                }

                error?.let {
                    Text(it, color = scheme.error, modifier = Modifier.padding(top = 12.dp))
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        val value = amount.toDoubleOrNull()
                        when {
                            value == null || value <= 0.0 -> error = "Enter a valid amount"
                            category.isBlank() -> error = "Pick a category"
                            else -> {
                                error = null
                                onSave(value, category, description.trim(), date)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Save Expense", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEDE7D3),
                        contentColor = scheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = scheme.primary,
                    selectedDayContentColor = scheme.onPrimary,
                    todayDateBorderColor = scheme.primary
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddExpensePreview() {
    MiniProjectTheme { AddExpenseContent(onSave = { _, _, _, _ -> }, onCancel = {}) }
}
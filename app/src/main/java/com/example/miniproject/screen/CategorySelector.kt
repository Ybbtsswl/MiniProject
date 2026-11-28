package com.example.miniproject.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Category
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted

/** Shared category picker used by Expenses and Budget Management. */
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    onSelect: (Category) -> Unit,
    onAddCategory: () -> Unit,
) {
    when {
        isLoading -> Text("Loading categories...", fontSize = 13.sp, color = MoneeMuted)
        else ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories, key = { it.categoryId.ifBlank { it.name } }) { category ->
                    MoneeChip(
                        selected = selectedCategoryId == category.categoryId,
                        onClick = { onSelect(category) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(category.icon ?: "•", fontSize = 16.sp)
                        Text(
                            category.name,
                            fontSize = 13.sp,
                            fontWeight =
                                if (selectedCategoryId == category.categoryId) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    }
                }

                item {
                    MoneeChip(
                        selected = false,
                        onClick = onAddCategory,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
    }
}

/** Small reusable form for creating a user-owned Supabase category. */
@Composable
fun AddCategoryDialog(
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🏷️") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Add Category", color = MoneeInk, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "This category will be available in Expenses and Budget Management.",
                    color = MoneeMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it.take(4) },
                        label = { Text("Icon") },
                        singleLine = true,
                        modifier = Modifier.width(92.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, icon) },
                enabled = !isSaving && name.isNotBlank(),
            ) {
                Text(if (isSaving) "Saving..." else "Add")
            }
        },
    )
}

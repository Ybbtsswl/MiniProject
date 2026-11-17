package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    @SerialName("expense_id") val expenseId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: String ="",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "",
    val description: String,
    @SerialName("is_from_split") val isFromSplit: Boolean = false,
    @SerialName("split_bill_id") val splitBillId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

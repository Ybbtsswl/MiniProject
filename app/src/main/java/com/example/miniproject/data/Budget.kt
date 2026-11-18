package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    @SerialName("budget_id") val budgetId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("budget_name") val budgetName: String = "",
    @SerialName("amount_limit") val amountLimit: Double = 0.0,
    @SerialName("total_spent") val totalSpent: Double = 0.0,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    val status: String = "ACTIVE",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class BudgetCategory(
    val name: String = "",
    val icon: String = "",
    val spent: Double = 0.0,
    val limit: Double = 0.0
) {
    val remaining: Double get() = limit - spent
    val progress: Float get() = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
}

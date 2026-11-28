package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecurringIncome(
    @SerialName("recurring_income_id") val recurringIncomeId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("category_id") val categoryId: String? = null,
    val amount: Double = 0.0,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("next_date") val nextDate: String = "",
    val frequency: String = "Monthly",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
)

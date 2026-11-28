package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Income(
    @SerialName("income_id") val incomeId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("income_category_id") val categoryId: String? = null,
    @SerialName("recurring_income_id") val recurringIncomeId: String? = null,
    val amount: Double = 0.0,
    @SerialName("income_date") val incomeDate: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val description: String = ""
)

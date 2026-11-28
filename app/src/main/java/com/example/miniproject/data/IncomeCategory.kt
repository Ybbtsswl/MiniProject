package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncomeCategory(
    @SerialName("income_category_id") val incomeCategoryId: String = "",
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    val icon: String? = null
)

package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    @SerialName ("user_id") val userId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val description: String? = null,
    @SerialName("receipt_url")
    val receiptImageUrl: String? = null,
    val isFromSplit: Boolean = false,
    @SerialName("category_id")
    val categoryId: String = ""
)

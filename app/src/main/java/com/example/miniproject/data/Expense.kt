package com.example.miniproject.data

import kotlinx.serialization.SerialName

data class Expense(
    val id: String = "",
    @SerialName ("user_id") val userId: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "",
    val description: String = "",
    @SerialName ("receipt_url") val receiptImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFromSplit: Boolean = false
)

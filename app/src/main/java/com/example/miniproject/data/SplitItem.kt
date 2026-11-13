package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitItem(
    val id: String = "",
    @SerialName("split_bill_id") val splitBillId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: String = ""
)


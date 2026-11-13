package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitBill(
    val id: String = "",
    val title: String = "",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("share_code") val shareCode: String = "",
    @SerialName("creator_id") val creatorId: String = "",
    @SerialName("bill_date") val billDate: String = "",
    val status: String = "Active",
    @SerialName("created_at") val createdAt: String = ""
)
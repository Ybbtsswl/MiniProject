package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitBill(
    @SerialName("split_bill_id") val splitBillId: String = "",
    val title: String = "",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("user_id") val creatorId: String = "",
    @SerialName("bill_date") val billDate: String = "",
    val status: String = "Active",
    @SerialName("participant_count") val participantCount: Int = 1,
)
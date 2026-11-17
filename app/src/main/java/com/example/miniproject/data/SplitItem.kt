package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitItem(
    @SerialName("split_item_id") val splitItemId: String = "",
    @SerialName("split_bill_id") val splitBillId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("payer_id") val payerId: String? = null
)


package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitAllocation(
    val id: String = "",
    @SerialName("split_item_id") val splitItemId: String = "",
    @SerialName("split_member_id") val splitMemberId: String = "",
    @SerialName("share_amount") val shareAmount: Double = 0.0,
    @SerialName("is_paid") val isPaid: Boolean = false,
    @SerialName("paid_at") val paidAt: String? = null
)

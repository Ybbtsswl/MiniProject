package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitAllocation(
    @SerialName("split_allocation_id") val splitAllocationId: String = "",
    @SerialName("split_item_id") val splitItemId: String = "",
    @SerialName("split_member_id") val splitMemberId: String = "",
    @SerialName("shared_amount") val shareAmount: Double = 0.0,
    @SerialName("is_paid") val isPaid: Boolean = false,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("payer_id") val payerId: String? = null,
    @SerialName("contribution_type") val contributionType: String = "amount",
    @SerialName("contribution_value") val contributionValue: Double = 0.0
)

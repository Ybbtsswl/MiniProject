package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitMember(
    @SerialName("split_member_id") val splitMemberId: String = "",
    @SerialName("split_bill_id") val splitBillId: String = "",
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    @SerialName("joined_at") val joinedAt: String = "",
    @SerialName("is_me") val isMe: Boolean = false
)

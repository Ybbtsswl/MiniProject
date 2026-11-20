package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    @SerialName("group_member_id") val groupMemberId: String = "",
    @SerialName("group_id") val groupId: String = "",
    @SerialName("user_id") val userId: String? = null,
    val name: String = ""
)

package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantGroup(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    val name: String
)

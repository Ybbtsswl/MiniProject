package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val userId: String? = null,
    @SerialName("created_at")
    val createdAt: String = ""
)

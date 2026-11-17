package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    @SerialName("category_id") val categoryId: String = "",
    val name: String = "",
    val icon: String? = null,
    val description: String? = null,
    @SerialName("user_id") val userId: String? = null
)

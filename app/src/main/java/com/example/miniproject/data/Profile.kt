package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("base_currency") val baseCurrency: String = "MYR",
    @SerialName("dark_mode") val darkMode: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

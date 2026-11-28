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
) {
    companion object {
        /** Builds a user-owned category before it is inserted into Supabase. */
        fun userCreated(
            userId: String,
            name: String,
            icon: String,
            description: String? = null,
        ): Category {
            require(name.isNotBlank()) { "Enter a category name." }
            return Category(
                name = name.trim(),
                icon = icon.trim().ifBlank { "•" },
                description = description?.trim()?.takeIf { it.isNotBlank() },
                userId = userId,
            )
        }
    }
}

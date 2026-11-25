package com.example.miniproject.data

/**
 * Reusable category information for screens that need a category name or icon.
 *
 * [aliases] contains older or shorter names that may already be stored in Supabase.
 */
data class CategoryDefinition(
    val key: String,
    val name: String,
    val icon: String,
    val aliases: Set<String> = emptySet(),
) {
    fun matches(value: String): Boolean {
        return name.equals(value, ignoreCase = true) ||
            aliases.any { alias -> alias.equals(value, ignoreCase = true) }
    }
}

/** Shared category list that can be used by budget, reports, expenses, and future screens. */
object Categories {
    val Food =
        CategoryDefinition(
            key = "food",
            name = "Food & Dining",
            icon = "🍔",
            aliases = setOf("Food", "Food & drinks"),
        )

    val Housing =
        CategoryDefinition(
            key = "housing",
            name = "Housing & Utilities",
            icon = "🏠",
            aliases = setOf("Bills"),
        )

    val Transportation =
        CategoryDefinition(
            key = "transportation",
            name = "Transportation",
            icon = "🚗",
            aliases = setOf("Transport"),
        )

    val Shopping = CategoryDefinition(key = "shopping", name = "Shopping", icon = "🛍️")

    val Entertainment =
        CategoryDefinition(key = "entertainment", name = "Entertainment", icon = "🎬")

    val Other = CategoryDefinition(key = "other", name = "Other", icon = "💰")

    val all: List<CategoryDefinition> =
        listOf(Food, Housing, Transportation, Shopping, Entertainment, Other)

    fun findByName(value: String): CategoryDefinition? {
        return all.firstOrNull { category -> category.matches(value) }
    }
}

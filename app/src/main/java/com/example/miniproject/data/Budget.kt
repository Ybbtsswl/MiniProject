package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    @SerialName("budget_id") val budgetId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("budget_name") val budgetName: String = "",
    @SerialName("amount_limit") val amountLimit: Double = 0.0,
    @SerialName("total_spent") val totalSpent: Double = 0.0,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    val status: String = "ACTIVE",
    @SerialName("created_at") val createdAt: String = "",
)

/** Calculated category values displayed by Budget Management. */
data class BudgetCategory(
    val name: String,
    val icon: String,
    val spent: Double,
    val limit: Double,
) {
    val remaining: Double
        get() = limit - spent

    val progress: Float
        get() =
            if (limit > 0) {
                (spent / limit).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
}

// Budget percentages belong to the budget feature, not the shared category catalog.
private val defaultBudgetShares =
    linkedMapOf(
        Categories.Food to 0.30,
        Categories.Housing to 0.32,
        Categories.Transportation to 0.16,
        Categories.Entertainment to 0.10,
    )

/** Creates Budget Management cards using the shared category catalog. */
fun buildBudgetCategories(
    spendingByCategory: Map<String, Double>,
    totalBudget: Double,
): List<BudgetCategory> {
    return defaultBudgetShares.map { (category, budgetShare) ->
        val spent =
            spendingByCategory
                .filterKeys { storedName -> category.matches(storedName) }
                .values
                .sum()

        BudgetCategory(
            name = category.name,
            icon = category.icon,
            spent = spent,
            limit = totalBudget * budgetShare,
        )
    }
}

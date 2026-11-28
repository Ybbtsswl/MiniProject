package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Budget
import com.example.miniproject.data.Expense
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BudgetRepository {
    private val supabase = SupabaseClient.instance
    private val budgetTable = "budgets"

    // READ — Load the signed-in user's active budget for the selected date.
    suspend fun getActiveBudget(userId: String, date: String): Result<Budget?> =
        withContext(Dispatchers.IO) {
            try {
                val budgets =
                    supabase
                        .from(budgetTable)
                        .select {
                            order("created_at", Order.DESCENDING)
                            filter {
                                eq("user_id", userId)
                                eq("status", "ACTIVE")
                                lte("start_date", date)
                                gte("end_date", date)
                            }
                        }
                        .decodeList<Budget>()
                Result.success(budgets.firstOrNull())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // CREATE — Store a new budget owned by the signed-in user.
    suspend fun createBudget(budget: Budget): Result<Budget> =
        withContext(Dispatchers.IO) {
            try {
                val result =
                    supabase
                        .from(budgetTable)
                        .insert(
                            buildJsonObject {
                                put("user_id", budget.userId)
                                put("budget_name", budget.budgetName)
                                put("amount_limit", budget.amountLimit)
                                put("total_spent", budget.totalSpent)
                                put("start_date", budget.startDate)
                                put("end_date", budget.endDate)
                                put("status", budget.status)
                            }
                        ) {
                            select()
                        }
                        .decodeSingle<Budget>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // UPDATE — The user filter prevents updating another user's budget.
    suspend fun updateBudget(budget: Budget): Result<Budget> =
        withContext(Dispatchers.IO) {
            try {
                val result =
                    supabase
                        .from(budgetTable)
                        .update(
                            buildJsonObject {
                                put("budget_name", budget.budgetName)
                                put("amount_limit", budget.amountLimit)
                                put("total_spent", budget.totalSpent)
                                put("start_date", budget.startDate)
                                put("end_date", budget.endDate)
                                put("status", budget.status)
                            }
                        ) {
                            select()
                            filter {
                                eq("budget_id", budget.budgetId)
                                eq("user_id", budget.userId)
                            }
                        }
                        .decodeSingle<Budget>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // UPDATE — Keep the budget summary synchronized with recorded expenses.
    suspend fun updateTotalSpent(budget: Budget, totalSpent: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(budgetTable).update(mapOf("total_spent" to totalSpent)) {
                    filter {
                        eq("budget_id", budget.budgetId)
                        eq("user_id", budget.userId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // SYNC — Recalculate total_spent from the user's expense table rows.
    suspend fun syncSpentForUser(userId: String, referenceDate: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val budget =
                    getActiveBudget(userId, referenceDate).getOrNull()
                        ?: return@withContext Result.success(Unit)
                val expenses =
                    supabase
                        .from("expenses")
                        .select {
                            filter {
                                eq("user_id", userId)
                                gte("date", budget.startDate)
                                lte("date", budget.endDate)
                            }
                        }
                        .decodeList<Expense>()
                val totalSpent = expenses.sumOf { it.amount }
                if (budget.totalSpent != totalSpent) {
                    updateTotalSpent(budget, totalSpent)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

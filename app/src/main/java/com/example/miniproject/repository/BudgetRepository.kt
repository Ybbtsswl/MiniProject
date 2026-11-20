package com.example.miniproject.repository


import com.example.miniproject.data.Budget
import com.example.miniproject.data.Expense
import com.example.miniproject.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetRepository {
    private val supabase = SupabaseClient.instance
    private val table = "budgets"

    suspend fun getActiveBudget(userId: String, date: String): Result<Budget?> = withContext(Dispatchers.IO) {
        try {
            val budgets = supabase.from(table).select {
                order("created_at", Order.DESCENDING)
                filter {
                    eq("user_id", userId)
                    eq("status", "ACTIVE")
                    lte("start_date", date)
                    gte("end_date", date)
                }
            }.decodeList<Budget>()
            Result.success(budgets.firstOrNull())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createBudget(budget: Budget): Result<Budget> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from(table).insert(
                mapOf(
                    "user_id" to budget.userId,
                    "budget_name" to budget.budgetName,
                    "amount_limit" to budget.amountLimit,
                    "total_spent" to budget.totalSpent,
                    "start_date" to budget.startDate,
                    "end_date" to budget.endDate,
                    "status" to budget.status
                )
            ) { select() }.decodeSingle<Budget>()
            Result.success(result)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateBudget(budget: Budget): Result<Budget> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from(table).update(
                mapOf(
                    "budget_name" to budget.budgetName,
                    "amount_limit" to budget.amountLimit,
                    "total_spent" to budget.totalSpent,
                    "start_date" to budget.startDate,
                    "end_date" to budget.endDate,
                    "status" to budget.status
                )
            ) {
                select()
                filter { eq("budget_id", budget.budgetId); eq("user_id", budget.userId) }
            }.decodeSingle<Budget>()
            Result.success(result)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateTotalSpent(budget: Budget, totalSpent: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(table).update(mapOf("total_spent" to totalSpent)) {
                filter { eq("budget_id", budget.budgetId); eq("user_id", budget.userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun syncSpentForUser(userId: String, referenceDate: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val budget = getActiveBudget(userId, referenceDate).getOrNull()
                    ?: return@withContext Result.success(Unit)
                val expenses = supabase.from("expenses").select {
                    filter {
                        eq("user_id", userId)
                        gte("date", budget.startDate)
                        lte("date", budget.endDate)
                    }
                }.decodeList<Expense>()
                val totalSpent = expenses.sumOf { it.amount }
                if (budget.totalSpent != totalSpent) {
                    updateTotalSpent(budget, totalSpent)
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }
}

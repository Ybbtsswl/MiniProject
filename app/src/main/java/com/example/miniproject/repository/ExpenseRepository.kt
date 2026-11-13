package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Expense
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepository {
    private val supabase = SupabaseClient.instance
    private val table = "expenses"

    // CREATE — ✅ Will be used in AddExpenseScreen
    suspend fun addExpense(expense: Expense): Result<Expense> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(table)
                    .insert(expense) { select() }
                    .decodeSingle<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // READ ALL — ✅ Used in ExpenseScreen & DashboardScreen
    suspend fun getExpenses(userId: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(table)
                    .select {
                        order("date", Order.DESCENDING)
                    }
                    .decodeList<Expense>()
                    .filter { it.userId == userId }
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // READ BY MONTH — ✅ Will be used for month filtering
    suspend fun getExpensesByMonth(userId: String, month: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val all = supabase.from(table)
                    .select {
                        order("date", Order.DESCENDING)
                    }
                    .decodeList<Expense>()
                val filtered = all.filter {
                    it.userId == userId && it.date.startsWith(month)
                }
                Result.success(filtered)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // UPDATE — ✅ Will be used for editing expenses
    suspend fun updateExpense(expense: Expense): Result<Expense> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(table)
                    .update(expense) {
                        select()
                        filter { eq("id", expense.id) }
                    }
                    .decodeSingle<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // DELETE — ✅ Will be used in ExpenseScreen
    suspend fun deleteExpense(expenseId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(table)
                    .delete {
                        filter { eq("id", expenseId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

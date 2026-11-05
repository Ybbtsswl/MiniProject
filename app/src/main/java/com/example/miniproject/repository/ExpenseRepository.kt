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

    // CREATE
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

    // READ — get all for user, newest first
    suspend fun getExpenses(userId: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val list = supabase.from(table)
                    .select {
                        filter { eq("user_id", userId) }
                        order("date", Order.DESCENDING)
                    }
                    .decodeList<Expense>()
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // UPDATE
    suspend fun updateExpense(expense: Expense, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(table).update({
                    set("amount", expense.amount)
                    set("category", expense.category)
                    set("description", expense.description)
                    set("date", expense.date)
                    // uncomment below if your table has these columns
                    // set("receipt_url", expense.receiptImageUrl)
                    // set("is_from_split", expense.isFromSplit)
                }) {
                    filter {
                        eq("id", expense.id)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // DELETE
    suspend fun deleteExpense(expenseId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(table).delete {
                    filter {
                        eq("id", expenseId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Category
import com.example.miniproject.data.Expense
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

class ExpenseRepository {
    private val supabase = SupabaseClient.instance
    private val expenseTable = "expenses"
    private val categoryTable = "categories"

    // ─── CATEGORIES ─────────────────────────────────────────────

    suspend fun getCategories(userId: String? = null): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(categoryTable)
                    .select()
                    .decodeList<Category>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun getSplitCategory(): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                fun JsonObject.str(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull

                val all = supabase.from(categoryTable)
                    .select()
                    .decodeList<JsonObject>()
                    .mapNotNull { row ->
                        val id = row.str("category_id") ?: row.str("id")
                        val name = row.str("name")
                        if (id != null && name != null) id to name else null
                    }

                all.firstOrNull { it.second.trim().equals("bills", ignoreCase = true) }
                    ?: all.firstOrNull { it.second.contains("other", ignoreCase = true) }
                    ?: all.firstOrNull()
            } catch (e: Exception) {
                android.util.Log.e("ExpenseRepo", "getSplitCategory failed", e)
                null
            }
        }

    // ─── EXPENSES ──────────────────────────────────────────────

    // CREATE — Add new expense
    suspend fun addExpense(expense: Expense): Result<Expense> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(expenseTable)
                    .insert(expense) { select() }
                    .decodeSingle<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // CREATE — Expense generated from a split bill (your share only)
    suspend fun addSplitBillExpense(
        userId: String,
        billId: String,
        title: String,
        date: String,
        amount: Double
    ): Result<Expense> {
        val category = getSplitCategory()
        return addExpense(
            Expense(
                userId = userId,
                categoryId = category?.first ?: "",
                amount = amount,
                category = category?.second ?: "Split Bill",
                date = date,
                description = title,
                isFromSplit = true,
                splitBillId = billId
            )
        )
    }

    // READ ALL — Get all expenses for a user
    suspend fun getExpenses(userId: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(expenseTable)
                    .select {
                        order("date", Order.DESCENDING)
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // READ BY MONTH — Filter expenses by month
    suspend fun getExpensesByMonth(userId: String, month: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(expenseTable)
                    .select {
                        order("date", Order.DESCENDING)
                        filter {
                            eq("user_id", userId)
                            and { ilike("date", "$month%") }
                        }
                    }
                    .decodeList<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // FILTER — By date range and/or category
    suspend fun filterExpenses(
        userId: String,
        startDate: String? = null,
        endDate: String? = null,
        categoryId: String? = null
    ): Result<List<Expense>> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from(expenseTable)
                .select {
                    order("date", Order.DESCENDING)
                    filter {
                        eq("user_id", userId)
                        startDate?.let { gte("date", it) }
                        endDate?.let { lte("date", it) }
                        if (!categoryId.isNullOrEmpty() && categoryId != "All") {
                            eq("category_id", categoryId)
                        }
                    }
                }
                .decodeList<Expense>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // UPDATE — Edit existing expense
    suspend fun updateExpense(expense: Expense): Result<Expense> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(expenseTable)
                    .update(expense) {
                        select()
                        filter { eq("expense_id", expense.expenseId) }
                    }
                    .decodeSingle<Expense>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // DELETE — Remove expense
    suspend fun deleteExpense(expenseId: String, string: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(expenseTable)
                    .delete {
                        filter { eq("expense_id", expenseId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // CLEAR SPLIT BILL REFERENCE — Keep expense but remove link
    suspend fun clearSplitBillReference(splitBillId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(expenseTable)
                    .update(
                        buildJsonObject {
                            put("split_bill_id", JsonNull)
                            put("is_from_split", false)
                        }
                    ) {
                        filter { eq("split_bill_id", splitBillId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

}

package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Income
import com.example.miniproject.data.IncomeCategory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.withContext

class IncomeRepository {
    private val supabase = SupabaseClient.instance
    private val incomeTable = "income"
    private val incomeCategoryTable = "income_categories"

    suspend fun getIncomeCategories(): Result<List<IncomeCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(incomeCategoryTable)
                    .select { order("income_category_id", Order.ASCENDING) }
                    .decodeList<IncomeCategory>()
                Result.success(result)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun getIncome(userId: String): Result<List<Income>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(incomeTable)
                    .select {
                        filter { eq("user_id", userId) }
                        order("income_date", Order.DESCENDING)
                    }
                    .decodeList<Income>()
                Result.success(result)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun addIncome(income: Income): Result<Income> =
        withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    put("user_id", income.userId)
                    put("amount", income.amount)
                    put("income_date", income.incomeDate)
                    put("description", income.description)
                    income.categoryId?.let { categoryId ->
                        put("income_category_id", categoryId)
                    }
                    income.recurringIncomeId?.let { put("recurring_income_id", it) }
                }
                val result = supabase.from(incomeTable)
                    .insert(payload) { select() }
                    .decodeSingle<Income>()
                Result.success(result)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun updateIncome(income: Income): Result<Income> =
        withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    put("amount", income.amount)
                    put("income_date", income.incomeDate)
                    put("description", income.description)
                    income.categoryId?.let { put("income_category_id", it) }
                }
                val result = supabase.from(incomeTable)
                    .update(payload) {
                        select()
                        filter {
                            eq("income_id", income.incomeId)
                            eq("user_id", income.userId)
                        }
                    }
                    .decodeSingle<Income>()
                Result.success(result)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun deleteIncome(incomeId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(incomeTable).delete {
                    filter {
                        eq("income_id", incomeId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}

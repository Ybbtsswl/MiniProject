package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Income
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.withContext
import java.util.UUID

class IncomeRepository {
    private val supabase = SupabaseClient.instance
    private val incomeTable = "income"

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
                // income.category_id is a UUID in Supabase. Some existing category
                // rows use codes such as CAT001, so do not send those codes to the
                // UUID column. The remaining income data can still be saved.
                val payload = buildJsonObject {
                    put("user_id", income.userId)
                    put("amount", income.amount)
                    put("income_date", income.incomeDate)
                    put("description", income.description)
                    income.categoryId?.let { categoryId ->
                        if (runCatching { UUID.fromString(categoryId) }.isSuccess) {
                            put("category_id", categoryId)
                        }
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
}

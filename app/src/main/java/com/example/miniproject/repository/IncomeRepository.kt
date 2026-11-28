package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Income
import com.example.miniproject.data.IncomeCategory
import com.example.miniproject.data.RecurringIncome
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import java.util.Locale

class IncomeRepository {
    private val supabase = SupabaseClient.instance
    private val incomeTable = "income"
    private val incomeCategoryTable = "income_categories"
    private val recurringIncomeTable = "recurring_income"

    private suspend fun createDueRecurringIncome(userId: String) {
        val today = LocalDate.now()
        val recurringRecords = supabase.from(recurringIncomeTable)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
                }
            }
            .decodeList<RecurringIncome>()

        recurringRecords.forEach { recurring ->
            var dueDate = runCatching { LocalDate.parse(recurring.nextDate) }.getOrNull()
                ?: return@forEach
            val startDate = runCatching { LocalDate.parse(recurring.startDate) }.getOrNull()
                ?: return@forEach
            val intervalIsWeekly = recurring.frequency.equals("weekly", ignoreCase = true)

            while (!dueDate.isAfter(today)) {
                val incomePayload = buildJsonObject {
                    put("user_id", userId)
                    put("amount", recurring.amount)
                    put("income_date", dueDate.toString())
                    put("description", "Recurring income")
                    recurring.categoryId?.let { put("income_category_id", it) }
                    put("recurring_income_id", recurring.recurringIncomeId)
                }
                supabase.from(incomeTable).insert(incomePayload)

                dueDate = if (intervalIsWeekly) dueDate.plusWeeks(1) else dueDate.plusMonths(1)
                if (dueDate.isBefore(startDate)) return@forEach
            }

            val updatePayload = buildJsonObject {
                put("next_date", dueDate.toString())
            }
            supabase.from(recurringIncomeTable).update(updatePayload) {
                filter {
                    eq("recurring_income_id", recurring.recurringIncomeId)
                    eq("user_id", userId)
                }
            }
        }
    }

    suspend fun addRecurringIncome(
        userId: String,
        categoryId: String?,
        amount: Double,
        startDate: String,
        frequency: String
    ): Result<RecurringIncome> = withContext(Dispatchers.IO) {
        try {
            val normalizedFrequency = frequency.lowercase(Locale.ROOT)
            val nextDate = runCatching {
                val date = LocalDate.parse(startDate)
                if (normalizedFrequency == "weekly") date.plusWeeks(1) else date.plusMonths(1)
            }.getOrDefault(LocalDate.parse(startDate))
            val payload = buildJsonObject {
                put("recurring_income_id", "REC" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase())
                put("user_id", userId)
                categoryId?.let { put("category_id", it) }
                put("amount", amount)
                put("start_date", startDate)
                put("next_date", nextDate.toString())
                put("frequency", normalizedFrequency)
                put("is_active", true)
            }
            val result = supabase.from(recurringIncomeTable)
                .insert(payload) { select() }
                .decodeSingle<RecurringIncome>()
            Result.success(result)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

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

    suspend fun getActiveRecurringIncome(userId: String): Result<List<RecurringIncome>> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.from(recurringIncomeTable)
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<RecurringIncome>()
                Result.success(result)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun stopRecurringIncome(recurringIncomeId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject { put("is_active", false) }
                supabase.from(recurringIncomeTable).update(payload) {
                    filter {
                        eq("recurring_income_id", recurringIncomeId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun getIncome(userId: String): Result<List<Income>> =
        withContext(Dispatchers.IO) {
            try {
                createDueRecurringIncome(userId)
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

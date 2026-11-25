package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.FinancialReport
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FinancialReportRepository {
    private val supabase = SupabaseClient.instance
    private val table = "financial_report"

    // READ — Load the signed-in user's saved report history.
    suspend fun getReports(userId: String): Result<List<FinancialReport>> =
        withContext(Dispatchers.IO) {
            try {
                val reports =
                    supabase
                        .from(table)
                        .select {
                            filter { eq("user_id", userId) }
                            order("generated_at", Order.DESCENDING)
                            limit(30)
                        }
                        .decodeList<FinancialReport>()
                Result.success(reports)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    // CREATE — Supabase calculates totals for the selected dates with its trigger.
    suspend fun createReport(
        userId: String,
        reportType: String,
        startDate: String,
        endDate: String,
    ): Result<FinancialReport> =
        withContext(Dispatchers.IO) {
            try {
                val report =
                    supabase
                        .from(table)
                        .insert(
                            buildJsonObject {
                                put("user_id", userId)
                                put("report_type", reportType)
                                put("start_date", startDate)
                                put("end_date", endDate)
                            }
                        ) {
                            select()
                        }
                        .decodeSingle<FinancialReport>()
                Result.success(report)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    // UPDATE — Remember the downloaded filename for this user's report record.
    suspend fun updateFilePath(userId: String, reportId: String, filePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from(table).update(buildJsonObject { put("file_path", filePath) }) {
                    filter {
                        eq("report_id", reportId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
}

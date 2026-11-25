package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase model for the financial_report table. */
@Serializable
data class FinancialReport(
    @SerialName("report_id") val reportId: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("report_type") val reportType: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("total_income") val totalIncome: Double,
    @SerialName("total_expenses") val totalExpenses: Double,
    @SerialName("total_savings") val totalSavings: Double,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("file_path") val filePath: String? = null,
)

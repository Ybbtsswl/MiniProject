package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase model for the saving_goal table. */
@Serializable
data class SavingGoal(
    @SerialName("goal_id") val goalId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("goal_name") val goalName: String,
    val description: String = "",
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("current_amount") val currentAmount: Double,
    @SerialName("remaining_amount") val remainingAmount: Double = 0.0,
    val deadline: String,
    @SerialName("progress_percentage") val progressPercentage: Double = 0.0,
    val status: String = "ACTIVE",
    @SerialName("created_at") val createdAt: String = "",
)

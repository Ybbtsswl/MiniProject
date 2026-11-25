package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.SavingGoal
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SavingGoalRepository {
    private val supabase = SupabaseClient.instance
    private val table = "saving_goal"

    // READ — Only goals owned by the signed-in user.
    suspend fun getGoals(userId: String): Result<List<SavingGoal>> =
        withContext(Dispatchers.IO) {
            try {
                val goals =
                    supabase
                        .from(table)
                        .select {
                            filter { eq("user_id", userId) }
                            order("created_at", Order.DESCENDING)
                        }
                        .decodeList<SavingGoal>()
                Result.success(goals)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    // CREATE — Store a new goal for the signed-in user.
    suspend fun createGoal(goal: SavingGoal): Result<SavingGoal> =
        withContext(Dispatchers.IO) {
            try {
                val saved =
                    supabase
                        .from(table)
                        .insert(goalPayload(goal)) { select() }
                        .decodeSingle<SavingGoal>()
                Result.success(saved)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    // UPDATE — User filter prevents changing another user's goal.
    suspend fun updateGoal(goal: SavingGoal, previous: SavingGoal): Result<SavingGoal> =
        withContext(Dispatchers.IO) {
            try {
                val saved =
                    supabase
                        .from(table)
                        .update(goalPayload(goal)) {
                            select()
                            filter {
                                eq("goal_id", goal.goalId)
                                eq("user_id", goal.userId)

                                // Avoid overwriting a deposit made on another device.
                                eq("current_amount", previous.currentAmount)
                                eq("target_amount", previous.targetAmount)
                                eq("goal_name", previous.goalName)
                                eq("deadline", previous.deadline)
                                eq("description", previous.description)
                            }
                        }
                        .decodeSingle<SavingGoal>()
                Result.success(saved)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun saveGoal(goal: SavingGoal, previous: SavingGoal?): Result<SavingGoal> {
        return if (previous == null) createGoal(goal) else updateGoal(goal, previous)
    }

    private fun goalPayload(goal: SavingGoal) = buildJsonObject {
        put("goal_id", goal.goalId)
        put("user_id", goal.userId)
        put("goal_name", goal.goalName)
        put("description", goal.description)
        put("target_amount", goal.targetAmount)
        put("current_amount", goal.currentAmount)
        put("deadline", goal.deadline)
    }
}

package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository {
    private val supabase = SupabaseClient.instance

    suspend fun upsert(profile: Profile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles").upsert(profile)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getById(userId: String): Result<Profile?> = withContext(Dispatchers.IO) {
        try {
            val profile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Profile>()
            Result.success(profile)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

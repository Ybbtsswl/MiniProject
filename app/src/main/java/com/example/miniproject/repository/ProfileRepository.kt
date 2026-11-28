package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Profile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository {
    private val supabase = SupabaseClient.instance

    suspend fun uploadAvatar(userId: String, bytes: ByteArray, extension: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val safeExtension = extension.lowercase().replace("[^a-z0-9]".toRegex(), "")
                    .ifBlank { "jpg" }
                val path = "$userId/avatar.$safeExtension"
                val bucket = supabase.storage.from("avatars")
                bucket.upload(path, bytes) { upsert = true }
                val publicUrl = bucket.publicUrl(path)

                supabase.from("profiles").update(
                    mapOf("avatar_url" to publicUrl)
                ) {
                    filter { eq("id", userId) }
                }

                Result.success(publicUrl)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

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

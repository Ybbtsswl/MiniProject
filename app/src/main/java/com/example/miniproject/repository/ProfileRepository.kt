package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileRepository {
    private val supabase = SupabaseClient.instance

    suspend fun uploadAvatar(userId: String, bytes: ByteArray, extension: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val authenticatedUserId = supabase.auth.currentUserOrNull()?.id
                    ?: error("No signed-in user session was found")
                if (authenticatedUserId != userId) {
                    error("The profile user ID does not match the signed-in user")
                }

                val safeExtension = extension.lowercase().replace("[^a-z0-9]".toRegex(), "")
                    .ifBlank { "jpg" }
                val path = "$authenticatedUserId/avatar.$safeExtension"
                val bucket = supabase.storage.from("avatars")
                bucket.upload(path, bytes) { upsert = true }
                val publicUrl = bucket.publicUrl(path)

                val updatedProfile = supabase.from("profiles").update(
                    buildJsonObject { put("avatar_url", publicUrl) }
                ) {
                    select()
                    filter { eq("User_id", authenticatedUserId) }
                }.decodeSingleOrNull<Profile>()

                if (updatedProfile?.avatarUrl != publicUrl) {
                    error("Avatar uploaded, but profiles.avatar_url was not saved. Check the profile row and RLS UPDATE policy.")
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

    suspend fun updateProfileDetails(
        userId: String,
        fullName: String,
        darkMode: Boolean
    ): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val updated = supabase.from("profiles").update(
                buildJsonObject {
                    put("full_name", fullName)
                    put("dark_mode", darkMode)
                }
            ) {
                select()
                filter { eq("User_id", userId) }
            }.decodeSingle<Profile>()
            Result.success(updated)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getById(userId: String): Result<Profile?> = withContext(Dispatchers.IO) {
        try {
            val profile = supabase.from("profiles")
                .select {
                    filter { eq("User_id", userId) }
                }
                .decodeSingleOrNull<Profile>()
            Result.success(profile)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.AuthRequest
import com.example.miniproject.data.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

sealed interface AuthOutcome {
    data object SignedIn : AuthOutcome
    data object EmailConfirmationRequired : AuthOutcome
}

class AuthRepository(
    private val profileRepository: ProfileRepository = ProfileRepository()
) {
    private val supabase = SupabaseClient.instance

    suspend fun authenticate(request: AuthRequest): Result<AuthOutcome> {
        return try {
            val normalizedEmail = request.email
                .replace("\u00A0", "")
                .replace("\u200B", "")
                .trim()
                .lowercase(Locale.ROOT)

            if (request.isRegister) {
                supabase.auth.signUpWith(Email) {
                    email = normalizedEmail
                    password = request.password
                    data = buildJsonObject {
                        put("full_name", request.name)
                    }
                }

                val user = supabase.auth.currentUserOrNull()
                if (user == null) {
                    Result.success(AuthOutcome.EmailConfirmationRequired)
                } else {
                    profileRepository.upsert(
                        Profile(
                            id = user.id,
                            fullName = request.name,
                            email = normalizedEmail
                        )
                    ).getOrThrow()
                    Result.success(AuthOutcome.SignedIn)
                }
            } else {
                supabase.auth.signInWith(Email) {
                    email = normalizedEmail
                    password = request.password
                }

                val user = supabase.auth.currentUserOrNull()
                    ?: error("Login succeeded but no user session was returned")

                // Read the profile after login so the account data is verified and available.
                profileRepository.getById(user.id).getOrThrow()
                Result.success(AuthOutcome.SignedIn)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

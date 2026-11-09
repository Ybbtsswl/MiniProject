package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.AuthRequest
import com.example.miniproject.data.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface AuthOutcome {
    data object SignedIn : AuthOutcome
    data object Registered : AuthOutcome
}

class AuthRepository(
    private val profileRepository: ProfileRepository = ProfileRepository()
) {
    private val supabase = SupabaseClient.instance

    suspend fun authenticate(request: AuthRequest): Result<AuthOutcome> {
        return try {
            if (request.isRegister) {
                supabase.auth.signUpWith(Email) {
                    email = request.email
                    password = request.password
                    data = buildJsonObject {
                        put("full_name", request.name)
                    }
                }

                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    profileRepository.upsert(
                        Profile(
                            id = user.id,
                            fullName = request.name,
                            email = request.email
                        )
                    ).getOrThrow()
                }

                // Registration must not leave the user inside the app.
                // The user will sign in explicitly from the Login screen.
                if (user != null) {
                    supabase.auth.signOut()
                }
                Result.success(AuthOutcome.Registered)
            } else {
                supabase.auth.signInWith(Email) {
                    email = request.email
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

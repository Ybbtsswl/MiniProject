package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.AuthRequest
import com.example.miniproject.data.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

sealed interface AuthOutcome {
    data object SignedIn : AuthOutcome
    data object RegistrationOtpRequired : AuthOutcome
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

                // With email confirmation enabled, signUpWith sends the code but
                // does not create an active session yet. The profile is created
                // only after the user enters a valid OTP.
                Result.success(AuthOutcome.RegistrationOtpRequired)
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

    suspend fun verifyRegistrationOtp(
        email: String,
        token: String,
        name: String
    ): Result<Unit> {
        return try {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = email,
                token = token
            )

            val user = supabase.auth.currentUserOrNull()
                ?: error("OTP verified but no user session was returned")

            profileRepository.upsert(
                Profile(
                    id = user.id,
                    fullName = name,
                    email = email
                )
            ).getOrThrow()

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun resendRegistrationOtp(email: String): Result<Unit> {
        return try {
            supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun requestPasswordRecovery(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun verifyRecoveryOtp(email: String, token: String): Result<Unit> {
        return try {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.RECOVERY,
                email = email,
                token = token
            )
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun updateRecoveredPassword(password: String): Result<Unit> {
        return try {
            supabase.auth.updateUser { this.password = password }
            // Force the user to log in again with the new password.
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun updatePassword(password: String): Result<Unit> {
        return try {
            supabase.auth.updateUser { this.password = password }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

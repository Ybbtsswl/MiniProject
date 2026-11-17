package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.AuthRequest
import com.example.miniproject.data.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

sealed interface AuthOutcome {
    data object SignedIn : AuthOutcome
    data object EmailConfirmationRequired : AuthOutcome
    data object PasswordRecoveryOtpRequired : AuthOutcome
    data object PasswordRecoveryVerified : AuthOutcome
    data object PasswordReset : AuthOutcome
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

            if (request.isPasswordRecovery) {
                if (!request.newPassword.isNullOrBlank()) {
                    val passwordToSet = request.newPassword
                        ?: error("A new password is required")
                    supabase.auth.updateUser {
                        password = passwordToSet
                    }
                    Result.success(AuthOutcome.PasswordReset)
                } else if (!request.otp.isNullOrBlank()) {
                    supabase.auth.verifyEmailOtp(
                        type = OtpType.Email.RECOVERY,
                        email = normalizedEmail,
                        token = request.otp.trim()
                    )
                    Result.success(AuthOutcome.PasswordRecoveryVerified)
                } else {
                    supabase.auth.resetPasswordForEmail(normalizedEmail)
                    Result.success(AuthOutcome.PasswordRecoveryOtpRequired)
                }
            } else if (request.isRegister) {
                if (!request.otp.isNullOrBlank()) {
                    supabase.auth.verifyEmailOtp(
                        type = OtpType.Email.SIGNUP,
                        email = normalizedEmail,
                        token = request.otp.trim()
                    )

                    val verifiedUser = supabase.auth.currentUserOrNull()
                        ?: error("OTP verified but no user session was returned")

                    profileRepository.upsert(
                        Profile(
                            id = verifiedUser.id,
                            fullName = request.name,
                            email = normalizedEmail
                        )
                    ).getOrThrow()

                    Result.success(AuthOutcome.SignedIn)
                } else {
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

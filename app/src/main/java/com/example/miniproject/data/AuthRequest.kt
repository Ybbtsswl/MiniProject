package com.example.miniproject.data

data class AuthRequest(
    val isRegister: Boolean,
    val name: String,
    val email: String,
    val password: String,
    val otp: String? = null,
    val isPasswordRecovery: Boolean = false,
    val newPassword: String? = null
)

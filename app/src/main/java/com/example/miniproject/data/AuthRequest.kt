package com.example.miniproject.data

data class AuthRequest(
    val isRegister: Boolean,
    val name: String,
    val email: String,
    val password: String
)

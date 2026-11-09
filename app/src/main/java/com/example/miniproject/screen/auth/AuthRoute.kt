package com.example.miniproject.screen.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.miniproject.screen.DashboardScreen
import com.example.miniproject.screen.onboarding.OnboardingRoute

@Composable
fun AuthRoute() {
    var onboardingFinished by rememberSaveable { mutableStateOf(false) }
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var showDashboard by rememberSaveable { mutableStateOf(false) } // ✅ 仅新增：控制进入主界面

    when {
        showDashboard -> {
            DashboardScreen()
        }

        signedIn -> {
            WelcomeScreen(
                onSignOut = {
                    signedIn = false
                    showDashboard = false
                },
                onEnterDashboard = {
                    showDashboard = true
                }
            )
        }

        !onboardingFinished -> {
            OnboardingRoute(
                onFinished = {
                    onboardingFinished = true
                    isRegister = true
                }
            )
        }

        else -> {
            AuthScreen(
                isRegister = isRegister,
                onModeChange = { isRegister = it },
                onSuccess = {
                    signedIn = true
                }
            )
        }
    }
}

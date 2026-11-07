package com.example.miniproject.screen.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun AuthRoute() {
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var signedIn by rememberSaveable { mutableStateOf(false) }

    if (signedIn) {
        WelcomeScreen(onSignOut = { signedIn = false })
    } else {
        AuthScreen(
            isRegister = isRegister,
            onModeChange = { isRegister = it },
            onSuccess = { signedIn = true }
        )
    }
}

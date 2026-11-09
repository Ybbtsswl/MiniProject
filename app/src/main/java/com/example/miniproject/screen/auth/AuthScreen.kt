package com.example.miniproject.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.screen.componentUI.AuthField
import com.example.miniproject.screen.componentUI.ModeButton
import com.example.miniproject.screen.componentUI.PasswordField

private val Cream = Color(0xFFFFFBF2)
private val Yellow = Color(0xFFFFD447)
private val Ink = Color(0xFF202020)
private val Muted = Color(0xFF777777)

@Composable
fun AuthScreen(
    isRegister: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF5D2))
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(Color(0xFFFFF1C2))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "monee",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = if (isRegister) {
                    "Create your smart finance account"
                } else {
                    "Welcome back to your smart finance companion"
                },
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeButton(
                    label = "Log in",
                    selected = !isRegister,
                    onClick = { onModeChange(false) },
                    modifier = Modifier.weight(1f)
                )

                ModeButton(
                    label = "Register",
                    selected = isRegister,
                    onClick = { onModeChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isRegister) {
                AuthField(
                    label = "Full name",
                    value = name,
                    onValueChange = {
                        name = it
                        error = ""
                    },
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            AuthField(
                label = "Email address",
                value = email,
                onValueChange = {
                    email = it
                    error = ""
                },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordField(
                label = "Password",
                value = password,
                onValueChange = {
                    password = it
                    error = ""
                },
                visible = passwordVisible,
                onToggle = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Next
            )

            if (isRegister) {
                Spacer(modifier = Modifier.height(12.dp))

                PasswordField(
                    label = "Confirm password",
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = ""
                    },
                    visible = passwordVisible,
                    onToggle = { passwordVisible = !passwordVisible },
                    imeAction = ImeAction.Done
                )
            }

            if (!isRegister) {
                TextButton(
                    onClick = {
                        error = "Enter your email to reset your password."
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Forgot password?",
                        color = Ink,
                        fontSize = 13.sp
                    )
                }
            }

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    val validationError = when {
                        email.isBlank() || !email.contains("@") -> {
                            "Please enter a valid email address."
                        }

                        password.length < 6 -> {
                            "Password must be at least 6 characters."
                        }

                        isRegister && name.isBlank() -> {
                            "Please enter your name."
                        }

                        isRegister && password != confirmPassword -> {
                            "Passwords do not match."
                        }

                        else -> ""
                    }

                    error = validationError

                    if (validationError.isBlank()) {
                        onSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Ink
                )
            ) {
                Text(
                    text = if (isRegister) "Create account" else "Log in",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your money, made smarter.",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WelcomeScreen(onSignOut: () -> Unit,
                  onEnterDashboard:()-> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "monee",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "You’re all set!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = "Your finance companion is ready.",
                color = Muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            Button(
                onClick = onEnterDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Ink
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Go to Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Ink
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Log out",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

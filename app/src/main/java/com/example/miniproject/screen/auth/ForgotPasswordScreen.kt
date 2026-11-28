package com.example.miniproject.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.miniproject.screen.componentUI.PasswordField

private val Cream = Color(0xFFFFFBF2)
private val Yellow = Color(0xFFFFD447)
private val Ink = Color(0xFF202020)
private val Muted = Color(0xFF777777)
private const val OTP_WINDOW_SECONDS = 10 * 60
private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun ForgotPasswordScreen(
    onRequestOtp: suspend (String) -> Result<Unit>,
    onVerifyOtp: suspend (String, String) -> Result<Unit>,
    onUpdatePassword: suspend (String) -> Result<Unit>,
    onCompleted: () -> Unit,
    onBack: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var secondsRemaining by rememberSaveable { mutableIntStateOf(OTP_WINDOW_SECONDS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(step, secondsRemaining) {
        if (step == 1 && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val expiryText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize().background(Cream).padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (step) {
                0 -> "Forgot password?"
                1 -> "Verify recovery OTP"
                else -> "Create new password"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (step) {
                0 -> "Enter your registered email address."
                1 -> "Enter the OTP sent to $email"
                else -> "Choose a new password for your account."
            },
            color = Muted
        )
        Spacer(Modifier.height(22.dp))

        when (step) {
            0 -> OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = "" },
                label = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            1 -> {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 8) { otp = it.filter(Char::isDigit); message = "" } },
                    label = { Text("8-digit OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (secondsRemaining > 0) "Code expires in $expiryText" else "This OTP has expired.",
                    color = if (secondsRemaining > 0) Muted else Color(0xFFB3261E),
                    fontSize = 13.sp
                )
            }

            else -> {
                PasswordField(
                    label = "New password",
                    value = newPassword,
                    onValueChange = { newPassword = it; message = "" },
                    visible = false,
                    onToggle = {},
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                )
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    label = "Confirm password",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; message = "" },
                    visible = false,
                    onToggle = {},
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                )
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFB3261E), fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !isLoading && when (step) {
                0 -> EmailPattern.matches(email.trim())
                1 -> otp.length == 8 && secondsRemaining > 0
                else -> newPassword.length >= 6 && newPassword == confirmPassword
            },
            onClick = {
                isLoading = true
                scope.launch {
                    val result = when (step) {
                        0 -> onRequestOtp(email.trim())
                        1 -> onVerifyOtp(email.trim(), otp)
                        else -> onUpdatePassword(newPassword)
                    }
                    result.onSuccess {
                        when (step) {
                            0 -> {
                                step = 1
                                secondsRemaining = OTP_WINDOW_SECONDS
                                message = ""
                            }
                            1 -> { step = 2; message = "" }
                            else -> onCompleted()
                        }
                    }.onFailure {
                        message = when (step) {
                            0 -> "Unable to send the recovery email. Check the email and try again."
                            1 -> "Incorrect or expired OTP."
                            else -> "Unable to update the password. Please try again."
                        }
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink)
        ) {
            Text(
                text = when (step) {
                    0 -> if (isLoading) "Sending..." else "Send OTP"
                    1 -> if (isLoading) "Verifying..." else "Verify OTP"
                    else -> if (isLoading) "Saving..." else "Reset password"
                },
                fontWeight = FontWeight.Bold
            )
        }

        if (step == 1) {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    scope.launch {
                        onRequestOtp(email.trim()).onSuccess {
                            otp = ""
                            secondsRemaining = OTP_WINDOW_SECONDS
                            message = "A new OTP was sent."
                        }.onFailure { message = "Unable to resend the OTP." }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Resend OTP", color = Ink) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onBack) { Text("Back", color = Ink) }
        }
    }
}

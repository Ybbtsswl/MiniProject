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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.miniproject.data.AuthRequest
import com.example.miniproject.repository.AuthOutcome
import kotlinx.coroutines.launch

private val Cream = Color(0xFFFFFBF2)
private val Yellow = Color(0xFFFFD447)
private val Ink = Color(0xFF202020)
private val Muted = Color(0xFF777777)
private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun AuthScreen(
    isRegister: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onAuthenticate: suspend (AuthRequest) -> Result<AuthOutcome>
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showOtpScreen by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }
    var showForgotScreen by rememberSaveable { mutableStateOf(false) }
    var showRecoveryOtpScreen by rememberSaveable { mutableStateOf(false) }
    var showResetPasswordScreen by rememberSaveable { mutableStateOf(false) }
    var recoveryEmail by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmNewPassword by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (isRegister && showOtpScreen) {
        RegistrationOtpScreen(
            email = email.trim(),
            otp = otp,
            error = error,
            isLoading = isLoading,
            onOtpChange = {
                otp = it.filter(Char::isDigit).take(8)
                error = ""
            },
            onBack = {
                showOtpScreen = false
                otp = ""
                error = ""
            },
            onVerify = {
                if (otp.length != 8) {
                    error = "Please enter the 8-digit OTP."
                } else if (!isLoading) {
                    isLoading = true
                    scope.launch {
                        onAuthenticate(
                            AuthRequest(
                                isRegister = true,
                                name = name.trim(),
                                email = email.trim(),
                                password = password,
                                otp = otp
                            )
                        ).onSuccess {
                            showOtpScreen = false
                            otp = ""
                            error = ""
                            onSuccess()
                        }.onFailure { throwable ->
                            error = throwable.message?.ifBlank {
                                "The OTP is incorrect or expired."
                            } ?: "The OTP is incorrect or expired."
                        }
                        isLoading = false
                    }
                }
            }
        )
        return
    }

    if (!isRegister && showForgotScreen) {
        ForgotPasswordScreen(
            email = recoveryEmail,
            error = error,
            isLoading = isLoading,
            onEmailChange = {
                recoveryEmail = it
                error = ""
            },
            onBack = {
                showForgotScreen = false
                error = ""
            },
            onSendOtp = {
                val normalizedEmail = recoveryEmail.trim()
                if (!EmailPattern.matches(normalizedEmail)) {
                    error = "Please enter a valid email address."
                } else if (!isLoading) {
                    isLoading = true
                    scope.launch {
                        onAuthenticate(
                            AuthRequest(
                                isRegister = false,
                                name = "",
                                email = normalizedEmail,
                                password = "",
                                isPasswordRecovery = true
                            )
                        ).onSuccess { outcome ->
                            if (outcome == AuthOutcome.PasswordRecoveryOtpRequired) {
                                showForgotScreen = false
                                showRecoveryOtpScreen = true
                                otp = ""
                                error = "Enter the 8-digit code sent to your email."
                            }
                        }.onFailure { throwable ->
                            error = throwable.message?.ifBlank {
                                "We could not send the OTP. Please try again."
                            } ?: "We could not send the OTP. Please try again."
                        }
                        isLoading = false
                    }
                }
            }
        )
        return
    }

    if (!isRegister && showRecoveryOtpScreen) {
        RecoveryOtpScreen(
            email = recoveryEmail,
            otp = otp,
            error = error,
            isLoading = isLoading,
            onOtpChange = {
                otp = it.filter(Char::isDigit).take(8)
                error = ""
            },
            onBack = {
                showRecoveryOtpScreen = false
                showForgotScreen = true
                otp = ""
                error = ""
            },
            onVerify = {
                if (otp.length != 8) {
                    error = "Please enter the 8-digit OTP."
                } else if (!isLoading) {
                    isLoading = true
                    scope.launch {
                        onAuthenticate(
                            AuthRequest(
                                isRegister = false,
                                name = "",
                                email = recoveryEmail.trim(),
                                password = "",
                                otp = otp,
                                isPasswordRecovery = true
                            )
                        ).onSuccess { outcome ->
                            if (outcome == AuthOutcome.PasswordRecoveryVerified) {
                                showRecoveryOtpScreen = false
                                showResetPasswordScreen = true
                                otp = ""
                                error = ""
                            }
                        }.onFailure { throwable ->
                            error = throwable.message?.ifBlank {
                                "The OTP is incorrect or expired."
                            } ?: "The OTP is incorrect or expired."
                        }
                        isLoading = false
                    }
                }
            }
        )
        return
    }

    if (!isRegister && showResetPasswordScreen) {
        ResetPasswordScreen(
            password = newPassword,
            confirmPassword = confirmNewPassword,
            error = error,
            isLoading = isLoading,
            onPasswordChange = {
                newPassword = it
                error = ""
            },
            onConfirmPasswordChange = {
                confirmNewPassword = it
                error = ""
            },
            onBack = {
                showResetPasswordScreen = false
                showRecoveryOtpScreen = true
                error = ""
            },
            onReset = {
                error = when {
                    newPassword.length < 6 -> "Password must be at least 6 characters."
                    newPassword != confirmNewPassword -> "Passwords do not match."
                    else -> ""
                }
                if (error.isBlank() && !isLoading) {
                    isLoading = true
                    scope.launch {
                        onAuthenticate(
                            AuthRequest(
                                isRegister = false,
                                name = "",
                                email = recoveryEmail.trim(),
                                password = "",
                                isPasswordRecovery = true,
                                newPassword = newPassword
                            )
                        ).onSuccess { outcome ->
                            if (outcome == AuthOutcome.PasswordReset) {
                                showResetPasswordScreen = false
                                recoveryEmail = ""
                                newPassword = ""
                                confirmNewPassword = ""
                                error = "Password reset successful. Please log in."
                            }
                        }.onFailure { throwable ->
                            error = throwable.message?.ifBlank {
                                "We could not reset your password. Please try again."
                            } ?: "We could not reset your password. Please try again."
                        }
                        isLoading = false
                    }
                }
            }
        )
        return
    }

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
                        recoveryEmail = email.trim()
                        showForgotScreen = true
                        error = ""
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
                        !EmailPattern.matches(
                            email
                                .replace("\\u00A0", "")
                                .replace("\\u200B", "")
                                .trim()
                        ) -> {
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

                    if (validationError.isBlank() && !isLoading) {
                        isLoading = true
                        scope.launch {
                            onAuthenticate(
                                AuthRequest(
                                    isRegister = isRegister,
                                    name = name.trim(),
                                    email = email.trim(),
                                    password = password
                                )
                            ).onSuccess { outcome ->
                                when (outcome) {
                                    AuthOutcome.SignedIn -> onSuccess()
                                    AuthOutcome.EmailConfirmationRequired -> {
                                        showOtpScreen = true
                                        otp = ""
                                        error = "Enter the 8-digit code sent to your email."
                                    }
                                    AuthOutcome.PasswordRecoveryOtpRequired,
                                    AuthOutcome.PasswordRecoveryVerified,
                                    AuthOutcome.PasswordReset -> Unit
                                }
                            }.onFailure { throwable ->
                                val message = throwable.message.orEmpty()
                                error = when {
                                    message.contains("email_address_invalid", ignoreCase = true) -> {
                                        "Supabase rejected this email. Use a real email address, not a test or example address."
                                    }

                                    message.contains("invalid_credentials", ignoreCase = true) -> {
                                        "Email or password is incorrect."
                                    }

                                    else -> message.ifBlank {
                                        "Authentication failed. Please try again."
                                    }
                                }
                            }
                            isLoading = false
                        }
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
                    text = if (isLoading) "Please wait..." else if (isRegister) "Create account" else "Log in",
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
private fun RegistrationOtpScreen(
    email: String,
    otp: String,
    error: String,
    isLoading: Boolean,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verify your email",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = "Enter the OTP sent to $email",
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )

            AuthField(
                label = "8-digit OTP",
                value = otp,
                onValueChange = onOtpChange,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onVerify,
                enabled = !isLoading,
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
                    text = if (isLoading) "Verifying..." else "Verify OTP",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onBack) {
                Text(text = "Back to registration", color = Ink)
            }
        }
    }
}

@Composable
private fun ForgotPasswordScreen(
    email: String,
    error: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onBack: () -> Unit,
    onSendOtp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Forgot password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = "Enter your registered email to receive an OTP.",
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )

            AuthField(
                label = "Email address",
                value = email,
                onValueChange = onEmailChange,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSendOtp,
                enabled = !isLoading,
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
                    text = if (isLoading) "Sending..." else "Send OTP",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onBack) {
                Text(text = "Back to login", color = Ink)
            }
        }
    }
}

@Composable
private fun RecoveryOtpScreen(
    email: String,
    otp: String,
    error: String,
    isLoading: Boolean,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verify your email",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = "Enter the OTP sent to $email",
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )

            AuthField(
                label = "8-digit OTP",
                value = otp,
                onValueChange = onOtpChange,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onVerify,
                enabled = !isLoading,
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
                    text = if (isLoading) "Verifying..." else "Verify OTP",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onBack) {
                Text(text = "Back to email", color = Ink)
            }
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    password: String,
    confirmPassword: String,
    error: String,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Reset password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                text = "Create a new password for your account.",
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )

            PasswordField(
                label = "New password",
                value = password,
                onValueChange = onPasswordChange,
                visible = passwordVisible,
                onToggle = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordField(
                label = "Confirm new password",
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                visible = passwordVisible,
                onToggle = { passwordVisible = !passwordVisible },
                imeAction = ImeAction.Done
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = if (error.startsWith("Password reset successful")) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFB3261E)
                    },
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onReset,
                enabled = !isLoading,
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
                    text = if (isLoading) "Saving..." else "Reset password",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onBack) {
                Text(text = "Back to OTP", color = Ink)
            }
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

package com.example.miniproject.screen.auth

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private val Cream = Color(0xFFFFFBF2)
private val Yellow = Color(0xFFFFD447)
private val Ink = Color(0xFF202020)
private const val OTP_WINDOW_SECONDS = 10 * 60

@Composable
fun RegistrationOtpScreen(
    email: String,
    onVerify: suspend (String) -> Result<Unit>,
    onResend: suspend () -> Result<Unit>,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    var otp by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var secondsRemaining by rememberSaveable { mutableIntStateOf(OTP_WINDOW_SECONDS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val expiryText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize().background(Cream).padding(28.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to login",
                    tint = Ink
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Verify your email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text("Enter the OTP sent to $email", color = Color(0xFF777777))
        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 8) { otp = it.filter(Char::isDigit); message = "" } },
            label = { Text("OTP code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (secondsRemaining > 0) {
                "Code expires in $expiryText"
            } else {
                "This OTP has expired. Request a new code."
            },
            color = if (secondsRemaining > 0) Color(0xFF777777) else Color(0xFFB3261E),
            fontSize = 13.sp
        )

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFB3261E), fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            enabled = otp.length == 8 && secondsRemaining > 0 && !isLoading,
            onClick = {
                isLoading = true
                scope.launch {
                    onVerify(otp)
                        .onSuccess { onVerified() }
                        .onFailure { message = "Incorrect or expired OTP. Please try again." }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink)
        ) { Text(if (isLoading) "Verifying..." else "Verify OTP", fontWeight = FontWeight.Bold) }

        Button(
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    onResend().onSuccess {
                        otp = ""
                        message = "A new OTP was sent."
                        secondsRemaining = OTP_WINDOW_SECONDS
                    }
                        .onFailure { message = "Unable to resend the OTP." }
                }
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Ink)
        ) { Text("Resend OTP") }

    }
}

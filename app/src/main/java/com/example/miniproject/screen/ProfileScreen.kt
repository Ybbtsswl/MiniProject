package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Profile
import com.example.miniproject.repository.ProfileRepository
import com.example.miniproject.ui.theme.MiniProjectTheme

private val ProfileBackground = Color(0xFFF8F8FF)
private val ProfileInk = Color(0xFF172033)
private val ProfileMuted = Color(0xFF686D79)
private val ProfileYellow = Color(0xFFFFD447)
private val ProfileBlue = Color(0xFFE7EEFF)
private val ProfilePeach = Color(0xFFFFDEB3)

@Composable
fun ProfileScreen(
    userId: String?,
    onSignOut: () -> Unit
) {
    var profile by remember { mutableStateOf<Profile?>(null) }

    if (userId != null) {
        val repository = remember { ProfileRepository() }
        LaunchedEffect(userId) {
            profile = repository.getById(userId).getOrNull()
        }
    }

    val displayName = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Your Name"
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: "your.email@gmail.com"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("monee", color = ProfileInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("•", color = ProfileInk, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 10.dp))
            Text("Profile", color = ProfileInk, fontSize = 15.sp)
        }

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE7B4)),
            contentAlignment = Alignment.Center
        ) {
            Text("👩🏻", fontSize = 38.sp)
        }

        Text(
            text = displayName,
            color = ProfileInk,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(email, color = ProfileMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))

        Spacer(Modifier.height(46.dp))

        SafeToSpendCard()
        Spacer(Modifier.height(18.dp))

        ProfileMenuItem("▣", "My Profile", ProfileBlue) { }
        ProfileMenuItem("♧", "Help & Support", ProfileBlue) { }
        ProfileMenuItem("ⓘ", "About monee", ProfileBlue, trailing = "v2.4.1") { }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ProfilePeach)
                .clickable(onClick = onSignOut)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("↪", color = ProfileInk, fontSize = 20.sp)
            Text(
                "Log Out",
                color = ProfileInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SafeToSpendCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF0C2)),
            contentAlignment = Alignment.Center
        ) {
            Text("▣", color = Color(0xFF806500), fontSize = 18.sp)
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text("May Safe-to-Spend", color = ProfileMuted, fontSize = 10.sp)
            Text("RM1,420.00", color = ProfileInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.weight(1f))

        Text(
            "82% On Track",
            color = Color(0xFF8A5B20),
            fontSize = 10.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFEBD2))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: String,
    title: String,
    iconColor: Color,
    trailing: String = "›",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = ProfileInk, fontSize = 16.sp)
        }
        Text(title, color = ProfileInk, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
        Spacer(Modifier.weight(1f))
        Text(trailing, color = ProfileMuted, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Monee Profile Screen"
)
@Composable
private fun ProfileScreenPreview() {
    MiniProjectTheme {
        ProfileScreen(
            userId = null,
            onSignOut = {}
        )
    }
}


package com.example.miniproject.screen

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Profile
import com.example.miniproject.repository.ProfileRepository
import com.example.miniproject.ui.theme.MiniProjectTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private val ProfileBackground = Color(0xFFF8F8FF)
private val ProfileText = Color(0xFF172033)
private val ProfileMuted = Color(0xFF686D79)
private val ProfileBlue = Color(0xFFE6EEFF)
private val ProfilePeach = Color(0xFFFFDEB3)

@Composable
fun ProfileScreen(userId: String?, onSignOut: () -> Unit) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ProfileRepository() }

    if (userId != null) {
        LaunchedEffect(userId) {
            profile = repository.getById(userId).getOrNull()
        }
    }

    fun uploadAvatar(bytes: ByteArray, extension: String) {
        val currentUserId = userId ?: return
        scope.launch {
            isUploadingAvatar = true
            repository.uploadAvatar(currentUserId, bytes, extension)
                .onSuccess { url ->
                    profile = profile?.copy(avatarUrl = url)
                    Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to upload profile picture",
                        Toast.LENGTH_LONG
                    ).show()
                }
            isUploadingAvatar = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) {
            val extension = context.contentResolver.getType(uri)
                ?.substringAfter("/", "jpg") ?: "jpg"
            uploadAvatar(bytes, extension)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        uploadAvatar(output.toByteArray(), "jpg")
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    ProfileContent(
        name = profile?.fullName?.ifBlank { null } ?: "Alex Morgan",
        email = profile?.email?.ifBlank { null } ?: "alex.morgan@gmail.com",
        avatarUrl = profile?.avatarUrl,
        isUploadingAvatar = isUploadingAvatar,
        onChangeAvatar = { showImagePicker = true },
        onSignOut = onSignOut
    )

    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("Change profile picture") },
            text = { Text("Choose an image source") },
            confirmButton = {
                TextButton(onClick = {
                    showImagePicker = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImagePicker = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Take photo") }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    name: String,
    email: String,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onChangeAvatar: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("monee", color = ProfileText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("•", color = ProfileText, modifier = Modifier.padding(horizontal = 10.dp))
            Text("Profile", color = ProfileText, fontSize = 15.sp)
        }

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(7.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE5B0))
                .clickable(onClick = onChangeAvatar),
            contentAlignment = Alignment.Center
        ) {
            if (isUploadingAvatar) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = ProfileText,
                    strokeWidth = 3.dp
                )
            } else if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text("👩🏻", fontSize = 40.sp)
            }
        }

        Text(
            "Tap the picture to change it",
            color = ProfileMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp).clickable(onClick = onChangeAvatar)
        )

        Text(name, color = ProfileText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text(email, color = ProfileMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))

        Spacer(Modifier.height(48.dp))
        SafeToSpendCard()
        Spacer(Modifier.height(18.dp))
        ProfileItem("▣", "My Profile") { }
        ProfileItem("♧", "Help & Support") { }
        ProfileItem("ⓘ", "About monee", "v2.4.1") { }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ProfilePeach)
                .clickable(onClick = onSignOut)
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) { Text("↪  Log Out", color = ProfileText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
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
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF0C2)),
            contentAlignment = Alignment.Center
        ) { Text("▣", color = Color(0xFF806500), fontSize = 18.sp) }

        Column(Modifier.padding(start = 12.dp)) {
            Text("May Safe-to-Spend", color = ProfileMuted, fontSize = 10.sp)
            Text("RM1,420.00", color = ProfileText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
private fun ProfileItem(icon: String, title: String, trailing: String = "›", onClick: () -> Unit) {
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
                .size(34.dp)
                .clip(CircleShape)
                .background(ProfileBlue),
            contentAlignment = Alignment.Center
        ) { Text(icon, color = ProfileText, fontSize = 16.sp) }
        Text(title, color = ProfileText, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
        Spacer(Modifier.weight(1f))
        Text(trailing, color = ProfileMuted, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Monee Profile Screen")
@Composable
private fun ProfileScreenPreview() {
    MiniProjectTheme {
        ProfileContent(
            name = "Alex Morgan",
            email = "alex.morgan@gmail.com",
            avatarUrl = null,
            isUploadingAvatar = false,
            onChangeAvatar = { },
            onSignOut = { }
        )
    }
}

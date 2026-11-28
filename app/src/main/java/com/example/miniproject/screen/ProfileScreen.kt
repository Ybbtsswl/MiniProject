package com.example.miniproject.screen

import android.Manifest
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.Profile
import com.example.miniproject.repository.ProfileRepository
import com.example.miniproject.repository.AuthRepository
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.LocalThemeController
import com.example.miniproject.ui.theme.ThemeMode
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private val ProfileBackground = Color(0xFFF8F8FF)
private val ProfileText = Color(0xFF172033)
private val ProfileMuted = Color(0xFF686D79)
private val ProfileBlue = Color(0xFFE6EEFF)
private val ProfilePeach = Color(0xFFFFDEB3)

@Composable
fun ProfileScreen(
    userId: String?,
    onSignOut: () -> Unit
) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var darkMode by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("main") }
    var showImagePicker by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ProfileRepository() }

    if (userId != null) {
        LaunchedEffect(userId) {
            repository.getById(userId).getOrNull()?.let { loadedProfile ->
                profile = loadedProfile
                avatarUrl = loadedProfile.avatarUrl
                darkMode = loadedProfile.darkMode
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray, extension: String, previewUrl: String? = null) {
        val currentUserId = userId ?: return
        if (previewUrl != null) avatarUrl = previewUrl
        scope.launch {
            isUploadingAvatar = true
            repository.uploadAvatar(currentUserId, bytes, extension)
                .onSuccess { url ->
                    // Keep the local preview even if the profile query returned
                    // no row, then refresh the image URL to avoid Coil cache.
                    profile = profile?.copy(avatarUrl = url)
                    avatarUrl = "$url?avatarVersion=${System.currentTimeMillis()}"
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
            uploadAvatar(bytes, extension, uri.toString())
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

    when (page) {
        "settings" -> ProfileSettingsScreen(
            profile = profile,
            darkMode = darkMode,
            onBack = { page = "main" },
            onAccountDeleted = onSignOut,
            onSaved = { updated ->
                profile = updated
                darkMode = updated.darkMode
            }
        )
        "help" -> HelpSupportScreen(onBack = { page = "main" })
        else -> ProfileContent(
            name = profile?.fullName?.ifBlank { null } ?: "Alex Morgan",
            email = profile?.email?.ifBlank { null } ?: "alex.morgan@gmail.com",
            userId = userId,
            avatarUrl = avatarUrl,
            isUploadingAvatar = isUploadingAvatar,
            onChangeAvatar = { showImagePicker = true },
            onMyProfile = { page = "settings" },
            onHelpSupport = { page = "help" },
            onSignOut = onSignOut
        )
    }

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
    userId: String?,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onChangeAvatar: () -> Unit,
    onMyProfile: () -> Unit,
    onHelpSupport: () -> Unit,
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
                    contentScale = ContentScale.Crop,
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

        userId?.takeIf { it.isNotBlank() }?.let { id ->
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("User ID", id))
                        Toast.makeText(context, "User ID copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User ID: ${id.take(12)}…",
                    color = ProfileMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text("⧉", color = ProfileText, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(48.dp))
        SafeToSpendCard()
        Spacer(Modifier.height(18.dp))
        ProfileItem("▣", "My Profile", onClick = onMyProfile)
        ProfileItem("♧", "Help & Support", onClick = onHelpSupport)
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
private fun ProfileSettingsScreen(
    profile: Profile?,
    darkMode: Boolean,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSaved: (Profile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileRepository = remember { ProfileRepository() }
    val authRepository = remember { AuthRepository() }
    val themeController = LocalThemeController.current
    var name by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordSaving by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameSaving by remember { mutableStateOf(false) }
    var nameMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var deleteMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(ProfileBackground)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ProfileText
                )
            }
            Text("My Profile", color = ProfileText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFF7D6)).padding(14.dp)
        ) {
            Text("MY ACCOUNT", color = ProfileText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFBEA)).clickable {
                        nameMessage = ""
                        showNameDialog = true
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤", fontSize = 19.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Name", color = ProfileMuted, fontSize = 11.sp)
                    Text(name.ifBlank { "Add your name" }, color = ProfileText, fontSize = 15.sp)
                }
                Text("›", color = ProfileMuted, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFBEA)).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✉️", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Email", color = ProfileMuted, fontSize = 11.sp)
                    Text(profile?.email.orEmpty(), color = ProfileText, fontSize = 14.sp)
                }
                Text("🔒", fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFF7D6)).padding(14.dp)
        ) {
            Text("PREFERENCES & SECURITY", color = ProfileText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier =
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFFBEA)).padding(12.dp)
            ) {
                Text("🎨  App theme", color = ProfileText, fontSize = 15.sp)
                Text(
                    "Choose an appearance or follow your phone setting",
                    color = ProfileMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { themeController.setMode(mode) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeController.mode == mode,
                            onClick = { themeController.setMode(mode) },
                        )
                        Column {
                            Text(
                                when (mode) {
                                    ThemeMode.LIGHT -> "Light mode"
                                    ThemeMode.DARK -> "Dark mode"
                                    ThemeMode.SYSTEM -> "Use phone setting"
                                },
                                color = ProfileText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                when (mode) {
                                    ThemeMode.LIGHT -> "Always use the light appearance"
                                    ThemeMode.DARK -> "Always use the dark appearance"
                                    ThemeMode.SYSTEM -> "Change with your phone's theme"
                                },
                                color = ProfileMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFBEA)).clickable {
                        passwordMessage = ""
                        showPasswordDialog = true
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔑", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Change password", color = ProfileText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Update your account password", color = ProfileMuted, fontSize = 12.sp)
                }
                Text("›", color = ProfileMuted, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFE8E8)).clickable {
                        deleteMessage = ""
                        showDeleteDialog = true
                    }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗑️", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Delete account", color = Color(0xFFB3261E), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Permanently remove your account and data", color = Color(0xFFB3261E), fontSize = 12.sp)
                }
                Text("›", color = Color(0xFFB3261E), fontSize = 20.sp)
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!passwordSaving) showPasswordDialog = false },
            title = { Text("Change password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordMessage = "" },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordMessage = "" },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordMessage.isNotBlank()) {
                        Text(passwordMessage, color = Color(0xFFB3261E), fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !passwordSaving,
                    onClick = {
                        passwordMessage = when {
                            newPassword.isBlank() -> "Enter a new password."
                            newPassword.length < 6 -> "Password must be at least 6 characters."
                            confirmPassword.isBlank() -> "Confirm your new password."
                            newPassword != confirmPassword -> "Passwords do not match."
                            else -> ""
                        }
                        if (passwordMessage.isNotBlank()) return@TextButton

                        passwordSaving = true
                        scope.launch {
                            authRepository.updatePassword(newPassword)
                                .onSuccess {
                                    newPassword = ""
                                    confirmPassword = ""
                                    showPasswordDialog = false
                                    Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { passwordMessage = it.message ?: "Password update failed" }
                            passwordSaving = false
                        }
                    }
                ) { Text(if (passwordSaving) "Saving..." else "Save", color = Color.Black) }
            },
            dismissButton = {
                TextButton(enabled = !passwordSaving, onClick = { showPasswordDialog = false }) { Text("Cancel", color = Color.Black) }
            }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!nameSaving) showNameDialog = false },
            title = { Text("Change name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameMessage = "" },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nameMessage.isNotBlank()) {
                        Text(nameMessage, color = Color(0xFFB3261E), fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !nameSaving,
                    onClick = {
                        val updatedName = name.trim()
                        if (updatedName.isBlank()) {
                            nameMessage = "Name cannot be empty."
                            return@TextButton
                        }
                        val currentProfile = profile
                        if (currentProfile == null) {
                            nameMessage = "Profile could not be loaded. Please try again."
                            return@TextButton
                        }
                        nameSaving = true
                        scope.launch {
                            profileRepository.updateProfileDetails(
                                currentProfile.id,
                                updatedName,
                                darkMode
                            ).onSuccess {
                                name = updatedName
                                onSaved(it)
                                showNameDialog = false
                                Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                nameMessage = it.message ?: "Name update failed"
                            }
                            nameSaving = false
                        }
                    }
                ) { Text(if (nameSaving) "Saving..." else "Save", color = Color.Black) }
            },
            dismissButton = {
                TextButton(enabled = !nameSaving, onClick = { showNameDialog = false }) { Text("Cancel", color = Color.Black) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deletingAccount) showDeleteDialog = false },
            title = { Text("Delete account?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This permanently deletes your profile, income, expenses, and other account data. This cannot be undone.")
                    if (deleteMessage.isNotBlank()) {
                        Text(deleteMessage, color = Color(0xFFB3261E), fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        deletingAccount = true
                        scope.launch {
                            authRepository.deleteAccount()
                                .onSuccess {
                                    showDeleteDialog = false
                                    Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                    onAccountDeleted()
                                }
                                .onFailure { deleteMessage = it.message ?: "Unable to delete account" }
                            deletingAccount = false
                        }
                    }
                ) { Text(if (deletingAccount) "Deleting..." else "Delete", color = Color(0xFFB3261E)) }
            },
            dismissButton = {
                TextButton(enabled = !deletingAccount, onClick = { showDeleteDialog = false }) { Text("Cancel", color = Color.Black) }
            }
        )
    }
}

@Composable
private fun HelpSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(ProfileBackground)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ProfileText
                )
            }
            Text("Help & Support", color = ProfileText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Frequently asked questions", color = ProfileText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        SupportInfo("How do I add a transaction?", "Use the + button on the dashboard, then choose income or expense.")
        SupportInfo("How do I change my profile picture?", "Open Profile and tap the picture to choose Gallery or Camera.")
        SupportInfo("How do I reset my password?", "Use Forgot password on the login screen and enter the email OTP.")
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@monee.app"))
                    .putExtra(Intent.EXTRA_SUBJECT, "Monee support request")
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD447), contentColor = ProfileText),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Contact support", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SupportInfo(question: String, answer: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White).padding(14.dp)
    ) {
        Text(question, color = ProfileText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(answer, color = ProfileMuted, fontSize = 13.sp)
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
            userId = null,
            avatarUrl = null,
            isUploadingAvatar = false,
            onChangeAvatar = { },
            onMyProfile = { },
            onHelpSupport = { },
            onSignOut = { }
        )
    }
}

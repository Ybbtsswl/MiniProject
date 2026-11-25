package com.example.miniproject.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.SavingGoal as SavingGoalRecord
import com.example.miniproject.repository.SavingGoalRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Screen colors.
private val SavingsGold = Color(0xFFFFC332)
private val SavingsInk = Color(0xFF202838)
private val SavingsMuted = Color(0xFF828A99)
private val SavingsGreen = Color(0xFF00AA80)

// UI amounts are stored in cents to avoid rounding errors during deposits.
private data class SavingGoal(
    val id: String,
    val name: String,
    val target: Long,
    val saved: Long,
    val deadline: String,
    val group: Boolean,
    val lastDeposit: String = "",
    val description: String = "",
)

// Convert between the screen model and Supabase's amounts in ringgit.
// Group/last-deposit metadata is kept in the ERD description field as JSON.
private fun SavingGoal.cloud(user: String) =
    SavingGoalRecord(
        goalId = id,
        userId = user,
        goalName = name,
        description =
            JSONObject()
                .put("group", group)
                .put("last", lastDeposit)
                .put("text", description)
                .toString(),
        targetAmount = target / 100.0,
        currentAmount = saved / 100.0,
        deadline = deadline,
    )

private fun SavingGoalRecord.local(): SavingGoal {
    val meta = runCatching { JSONObject(description) }.getOrNull()
    return SavingGoal(
        goalId,
        goalName,
        (targetAmount * 100).roundToLong(),
        (currentAmount * 100).roundToLong(),
        deadline,
        meta?.optBoolean("group") ?: false,
        meta?.optString("last").orEmpty(),
        meta?.optString("text") ?: description,
    )
}

private fun money(cents: Long) = "RM " + String.format(Locale.US, "%,.2f", cents / 100.0)

// Reject invalid amounts, extra decimal places, and amounts above the limit.
private fun cents(text: String): Long? =
    try {
        text
            .toBigDecimal()
            .setScale(2, java.math.RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
            .takeIf { it in 1..100000000000L }
    } catch (_: Exception) {
        null
    }

@Composable
fun SavingsScreen(userId: String?, onBack: () -> Unit) {
    // Data access and screen state.
    val context = LocalContext.current
    val prefs =
        remember(userId) {
            context.getSharedPreferences("savings_${userId ?: "guest"}", Context.MODE_PRIVATE)
        }
    val repository = remember { SavingGoalRepository() }
    val scope = rememberCoroutineScope()
    var goals by remember(userId) { mutableStateOf<List<SavingGoal>>(emptyList()) }
    var cloudGoals by remember(userId) { mutableStateOf<List<SavingGoalRecord>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    // Load cloud goals and migrate legacy device-only goals once per user.
    LaunchedEffect(userId, refresh) {
        busy = true
        loadError = null
        runCatching {
                requireNotNull(userId) { "Sign in to use savings." }
                val remote = repository.getGoals(userId).getOrThrow()
                if (!prefs.getBoolean("cloud_migrated", false)) {
                    val array = JSONArray(prefs.getString("goals", "[]"))
                    val legacy =
                        List(array.length()) { i ->
                            array.getJSONObject(i).let {
                                SavingGoal(
                                    it.getString("id"),
                                    it.getString("name"),
                                    it.getLong("target"),
                                    it.getLong("saved"),
                                    it.getString("deadline"),
                                    it.getBoolean("group"),
                                    it.optString("last"),
                                )
                            }
                        }
                    legacy
                        .filter { old -> remote.none { it.goalId == old.id } }
                        .forEach { repository.saveGoal(it.cloud(userId), null).getOrThrow() }
                    prefs.edit().putBoolean("cloud_migrated", true).apply()
                }
                cloudGoals = repository.getGoals(userId).getOrThrow()
                goals = cloudGoals.map { it.local() }
            }
            .onFailure { loadError = "Unable to load savings. Check your connection and retry." }
        busy = false
    }
    // Update the screen only after Supabase confirms a successful save.
    fun save(goal: SavingGoal, onSuccess: () -> Unit) {
        if (busy || userId == null) return
        busy = true
        loadError = null
        scope.launch {
            runCatching {
                    repository
                        .saveGoal(goal.cloud(userId), cloudGoals.find { it.goalId == goal.id })
                        .getOrThrow()
                }
                .onSuccess { saved ->
                    cloudGoals = cloudGoals.filterNot { it.goalId == saved.goalId } + saved
                    goals = cloudGoals.map { it.local() }
                    onSuccess()
                }
                .onFailure {
                    loadError =
                        "Save failed or this goal changed on another device. Close the dialog and refresh before retrying."
                }
            busy = false
        }
    }
    // Dialog state: null means closed; creating=false/true means personal/group.
    var creating by remember { mutableStateOf<Boolean?>(null) }
    var editing by remember { mutableStateOf<SavingGoal?>(null) }
    var depositing by remember { mutableStateOf<SavingGoal?>(null) }
    // Summary totals for all goals.
    val total = goals.sumOf { it.saved }
    val target = goals.sumOf { it.target }
    val ratio = if (target > 0) total.toFloat() / target else 0f
    // Scrollable screen: summary, personal goals, then group goals.
    Surface(color = Color(0xFFF7F8FA), contentColor = SavingsInk) {
        LazyColumn(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(56.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        "Savings Management",
                        Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }
            item {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { refresh++ }, enabled = !busy) { Text("Refresh savings") }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFF9E4),
                    border = BorderStroke(1.dp, Color(0xFFFFE7A0)),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "TOTAL SAVINGS POOL",
                                fontSize = 11.sp,
                                color = Color(0xFFB98047),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (ratio >= 1f) "● Achieved" else "● Building savings",
                                color = SavingsGreen,
                                fontSize = 11.sp,
                            )
                        }
                        Text(money(total), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Color(0xFFFFE9AD))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Target: ${money(target)}", fontSize = 12.sp)
                            Text(
                                String.format(Locale.US, "%.1f%% achieved", ratio * 100),
                                fontSize = 12.sp,
                                color = Color(0xFFCE791B),
                            )
                        }
                        SavingsProgress(ratio)
                    }
                }
            }
            listOf(false, true).forEach { group ->
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (group) "Group Challenges"
                            else "Personal Goals  ${goals.count { !it.group }}",
                            fontWeight = FontWeight.Bold,
                        )
                        Button(
                            onClick = { creating = group },
                            enabled = !busy && loadError == null && userId != null,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = SavingsGold,
                                    contentColor = SavingsInk,
                                ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        ) {
                            Text(if (group) "+ Create Group" else "+ New Goal", fontSize = 12.sp)
                        }
                    }
                    if (group)
                        Text(
                            "Saved to your account · no member sharing",
                            fontSize = 11.sp,
                            color = SavingsMuted,
                        )
                }
                val section = goals.filter { it.group == group }
                if (section.isEmpty())
                    item {
                        Text(
                            if (group) "Plan a shared trip or a group saving goal."
                            else "Create your first goal and start saving.",
                            color = SavingsMuted,
                            fontSize = 13.sp,
                        )
                    }
                items(section, key = { it.id }) { goal ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border =
                            BorderStroke(1.dp, if (group) Color(0xFFFFE7A0) else Color(0xFFF0F1F3)),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFF7DE),
                                ) {
                                    Text(
                                        if (group) "🏝" else "🎯",
                                        Modifier.padding(12.dp),
                                        fontSize = 22.sp,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Deadline: ${goal.deadline}",
                                        color = SavingsMuted,
                                        fontSize = 12.sp,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editing = goal
                                        creating = goal.group
                                    },
                                    enabled = !busy,
                                ) {
                                    Text("Edit")
                                }
                            }
                            Text(
                                if (group) "OVERALL POOL PROGRESS" else "SAVED SO FAR",
                                color = SavingsMuted,
                                fontSize = 10.sp,
                            )
                            Text(
                                "${money(goal.saved)} / ${money(goal.target)}",
                                fontWeight = FontWeight.Bold,
                            )
                            SavingsProgress(goal.saved.toFloat() / goal.target)
                            val days =
                                ChronoUnit.DAYS.between(
                                    LocalDate.now(),
                                    LocalDate.parse(goal.deadline),
                                )
                            Text(
                                when {
                                    goal.saved >= goal.target -> "Goal achieved!"
                                    days <= 0 -> "Deadline reached · keep saving"
                                    else ->
                                        "${(100.0 * goal.saved / goal.target).toInt()}% saved · ${money((goal.target - goal.saved + days - 1) / days)}/day to reach goal"
                                },
                                color =
                                    if (goal.saved >= goal.target) SavingsGreen
                                    else Color(0xFFC27B20),
                                fontSize = 11.sp,
                            )
                            HorizontalDivider(color = Color(0xFFF4F4F4))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    goal.lastDeposit.ifBlank { "Ready for your first deposit" },
                                    Modifier.weight(1f),
                                    color = SavingsMuted,
                                    fontSize = 11.sp,
                                )
                                TextButton(onClick = { depositing = goal }, enabled = !busy) {
                                    Text("+ Deposit", color = Color(0xFFCC7510), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Savings sync to your Supabase account. Deposits track money saved; they do not transfer money.",
                    color = SavingsMuted,
                    fontSize = 11.sp,
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    // Create/edit dialog: validate the fields before saving to Supabase.
    if (creating != null) {
        val goalId = remember { editing?.id ?: UUID.randomUUID().toString() }
        var name by remember { mutableStateOf(editing?.name.orEmpty()) }
        var description by remember { mutableStateOf(editing?.description.orEmpty()) }
        var amount by remember {
            mutableStateOf(
                editing
                    ?.let { java.math.BigDecimal.valueOf(it.target, 2).toPlainString() }
                    .orEmpty()
            )
        }
        var date by remember {
            mutableStateOf(editing?.deadline ?: LocalDate.now().plusMonths(3).toString())
        }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    creating = null
                    editing = null
                }
            },
            title = {
                Text(
                    if (editing != null) "Edit Savings Goal"
                    else if (creating == true) "Create Group Goal" else "New Savings Goal"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it.take(60) },
                        label = { Text("Goal name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        description,
                        { description = it.take(500) },
                        label = { Text("Description") },
                    )
                    OutlinedTextField(
                        amount,
                        { amount = it },
                        label = { Text("Target amount (RM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        date,
                        { date = it },
                        label = { Text("Deadline (YYYY-MM-DD)") },
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val value = cents(amount)
                        val deadline = runCatching { LocalDate.parse(date) }.getOrNull()
                        if (
                            name.isBlank() ||
                                value == null ||
                                deadline == null ||
                                (deadline.isBefore(LocalDate.now()) &&
                                    deadline.toString() != editing?.deadline)
                        ) {
                            error =
                                "Enter a name, a positive amount (up to 2 decimal places), and a deadline today or later."
                        } else {
                            save(
                                SavingGoal(
                                    goalId,
                                    name.trim(),
                                    value,
                                    editing?.saved ?: 0,
                                    deadline.toString(),
                                    creating == true,
                                    editing?.lastDeposit.orEmpty(),
                                    description,
                                )
                            ) {
                                creating = null
                                editing = null
                            }
                        }
                    },
                ) {
                    Text(if (busy) "Saving…" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        creating = null
                        editing = null
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
    // Deposit dialog: add a positive amount to the selected goal's balance.
    depositing?.let { goal ->
        var amount by remember(goal.id) { mutableStateOf("") }
        var error by remember(goal.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) depositing = null },
            title = { Text("Deposit to ${goal.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        amount,
                        { amount = it },
                        label = { Text("Amount (RM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val value = cents(amount)
                        if (value == null || goal.saved > 100000000000L - value)
                            error = "Enter a valid positive amount within the savings limit."
                        else {
                            save(
                                goal.copy(
                                    saved = goal.saved + value,
                                    lastDeposit = "+ ${money(value)} on ${LocalDate.now()}",
                                )
                            ) {
                                depositing = null
                            }
                        }
                    },
                ) {
                    Text(if (busy) "Saving…" else "Add deposit")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { depositing = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SavingsProgress(value: Float) {
    LinearProgressIndicator(
        progress = { value.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        color = SavingsGold,
        trackColor = Color(0xFFF0F1F4),
    )
}

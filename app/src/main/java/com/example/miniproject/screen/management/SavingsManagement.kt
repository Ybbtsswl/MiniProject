/*
 * ============================================================================
 * Module 2: Savings Management (FR-SVG-001 to FR-SVG-009)
 * File: SavingsManagement.kt
 * ============================================================================
 */
package com.example.miniproject.screen.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeButtonBrush
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

// ============================================================================
// 1. SAVINGS & GROUP CHALLENGE DATA MODELS (MATCHING UML CLASS DIAGRAM)
// ============================================================================
enum class SavingsSubTab(val label: String) {
    PERSONAL("Personal Goals"),
    GROUP("Group Goals"),
}

data class SavingsContribution(val id: String, val amount: Double, val date: String)

/**
 * UML Entity: Saving Goal +String goalId, +String userId, +String goalName, +String description,
 * +double targetAmount, +double currentAmount, +double remainingAmount, +String deadline, +double
 * progressPercentage, +String status = "ACTIVE"
 */
data class SavingGoal(
    val goalId: String,
    val userId: String,
    val goalName: String,
    val description: String = "",
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val remainingAmount: Double = max(0.0, targetAmount - currentAmount),
    val deadline: String,
    val progressPercentage: Double =
        if (targetAmount > 0.0) min(100.0, (currentAmount / targetAmount) * 100.0) else 0.0,
    val status: String = "ACTIVE", // "ACTIVE" | "COMPLETED" | "EXPIRED"
    val startDate: String = LocalDate.now().toString(),
    val contributions: List<SavingsContribution> = emptyList(),
)

data class RegisteredUser(
    val id: String,
    val name: String,
    val email: String,
    val initials: String,
    val avatarColor: Color,
)

data class GroupMemberProgress(
    val name: String,
    val goal: String,
    val saved: Double,
    val targetShare: Double,
    val isMe: Boolean = false,
) {
    val progressPct: Double
        get() = if (targetShare > 0.0) min(100.0, (saved / targetShare) * 100.0) else 0.0
}

data class GroupInvitation(
    val userId: String,
    val name: String,
    val status: String, // "Pending" | "Accepted" | "Declined"
)

data class GroupChallenge(
    val id: String,
    val name: String,
    val owner: String,
    val target: Double,
    val startDate: String,
    val deadline: String,
    val members: List<GroupMemberProgress>,
    val invitations: List<GroupInvitation> = emptyList(),
) {
    val totalSaved: Double
        get() = members.sumOf { it.saved }

    val progressPct: Double
        get() = if (target > 0.0) min(100.0, (totalSaved / target) * 100.0) else 0.0
}

data class PendingGroupInvite(
    val id: String,
    val challengeName: String,
    val invitedBy: String,
    val target: Double,
    val startDate: String,
    val deadline: String,
    val members: List<GroupMemberProgress>,
)

fun computeSavingGoalStatus(currentAmount: Double, targetAmount: Double, deadline: String): String {
    if (currentAmount >= targetAmount) return "COMPLETED"
    val today = LocalDate.now()
    val dl = runCatching { LocalDate.parse(deadline) }.getOrNull()
    return if (dl != null && dl.isBefore(today)) "EXPIRED" else "ACTIVE"
}

// ============================================================================
// 2. SAVINGS VIEWMODEL
// ============================================================================
// ============================================================================
// 3. SAVINGS MANAGEMENT SCREEN & DIALOGS
// ============================================================================
@Composable
fun SavingsManagementScreen(
    vm: SavingsViewModel,
    showMessage: (String) -> Unit,
    onAddGoal: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var editingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var detailsGoalId by remember { mutableStateOf<String?>(null) }

    var showCreateChallengeDialog by remember { mutableStateOf(false) }
    var inviteChallengeId by remember { mutableStateOf<String?>(null) }
    var groupContribChallengeId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(MoneeColors.CardWhite)
                        .border(1.dp, MoneeBorder, RoundedCornerShape(50.dp))
                        .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SavingsSubTab.values().forEach { sub ->
                        val active = vm.savingsSubTab == sub
                        Box(
                            modifier =
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(50.dp))
                                    .then(
                                        if (active) Modifier.background(MoneeButtonBrush)
                                        else Modifier.background(Color.Transparent)
                                    )
                                    .clickable {
                                        vm.savingsSubTab = sub
                                        vm.selectedChallengeId = null
                                    }
                                    .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    sub.label,
                                    fontSize = 16.sp,
                                    fontWeight =
                                        if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) MoneeInk else MoneeMuted,
                                )
                                if (
                                    sub == SavingsSubTab.GROUP &&
                                        vm.pendingInvitations.isNotEmpty()
                                ) {
                                    Surface(
                                        color = MoneeColors.WarningOrange,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            "${vm.pendingInvitations.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier =
                                                Modifier.padding(
                                                    horizontal = 5.dp,
                                                    vertical = 1.dp,
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (vm.savingsSubTab == SavingsSubTab.PERSONAL) {
            val totalTarget = vm.savingsGoals.sumOf { it.targetAmount }
            val totalSaved = vm.savingsGoals.sumOf { it.currentAmount }
            val overallPct =
                if (totalTarget > 0.0) min(100.0, (totalSaved / totalTarget) * 100.0) else 0.0

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MoneeColors.BrandSoft,
                                            MoneeColors.BrandYellow,
                                            MoneeColors.BrandGold,
                                        )
                                    )
                                )
                                .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "TOTAL PERSONAL SAVINGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.InkDark.copy(alpha = 0.75f),
                                )
                                Surface(
                                    color = MoneeColors.InkDark.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        String.format(Locale.US, "%.1f%%", overallPct),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneeColors.InkDark,
                                        modifier =
                                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Text(
                                formatRM(totalSaved),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MoneeColors.InkDark,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (overallPct / 100.0).toFloat() },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                color = MoneeColors.InkDark,
                                trackColor = MoneeColors.InkDark.copy(alpha = 0.15f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Target: ${formatRM(totalTarget)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MoneeColors.InkDark,
                                )
                                Text(
                                    "Remaining: ${formatRM(max(0.0, totalTarget - totalSaved))}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MoneeColors.InkDark,
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "YOUR GOALS (${vm.savingsGoals.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.Slate500,
                    )
                }

                items(vm.savingsGoals, key = { it.goalId }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onViewDetails = { detailsGoalId = goal.goalId },
                        onEdit = { editingGoal = goal },
                        onDelete = { deletingGoal = goal },
                    )
                }
            }
        } else {
            val selectedCh = vm.groupChallenges.find { it.id == vm.selectedChallengeId }
            if (selectedCh != null) {
                GroupChallengeDetailContent(
                    challenge = selectedCh,
                    onBack = { vm.selectedChallengeId = null },
                    onInvite = { inviteChallengeId = selectedCh.id },
                    onContribute = { groupContribChallengeId = selectedCh.id },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (vm.pendingInvitations.isNotEmpty()) {
                        item {
                            Text(
                                "PENDING INVITATIONS (${vm.pendingInvitations.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.WarningOrange,
                            )
                        }
                        items(vm.pendingInvitations, key = { it.id }) { inv ->
                            Surface(
                                color = MoneeColors.WarningBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MoneeColors.WarningBorder),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        inv.challengeName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneeColors.InkDark,
                                    )
                                    Text(
                                        "Invited by ${inv.invitedBy} · Target ${formatRM(inv.target)}",
                                        fontSize = 11.sp,
                                        color = MoneeColors.Slate700,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    vm.respondToInvitation(inv.id, false)
                                                        .onSuccess(showMessage)
                                                        .onFailure { showMessage(it.message ?: "Unable to decline invitation.") }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text(
                                                "Decline",
                                                color = MoneeColors.Slate700,
                                                fontSize = 12.sp,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    vm.respondToInvitation(inv.id, true)
                                                        .onSuccess(showMessage)
                                                        .onFailure { showMessage(it.message ?: "Unable to accept invitation.") }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor = MoneeColors.BrandYellow,
                                                    contentColor = MoneeColors.InkDark,
                                                ),
                                        ) {
                                            Text(
                                                "Accept",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "GROUP CHALLENGES (${vm.groupChallenges.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.Slate500,
                            )
                            Button(
                                onClick = { showCreateChallengeDialog = true },
                                shape = RoundedCornerShape(50),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MoneeColors.BrandYellow,
                                        contentColor = MoneeColors.InkDark,
                                    ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Create Challenge",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    items(vm.groupChallenges, key = { it.id }) { ch ->
                        Surface(
                            color = MoneeColors.CardWhite,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MoneeColors.Slate100),
                            modifier =
                                Modifier.fillMaxWidth().clickable { vm.selectedChallengeId = ch.id },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(
                                            ch.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MoneeColors.InkDark,
                                        )
                                        Text(
                                            "${ch.members.size} members · Due ${formatShortDate(ch.deadline)}",
                                            fontSize = 11.sp,
                                            color = MoneeColors.Slate400,
                                        )
                                    }
                                    Surface(
                                        color = MoneeColors.BrandBadgeBg,
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            "${ch.progressPct.toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MoneeColors.BrandDarkGold,
                                            modifier =
                                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { (ch.progressPct / 100.0).toFloat() },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50)),
                                    color = MoneeColors.BrandGold,
                                    trackColor = MoneeColors.Slate100,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${formatRM(ch.totalSaved)} / ${formatRM(ch.target)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneeColors.Slate700,
                                    )
                                    Text(
                                        "View Group >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneeColors.BrandDarkGold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        } // Content column

        // Personal goals use the same persistent bottom action as Expenses.
        if (vm.savingsSubTab == SavingsSubTab.PERSONAL) {
            Box(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, MoneeColors.CardWhite))
                        )
                        .navigationBarsPadding()
                        .padding(top = 24.dp, bottom = 16.dp)
            ) {
                MoneeGradientButton(
                    onClick = onAddGoal,
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
                ) {
                    Text("+ Add Goal", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    editingGoal?.let { g ->
        SavingsGoalFormDialog(
            title = "Edit Savings Goal",
            confirmText = "Save Changes",
            initial = g,
            onDismiss = { editingGoal = null },
            onSave = { name, desc, target, _, dl ->
                scope.launch {
                    try {
                        vm.updateSavingsGoal(g.goalId, name, desc, target, dl)
                            .onSuccess {
                                editingGoal = null
                                showMessage(it)
                            }
                            .onFailure { showMessage(it.message ?: "Invalid input") }
                    } catch (error: Exception) {
                        showMessage(error.message ?: "Unable to save changes.")
                    }
                }
            },
        )
    }

    deletingGoal?.let { g ->
        ConfirmDeleteDialog(
            title = "Delete Savings Goal",
            message =
                "Are you sure you want to delete \"${g.goalName}\" and its contribution history?",
            onDismiss = { deletingGoal = null },
            onConfirm = {
                scope.launch {
                    try {
                        vm.deleteSavingsGoal(g.goalId)
                        deletingGoal = null
                        showMessage("Savings goal removed.")
                    } catch (error: Exception) {
                        showMessage(error.message ?: "Unable to save changes.")
                    }
                }
            },
        )
    }

    detailsGoalId?.let { id ->
        val liveGoal = vm.savingsGoals.find { it.goalId == id }
        if (liveGoal != null) {
            SavingsGoalDetailsDialog(
                goal = liveGoal,
                onDismiss = { detailsGoalId = null },
                onAddContribution = { amount, date ->
                    scope.launch {
                        try {
                            vm.addSavingsContribution(liveGoal.goalId, amount, date)
                                .onSuccess { showMessage(it) }
                                .onFailure { showMessage(it.message ?: "Error") }
                        } catch (error: Exception) {
                            showMessage(error.message ?: "Unable to save changes.")
                        }
                    }
                },
            )
        }
    }

    if (showCreateChallengeDialog) {
        CreateGroupChallengeDialog(
            onDismiss = { showCreateChallengeDialog = false },
            onCreate = { name, target, start, dl ->
                scope.launch {
                    vm.createGroupChallenge(name, target, start, dl)
                        .onSuccess {
                            showCreateChallengeDialog = false
                            showMessage(it)
                        }
                        .onFailure { showMessage(it.message ?: "Invalid input") }
                }
            },
        )
    }

    inviteChallengeId?.let { cid ->
        val liveCh = vm.groupChallenges.find { it.id == cid }
        if (liveCh != null) {
            InviteFriendsDialog(
                challenge = liveCh,
                users = vm.registeredUsers,
                onDismiss = { inviteChallengeId = null },
                onInvite = { u ->
                    scope.launch {
                        vm.inviteFriendToChallenge(liveCh.id, u)
                            .onSuccess { showMessage(it) }
                            .onFailure { showMessage(it.message ?: "Error") }
                    }
                },
            )
        }
    }

    groupContribChallengeId?.let { cid ->
        val liveCh = vm.groupChallenges.find { it.id == cid }
        if (liveCh != null) {
            GroupContributionDialog(
                challengeName = liveCh.name,
                onDismiss = { groupContribChallengeId = null },
                onSubmit = { amt ->
                    scope.launch {
                        vm.contributeToGroupChallenge(liveCh.id, amt)
                            .onSuccess {
                                groupContribChallengeId = null
                                showMessage(it)
                            }
                            .onFailure { showMessage(it.message ?: "Invalid amount") }
                    }
                },
            )
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingGoal,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = goal.status
    val badgeBg =
        when (status) {
            "COMPLETED" -> MoneeColors.BrandSoft
            "EXPIRED" -> MoneeColors.DangerBg
            else -> MoneeColors.BrandLight
        }
    val badgeText =
        when (status) {
            "COMPLETED" -> MoneeColors.BrandDarkGold
            "EXPIRED" -> MoneeColors.DangerRed
            else -> MoneeColors.BrandDeep
        }

    Surface(
        color = MoneeColors.CardWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MoneeColors.Slate100),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.goalName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.InkDark,
                    )
                    if (goal.description.isNotBlank()) {
                        Text(
                            goal.description,
                            fontSize = 11.sp,
                            color = MoneeColors.Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "Deadline: ${formatShortDate(goal.deadline)}",
                        fontSize = 11.sp,
                        color = MoneeColors.Slate400,
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${formatRM(goal.currentAmount)} of ${formatRM(goal.targetAmount)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneeColors.InkDark,
                )
                Text(
                    "${goal.progressPercentage.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MoneeColors.Slate700,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (goal.progressPercentage / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                color = if (status == "EXPIRED") MoneeColors.DangerRed else MoneeColors.BrandGold,
                trackColor = MoneeColors.Slate100,
            )
            Text(
                "Remaining: ${formatRM(goal.remainingAmount)}",
                fontSize = 10.sp,
                color = MoneeColors.Slate500,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MoneeColors.Slate100,
                            contentColor = MoneeColors.Slate700,
                        ),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text("Details", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MoneeColors.BrandBadgeBg,
                            contentColor = MoneeColors.BrandDarkGold,
                        ),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text("+ Contribute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onEdit,
                    modifier =
                        Modifier.size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MoneeColors.Slate100),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(15.dp),
                        tint = MoneeColors.Slate700,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MoneeColors.DangerBg),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(15.dp),
                        tint = MoneeColors.DangerRed,
                    )
                }
            }
        }
    }
}

@Composable
fun GroupChallengeDetailContent(
    challenge: GroupChallenge,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onContribute: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBack() },
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MoneeColors.Slate500,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Back to Group Challenges",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeColors.Slate500,
                )
            }
        }

        item {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MoneeColors.BrandSoft,
                                    MoneeColors.BrandYellow,
                                    MoneeColors.BrandGold,
                                )
                            )
                        )
                        .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "GROUP SAVINGS CHALLENGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark.copy(alpha = 0.75f),
                            )
                            Text(
                                challenge.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MoneeColors.InkDark,
                            )
                        }
                        Surface(
                            color = MoneeColors.InkDark.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                String.format(Locale.US, "%.1f%%", challenge.progressPct),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${formatRM(challenge.totalSaved)} / ${formatRM(challenge.target)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MoneeColors.InkDark,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (challenge.progressPct / 100.0).toFloat() },
                        modifier =
                            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = MoneeColors.InkDark,
                        trackColor = MoneeColors.InkDark.copy(alpha = 0.15f),
                    )
                    Text(
                        "Deadline: ${formatShortDate(challenge.deadline)} · Owner: ${challenge.owner}",
                        fontSize = 10.sp,
                        color = MoneeColors.InkDark.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onInvite,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MoneeColors.BrandDeep,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Invite Friends",
                        fontSize = 12.sp,
                        color = MoneeColors.InkDark,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onContribute,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MoneeColors.BrandYellow,
                            contentColor = MoneeColors.InkDark,
                        ),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Contribute", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                "MEMBERS' PROGRESS (${challenge.members.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MoneeColors.Slate500,
            )
        }
        items(challenge.members) { m ->
            Surface(
                color = MoneeColors.CardWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MoneeColors.Slate100),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                m.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.InkDark,
                            )
                            Text("Goal: ${m.goal}", fontSize = 10.sp, color = MoneeColors.Slate400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatRM(m.saved),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MoneeColors.BrandDarkGold,
                            )
                            Text(
                                "${m.progressPct.toInt()}% of ${formatRM(m.targetShare)}",
                                fontSize = 10.sp,
                                color = MoneeColors.Slate400,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (m.progressPct / 100.0).toFloat() },
                        modifier =
                            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        color = MoneeColors.BrandYellow,
                        trackColor = MoneeColors.Slate100,
                    )
                }
            }
        }
    }
}

/** Full-page flow for creating a personal savings goal. */
@Composable
fun AddSavingsGoalScreen(
    vm: SavingsViewModel,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var deadline by remember { mutableStateOf(LocalDate.now().plusMonths(6).toString()) }
    var isSaving by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var deadlineError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Set a target and deadline. You can add contributions after the goal is created.",
                color = MoneeColors.Slate500,
                fontSize = 13.sp,
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Goal name") },
                placeholder = { Text("Example: Emergency fund") },
                isError = nameError != null,
                supportingText = nameError?.let { message -> { Text(message) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("What are you saving for?") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = {
                    targetAmount = it
                    amountError = null
                },
                label = { Text("Target amount (RM)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = amountError?.let { message -> { Text(message) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = {
                    startDate = it
                    startDateError = null
                    deadlineError = null
                },
                label = { Text("Start date") },
                placeholder = { Text("YYYY-MM-DD") },
                isError = startDateError != null,
                supportingText = {
                    Text(startDateError ?: "Use YYYY-MM-DD")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = deadline,
                onValueChange = {
                    deadline = it
                    deadlineError = null
                },
                label = { Text("Deadline") },
                placeholder = { Text("YYYY-MM-DD") },
                isError = deadlineError != null,
                supportingText = {
                    Text(deadlineError ?: "Use YYYY-MM-DD")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            saveError?.let { Text(it, color = MoneeColors.DangerRed, fontSize = 13.sp) }
        }

        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, MoneeColors.CardWhite))
                    )
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = 16.dp)
        ) {
            MoneeGradientButton(
                enabled = !isSaving,
                onClick = {
                    val parsedAmount = targetAmount.toDoubleOrNull()
                    val parsedStart = runCatching { LocalDate.parse(startDate) }.getOrNull()
                    val parsedDeadline = runCatching { LocalDate.parse(deadline) }.getOrNull()

                    nameError = if (name.isBlank()) "Enter a goal name." else null
                    amountError =
                        if (parsedAmount == null || parsedAmount <= 0) {
                            "Enter a positive target amount."
                        } else {
                            null
                        }
                    startDateError =
                        if (parsedStart == null) "Enter a valid start date." else null
                    deadlineError =
                        when {
                            parsedDeadline == null -> "Enter a valid deadline."
                            parsedStart != null && parsedDeadline.isBefore(parsedStart) ->
                                "Deadline must be on or after the start date."
                            else -> null
                        }

                    if (
                        nameError == null && amountError == null && startDateError == null &&
                            deadlineError == null
                    ) {
                        isSaving = true
                        saveError = null
                        scope.launch {
                            vm.addSavingsGoal(
                                    name,
                                    description,
                                    parsedAmount!!,
                                    startDate,
                                    deadline,
                                )
                                .onSuccess {
                                    isSaving = false
                                    showMessage(it)
                                    onBack()
                                }
                                .onFailure {
                                    isSaving = false
                                    saveError = it.message ?: "Unable to save goal."
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
            ) {
                Text(
                    if (isSaving) "Saving..." else "Add Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun SavingsGoalFormDialog(
    title: String,
    confirmText: String,
    initial: SavingGoal?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.goalName ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var targetStr by remember { mutableStateOf(initial?.targetAmount?.toString() ?: "") }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now().toString()) }
    var deadline by remember {
        mutableStateOf(initial?.deadline ?: LocalDate.now().plusMonths(6).toString())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MoneeColors.CardWhite) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MoneeColors.InkDark,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Amount (RM) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (initial == null) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel", color = MoneeColors.Slate700)
                    }
                    Button(
                        onClick = {
                            onSave(
                                name,
                                description,
                                targetStr.toDoubleOrNull() ?: 0.0,
                                startDate,
                                deadline,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MoneeColors.BrandYellow,
                                contentColor = MoneeColors.InkDark,
                            ),
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalDetailsDialog(
    goal: SavingGoal,
    onDismiss: () -> Unit,
    onAddContribution: (Double, String) -> Unit,
) {
    var amt by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MoneeColors.CardWhite,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.82f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            goal.goalName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MoneeColors.InkDark,
                        )
                        if (goal.description.isNotBlank()) {
                            Text(goal.description, fontSize = 11.sp, color = MoneeColors.Slate500)
                        }
                        Text(
                            "Deadline: ${formatShortDate(goal.deadline)}",
                            fontSize = 11.sp,
                            color = MoneeColors.Slate400,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(
                    color = MoneeColors.Slate100,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(color = MoneeColors.SurfaceBg, shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    goal.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.BrandDarkGold,
                                )
                                Text(
                                    "${String.format(Locale.US, "%.1f", goal.progressPercentage)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (goal.progressPercentage / 100.0).toFloat() },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                color = MoneeColors.BrandGold,
                                trackColor = MoneeColors.Slate200,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Saved: ${formatRM(goal.currentAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.BrandDarkGold,
                                )
                                Text("Target: ${formatRM(goal.targetAmount)}", fontSize = 11.sp)
                            }
                        }
                    }

                    Surface(
                        color = MoneeColors.BrandLight,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MoneeColors.BrandSoft),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Record Savings Contribution",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneeColors.BrandDarkGold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amt,
                                    onValueChange = { amt = it },
                                    label = { Text("Amount (RM)") },
                                    keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = { date = it },
                                    label = { Text("Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Button(
                                onClick = {
                                    onAddContribution(amt.toDoubleOrNull() ?: 0.0, date)
                                    amt = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MoneeColors.BrandYellow,
                                        contentColor = MoneeColors.InkDark,
                                    ),
                            ) {
                                Text("Add Contribution", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        "CONTRIBUTION HISTORY (${goal.contributions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeColors.Slate400,
                    )
                    goal.contributions.forEach { c ->
                        Surface(color = MoneeColors.SurfaceBg, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    formatShortDate(c.date),
                                    fontSize = 12.sp,
                                    color = MoneeColors.Slate700,
                                )
                                Text(
                                    "+${formatRM(c.amount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneeColors.BrandDarkGold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var deadline by remember { mutableStateOf(LocalDate.now().plusMonths(6).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MoneeColors.CardWhite) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Create Group Challenge", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Challenge Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Shared Target (RM) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MoneeColors.Slate700)
                    }
                    Button(
                        onClick = {
                            onCreate(name, target.toDoubleOrNull() ?: 0.0, start, deadline)
                        },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MoneeColors.BrandYellow,
                                contentColor = MoneeColors.InkDark,
                            ),
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InviteFriendsDialog(
    challenge: GroupChallenge,
    users: List<RegisteredUser>,
    onDismiss: () -> Unit,
    onInvite: (RegisteredUser) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        users.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MoneeColors.CardWhite) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Invite Friends", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search registered users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier =
                        Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filtered.forEach { u ->
                        val isMember = challenge.members.any { it.name.contains(u.name) }
                        val inv = challenge.invitations.find { it.userId == u.id }
                        Surface(color = MoneeColors.SurfaceBg, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(34.dp)
                                                .clip(CircleShape)
                                                .background(u.avatarColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            u.initials,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(u.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            u.email,
                                            fontSize = 10.sp,
                                            color = MoneeColors.Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                when {
                                    isMember ->
                                        Text(
                                            "Accepted",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MoneeColors.BrandDarkGold,
                                        )
                                    inv != null ->
                                        Text(
                                            inv.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color =
                                                if (inv.status == "Pending")
                                                    MoneeColors.WarningOrange
                                                else MoneeColors.DangerRed,
                                        )
                                    else ->
                                        Button(
                                            onClick = { onInvite(u) },
                                            contentPadding =
                                                PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor = MoneeColors.BrandYellow,
                                                    contentColor = MoneeColors.InkDark,
                                                ),
                                        ) {
                                            Text(
                                                "Invite",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupContributionDialog(
    challengeName: String,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit,
) {
    var amt by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MoneeColors.CardWhite) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Contribute to $challengeName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                OutlinedTextField(
                    value = amt,
                    onValueChange = { amt = it },
                    label = { Text("Contribution Amount (RM) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MoneeColors.Slate700)
                    }
                    Button(
                        onClick = { onSubmit(amt.toDoubleOrNull() ?: 0.0) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MoneeColors.BrandYellow,
                                contentColor = MoneeColors.InkDark,
                            ),
                    ) {
                        Text("Contribute", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

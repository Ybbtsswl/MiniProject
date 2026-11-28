package com.example.miniproject.screen.management

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.Budget as BudgetRecord
import com.example.miniproject.data.Profile
import com.example.miniproject.data.SavingGoal as GoalRecord
import com.example.miniproject.repository.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject

private fun userId(): String =
    requireNotNull(SupabaseClient.instance.auth.currentUserOrNull()?.id) { "Please sign in." }

private fun metadata(value: String): JSONObject =
    runCatching { JSONObject(value) }.getOrDefault(JSONObject())

private fun categoryName(value: String): String =
    when {
        value.equals("Bills & Utilities", true) -> "Housing & Utilities"
        else -> com.example.miniproject.data.Categories.findByName(value)?.name ?: value
    }

class BudgetViewModel : ViewModel() {
    var budgets by mutableStateOf<List<Budget>>(emptyList())
    var dismissedAlertIds by mutableStateOf(setOf<String>())
    private val lock = Mutex()
    private val repository = BudgetRepository()
    private var records = emptyList<BudgetRecord>()

    suspend fun load() {
        val uid = userId()
        records =
            SupabaseClient.instance
                .from("budgets")
                .select { filter { eq("user_id", uid) } }
                .decodeList<BudgetRecord>()
        val expenses = ExpenseRepository().getExpenses(uid).getOrThrow()
        budgets =
            records
                .filterNot { metadata(it.budgetName).optBoolean("archived") }
                .map { row ->
                    val meta = metadata(row.budgetName)
                    val name = meta.optString("name", row.budgetName)
                    val category = meta.optString("category", name)
                    val matches =
                        expenses.filter { e ->
                            e.date.take(10) in row.startDate..row.endDate &&
                                categoryName(e.category).equals(categoryName(category), true)
                        }
                    val spent = matches.sumOf { it.amount }
                    Budget(
                        row.budgetId,
                        uid,
                        name,
                        row.amountLimit,
                        spent,
                        LocalDate.parse(row.startDate),
                        LocalDate.parse(row.endDate),
                        computeBudgetStatus(spent, row.amountLimit),
                        category = category,
                        expenses =
                            matches.map {
                                BudgetExpense(it.expenseId, it.description, it.amount, it.date)
                            },
                    )
                }
    }

    fun getActiveBudgetAlerts() =
        budgets.mapNotNull { b ->
            if (b.usagePct < 80) null
            else
                BudgetAlert(
                    b.budgetId,
                    b.budgetId,
                    b.usagePct >= 100,
                    "${b.budgetName}: ${b.usagePct.toInt()}% used",
                )
        }

    fun dismissAlert(id: String) {
        dismissedAlertIds = dismissedAlertIds + id
    }

    fun restoreAlerts() {
        dismissedAlertIds = emptySet()
    }

    private suspend fun save(
        id: String?,
        name: String,
        amount: Double,
        category: String,
        start: String,
        end: String,
    ): Result<String> =
        lock.withLock {
            runCatching {
                require(name.isNotBlank() && amount.isFinite() && amount > 0) {
                    "Enter a name and a positive amount."
                }
                require(!LocalDate.parse(start).isAfter(LocalDate.parse(end))) {
                    "End date must follow start date."
                }
                val old = records.find { it.budgetId == id }
                val record =
                    BudgetRecord(
                        budgetId = id.orEmpty(),
                        userId = userId(),
                        budgetName =
                            JSONObject()
                                .put("name", name.trim())
                                .put("category", category)
                                .toString(),
                        amountLimit = amount,
                        totalSpent = old?.totalSpent ?: 0.0,
                        startDate = start,
                        endDate = end,
                    )
                if (old == null) repository.createBudget(record).getOrThrow()
                else repository.updateBudget(record).getOrThrow()
                load()
                "Budget saved."
            }
        }

    suspend fun addBudget(
        name: String,
        allocated: Double,
        category: String,
        startDateStr: String,
        endDateStr: String,
    ) = save(null, name, allocated, category, startDateStr, endDateStr)

    suspend fun updateBudget(
        budgetId: String,
        name: String,
        allocated: Double,
        category: String,
        startDateStr: String,
        endDateStr: String,
    ) = save(budgetId, name, allocated, category, startDateStr, endDateStr)

    suspend fun deleteBudget(id: String) {
        lock.withLock {
            val old = records.first { it.budgetId == id }
            val meta =
                metadata(old.budgetName)
                    .put("name", budgets.first { it.budgetId == id }.budgetName)
                    .put("archived", true)
            repository.updateBudget(old.copy(budgetName = meta.toString())).getOrThrow()
            load()
        }
    }

    suspend fun addExpenseToBudget(
        budgetId: String,
        amount: Double,
        date: String,
        description: String,
    ): Result<String> =
        lock.withLock {
            runCatching {
                require(amount.isFinite() && amount > 0 && description.isNotBlank()) {
                    "Enter a positive amount and description."
                }
                val budget = budgets.first { it.budgetId == budgetId }
                val parsed = LocalDate.parse(date)
                require(parsed in budget.startDate..budget.endDate) {
                    "Expense must be within the budget dates."
                }
                val categories = ExpenseRepository().getCategories(userId()).getOrThrow()
                val category =
                    categories.find {
                        categoryName(it.name).equals(categoryName(budget.category), true)
                    }
                SupabaseClient.instance
                    .from("expenses")
                    .insert(
                        buildJsonObject {
                            put("user_id", userId())
                            put("amount", amount)
                            put("date", date)
                            put("description", description)
                            put("category", category?.name ?: budget.category)
                            category?.let { put("category_id", it.categoryId) }
                        }
                    )
                load()
                val updated = budgets.first { it.budgetId == budgetId }
                repository
                    .updateTotalSpent(records.first { it.budgetId == budgetId }, updated.totalSpent)
                    .getOrThrow()
                "Expense recorded."
            }
        }
}

class SavingsViewModel : ViewModel() {
    val currentUserId
        get() = userId()

    var currentUserName by mutableStateOf("")
    var savingsSubTab by mutableStateOf(SavingsSubTab.PERSONAL)
    var selectedChallengeId by mutableStateOf<String?>(null)
    var registeredUsers by mutableStateOf<List<RegisteredUser>>(emptyList())
    var savingsGoals by mutableStateOf<List<SavingGoal>>(emptyList())
    var pendingInvitations by mutableStateOf<List<PendingGroupInvite>>(emptyList())
    var groupChallenges by mutableStateOf<List<GroupChallenge>>(emptyList())
    private val repository = SavingGoalRepository()
    private val groupRepository = SavingGroupRepository()
    private var records = emptyList<GoalRecord>()
    private val lock = Mutex()

    suspend fun load() {
        val profiles =
            SupabaseClient.instance
                .from("profiles")
                .select { filter { eq("User_id", userId()) } }
                .decodeList<Profile>()
        currentUserName = profiles.firstOrNull()?.fullName.orEmpty()
        records = repository.getGoals(userId()).getOrThrow()
        savingsGoals =
            records
                .filterNot { metadata(it.description).optBoolean("archived") }
                .map { row ->
                    val meta = metadata(row.description)
                    val entries = meta.optJSONArray("contributions") ?: JSONArray()
                    SavingGoal(
                        row.goalId,
                        row.userId,
                        row.goalName,
                        meta.optString("text", row.description),
                        row.targetAmount,
                        row.currentAmount,
                        row.remainingAmount,
                        row.deadline,
                        row.progressPercentage,
                        computeSavingGoalStatus(row.currentAmount, row.targetAmount, row.deadline),
                        meta.optString(
                            "start",
                            row.createdAt.take(10).ifBlank { LocalDate.now().toString() },
                        ),
                        List(entries.length()) { i ->
                            entries.getJSONObject(i).let {
                                SavingsContribution(
                                    it.getString("id"),
                                    it.getDouble("amount"),
                                    it.getString("date"),
                                )
                            }
                        },
                    )
                }

        loadSharedSavings()
    }

    private suspend fun loadSharedSavings() {
        val uid = userId()
        val data = groupRepository.load().getOrThrow()
        val usersById = data.users.associateBy { it.id }
        val displayName: (String) -> String = { id ->
            usersById[id]?.fullName?.ifBlank { usersById[id]?.email.orEmpty() }
                ?.ifBlank { "Monee user" } ?: "Monee user"
        }
        val avatarColors =
            listOf(
                Color(0xFF3B82F6),
                Color(0xFF8B5CF6),
                Color(0xFFEC4899),
                Color(0xFF14B8A6),
                Color(0xFFF97316),
            )

        currentUserName = displayName(uid)
        registeredUsers =
            data.users.filter { it.id != uid }.map { profile ->
                val name = profile.fullName.ifBlank { profile.email.substringBefore('@') }
                RegisteredUser(
                    id = profile.id,
                    name = name,
                    email = profile.email,
                    initials =
                        name.split(' ').filter { it.isNotBlank() }.take(2)
                            .joinToString("") { it.take(1).uppercase() }.ifBlank { "U" },
                    avatarColor = avatarColors[(profile.id.hashCode() and Int.MAX_VALUE) % avatarColors.size],
                )
            }

        fun membersFor(group: com.example.miniproject.data.SavingGroupRecord): List<GroupMemberProgress> {
            val rows = data.members.filter { it.groupId == group.groupId }
            val equalShare = if (rows.isEmpty()) group.targetAmount else group.targetAmount / rows.size
            return rows.map { member ->
                GroupMemberProgress(
                    name = displayName(member.userId),
                    goal = member.goalLabel,
                    saved =
                        data.contributions.filter {
                            it.groupId == group.groupId && it.userId == member.userId
                        }.sumOf { it.amount },
                    targetShare = member.targetShare.takeIf { it > 0 } ?: equalShare,
                    isMe = member.userId == uid,
                )
            }
        }

        val joinedGroupIds = data.members.filter { it.userId == uid }.map { it.groupId }.toSet()
        groupChallenges =
            data.groups.filter { it.ownerId == uid || it.groupId in joinedGroupIds }.map { group ->
                GroupChallenge(
                    id = group.groupId,
                    name = group.groupName,
                    owner = displayName(group.ownerId),
                    target = group.targetAmount,
                    startDate = group.startDate,
                    deadline = group.deadline,
                    members = membersFor(group),
                    invitations =
                        data.invitations.filter { it.groupId == group.groupId }.map { invitation ->
                            GroupInvitation(
                                userId = invitation.inviteeId,
                                name = displayName(invitation.inviteeId),
                                status = invitation.status.lowercase().replaceFirstChar { it.uppercase() },
                            )
                        },
                )
            }

        pendingInvitations =
            data.invitations.filter { it.inviteeId == uid && it.status == "PENDING" }.mapNotNull { invitation ->
                data.groups.find { it.groupId == invitation.groupId }?.let { group ->
                    PendingGroupInvite(
                        id = invitation.invitationId,
                        challengeName = group.groupName,
                        invitedBy = displayName(invitation.inviterId),
                        target = group.targetAmount,
                        startDate = group.startDate,
                        deadline = group.deadline,
                        members = membersFor(group),
                    )
                }
            }
    }

    private suspend fun save(
        id: String?,
        name: String,
        description: String,
        target: Double,
        start: String,
        deadline: String,
    ): Result<String> =
        lock.withLock {
            runCatching {
                require(name.isNotBlank() && target.isFinite() && target > 0) {
                    "Enter a name and a positive target."
                }
                require(!LocalDate.parse(start).isAfter(LocalDate.parse(deadline))) {
                    "Check the goal dates."
                }
                val old = records.find { it.goalId == id }
                val meta =
                    metadata(old?.description.orEmpty())
                        .put("text", description)
                        .put("start", start)
                repository
                    .saveGoal(
                        GoalRecord(
                            id ?: UUID.randomUUID().toString(),
                            userId(),
                            name,
                            meta.toString(),
                            target,
                            old?.currentAmount ?: 0.0,
                            deadline = deadline,
                        ),
                        old,
                    )
                    .getOrThrow()
                load()
                "Goal saved."
            }
        }

    suspend fun addSavingsGoal(
        name: String,
        description: String,
        target: Double,
        startDate: String,
        deadline: String,
    ) = save(null, name, description, target, startDate, deadline)

    suspend fun updateSavingsGoal(
        goalId: String,
        name: String,
        description: String,
        target: Double,
        deadline: String,
    ) =
        save(
            goalId,
            name,
            description,
            target,
            savingsGoals.first { it.goalId == goalId }.startDate,
            deadline,
        )

    suspend fun deleteSavingsGoal(id: String) =
        lock.withLock {
            val old = records.first { it.goalId == id }
            repository
                .saveGoal(
                    old.copy(
                        description = metadata(old.description).put("archived", true).toString()
                    ),
                    old,
                )
                .getOrThrow()
            load()
        }

    suspend fun addSavingsContribution(
        goalId: String,
        amount: Double,
        date: String,
    ): Result<String> =
        lock.withLock {
            runCatching {
                require(amount.isFinite() && amount > 0) { "Enter a positive amount." }
                LocalDate.parse(date)
                val old = records.first { it.goalId == goalId }
                val meta = metadata(old.description)
                val entries = meta.optJSONArray("contributions") ?: JSONArray()
                entries.put(
                    JSONObject()
                        .put("id", UUID.randomUUID().toString())
                        .put("amount", amount)
                        .put("date", date)
                )
                meta.put("contributions", entries)
                repository
                    .saveGoal(
                        old.copy(
                            currentAmount = old.currentAmount + amount,
                            description = meta.toString(),
                        ),
                        old,
                    )
                    .getOrThrow()
                load()
                "Contribution saved."
            }
        }

    suspend fun respondToInvitation(id: String, accept: Boolean): Result<String> =
        lock.withLock {
            runCatching {
                groupRepository.respond(id, if (accept) "ACCEPTED" else "DECLINED").getOrThrow()
                load()
                if (accept) "Invitation accepted." else "Invitation declined."
            }
        }

    suspend fun createGroupChallenge(
        name: String,
        target: Double,
        start: String,
        end: String,
    ): Result<String> = lock.withLock {
        runCatching {
            require(name.isNotBlank() && target.isFinite() && target > 0) {
                "Enter a name and positive target."
            }
            require(!LocalDate.parse(start).isAfter(LocalDate.parse(end))) {
                "The deadline must be on or after the start date."
            }
            groupRepository.createGroup(userId(), name.trim(), target, start, end).getOrThrow()
            load()
            "Group challenge created."
        }
    }

    suspend fun inviteFriendToChallenge(id: String, user: RegisteredUser): Result<String> =
        lock.withLock {
            runCatching {
                require(user.id != userId()) { "You are already in this challenge." }
                groupRepository.invite(id, userId(), user.id).getOrThrow()
                load()
                "Invitation sent to ${user.name}."
            }
        }

    suspend fun contributeToGroupChallenge(id: String, amount: Double): Result<String> =
        lock.withLock {
            runCatching {
                require(amount.isFinite() && amount > 0) { "Enter a positive amount." }
                groupRepository.contribute(id, userId(), amount).getOrThrow()
                load()
                "Contribution saved."
            }
        }
}

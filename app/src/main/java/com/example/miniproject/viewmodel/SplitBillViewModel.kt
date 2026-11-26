package com.example.miniproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.miniproject.data.GroupMember
import com.example.miniproject.data.ParticipantGroup
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitMember
import com.example.miniproject.data.SplitItem
import com.example.miniproject.repository.ExpenseRepository
import com.example.miniproject.repository.SplitBillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SplitBillUiState(
    val bills: List<SplitBill> = emptyList(),
    val filteredBills: List<SplitBill> = emptyList(),
    val youOwe: Double = 0.0,
    val owedToYou: Double = 0.0,
    val openSplits: Int = 0,
    val settledAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
    val groups: List<ParticipantGroup> = emptyList(),
    val isLoadingGroups: Boolean = false,
    val searchedUser: UserResult? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null
)

data class UserResult(
    val id: String,
    val name: String
)

class SplitBillViewModel(
    private val repo: SplitBillRepository = SplitBillRepository(),
    private val expenseRepo: ExpenseRepository = ExpenseRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplitBillUiState())
    val uiState: StateFlow<SplitBillUiState> = _uiState.asStateFlow()

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchedUser = null,
            searchError = null,
            isSearching = false
        )
    }

    fun searchUserById(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                searchError = null,
                searchedUser = null
            )

            repo.searchUserById(userId)
                .onSuccess { userData ->
                    if (userData != null) {
                        val name = userData["name"]
                            ?: userData["full_name"]
                            ?: "User"
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchedUser = UserResult(id = userId, name = name)
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchError = "User not found"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchError = err.message ?: "Search failed"
                    )
                }
        }
    }

    fun loadBills(userId: String, selectedMonth: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            repo.getSplitBillsForUser(userId)
                .onSuccess { allBills ->
                    val monthStr = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    val filtered = allBills.filter { bill ->
                        bill.billDate.startsWith(monthStr, ignoreCase = true)
                    }

                    val openSplits = filtered.count {
                        it.status.equals("ACTIVE", ignoreCase = true)
                    }
                    val settledAmount = filtered
                        .filter { it.status.equals("SETTLED", ignoreCase = true) }
                        .sumOf { it.totalAmount }

                    var youOwe = 0.0
                    var owedToYou = 0.0

                    filtered.forEach { bill ->
                        repo.getMembers(bill.splitBillId).getOrNull()?.let { members ->
                            repo.getItems(bill.splitBillId).getOrNull()?.let { items ->
                                repo.getAllocations(bill.splitBillId).getOrNull()?.forEach { alloc ->
                                    val item = items.find { it.splitItemId == alloc.splitItemId }
                                    val member = members.find { it.splitMemberId == alloc.splitMemberId }

                                    if (member != null && item != null) {
                                        val isPayer = item.payerId == member.splitMemberId

                                        when {
                                            member.userId == userId && !alloc.isPaid && !isPayer -> {
                                                youOwe += alloc.shareAmount
                                            }
                                            item.payerId?.let { payerId ->
                                                val payer = members.find { it.splitMemberId == payerId }
                                                payer?.userId == userId &&
                                                        member.userId != userId &&
                                                        !alloc.isPaid
                                            } == true -> {
                                                owedToYou += alloc.shareAmount
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        bills = allBills,
                        filteredBills = filtered,
                        youOwe = youOwe,
                        owedToYou = owedToYou,
                        openSplits = openSplits,
                        settledAmount = settledAmount,
                        isLoading = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to load bills",
                        isLoading = false
                    )
                }
        }
    }

    // Loads EVERY bill (all months) into `bills` for the "All Split Bills" screen.
    // The month view's filteredBills and totals are left untouched, so going back to the
    // Split bills screen still shows the month it was on.
    fun loadAllBills(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repo.getSplitBillsForUser(userId)
                .onSuccess { allBills ->
                    _uiState.value = _uiState.value.copy(
                        bills = allBills,
                        isLoading = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to load bills",
                        isLoading = false
                    )
                }
        }
    }

    suspend fun calculateSettlements(billId: String): Result<Map<String, Double>> {
        return try {
            val items = repo.getItems(billId).getOrThrow()
            val members = repo.getMembers(billId).getOrThrow()

            val balances = mutableMapOf<String, Double>()

            items.forEach { item ->
                val payerId = item.payerId ?: return@forEach
                val amount = item.amount
                balances[payerId] = balances.getOrDefault(payerId, 0.0) + amount

                repo.getAllocationsByItem(item.splitItemId).getOrNull()?.forEach { alloc ->
                    balances[alloc.splitMemberId] =
                        balances.getOrDefault(alloc.splitMemberId, 0.0) - alloc.shareAmount
                }
            }

            Result.success(balances.toMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // GROUPS
    // ==========================================
    fun loadGroups(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingGroups = true,
                error = null
            )

            repo.getGroupsForUser(userId)
                .onSuccess { groups ->
                    _uiState.value = _uiState.value.copy(
                        groups = groups,
                        isLoadingGroups = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to load groups",
                        isLoadingGroups = false
                    )
                }
        }
    }

    fun createGroup(
        group: ParticipantGroup,
        members: List<GroupMember>,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repo.createGroupWithMembers(group, members)
                .onSuccess { groupId ->
                    _uiState.value = _uiState.value.copy(
                        groups = _uiState.value.groups + group.copy(groupId = groupId),
                        error = null
                    )
                    onSuccess(groupId)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Failed to create group"
                    _uiState.value = _uiState.value.copy(error = msg)
                    onError(msg)
                }
        }
    }

    suspend fun loadGroupMembers(groupId: String): Result<List<GroupMember>> =
        repo.getGroupMembers(groupId)

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            repo.deleteGroup(groupId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groups = _uiState.value.groups.filter { it.groupId != groupId }
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to delete group"
                    )
                }
        }
    }

    data class BillDetailData(
        val bill: SplitBill,
        val members: List<SplitMember>,
        val items: List<SplitItem>,
        val allocations: List<SplitAllocation>,
        val settlements: Map<String, Double> = emptyMap()
    )

    suspend fun loadBillDetail(billId: String): Result<BillDetailData> {
        return try {
            // Fetch from the database: a fresh ViewModel instance has no cached bills
            val bill = repo.getSplitBill(billId).getOrThrow()

            val members = repo.getMembers(billId).getOrThrow()
            val items = repo.getItems(billId).getOrThrow()
            val allocations = repo.getAllocations(billId).getOrThrow()
            val settlements = calculateSettlements(billId).getOrNull() ?: emptyMap()

            Result.success(BillDetailData(bill, members, items, allocations, settlements))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createSplitBill(
        bill: SplitBill,
        members: List<SplitMember>,
        items: List<SplitItem>,
        allocations: List<SplitAllocation>,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreating = true,
                error = null
            )

            val billData = SplitBillRepository.SplitBillData(
                bill = bill,
                members = members,
                items = items,
                allocations = allocations
            )

            repo.createCompleteSplitBill(billData)
                .onSuccess { billId ->
                    // Record YOUR share (incl. SST/service charge) as an expense
                    val myMemberId = members.firstOrNull { it.isMe }?.splitMemberId
                    val myShare = allocations
                        .filter { it.splitMemberId == myMemberId }
                        .sumOf { it.shareAmount }

                    if (myMemberId != null && myShare > 0) {
                        expenseRepo.addSplitBillExpense(
                            userId = bill.creatorId,
                            billId = billId,
                            title = bill.title,
                            date = bill.billDate,
                            amount = myShare
                        ).onFailure {
                            // The bill itself is saved; don't fail it because of the expense
                            android.util.Log.e("SplitBillVM", "Could not add expense for bill", it)
                        }
                    }

                    _uiState.value = _uiState.value.copy(isCreating = false)
                    onSuccess(billId)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = err.message ?: "Failed to create bill"
                    )
                    onError(err.message ?: "Unknown error")
                }
        }
    }

    fun deleteBill(billId: String, userId: String, selectedMonth: LocalDate) {
        viewModelScope.launch {
            repo.deleteSplitBill(billId)
                .onSuccess {
                    loadBills(userId, selectedMonth)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to delete bill"
                    )
                }
        }
    }

    fun settleAllocation(
        splitAllocationId: String,
        billId: String,
        userId: String,
        selectedMonth: LocalDate
    ) {
        viewModelScope.launch {
            repo.markAsPaid(splitAllocationId, System.currentTimeMillis())
                .onSuccess {
                    loadBills(userId, selectedMonth)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to settle"
                    )
                }
        }
    }

    fun settleBill(billId: String, userId: String, selectedMonth: LocalDate) {
        viewModelScope.launch {
            repo.updateStatus(billId, "SETTLED")
                .onSuccess {
                    loadBills(userId, selectedMonth)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to settle bill"
                    )
                }
        }
    }

    // Marks ALL of one member's unpaid allocations as paid (a member has one per item).
    // When a PARTICIPANT (not the bill's creator) marks their own share as paid, that share is
    // also recorded as an expense in their account, under the "Bills" category.
    fun settleMember(
        billId: String,
        memberId: String,
        currentUserId: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val unpaid = repo.getAllocations(billId).getOrThrow()
                    .filter { it.splitMemberId == memberId && !it.isPaid }
                unpaid.forEach { repo.markAsPaid(it.splitAllocationId, now).getOrThrow() }

                if (currentUserId != null && unpaid.isNotEmpty()) {
                    val bill = repo.getSplitBill(billId).getOrNull()
                    val member = repo.getMembers(billId).getOrNull()
                        ?.find { it.splitMemberId == memberId }
                    val share = unpaid.sumOf { it.shareAmount }

                    // Only for the participant paying their OWN share. The creator's share was
                    // already added as an expense when the bill was created.
                    if (bill != null && member?.userId == currentUserId &&
                        bill.creatorId != currentUserId && share > 0
                    ) {
                        expenseRepo.addSplitBillExpense(
                            userId = currentUserId,
                            billId = billId,
                            title = bill.title,
                            date = LocalDate.now().toString(),
                            amount = share
                        ).onFailure {
                            android.util.Log.e("SplitBillVM", "Could not add expense for paid share", it)
                        }
                    }
                }
                onDone()
            } catch (e: Exception) {
                android.util.Log.e("SplitBillVM", "settleMember failed", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to settle")
            }
        }
    }

    // Marks every unpaid allocation as paid, then the bill itself as SETTLED
    fun settleWholeBill(billId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repo.getAllocations(billId).getOrThrow()
                    .filter { !it.isPaid }
                    .forEach { repo.markAsPaid(it.splitAllocationId, now).getOrThrow() }
                repo.updateStatus(billId, "SETTLED").getOrThrow()
                onDone()
            } catch (e: Exception) {
                android.util.Log.e("SplitBillVM", "settleWholeBill failed", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to settle bill")
            }
        }
    }

    fun filterBillsByStatus(status: String): List<SplitBill> {
        return when (status.uppercase()) {
            "ACTIVE" -> _uiState.value.filteredBills.filter {
                it.status.equals("ACTIVE", ignoreCase = true)
            }
            "SETTLED" -> _uiState.value.filteredBills.filter {
                it.status.equals("SETTLED", ignoreCase = true)
            }
            "CANCELLED" -> _uiState.value.filteredBills.filter {
                it.status.equals("CANCELLED", ignoreCase = true)
            }
            else -> _uiState.value.filteredBills
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

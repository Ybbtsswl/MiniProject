package com.example.miniproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.ParticipantGroup
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitMember
import com.example.miniproject.data.SplitItem
import com.example.miniproject.repository.SplitBillRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
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
    val isLoadingGroups: Boolean = false
)

class SplitBillViewModel(
    private val repo: SplitBillRepository = SplitBillRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplitBillUiState())
    val uiState: StateFlow<SplitBillUiState> = _uiState.asStateFlow()

    // ==========================================
    // 📅 LOAD BILLS + CALCULATE WHO OWES WHAT
    // ==========================================
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

                    // 💰 Calculate youOwe & owedToYou using payerId
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
                                            // Current user owes this amount (not paid yet, and NOT the payer)
                                            member.userId == userId && !alloc.isPaid && !isPayer -> {
                                                youOwe += alloc.shareAmount
                                            }
                                            // Current user is the payer → others owe me
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

    // ==========================================
    // 📈 CALCULATE FULL SETTLEMENT — Who Owes Whom
    // ==========================================
    suspend fun calculateSettlements(billId: String): Result<Map<String, Double>> {
        return try {
            val items = repo.getItems(billId).getOrThrow()
            val members = repo.getMembers(billId).getOrThrow()

            val balances = mutableMapOf<String, Double>()

            items.forEach { item ->
                val payerId = item.payerId ?: return@forEach
                val amount = item.amount

                // Payer paid full amount → +amount
                balances[payerId] = balances.getOrDefault(payerId, 0.0) + amount

                // Get allocations for this item
                repo.getAllocationsByItem(item.splitItemId).getOrNull()?.forEach { alloc ->
                    // Each participant owes their share → -shareAmount
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
    // 📂 LOAD USER GROUPS
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

    // ==========================================
    // ➕ CREATE GROUP
    // ==========================================
    suspend fun createGroup(
        group: ParticipantGroup,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ): Result<String> {
        return repo.createGroup(group)
            .onSuccess { groupId ->
                _uiState.value = _uiState.value.copy(
                    groups = _uiState.value.groups + group.copy(groupId = groupId)
                )
                onSuccess(groupId)
            }
            .onFailure { err ->
                onError(err.message ?: "Failed to create group")
            }
    }

    // ==========================================
    // 🗑️ DELETE GROUP
    // ==========================================
    fun deleteGroup(groupId: String, userId: String) {
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

    // ==========================================
    // 📋 LOAD BILL DETAILS
    // ==========================================
    data class BillDetailData(
        val bill: SplitBill,
        val members: List<SplitMember>,
        val items: List<SplitItem>,
        val allocations: List<SplitAllocation>,
        val settlements: Map<String, Double> = emptyMap()
    )

    suspend fun loadBillDetail(billId: String): Result<BillDetailData> {
        return try {
            val bill = _uiState.value.bills.find { it.splitBillId == billId }
                ?: return Result.failure(Exception("Bill not found"))

            val members = repo.getMembers(billId).getOrThrow()
            val items = repo.getItems(billId).getOrThrow()
            val allocations = repo.getAllocations(billId).getOrThrow()

            // ✅ Calculate settlements
            val settlements = calculateSettlements(billId).getOrNull() ?: emptyMap()

            Result.success(BillDetailData(bill, members, items, allocations, settlements))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ➕ CREATE BILL — now with payerId in items & allocations
    // ==========================================
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

    // ==========================================
    // 🗑️ DELETE BILL
    // ==========================================
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

    // ==========================================
    // ✅ SETTLE ALLOCATION
    // ==========================================
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

    // ==========================================
    // ✅ SETTLE BILL
    // ==========================================
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

    // ==========================================
    // 🏷️ FILTER BY STATUS
    // ==========================================
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

    // ==========================================
    // 🔄 CLEAR ERROR
    // ==========================================
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun createGroup(group: ParticipantGroup): Result<String> {
        return try {
            val id = SupabaseClient.instance.postgrest["participant_group"]
                .insert(group) { select() }
                .decodeSingle<JsonObject>()["id"]?.toString()
                ?.removeSurrounding("\"") ?: ""
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadGroupMembers(groupId: String): Result<List<SplitMember>> {
        return try {
            val members = SupabaseClient.instance.postgrest["split_members"]
                .select { filter { eq("group_id", groupId) } }
                .decodeList<SplitMember>()
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

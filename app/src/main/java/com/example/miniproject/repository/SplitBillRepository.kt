package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient as MySupabaseClient
import com.example.miniproject.data.ParticipantGroup
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.data.SplitAllocation
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplitBillRepository(
    private val supabase: io.github.jan.supabase.SupabaseClient = MySupabaseClient.instance
) {
    // ==========================================
    // SPLIT BILL — CRUD
    // ==========================================
    suspend fun getSplitBillsForUser(userId: String): Result<List<SplitBill>> =
        withContext(Dispatchers.IO) {
            try {
                val bills = supabase.postgrest["split_bills"]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<SplitBill>()
                Result.success(bills)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createSplitBill(bill: SplitBill): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.postgrest["split_bills"]
                    .insert(bill) { select() }
                    .decodeSingle<SplitBill>()
                Result.success(result.splitBillId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateSplitBill(bill: SplitBill): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_bills"]
                    .update(bill) {
                        filter { eq("split_bill_id", bill.splitBillId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteSplitBill(splitBillId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_bills"]
                    .delete {
                        filter { eq("split_bill_id", splitBillId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateStatus(splitBillId: String, status: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_bills"]
                    .update(mapOf("status" to status)) {
                        filter { eq("split_bill_id", splitBillId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================
    // SPLIT MEMBER — CRUD
    // ==========================================
    suspend fun getMembers(splitBillId: String): Result<List<SplitMember>> =
        withContext(Dispatchers.IO) {
            try {
                val list = supabase.postgrest["split_members"]
                    .select {
                        filter { eq("split_bill_id", splitBillId) }
                    }
                    .decodeList<SplitMember>()
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addMember(member: SplitMember): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.postgrest["split_members"]
                    .insert(member) { select() }
                    .decodeSingle<SplitMember>()
                Result.success(result.splitMemberId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun removeMember(splitMemberId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_members"]
                    .delete {
                        filter { eq("split_member_id", splitMemberId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================
    // SPLIT ITEM — CRUD
    // ==========================================
    suspend fun getItems(splitBillId: String): Result<List<SplitItem>> =
        withContext(Dispatchers.IO) {
            try {
                val list = supabase.postgrest["split_items"]
                    .select {
                        filter { eq("split_bill_id", splitBillId) }
                    }
                    .decodeList<SplitItem>()
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addItem(item: SplitItem): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.postgrest["split_items"]
                    .insert(item) { select() }
                    .decodeSingle<SplitItem>()
                Result.success(result.splitItemId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteItem(splitItemId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_items"]
                    .delete {
                        filter { eq("split_item_id", splitItemId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================
    // SPLIT ALLOCATION — CRUD
    // ==========================================
    suspend fun getAllocations(splitBillId: String): Result<List<SplitAllocation>> =
        withContext(Dispatchers.IO) {
            try {
                val items = supabase.postgrest["split_items"]
                    .select { filter { eq("split_bill_id", splitBillId) } }
                    .decodeList<SplitItem>()

                val allAllocations = mutableListOf<SplitAllocation>()
                items.forEach { item ->
                    val allocations = supabase.postgrest["split_allocations"]
                        .select { filter { eq("split_item_id", item.splitItemId) } }
                        .decodeList<SplitAllocation>()
                    allAllocations.addAll(allocations)
                }
                Result.success(allAllocations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ✅ NEW: Get allocations for ONE specific item (used in settlement calculation)
    suspend fun getAllocationsByItem(splitItemId: String): Result<List<SplitAllocation>> =
        withContext(Dispatchers.IO) {
            try {
                val list = supabase.postgrest["split_allocations"]
                    .select { filter { eq("split_item_id", splitItemId) } }
                    .decodeList<SplitAllocation>()
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addAllocation(allocation: SplitAllocation): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.postgrest["split_allocations"]
                    .insert(allocation) { select() }
                    .decodeSingle<SplitAllocation>()
                Result.success(result.splitAllocationId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateAllocation(allocation: SplitAllocation): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_allocations"]
                    .update(allocation) {
                        filter { eq("split_allocation_id", allocation.splitAllocationId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun markAsPaid(splitAllocationId: String, paidAt: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["split_allocations"]
                    .update(mapOf("is_paid" to true, "paid_at" to paidAt)) {
                        filter { eq("split_allocation_id", splitAllocationId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================
    // PARTICIPANT GROUP — CRUD
    // ==========================================
    suspend fun getGroupsForUser(userId: String): Result<List<ParticipantGroup>> =
        withContext(Dispatchers.IO) {
            try {
                val groups = supabase.postgrest["participant_group"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<ParticipantGroup>()
                Result.success(groups)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createGroup(group: ParticipantGroup): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = supabase.postgrest["participant_group"]
                    .insert(group) { select() }
                    .decodeSingle<ParticipantGroup>()
                Result.success(result.groupId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteGroup(groupId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.postgrest["participant_group"]
                    .delete { filter { eq("group_id", groupId) } }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================
    // BATCH CREATE
    // ==========================================
    data class SplitBillData(
        val bill: SplitBill,
        val members: List<SplitMember>,
        val items: List<SplitItem>,
        val allocations: List<SplitAllocation>
    )

    suspend fun createCompleteSplitBill(data: SplitBillData): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val billId = createSplitBill(data.bill).getOrThrow()
                data.members.forEach { addMember(it.copy(splitBillId = billId)).getOrThrow() }
                data.items.forEach { addItem(it.copy(splitBillId = billId)).getOrThrow() }
                data.allocations.forEach { addAllocation(it).getOrThrow() }
                Result.success(billId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

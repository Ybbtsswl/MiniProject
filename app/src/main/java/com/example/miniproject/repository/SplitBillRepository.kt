package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient as MySupabaseClient
import com.example.miniproject.data.GroupMember
import com.example.miniproject.data.ParticipantGroup
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.data.SplitAllocation
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

class SplitBillRepository(
    private val supabase: io.github.jan.supabase.SupabaseClient = MySupabaseClient.instance
) {
    // ==========================================
    // SPLIT BILL — CRUD
    // ==========================================
    suspend fun getSplitBillsForUser(userId: String): Result<List<SplitBill>> =
        withContext(Dispatchers.IO) {
            try {
                // Bills you created ...
                val own = supabase.postgrest["split_bills"]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<SplitBill>()

                // ... plus bills where you are a participant (a split_members row with your user id).
                // If this second lookup fails, still show your own bills.
                val shared = try {
                    val billIds = supabase.postgrest["split_members"]
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<SplitMember>()
                        .map { it.splitBillId }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .filter { id -> own.none { it.splitBillId == id } }

                    if (billIds.isEmpty()) emptyList()
                    else supabase.postgrest["split_bills"]
                        .select { filter { isIn("split_bill_id", billIds) } }
                        .decodeList<SplitBill>()
                } catch (e: Exception) {
                    android.util.Log.e("SplitBillRepo", "loading shared bills failed", e)
                    emptyList()
                }

                Result.success((own + shared).distinctBy { it.splitBillId })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSplitBill(splitBillId: String): Result<SplitBill> =
        withContext(Dispatchers.IO) {
            try {
                val bill = supabase.postgrest["split_bills"]
                    .select { filter { eq("split_bill_id", splitBillId) } }
                    .decodeSingle<SplitBill>()
                Result.success(bill)
            } catch (e: Exception) {
                android.util.Log.e("SplitBillRepo", "getSplitBill failed", e)
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
                    // mapOf(Boolean, Long) becomes Map<String, Any>, which has no serializer
                    .update(buildJsonObject {
                        put("is_paid", true)
                        // timestamp column: send ISO-8601 (e.g. 2026-11-20T03:50:00Z), not epoch millis
                        put("paid_at", java.time.Instant.ofEpochMilli(paidAt).toString())
                    }) {
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

    suspend fun createGroupWithMembers(
        group: ParticipantGroup,
        members: List<GroupMember>
    ): Result<String> =
        withContext(Dispatchers.IO) {
            var groupId: String? = null
            try {
                groupId = createGroup(group).getOrThrow()
                if (members.isNotEmpty()) {
                    supabase.postgrest["participant_group_members"]
                        .insert(members.map { it.copy(groupId = groupId) })
                }
                Result.success(groupId)
            } catch (e: Exception) {
                android.util.Log.e("SplitBillRepo", "createGroupWithMembers failed", e)
                groupId?.let { deleteGroup(it) }
                Result.failure(e)
            }
        }

    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> =
        withContext(Dispatchers.IO) {
            try {
                val list = supabase.postgrest["participant_group_members"]
                    .select { filter { eq("group_id", groupId) } }
                    .decodeList<GroupMember>()
                Result.success(list)
            } catch (e: Exception) {
                android.util.Log.e("SplitBillRepo", "getGroupMembers failed", e)
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
            var billId: String? = null
            try {
                billId = createSplitBill(data.bill).getOrThrow()
                data.members.forEach { addMember(it.copy(splitBillId = billId)).getOrThrow() }
                data.items.forEach { addItem(it.copy(splitBillId = billId)).getOrThrow() }
                data.allocations.forEach { addAllocation(it).getOrThrow() }
                Result.success(billId)
            } catch (e: Exception) {
                android.util.Log.e("SplitBillRepo", "createCompleteSplitBill failed", e)
                // Don't leave a half-created bill behind
                billId?.let { deleteSplitBill(it) }
                Result.failure(e)
            }
        }

    // ==========================================
    // USER SEARCH
    // ==========================================
    // Map<String, Any> can't be decoded by kotlinx.serialization (Any has no serializer),
    // which made every search throw. Decode to JsonObject and flatten to strings instead.
    suspend fun searchUserById(userId: String): Result<Map<String, String>?> =
        withContext(Dispatchers.IO) {
            try {
                val user = supabase.postgrest["profiles"]
                    .select {
                        filter { eq("id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<JsonObject>()

                android.util.Log.d("SplitBillRepo", "searchUserById($userId) -> $user")
                Result.success(
                    user?.entries
                        ?.mapNotNull { (key, value) ->
                            (value as? JsonPrimitive)?.contentOrNull?.let { key to it }
                        }
                        ?.toMap()
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

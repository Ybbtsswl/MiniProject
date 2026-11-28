package com.example.miniproject.repository

import com.example.miniproject.SupabaseClient
import com.example.miniproject.data.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SavingGroupRepository {
    private val supabase = SupabaseClient.instance

    suspend fun load(): Result<SavingGroupData> = withContext(Dispatchers.IO) {
        runCatching {
            val groups =
                supabase.from("saving_groups").select {
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavingGroupRecord>()

            val groupIds = groups.map { it.groupId }
            val members = if (groupIds.isEmpty()) emptyList() else
                supabase.from("saving_group_members").select {
                    filter { isIn("group_id", groupIds) }
                }.decodeList<SavingGroupMemberRecord>()
            val invitations = if (groupIds.isEmpty()) emptyList() else
                supabase.from("saving_group_invitations").select {
                    filter { isIn("group_id", groupIds) }
                }.decodeList<SavingGroupInvitationRecord>()
            val contributions = if (groupIds.isEmpty()) emptyList() else
                supabase.from("saving_group_contributions").select {
                    filter { isIn("group_id", groupIds) }
                }.decodeList<SavingGroupContributionRecord>()
            val users =
                supabase.from("saving_group_user_directory").select()
                    .decodeList<SavingGroupDirectoryUser>()

            SavingGroupData(groups, members, invitations, contributions, users)
        }
    }

    suspend fun createGroup(
        ownerId: String,
        name: String,
        target: Double,
        start: String,
        deadline: String,
    ): Result<SavingGroupRecord> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from("saving_groups").insert(
                buildJsonObject {
                    put("owner_id", ownerId)
                    put("group_name", name)
                    put("target_amount", target)
                    put("start_date", start)
                    put("deadline", deadline)
                }
            ) { select() }.decodeSingle<SavingGroupRecord>()
        }
    }

    suspend fun invite(groupId: String, inviterId: String, inviteeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.from("saving_group_invitations").insert(
                    buildJsonObject {
                        put("group_id", groupId)
                        put("inviter_id", inviterId)
                        put("invitee_id", inviteeId)
                    }
                )
                Unit
            }
        }

    suspend fun respond(invitationId: String, status: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.from("saving_group_invitations").update(
                    buildJsonObject { put("status", status) }
                ) { filter { eq("invitation_id", invitationId) } }
                Unit
            }
        }

    suspend fun contribute(groupId: String, userId: String, amount: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.from("saving_group_contributions").insert(
                    buildJsonObject {
                        put("group_id", groupId)
                        put("user_id", userId)
                        put("amount", amount)
                    }
                )
                Unit
            }
        }
}

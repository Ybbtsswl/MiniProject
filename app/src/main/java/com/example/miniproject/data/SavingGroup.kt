package com.example.miniproject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavingGroupRecord(
    @SerialName("group_id") val groupId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("start_date") val startDate: String,
    val deadline: String,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class SavingGroupMemberRecord(
    @SerialName("membership_id") val membershipId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("target_share") val targetShare: Double = 0.0,
    @SerialName("goal_label") val goalLabel: String = "Shared goal",
)

@Serializable
data class SavingGroupInvitationRecord(
    @SerialName("invitation_id") val invitationId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("invitee_id") val inviteeId: String,
    val status: String,
)

@Serializable
data class SavingGroupContributionRecord(
    @SerialName("contribution_id") val contributionId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    @SerialName("contributed_on") val contributedOn: String,
)

@Serializable
data class SavingGroupDirectoryUser(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
)

data class SavingGroupData(
    val groups: List<SavingGroupRecord>,
    val members: List<SavingGroupMemberRecord>,
    val invitations: List<SavingGroupInvitationRecord>,
    val contributions: List<SavingGroupContributionRecord>,
    val users: List<SavingGroupDirectoryUser>,
)

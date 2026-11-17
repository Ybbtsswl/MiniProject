package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeYellow
import java.text.NumberFormat
import java.util.*

// ==========================================
// 🛠️ HELPER FUNCTIONS
// ==========================================
private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ms_MY"))
        .apply { maximumFractionDigits = 2 }
        .format(amount)
        .replace("MYR", "RM")
}

// ==========================================
// 📱 MAIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillDetailScreen(
    splitBill: SplitBill,
    members: List<SplitMember>,
    items: List<SplitItem>,
    allocations: List<SplitAllocation>,
    currentUserId: String = "",
    onBack: () -> Unit = {},
    onSettleAllocation: (String) -> Unit = {},
    onSettleBill: () -> Unit = {}
) {
    // Find current user's member ID and allocation
    val currentMemberId = members.find { it.userId == currentUserId }?.splitMemberId
    val currentAlloc = allocations.find { it.splitMemberId == currentMemberId }
    val isPayer = members.firstOrNull()?.userId == currentUserId
    val isSettled = currentAlloc?.isPaid == true

    // Items with their assigned members
    val itemsWithMembers = items.map { item ->
        val itemAllocs = allocations.filter { it.splitItemId == item.splitItemId }
        val itemMembers = itemAllocs.mapNotNull { alloc ->
            members.find { it.splitMemberId == alloc.splitMemberId }?.name
        }
        item to itemMembers
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MoneeYellow.copy(alpha = 0.08f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 📌 BILL SUMMARY CARD
            item {
                BillSummaryCard(
                    bill = splitBill,
                    memberCount = members.size,
                    payerName = members.firstOrNull()?.name ?: ""
                )
            }

            // 📋 ITEMIZED BILL
            item {
                ItemizedBillSection(
                    itemsWithMembers = itemsWithMembers,
                    totalAmount = splitBill.totalAmount
                )
            }

            // 👥 CONTRIBUTIONS
            item {
                ContributionsSection(
                    members = members,
                    allocations = allocations,
                    currentMemberId = currentMemberId
                )
            }

            // 💰 YOUR SETTLEMENT
            item {
                YourSettlementSection(
                    currentAlloc = currentAlloc,
                    payerName = members.firstOrNull()?.name ?: "",
                    isPayer = isPayer,
                    isSettled = isSettled,
                    onSettleAllocation = {
                        if (currentAlloc != null) {
                            onSettleAllocation(currentAlloc.splitAllocationId)
                        }
                    },
                    onSettleBill = onSettleBill
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ==========================================
// 🧩 BILL SUMMARY CARD
// ==========================================
@Composable
fun BillSummaryCard(bill: SplitBill, memberCount: Int, payerName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MoneeYellow.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = bill.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${bill.billDate} · $memberCount people",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "TOTAL AMOUNT", fontSize = 12.sp, color = Color.Gray)
            Text(
                text = formatCurrency(bill.totalAmount),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Paid by $payerName · Split by item",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// ==========================================
// 🧩 ITEMIZED BILL SECTION
// ==========================================
@Composable
fun ItemizedBillSection(
    itemsWithMembers: List<Pair<SplitItem, List<String>>>,
    totalAmount: Double
) {
    Column {
        Text("Itemized Bill", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        itemsWithMembers.forEach { (item, assignees) ->
            ItemRow(
                name = item.description,
                assignedTo = if (assignees.size <= 2) assignees.joinToString(", ")
                else "${assignees.take(2).joinToString(", ")} +${assignees.size - 2}",
                amount = item.amount
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(formatCurrency(totalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ItemRow(name: String, assignedTo: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = assignedTo, fontSize = 11.sp, color = Color.Gray)
        }
        Text(
            text = formatCurrency(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 🧩 CONTRIBUTIONS SECTION
// ==========================================
@Composable
fun ContributionsSection(
    members: List<SplitMember>,
    allocations: List<SplitAllocation>,
    currentMemberId: String?
) {
    Column {
        Text("Contributions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Person", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
            Text("Share", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
            Text("Paid", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))

        members.forEach { member ->
            val share = allocations
                .filter { it.splitMemberId == member.splitMemberId }
                .sumOf { it.shareAmount }
            val isPaid = allocations
                .filter { it.splitMemberId == member.splitMemberId }
                .all { it.isPaid }
            val isPayer = member == members.firstOrNull()
            val displayName = if (member.splitMemberId == currentMemberId) "${member.name} (you)" else member.name

            ContributionRow(
                name = displayName,
                share = share,
                isPaid = if (isPayer) null else isPaid,
                isPayer = isPayer
            )
        }
    }
}

@Composable
fun ContributionRow(name: String, share: Double, isPaid: Boolean?, isPayer: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            text = formatCurrency(share),
            fontSize = 13.sp,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.Center
        )
        when {
            isPayer -> StatusBadge(text = "Payer", modifier = Modifier.width(70.dp))
            isPaid == true -> StatusBadge(text = "✓ Paid", isSettled = true, modifier = Modifier.width(70.dp))
            else -> StatusBadge(text = "Pending", isPending = true, modifier = Modifier.width(70.dp))
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    isSettled: Boolean = false,
    isPending: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSettled -> MoneeGreen.copy(alpha = 0.12f)
        isPending -> Color(0xFFF0F0F0)
        else -> MoneeYellow.copy(alpha = 0.18f)
    }
    val textColor = when {
        isSettled -> MoneeGreen
        isPending -> Color.Gray
        else -> Color(0xFFB8860B)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 🧩 YOUR SETTLEMENT SECTION
// ==========================================
@Composable
fun YourSettlementSection(
    currentAlloc: SplitAllocation?,
    payerName: String,
    isPayer: Boolean,
    isSettled: Boolean,
    onSettleAllocation: () -> Unit,
    onSettleBill: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Your Settlement", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            if (isPayer) {
                Text(
                    text = "You are the payer for this bill.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSettleBill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoneeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark Bill as Settled", fontWeight = FontWeight.SemiBold)
                }
            } else {
                val amount = currentAlloc?.shareAmount ?: 0.0
                TextRow(label = "Your share", value = formatCurrency(amount))
                TextRow(
                    label = "You owe $payerName",
                    value = formatCurrency(amount),
                    isBold = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSettleAllocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSettled) Color(0xFFE0E0E0) else MoneeYellow,
                        contentColor = if (isSettled) Color.Gray else Color(0xFF424242)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSettled
                ) {
                    Text(
                        text = if (isSettled) "✓ You've Paid" else "Pay ${formatCurrency(amount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun TextRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}


package com.example.miniproject.screen

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeGreenBrush
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeCardColors
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.ui.theme.moneeTopInsets
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

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = moneeCardColors(),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
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
    errorMessage: String? = null,
    onBack: () -> Unit = {},
    onSettleMember: (String) -> Unit = {},   // memberId -> marks all of their shares paid
    onSettleBill: () -> Unit = {}
) {
    val context = LocalContext.current
    val markPaid: (String) -> Unit = { id ->
        onSettleMember(id)
        Toast.makeText(context, "Marked as paid", Toast.LENGTH_SHORT).show()
    }
    val settleBill: () -> Unit = {
        onSettleBill()
        Toast.makeText(context, "Bill marked as settled", Toast.LENGTH_SHORT).show()
    }

    // The creator is the payer: they are saved as payerId on every item
    val payerMemberId = items.firstNotNullOfOrNull { it.payerId }
    val payer = members.find { it.splitMemberId == payerMemberId } ?: members.firstOrNull()
    val payerName = payer?.name ?: ""

    val currentMember = if (currentUserId.isNotEmpty()) members.find { it.userId == currentUserId } else null
    val currentMemberId = currentMember?.splitMemberId
    val isPayer = currentMemberId != null && currentMemberId == payer?.splitMemberId

    // A member has one allocation per item, so add them all up
    val myAllocs = allocations.filter { it.splitMemberId == currentMemberId }
    val myShare = myAllocs.sumOf { it.shareAmount }
    val isSettled = myAllocs.isNotEmpty() && myAllocs.all { it.isPaid }

    val isBillSettled = splitBill.status.equals("SETTLED", ignoreCase = true)

    val others = members.filter { it.splitMemberId != payer?.splitMemberId }
    val pendingOthers = others.count { m ->
        allocations.filter { it.splitMemberId == m.splitMemberId }.any { !it.isPaid }
    }

    val itemsWithMembers = items.map { item ->
        val itemAllocs = allocations.filter { it.splitItemId == item.splitItemId }
        val itemMembers = itemAllocs.mapNotNull { alloc ->
            members.find { it.splitMemberId == alloc.splitMemberId }?.name
        }
        item to itemMembers
    }

    Box(modifier = Modifier.fillMaxSize().moneeBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Bill Details", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    windowInsets = moneeTopInsets(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MoneeInk,
                        navigationIconContentColor = MoneeInk
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                errorMessage?.let { msg ->
                    item {
                        Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }

                item {
                    BillSummaryCard(
                        bill = splitBill,
                        memberCount = members.size,
                        payerName = payerName,
                        isSettled = isBillSettled
                    )
                }

                item {
                    ItemizedBillSection(
                        itemsWithMembers = itemsWithMembers,
                        totalAmount = splitBill.totalAmount
                    )
                }

                item {
                    ContributionsSection(
                        members = members,
                        allocations = allocations,
                        currentMemberId = currentMemberId,
                        payerMemberId = payer?.splitMemberId,
                        canMarkOthersPaid = isPayer && !isBillSettled,
                        onMarkPaid = markPaid
                    )
                }

                item {
                    YourSettlementSection(
                        isBillSettled = isBillSettled,
                        isMember = currentMemberId != null,
                        isPayer = isPayer,
                        payerName = payerName,
                        myShare = myShare,
                        isSettled = isSettled,
                        pendingOthers = pendingOthers,
                        onPay = { currentMemberId?.let(markPaid) },
                        onSettleBill = settleBill
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ==========================================
// 🧩 BILL SUMMARY CARD
// ==========================================
@Composable
fun BillSummaryCard(bill: SplitBill, memberCount: Int, payerName: String, isSettled: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .moneeHero(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneeInk,
                    modifier = Modifier.weight(1f)
                )
                if (isSettled) StatusBadge(text = "✓ Settled", isSettled = true)
            }
            Text(
                text = "${bill.billDate} · $memberCount people",
                fontSize = 13.sp,
                color = MoneeMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "TOTAL AMOUNT", fontSize = 12.sp, color = MoneeMuted)
            Text(
                text = formatCurrency(bill.totalAmount),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MoneeInk,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Paid by $payerName · Split by item",
                fontSize = 12.sp,
                color = MoneeMuted
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
    val subtotal = itemsWithMembers.sumOf { (item, _) -> item.amount * item.quantity }
    val taxAndCharges = (totalAmount - subtotal).coerceAtLeast(0.0)

    SectionCard {
        Text("Itemized Bill", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
        Spacer(modifier = Modifier.height(12.dp))

        itemsWithMembers.forEach { (item, assignees) ->
            ItemRow(
                name = if (item.quantity > 1) "${item.description} ×${item.quantity}" else item.description,
                assignedTo = if (assignees.size <= 2) assignees.joinToString(", ")
                else "${assignees.take(2).joinToString(", ")} +${assignees.size - 2}",
                amount = item.amount * item.quantity
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MoneeBorder)

        TextRow(label = "Subtotal", value = formatCurrency(subtotal))
        if (taxAndCharges > 0.005) {
            TextRow(label = "SST & service charge", value = formatCurrency(taxAndCharges))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MoneeInk)
            Text(formatCurrency(totalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MoneeInk)
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
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MoneeInk)
            Text(text = assignedTo, fontSize = 11.sp, color = MoneeMuted)
        }
        Text(
            text = formatCurrency(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MoneeInk
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
    currentMemberId: String?,
    payerMemberId: String?,
    canMarkOthersPaid: Boolean,
    onMarkPaid: (String) -> Unit
) {
    SectionCard {
        Text("Contributions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Person", fontSize = 12.sp, color = MoneeMuted, modifier = Modifier.weight(1f))
            Text("Share", fontSize = 12.sp, color = MoneeMuted, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
            Text("Status", fontSize = 12.sp, color = MoneeMuted, modifier = Modifier.width(90.dp), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))

        members.forEach { member ->
            val memberAllocs = allocations.filter { it.splitMemberId == member.splitMemberId }
            val isPayer = member.splitMemberId == payerMemberId
            val displayName =
                if (member.splitMemberId == currentMemberId) "${member.name} (you)" else member.name

            ContributionRow(
                name = displayName,
                share = memberAllocs.sumOf { it.shareAmount },
                isPayer = isPayer,
                isPaid = memberAllocs.isNotEmpty() && memberAllocs.all { it.isPaid },
                showMarkPaid = canMarkOthersPaid && !isPayer,
                onMarkPaid = { onMarkPaid(member.splitMemberId) }
            )
        }
    }
}

@Composable
fun ContributionRow(
    name: String,
    share: Double,
    isPayer: Boolean,
    isPaid: Boolean,
    showMarkPaid: Boolean,
    onMarkPaid: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 14.sp, color = MoneeInk, modifier = Modifier.weight(1f))
        Text(
            text = formatCurrency(share),
            fontSize = 13.sp,
            color = MoneeInk,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
            when {
                isPayer -> StatusBadge(text = "Payer")
                isPaid -> StatusBadge(text = "✓ Paid", isSettled = true)
                showMarkPaid -> TextButton(
                    onClick = onMarkPaid,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Mark paid", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MoneeAmber)
                }
                else -> StatusBadge(text = "Pending", isPending = true)
            }
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
        isSettled -> MoneeGreen.copy(alpha = 0.14f)
        isPending -> MoneeBorder.copy(alpha = 0.5f)
        else -> MoneeYellow.copy(alpha = 0.3f)
    }
    val textColor = when {
        isSettled -> MoneeGreen
        isPending -> MoneeMuted
        else -> MoneeAmber
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
// 🧩 YOUR SETTLEMENT SECTION (settle buttons live here)
// ==========================================
@Composable
fun YourSettlementSection(
    isBillSettled: Boolean,
    isMember: Boolean,
    isPayer: Boolean,
    payerName: String,
    myShare: Double,
    isSettled: Boolean,
    pendingOthers: Int,
    onPay: () -> Unit,
    onSettleBill: () -> Unit
) {
    SectionCard {
        Text("Settlement", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MoneeInk)
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isBillSettled -> {
                Text(
                    text = "✓ This bill has been settled.",
                    fontSize = 14.sp,
                    color = MoneeGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            !isMember -> {
                Text(
                    text = "You are not part of this bill.",
                    fontSize = 14.sp,
                    color = MoneeMuted
                )
            }

            isPayer -> {
                Text(
                    text = if (pendingOthers == 0) "Everyone has paid you back."
                    else "$pendingOthers ${if (pendingOthers == 1) "person has" else "people have"} not paid you yet. " +
                            "Use \"Mark paid\" above as each person pays you back.",
                    fontSize = 14.sp,
                    color = MoneeMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                MoneeGradientButton(
                    onClick = onSettleBill,
                    brush = MoneeGreenBrush,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Mark Bill as Settled", fontWeight = FontWeight.SemiBold)
                }
            }

            else -> {
                TextRow(label = "Your share", value = formatCurrency(myShare))
                TextRow(
                    label = "You owe $payerName",
                    value = if (isSettled) formatCurrency(0.0) else formatCurrency(myShare),
                    isBold = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                MoneeGradientButton(
                    onClick = onPay,
                    enabled = !isSettled,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (isSettled) "✓ You've Paid" else "Pay ${formatCurrency(myShare)}",
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
        Text(text = label, fontSize = 14.sp, color = MoneeMuted)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = MoneeInk
        )
    }
}

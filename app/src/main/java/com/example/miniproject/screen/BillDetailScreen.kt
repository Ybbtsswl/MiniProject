package com.example.miniproject.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeRed
import java.text.NumberFormat
import java.util.*

private val CardBg = Color(0xFFFFF9E6)
private val DividerColor = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFF777777)
private val PendingColor = Color(0xFFF5A623)

private fun formatRM(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

data class BillItem(
    val name: String,
    val amount: Double,
    val assignedTo: String
)

data class Contribution(
    val person: String,
    val share: Double,
    val repaid: Double,
    val status: ContributionStatus
)

enum class ContributionStatus {
    PAYER, SETTLED, PENDING
}

@Composable
fun BillDetailsScreen(
    onBack: () -> Unit,
    onSettle: () -> Unit
) {
    val billTitle = "Dinner with friends"
    val billDate = "13 October 2026 · 4 people"
    val totalBill = 180.00
    val paidBy = "Maya · Split by item"

    val items = listOf(
        BillItem("Grilled chicken", 36.00, "Alex (you)"),
        BillItem("Creamy pasta", 36.00, "Maya"),
        BillItem("Teriyaki rice bowl", 36.00, "Ben"),
        BillItem("Grilled salmon", 36.00, "Sara"),
        BillItem("Shared drinks", 24.00, "All 4 · RM 6.00 each"),
        BillItem("Service charge", 12.00, "All 4 · RM 3.00 each")
    )

    val contributions = listOf(
        Contribution("Maya", 45.00, 0.00, ContributionStatus.PAYER),
        Contribution("Alex (you)", 45.00, 0.00, ContributionStatus.PENDING),
        Contribution("Ben", 45.00, 45.00, ContributionStatus.SETTLED),
        Contribution("Sara", 45.00, 0.00, ContributionStatus.PENDING)
    )

    val youOwe = 45.00
    val youPersonalItem = 36.00
    val yourShareService = 9.00

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Top Bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Bill details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            item {
                // Bill Header Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(billTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(billDate, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("TOTAL BILL", fontSize = 12.sp, color = TextSecondary)
                        Text(formatRM(totalBill), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Paid by $paidBy", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item {
                // Itemized Bill
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Itemized bill",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${items.size} items",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 13.sp)
                                Text(
                                    "Assigned to: ${item.assignedTo}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            Text(
                                formatRM(item.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total bill", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatRM(totalBill), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                // Contributions
                Column {
                    Text(
                        text = "Contributions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Maya paid ${formatRM(totalBill)} upfront",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    // Table Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Person", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("Share (RM)", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Repaid", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Status", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    contributions.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.person, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            Text(String.format(Locale.US, "%.2f", c.share), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(String.format(Locale.US, "%.2f", c.repaid), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            val statusText = when (c.status) {
                                ContributionStatus.PAYER -> "Payer"
                                ContributionStatus.SETTLED -> "Settled"
                                ContributionStatus.PENDING -> "Pending"
                            }
                            val statusColor = when (c.status) {
                                ContributionStatus.PAYER -> TextSecondary
                                ContributionStatus.SETTLED -> MoneeGreen
                                ContributionStatus.PENDING -> PendingColor
                            }
                            Text(
                                statusText,
                                fontSize = 11.sp,
                                color = statusColor,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("RM 90.00 still owed to Maya", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                // Your Settlement
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your settlement",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pay Maya",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your personal item", fontSize = 13.sp, color = TextSecondary)
                                Text(formatRM(youPersonalItem), fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shared drinks + service", fontSize = 13.sp, color = TextSecondary)
                                Text(formatRM(yourShareService), fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = DividerColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("You owe", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatRM(youOwe), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MoneeRed)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Settle Button
                Button(
                    onClick = onSettle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "✓ Settle ${formatRM(youOwe)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Bill Details")
@Composable
fun BillDetailsScreenPreview() {
    MiniProjectTheme {
        BillDetailsScreen(
            onBack = {},
            onSettle = {}
        )
    }
}

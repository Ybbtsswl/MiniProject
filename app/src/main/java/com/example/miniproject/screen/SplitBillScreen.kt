package com.example.miniproject.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val Yellow = Color(0xFFFFD447)
private val MutedText = Color(0xFF686D79)
private val CardBg = Color(0xFFFFF9E6)
private val DividerColor = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFF777777)

data class SplitBillItem(
    val title: String,
    val date: String,
    val people: String,
    val amount: Double,
    val type: SplitType,
    val month: String = "October 2026"
)

enum class SplitType {
    YOU_OWE, OWED_TO_YOU, SETTLED
}

private fun formatRM(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

@Composable
fun SplitBillScreen(
    onBillClick: () -> Unit,
    onCreateSplitBill: () -> Unit,
    onBack: () -> Unit = {},
    showTitle: Boolean = true
) {
    val splits = remember {
        listOf(
            SplitBillItem("Dinner with friends", "13 Oct · Maya paid · 4 people", "", 45.00, SplitType.OWED_TO_YOU),
            SplitBillItem("Weekly groceries", "12 Oct · You paid · 3 people", "", 56.00, SplitType.YOU_OWE),
            SplitBillItem("Coffee catch-up", "11 Oct · Ben paid · 2 people", "", 32.00, SplitType.YOU_OWE),
            SplitBillItem("Movie night", "10 Oct · You paid · 3 people", "", 40.00, SplitType.OWED_TO_YOU),
            SplitBillItem("Lunch at Kopitiam", "09 Oct · Maya paid · 2 people", "", 28.50, SplitType.SETTLED)
        )
    }

    var selectedTab by remember { mutableStateOf("All") }
    val filteredSplits = when (selectedTab) {
        "You owe" -> splits.filter { it.type == SplitType.YOU_OWE }
        "Owed to you" -> splits.filter { it.type == SplitType.OWED_TO_YOU }
        "Settled" -> splits.filter { it.type == SplitType.SETTLED }
        else -> splits
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // ✅ Only show title when inside dashboard tab
        if (showTitle) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Split Bills",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp)) // Just small space for full screen
        }

        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 20.sp, color = MutedText)
            Text("October 2026", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("›", fontSize = 20.sp, color = MutedText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("YOU OWE", fontSize = 11.sp, color = MutedText)
                        Text(
                            formatRM(77.00),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeRed
                        )
                        Text("2 unpaid splits", fontSize = 11.sp, color = MutedText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("OWED TO YOU", fontSize = 11.sp, color = MutedText)
                        Text(
                            formatRM(96.00),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneeGreen
                        )
                        Text("2 awaiting payment", fontSize = 11.sp, color = MutedText)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Net balance", fontSize = 13.sp, color = MutedText)
                    Text("+ RM 19.00 to receive", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Split Bill History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val tabs = listOf("All", "You owe", "Owed to you", "Settled")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = Color(0xFF6B5500)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredSplits) { item ->
                SplitBillRow(item = item, onClick = onBillClick)
                HorizontalDivider(color = DividerColor)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = onCreateSplitBill,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Color(0xFF6B5500)
            )
        ) {
            Text("+ Create Split Bill", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SplitBillRow(
    item: SplitBillItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(item.date, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatRM(item.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            val statusText = when (item.type) {
                SplitType.YOU_OWE -> "You owe"
                SplitType.OWED_TO_YOU -> "Owed to you"
                SplitType.SETTLED -> "Settled"
            }
            val statusColor = when (item.type) {
                SplitType.YOU_OWE -> MoneeRed
                SplitType.OWED_TO_YOU -> MoneeGreen
                SplitType.SETTLED -> TextSecondary
            }
            Text(
                statusText,
                fontSize = 10.sp,
                color = statusColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Split Bill Screen")
@Composable
fun SplitBillScreenPreview() {
    MiniProjectTheme {
        SplitBillScreen(
            onBillClick = {},
            onCreateSplitBill = {},
            onBack = {}
        )
    }
}

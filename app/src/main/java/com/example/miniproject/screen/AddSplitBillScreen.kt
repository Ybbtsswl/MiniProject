package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.ui.theme.MiniProjectTheme
import java.text.NumberFormat
import java.util.*

private val CardBg = Color(0xFFFFF9E6)
private val DividerColor = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFF777777)
private val PrimaryYellow = Color(0xFFFFD447)

data class SplitParticipant(
    val id: String,
    val name: String,
    var contribution: Double = 0.0,
    var percentage: Double = 0.0
)

data class SplitExpenseItem(
    val id: String,
    val name: String,
    var amount: Double,
    var assignedTo: List<String> = emptyList()
)

data class SharedCharge(
    val name: String,
    val percentage: Double,
    val amount: Double
)

private fun formatRM(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "RM ${fmt.format(amount)}"
}

@Composable
fun AddSplitBillScreen(
    onBack: () -> Unit,
    onCreateSplit: () -> Unit
) {
    var groupName by remember { mutableStateOf("Dinner crew") }
    var participants by remember {
        mutableStateOf(
            listOf(
                SplitParticipant("1", "You"),
                SplitParticipant("2", "Aina"),
                SplitParticipant("3", "Ben")
            )
        )
    }
    var expenseItems by remember {
        mutableStateOf(
            mutableListOf(
                SplitExpenseItem("1", "Pasta", 30.00, listOf("1")),
                SplitExpenseItem("2", "Pizza", 60.00, listOf("1", "2")),
                SplitExpenseItem("3", "Iced tea", 12.00, listOf("3"))
            )
        )
    }
    var sharedCharges by remember {
        mutableStateOf(
            listOf(
                SharedCharge("SST", 6.0, 6.12),
                SharedCharge("Service charge", 10.0, 10.20)
            )
        )
    }

    val subtotal = expenseItems.sumOf { it.amount }
    val totalBill = subtotal + sharedCharges.sumOf { it.amount }

    fun calculateShares() {
        val totalContribution = participants.size
        participants = participants.mapIndexed { index, p ->
            val pct = 1.0 / totalContribution
            p.copy(
                percentage = pct * 100,
                contribution = totalBill * pct
            )
        }
    }

    LaunchedEffect(totalBill) {
        calculateShares()
    }

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
                        text = "Split Bill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "Manage",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { }
                    )
                }
            }

            item {
                // Participants Group
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Participants",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("👥 $groupName · ${participants.size} people", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        participants.forEach { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("✓ ${p.name}", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                // Receipt Section
                Column {
                    Text(
                        text = "Receipt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Optional",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 14.dp)
                            ) {
                                Text("📁", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload receipt", fontSize = 13.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 14.dp)
                            ) {
                                Text("📷", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Take photo", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                // Expense Items Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Expense items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${expenseItems.size} items",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "Assign each item to one or more people",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    expenseItems.forEachIndexed { index, item ->
                        ExpenseItemRow(
                            item = item,
                            participants = participants,
                            onAssignedChange = { newAssigned ->
                                expenseItems = expenseItems.toMutableList().apply {
                                    this[index] = item.copy(assignedTo = newAssigned)
                                }
                            },
                            onDelete = {
                                expenseItems = expenseItems.toMutableList().apply { removeAt(index) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            expenseItems = expenseItems.toMutableList().apply {
                                add(
                                    SplitExpenseItem(
                                        id = UUID.randomUUID().toString(),
                                        name = "",
                                        amount = 0.00
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBg,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add item", fontSize = 14.sp)
                    }
                }
            }

            item {
                // Shared Charges
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Shared charges",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Add charge",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    sharedCharges.forEach { charge ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(charge.name, fontSize = 13.sp)
                            Text("${charge.percentage}%", fontSize = 13.sp, color = TextSecondary)
                            Text(formatRM(charge.amount), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Based on subtotal · Shared by item contribution",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            item {
                // Totals
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Items subtotal", fontSize = 13.sp, color = TextSecondary)
                            Text(formatRM(subtotal), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total bill",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatRM(totalBill),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        participants.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(p.name, fontSize = 13.sp)
                                Text(
                                    String.format(Locale.US, "RM %.2f", p.contribution),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Create Split Button
                Button(
                    onClick = onCreateSplit,
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
                        text = "✓ Create Split",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ExpenseItemRow(
    item: SplitExpenseItem,
    participants: List<SplitParticipant>,
    onAssignedChange: (List<String>) -> Unit,
    onDelete: () -> Unit
) {
    var showAssignMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.name.isNotEmpty()) item.name else "New item",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRM(item.amount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Assigned to: ${
                    if (item.assignedTo.isEmpty()) "Tap to assign"
                    else item.assignedTo.joinToString(", ") { id -> participants.find { it.id == id }?.name ?: "Unknown" }
                }",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.clickable { showAssignMenu = true }
            )

            if (item.assignedTo.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contribution",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Row {
                        TextButton(onClick = { }) {
                            Text("Equal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { }) {
                            Text("Custom", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val splitAmount = item.amount / item.assignedTo.size
                    item.assignedTo.forEach { id ->
                        val name = participants.find { it.id == id }?.name ?: ""
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, fontSize = 11.sp)
                            Text(
                                String.format(Locale.US, "RM %.2f", splitAmount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAssignMenu) {
        AlertDialog(
            onDismissRequest = { showAssignMenu = false },
            title = { Text("Assign to") },
            text = {
                Column {
                    participants.forEach { p ->
                        val selected = item.assignedTo.contains(p.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newList = if (selected) {
                                        item.assignedTo - p.id
                                    } else {
                                        item.assignedTo + p.id
                                    }
                                    onAssignedChange(newList)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(p.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignMenu = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Add Split Bill")
@Composable
fun AddSplitBillScreenPreview() {
    MiniProjectTheme {
        AddSplitBillScreen(
            onBack = {},
            onCreateSplit = {}
        )
    }
}


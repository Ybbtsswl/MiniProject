package com.example.miniproject.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miniproject.data.ParticipantGroup
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeYellow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 🛠️ HELPER FUNCTIONS
// ==========================================
private var billCounter = 0
private var itemCounter = 0
private var memberCounter = 0
private var allocationCounter = 0
private var groupCounter = 0

@SuppressLint("DefaultLocale")
fun generateBillId(): String = "BILL${String.format("%03d", ++billCounter)}"
@SuppressLint("DefaultLocale")
fun generateItemId(): String = "ITEM${String.format("%03d", ++itemCounter)}"
@SuppressLint("DefaultLocale")
fun generateMemberId(): String = "MEMBER${String.format("%03d", ++memberCounter)}"
@SuppressLint("DefaultLocale")
fun generateAllocationId(): String = "ALLOC${String.format("%03d", ++allocationCounter)}"
@SuppressLint("DefaultLocale")
fun generateGroupId(): String = "GRP${String.format("%03d", ++groupCounter)}"

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ms_MY"))
        .apply { maximumFractionDigits = 2 }
        .format(amount)
        .replace("MYR", "RM")
}

private fun getCurrentDate(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
}

// ==========================================
// 📦 DATA — Item Participant with Amount
// ==========================================
data class ItemParticipant(
    val memberId: String,
    val name: String,
    val amount: Double = 0.0,
    val isIncluded: Boolean = true
)

data class BillItemWithParticipants(
    val item: SplitItem,
    val participants: List<ItemParticipant> = emptyList()
)

// ==========================================
// 📱 MAIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSplitBillScreen(
    userId: String,
    viewModel: com.example.miniproject.viewmodel.SplitBillViewModel,
    onBack: () -> Unit,
    onCreateSplit: (SplitBill, List<SplitMember>, List<SplitItem>, List<SplitAllocation>) -> Unit,
    onSaveGroup: suspend (ParticipantGroup) -> Result<String> = { Result.failure(Exception("Not implemented")) },
    onLoadGroup: suspend (String) -> Result<List<SplitMember>> = { Result.success(emptyList()) },
    onLookupUser: suspend (String) -> Result<String>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // 📝 State
    var billTitle by remember { mutableStateOf("") }
    var billDate by remember { mutableStateOf(getCurrentDate()) }
    var splitMembers by remember { mutableStateOf(listOf<SplitMember>()) }
    var billItems by remember { mutableStateOf(listOf<BillItemWithParticipants>()) }
    var splitMode by remember { mutableStateOf("equal") }  // equal, custom
    var showSummary by remember { mutableStateOf(false) }

    // Group Dialogs
    var showGroupDialog by remember { mutableStateOf(false) }
    var showSaveGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }

    // Validation & Errors
    var titleError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-calculations
    val totalAmount = remember(billItems) {
        billItems.sumOf { it.item.amount }
    }

    // Calculate each participant's TOTAL share across ALL items
    val participantTotals = remember(billItems, splitMode) {
        buildMap<String, Double> {
            splitMembers.forEach { member ->
                var totalShare = 0.0
                billItems.forEach { item ->
                    val part = item.participants.find { it.memberId == member.splitMemberId }
                    if (part != null && part.isIncluded) {
                        totalShare += when (splitMode) {
                            "equal" -> {
                                val includedCount = item.participants.count { it.isIncluded }
                                if (includedCount > 0) item.item.amount / includedCount else 0.0
                            }
                            else -> part.amount
                        }
                    }
                }
                put(member.splitMemberId, totalShare)
            }
        }
    }

    // Initialize with current user
    LaunchedEffect(Unit) {
        val alreadyHasMe = splitMembers.any { it.userId == userId }
        if (!alreadyHasMe) {
            splitMembers = listOf(
                SplitMember(
                    splitMemberId = generateMemberId(),
                    splitBillId = "",
                    userId = userId,
                    name = "You",
                    joinedAt = System.currentTimeMillis().toString()
                )
            )
        }
    }

    // Update item participants when members or split mode changes
    LaunchedEffect(splitMembers, splitMode) {
        billItems = billItems.map { itemWithParts ->
            val updatedParticipants = splitMembers.map { member ->
                val existing = itemWithParts.participants.find { it.memberId == member.splitMemberId }
                val amount = if (splitMode == "equal") {
                    val includedCount = itemWithParts.participants.count { it.isIncluded }
                    if (includedCount > 0) itemWithParts.item.amount / includedCount else 0.0
                } else {
                    existing?.amount ?: 0.0
                }
                ItemParticipant(
                    memberId = member.splitMemberId,
                    name = member.name,
                    amount = amount,
                    isIncluded = existing?.isIncluded ?: true
                )
            }
            itemWithParts.copy(participants = updatedParticipants)
        }
    }

    // Validation function
    fun validate(): Boolean {
        titleError = null
        errorMessage = null

        if (billTitle.isBlank()) {
            titleError = "Please enter a bill name"
            return false
        }
        if (billItems.isEmpty()) {
            errorMessage = "Please add at least one expense item"
            return false
        }
        if (splitMembers.size < 2) {
            errorMessage = "Add at least 2 participants to split a bill"
            return false
        }
        billItems.forEachIndexed { index, item ->
            if (item.item.amount <= 0.0) {
                errorMessage = "Item ${index + 1} amount must be greater than 0"
                return false
            }
            val includedParts = item.participants.filter { it.isIncluded }
            if (includedParts.isEmpty()) {
                errorMessage = "Item ${index + 1} must have at least one participant"
                return false
            }
            if (splitMode == "custom") {
                val totalAlloc = includedParts.sumOf { it.amount }
                if (Math.abs(totalAlloc - item.item.amount) > 0.01) {
                    errorMessage = "Item ${index + 1}: total must equal ${formatCurrency(item.item.amount)}"
                    return false
                }
            }
        }
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Split Bill", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ===== ❌ ERROR MESSAGE =====
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = Color(0xFFCC0000),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ===== 📝 BILL DETAILS =====
            OutlinedTextField(
                value = billTitle,
                onValueChange = { billTitle = it; titleError = null },
                label = { Text("Bill Name *") },
                placeholder = { Text("e.g. Dinner with friends") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError != null,
                supportingText = { titleError?.let { Text(it) } },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = billDate,
                onValueChange = {},
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 👥 PARTICIPANT GROUPS =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4E8))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Participant Groups",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showGroupDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MoneeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load Group", fontSize = 13.sp)
                        }
                        Button(
                            onClick = { showSaveGroupDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MoneeYellow,
                                contentColor = Color(0xFF2D2D2D)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Group", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== ⚖️ SPLIT MODE =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F6ED))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Split Method",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = splitMode == "equal",
                            onClick = { splitMode = "equal" },
                            label = { Text("Equally") },
                            leadingIcon = { if (splitMode == "equal") Text("✓") }
                        )
                        FilterChip(
                            selected = splitMode == "custom",
                            onClick = { splitMode = "custom" },
                            label = { Text("By Contribution") },
                            leadingIcon = { if (splitMode == "custom") Text("✓") }
                        )
                    }
                    if (splitMode == "custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set amounts per participant for each item",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 👥 PARTICIPANTS SECTION =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F6ED))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Participants (${splitMembers.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showMemberDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage", color = MoneeGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    splitMembers.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8E4D9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (member.userId != null) "✓ App User" else "Guest",
                                    fontSize = 11.sp,
                                    color = if (member.userId != null) MoneeGreen else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 🛒 ITEMS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expense Items (${billItems.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    val newItem = SplitItem(
                        splitItemId = generateItemId(),
                        splitBillId = "",
                        description = "",
                        amount = 0.0
                    )
                    val newParticipants = splitMembers.map { member ->
                        ItemParticipant(
                            memberId = member.splitMemberId,
                            name = member.name,
                            amount = 0.0,
                            isIncluded = true
                        )
                    }
                    billItems = billItems + BillItemWithParticipants(
                        item = newItem,
                        participants = newParticipants
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", color = MoneeGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            billItems.forEachIndexed { itemIndex, itemWithParts ->
                var itemDesc by remember { mutableStateOf(itemWithParts.item.description) }
                var itemAmount by remember { mutableStateOf(itemWithParts.item.amount.toString()) }
                var itemPayerId by remember(itemWithParts.item.payerId, splitMembers) {
                    mutableStateOf(
                        itemWithParts.item.payerId ?: splitMembers.firstOrNull()?.splitMemberId ?: ""
                    )
                }

                LaunchedEffect(itemDesc, itemAmount, itemPayerId) {
                    billItems = billItems.toMutableList().also { list ->
                        list[itemIndex] = list[itemIndex].copy(
                            item = list[itemIndex].item.copy(
                                description = itemDesc,
                                amount = itemAmount.toDoubleOrNull() ?: 0.0,
                                payerId = itemPayerId
                            )
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Item ${itemIndex + 1}", fontWeight = FontWeight.Medium)
                            if (billItems.size > 1) {
                                IconButton(
                                    onClick = {
                                        billItems = billItems.toMutableList().also {
                                            it.removeAt(itemIndex)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = itemDesc,
                            onValueChange = { itemDesc = it },
                            placeholder = { Text("Item name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = itemAmount,
                            onValueChange = { itemAmount = it },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("RM ") },
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "💳 Who paid for this item?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(splitMembers) { member ->
                                FilterChip(
                                    selected = itemPayerId == member.splitMemberId,
                                    onClick = { itemPayerId = member.splitMemberId },
                                    label = { Text(member.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Who pays for this item?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        itemWithParts.participants.forEach { part ->
                            val partAmount = remember(part.amount, itemWithParts.item.amount, splitMode) {
                                if (splitMode == "equal" && itemWithParts.item.amount > 0) {
                                    val includedCount = itemWithParts.participants.count { it.isIncluded }
                                    if (includedCount > 0) itemWithParts.item.amount / includedCount else 0.0
                                } else {
                                    part.amount
                                }
                            }
                            var localAmount by remember(partAmount) {
                                mutableStateOf(if (partAmount > 0) String.format("%.2f", partAmount) else "")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = part.isIncluded,
                                    onCheckedChange = { checked ->
                                        billItems = billItems.toMutableList().also { list ->
                                            val updated = list[itemIndex].participants.map { p ->
                                                if (p.memberId == part.memberId) p.copy(isIncluded = checked) else p
                                            }
                                            list[itemIndex] = list[itemIndex].copy(participants = updated)
                                        }
                                    }
                                )
                                Text(
                                    text = part.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                                if (splitMode == "custom" && part.isIncluded) {
                                    OutlinedTextField(
                                        value = localAmount,
                                        onValueChange = { newVal ->
                                            localAmount = newVal
                                            val newAmount = newVal.toDoubleOrNull() ?: 0.0
                                            billItems = billItems.toMutableList().also { list ->
                                                val updated = list[itemIndex].participants.map { p ->
                                                    if (p.memberId == part.memberId) p.copy(amount = newAmount) else p
                                                }
                                                list[itemIndex] = list[itemIndex].copy(participants = updated)
                                            }
                                        },
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        prefix = { Text("RM ") },
                                        shape = RoundedCornerShape(6.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                } else if (part.isIncluded) {
                                    Text(
                                        text = formatCurrency(partAmount),
                                        fontSize = 13.sp,
                                        color = MoneeGreen
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 🧾 SUMMARY =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MoneeYellow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontSize = 14.sp, color = Color.Gray)
                        Text(formatCurrency(totalAmount), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Participants", fontSize = 13.sp, color = Color.Gray)
                        Text("${splitMembers.size} people", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ CREATE BUTTON
            Button(
                onClick = {
                    if (!validate()) return@Button

                    val billId = generateBillId()

                    val finalMembers = splitMembers.map { m ->
                        m.copy(
                            splitBillId = billId,
                            joinedAt = System.currentTimeMillis().toString()
                        )
                    }

                    val finalItems = billItems.mapIndexed { idx, item ->
                        item.item.copy(splitBillId = billId)
                    }

                    val finalAllocations = mutableListOf<SplitAllocation>()
                    billItems.forEachIndexed { idx, itemWithParts ->
                        val item = finalItems[idx]
                        itemWithParts.participants
                            .filter { it.isIncluded && it.amount > 0 }
                            .forEach { part ->
                                finalAllocations.add(
                                    SplitAllocation(
                                        splitAllocationId = generateAllocationId(),
                                        splitItemId = item.splitItemId,
                                        splitMemberId = part.memberId,
                                        payerId = item.payerId,
                                        shareAmount = part.amount,
                                        isPaid = (splitMembers.find { it.splitMemberId == part.memberId }?.userId == userId),
                                        paidAt = if (splitMembers.find { it.splitMemberId == part.memberId }?.userId == userId)
                                            System.currentTimeMillis().toString() else null
                                    )
                                )
                            }
                    }

                    val newBill = SplitBill(
                        splitBillId = billId,
                        creatorId = userId,
                        title = billTitle,
                        totalAmount = totalAmount,
                        billDate = billDate,
                        status = "ACTIVE",
                        participantCount = finalMembers.size
                    )

                    onCreateSplit(newBill, finalMembers, finalItems, finalAllocations)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoneeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = "Create Split Bill",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 📅 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDateSelected = { millis ->
                millis?.let {
                    billDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            state = datePickerState
        )
    }

    // 📂 LOAD GROUP DIALOG
    if (showGroupDialog) {
        LaunchedEffect(Unit) {
            viewModel.loadGroups(userId)
        }
        val groups by viewModel.uiState.collectAsState()

        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Load Participant Group") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (groups.groups.isEmpty()) {
                        Text("No saved groups yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(groups.groups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            scope.launch {
                                                val result = onLoadGroup(group.groupId)
                                                result.onSuccess { loadedMembers ->
                                                    if (loadedMembers.isNotEmpty()) {
                                                        val me = splitMembers.firstOrNull { it.userId == userId }
                                                        val others = loadedMembers.filter { it.userId != userId }
                                                        splitMembers = listOfNotNull(me) + others

                                                        billItems = billItems.map { item ->
                                                            val newParts = splitMembers.map { m ->
                                                                ItemParticipant(
                                                                    memberId = m.splitMemberId,
                                                                    name = m.name,
                                                                    amount = 0.0,
                                                                    isIncluded = true
                                                                )
                                                            }
                                                            item.copy(participants = newParts)
                                                        }
                                                    }
                                                    showGroupDialog = false
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(group.name, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 💾 SAVE GROUP DIALOG
    if (showSaveGroupDialog) {
        AlertDialog(
            onDismissRequest = { showSaveGroupDialog = false },
            title = { Text("Save Participant Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${splitMembers.size} participants will be saved",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val newGroup = ParticipantGroup(
                                groupId = generateGroupId(),
                                userId = userId,
                                name = newGroupName
                            )
                            scope.launch {
                                onSaveGroup(newGroup)
                                    .onSuccess {
                                        newGroupName = ""
                                        showSaveGroupDialog = false
                                    }
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 👥 MANAGE MEMBERS DIALOG
    if (showMemberDialog) {
        var lookupError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showMemberDialog = false },
            title = { Text("Manage Participants", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(splitMembers) { member ->
                            var name by remember { mutableStateOf(member.name) }
                            var isAppUser by remember { mutableStateOf(member.userId != null) }
                            var userIdInput by remember { mutableStateOf(member.userId ?: "") }
                            var isLookingUp by remember { mutableStateOf(false) }

                            LaunchedEffect(member.userId) {
                                isAppUser = member.userId != null
                                userIdInput = member.userId ?: ""
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = !isAppUser,
                                            onClick = { isAppUser = false; userIdInput = ""; name = "" },
                                            label = { Text("Guest") }
                                        )
                                        FilterChip(
                                            selected = isAppUser,
                                            onClick = { isAppUser = true; name = "" },
                                            label = { Text("App User") }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isAppUser) {
                                        OutlinedTextField(
                                            value = userIdInput,
                                            onValueChange = { userIdInput = it; lookupError = null },
                                            label = { Text("User ID") },
                                            placeholder = { Text("Enter User ID") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            trailingIcon = {
                                                if (isLookingUp) {
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                }
                                            },
                                            supportingText = { lookupError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isLookingUp = true
                                                    lookupError = null
                                                    val result = onLookupUser(userIdInput.trim())
                                                    result
                                                        .onSuccess { fetchedName ->
                                                            name = fetchedName
                                                            val idx = splitMembers.indexOfFirst {
                                                                it.splitMemberId == member.splitMemberId
                                                            }
                                                            if (idx != -1) {
                                                                splitMembers = splitMembers.toMutableList().also { list ->
                                                                    list[idx] = list[idx].copy(
                                                                        userId = userIdInput.trim(),
                                                                        name = fetchedName
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        .onFailure {
                                                            lookupError = "User not found — switch to Guest and enter name manually"
                                                        }
                                                    isLookingUp = false
                                                }
                                            },
                                            enabled = userIdInput.isNotBlank() && !isLookingUp,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Find Name")
                                        }
                                        if (name.isNotEmpty()) {
                                            Text(
                                                text = "✓ Found: $name",
                                                color = MoneeGreen,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = {
                                                name = it
                                                val idx = splitMembers.indexOfFirst {
                                                    it.splitMemberId == member.splitMemberId
                                                }
                                                if (idx != -1) {
                                                    splitMembers = splitMembers.toMutableList().also { list ->
                                                        list[idx] = list[idx].copy(name = it)
                                                    }
                                                }
                                            },
                                            label = { Text("Name") },
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }

                                    if (splitMembers.size > 1 && member.userId != userId) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = {
                                                splitMembers = splitMembers.toMutableList().also {
                                                    it.removeAt(it.indexOfFirst { m ->
                                                        m.splitMemberId == member.splitMemberId
                                                    })
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                        ) {
                                            Text("Remove Participant")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            splitMembers = splitMembers + SplitMember(
                                splitMemberId = generateMemberId(),
                                splitBillId = "",
                                userId = null,
                                name = "",
                                joinedAt = System.currentTimeMillis().toString()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneeYellow.copy(alpha = 0.3f),
                            contentColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Participant")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val validNames = splitMembers.all { it.name.isNotBlank() }
                        if (validNames) {
                            showMemberDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// 🧩 DATE PICKER COMPONENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    state: DatePickerState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(state.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DatePicker(state = state)
        }
    )
}

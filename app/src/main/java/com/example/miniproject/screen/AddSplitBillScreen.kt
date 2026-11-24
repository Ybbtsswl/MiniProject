package com.example.miniproject.screen

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.miniproject.data.GroupMember
import com.example.miniproject.data.ParticipantGroup
import kotlinx.coroutines.launch
import com.example.miniproject.data.SplitAllocation
import com.example.miniproject.data.SplitBill
import com.example.miniproject.data.SplitItem
import com.example.miniproject.data.SplitMember
import com.example.miniproject.receipt.ReceiptData
import com.example.miniproject.receipt.ReceiptScanButton
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeCream
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.MoneeYellowSoft
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeCardColors
import com.example.miniproject.ui.theme.moneeFieldColors
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.ui.theme.moneeTopInsets
import com.example.miniproject.viewmodel.SplitBillViewModel
import com.example.miniproject.viewmodel.UserResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ==========================================
// DATA CLASSES
// ==========================================
data class ExpenseItemState(
    val item: SplitItem,
    val assignedMemberIds: List<String>,
    val splitMode: String,                 // "equal" | "custom"
    val percentages: Map<String, Double>
)

data class TempParticipant(
    val id: String,
    val name: String,
    val isUser: Boolean,
    val userId: String? = null
)

private val DECIMAL_REGEX = Regex("^\\d*\\.?\\d*$")

// ==========================================
// MAIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSplitBillScreen(
    userId: String,
    viewModel: SplitBillViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load saved groups when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadGroups(userId)
    }

    var billTitle by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var showManageDialog by remember { mutableStateOf(false) }
    var dialogTab by remember { mutableIntStateOf(0) }
    var groupMenuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var participants by remember {
        mutableStateOf(
            listOf(TempParticipant(id = "m_$userId", name = "You", isUser = true, userId = userId))
        )
    }
    var expenseItems by remember { mutableStateOf(listOf<ExpenseItemState>()) }
    var sstPercent by remember { mutableStateOf("6") }
    var serviceChargePercent by remember { mutableStateOf("10") }
    var scanNotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Pre-fill the form from a scanned receipt. Nothing is saved: the user reviews and edits first.
    fun applyReceipt(r: ReceiptData) {
        scanError = null
        r.merchant?.let { if (billTitle.isBlank()) billTitle = it }

        fun newItem(name: String, unitPrice: Double, qty: Int) = ExpenseItemState(
            item = SplitItem(
                splitItemId = UUID.randomUUID().toString(),
                splitBillId = "",
                description = name,
                amount = unitPrice,
                quantity = qty,
                payerId = null
            ),
            assignedMemberIds = emptyList(),   // who shared what is for the user to decide
            splitMode = "equal",
            percentages = emptyMap()
        )

        var scanned = r.items.map { newItem(it.name, it.unitPrice, it.quantity) }
        // Nothing itemised but a total was read: one line, so there is still something to split
        val receiptTotal = r.total
        if (scanned.isEmpty() && receiptTotal != null) {
            val base = r.subtotal ?: (receiptTotal - (r.sstAmount ?: 0.0) - (r.serviceChargeAmount ?: 0.0))
            scanned = listOf(newItem("Receipt total", base, 1))
        }

        val untouched = expenseItems.all { it.item.description.isBlank() && it.item.amount == 0.0 }
        expenseItems = if (untouched) scanned else expenseItems + scanned

        // Use the receipt's own tax figures (0 when it has none) so the total matches the receipt
        fun pctText(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
        sstPercent = pctText(r.sstPercent ?: 0.0)
        serviceChargePercent = pctText(r.serviceChargePercent ?: 0.0)

        scanNotes = r.warnings + "Assign each item to the people who shared it, then check the amounts."
        Toast.makeText(context, "Receipt scanned. Please review the details", Toast.LENGTH_SHORT).show()
    }

    // Pick a saved group from the dropdown: you + that group's members become the participants
    fun selectGroup(groupId: String, groupName: String) {
        scope.launch {
            viewModel.loadGroupMembers(groupId).onSuccess { saved ->
                val people = saved
                    .filter { it.userId != userId }
                    .map { m ->
                        if (m.userId != null) TempParticipant(m.userId, m.name, true, m.userId)
                        else TempParticipant("n_${m.groupMemberId}", m.name, false)
                    }
                val me = participants.first { it.id == "m_$userId" }
                participants = listOf(me) + people
                val keep = participants.map { it.id }.toSet()
                // people who left the list can't keep their item assignments
                expenseItems = expenseItems.map { st ->
                    st.copy(
                        assignedMemberIds = st.assignedMemberIds.filter { it in keep },
                        percentages = st.percentages.filterKeys { it in keep }
                    )
                }
                selectedGroup = groupName
                Toast.makeText(context, "Group selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Totals
    val subtotal = expenseItems.sumOf { it.item.amount * it.item.quantity }
    val sstAmount = subtotal * (sstPercent.toDoubleOrNull() ?: 0.0) / 100
    val serviceChargeAmount = subtotal * (serviceChargePercent.toDoubleOrNull() ?: 0.0) / 100
    val totalAmount = subtotal + sstAmount + serviceChargeAmount

    val canCreate = billTitle.isNotBlank() &&
            participants.size >= 2 &&
            expenseItems.isNotEmpty() &&
            expenseItems.all {
                it.item.description.isNotBlank() &&
                        it.item.amount > 0 &&
                        it.assignedMemberIds.isNotEmpty()
            } &&
            !uiState.isCreating

    fun createBill() {
        if (!canCreate) return

        val newBill = SplitBill(
            splitBillId = UUID.randomUUID().toString(),
            title = billTitle,
            totalAmount = totalAmount,
            creatorId = userId,
            billDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            status = "ACTIVE",
            participantCount = participants.size
        )

        // Fresh member id per bill: reusing "m_<userId>" collides on the primary key
        // as soon as the same person is in a second bill.
        val memberIdMap = participants.associate { it.id to UUID.randomUUID().toString() }
        val myMemberId = memberIdMap.getValue("m_$userId")

        val members = participants.map { p ->
            SplitMember(
                splitMemberId = memberIdMap.getValue(p.id),
                splitBillId = "",
                userId = if (p.isUser) p.userId else null,
                name = p.name,
                joinedAt = LocalDate.now().toString(),
                isMe = p.id == "m_$userId"
            )
        }

        // The creator paid, so they are the payer (the ViewModel's owe/owed maths needs this)
        val items = expenseItems.map { it.item.copy(splitBillId = "", payerId = myMemberId) }

        // Spread SST + service charge proportionally across every share
        val taxFactor = if (subtotal > 0) totalAmount / subtotal else 1.0

        val allocations = mutableListOf<SplitAllocation>()
        expenseItems.forEach { state ->
            val itemTotal = state.item.amount * state.item.quantity
            val ids = state.assignedMemberIds
            val pcts: Map<String, Double> =
                if (state.splitMode == "equal") ids.associateWith { 100.0 / ids.size }
                else ids.associateWith { state.percentages[it] ?: 0.0 }

            pcts.forEach { (memberId, percent) ->
                allocations.add(
                    SplitAllocation(
                        splitAllocationId = UUID.randomUUID().toString(),
                        splitItemId = state.item.splitItemId,
                        splitMemberId = memberIdMap.getValue(memberId),
                        shareAmount = itemTotal * percent / 100.0 * taxFactor,
                        isPaid = false,
                        paidAt = null,
                        payerId = null,
                        contributionType = state.splitMode,
                        contributionValue = percent
                    )
                )
            }
        }

        viewModel.createSplitBill(
            bill = newBill,
            members = members,
            items = items,
            allocations = allocations,
            onSuccess = {
                Toast.makeText(context, "Split bill created", Toast.LENGTH_SHORT).show()
                onSuccess()
            },
            onError = { }
        )
    }

    Box(modifier = Modifier.fillMaxSize().moneeBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Create Split Bill") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = billTitle,
                        onValueChange = { billTitle = it },
                        label = { Text("Bill Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = moneeFieldColors()
                    )
                }

                item {
                    Column {
                        ReceiptScanButton(
                            onResult = { applyReceipt(it) },
                            onError = { scanError = it }
                        )
                        scanError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (scanNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MoneeYellowSoft),
                                border = BorderStroke(1.dp, MoneeBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Please review", fontWeight = FontWeight.SemiBold, color = MoneeInk)
                                    scanNotes.forEach {
                                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = MoneeInk)
                                    }
                                    TextButton(onClick = { scanNotes = emptyList() }) {
                                        Text("Dismiss", color = MoneeAmber)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { dialogTab = 0; showManageDialog = true }) {
                                Text("Manage", color = MoneeAmber, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Saved groups: pick one from the dropdown
                        Box {
                            OutlinedButton(
                                onClick = { groupMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MoneeBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneeInk)
                            ) {
                                Text(if (selectedGroup.isNotEmpty()) "Group: $selectedGroup" else "Select a saved group")
                            }
                            DropdownMenu(
                                expanded = groupMenuOpen,
                                onDismissRequest = { groupMenuOpen = false },
                                containerColor = MoneeCream
                            ) {
                                when {
                                    uiState.isLoadingGroups -> DropdownMenuItem(
                                        text = { Text("Loading groups...", color = MoneeMuted) },
                                        onClick = {},
                                        enabled = false
                                    )
                                    uiState.groups.isEmpty() -> DropdownMenuItem(
                                        text = { Text("No saved groups yet", color = MoneeMuted) },
                                        onClick = {},
                                        enabled = false
                                    )
                                    else -> uiState.groups.forEach { group ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    group.name,
                                                    fontWeight = if (group.name == selectedGroup) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteGroup(group.groupId)
                                                        Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, "Delete group", tint = MoneeMuted, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                groupMenuOpen = false
                                                selectGroup(group.groupId, group.name)
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(color = MoneeBorder)
                                DropdownMenuItem(
                                    text = { Text("+ Create new group", color = MoneeAmber, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        groupMenuOpen = false
                                        dialogTab = 1
                                        showManageDialog = true
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        WrapParticipantsChips(participants = participants)
                    }
                }

                item {
                    Column {
                        Text("Expense Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        expenseItems.forEachIndexed { index, itemState ->
                            key(itemState.item.splitItemId) {
                                ExpenseItemCard(
                                    itemState = itemState,
                                    participants = participants,
                                    onRemove = { expenseItems = expenseItems.filterIndexed { i, _ -> i != index } },
                                    onUpdate = { updated ->
                                        expenseItems = expenseItems.toMutableList().also { it[index] = updated }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        MoneeGradientButton(
                            onClick = {
                                val newItem = ExpenseItemState(
                                    item = SplitItem(
                                        splitItemId = UUID.randomUUID().toString(),
                                        splitBillId = "",
                                        description = "",
                                        amount = 0.0,
                                        quantity = 1,
                                        payerId = null
                                    ),
                                    assignedMemberIds = emptyList(),
                                    splitMode = "equal",
                                    percentages = emptyMap()
                                )
                                expenseItems = expenseItems + newItem
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Item", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    Column {
                        Text("Tax & Charges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SST (%)")
                            OutlinedTextField(
                                value = sstPercent,
                                onValueChange = { if (it.matches(DECIMAL_REGEX)) sstPercent = it },
                                modifier = Modifier.width(100.dp),
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = moneeFieldColors()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Service Charge (%)")
                            OutlinedTextField(
                                value = serviceChargePercent,
                                onValueChange = { if (it.matches(DECIMAL_REGEX)) serviceChargePercent = it },
                                modifier = Modifier.width(100.dp),
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = moneeFieldColors()
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .moneeHero(RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal")
                                Text("RM %.2f".format(subtotal))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SST")
                                Text("RM %.2f".format(sstAmount))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Charge")
                                Text("RM %.2f".format(serviceChargeAmount))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.7f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("RM %.2f".format(totalAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                item {
                    MoneeGradientButton(
                        onClick = { createBill() },
                        enabled = canCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MoneeInk, strokeWidth = 2.dp)
                        } else {
                            Text("✓ Create Split Bill", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showManageDialog) {
        ManageGroupDialog(
            viewModel = viewModel,
            currentUserId = userId,
            currentParticipants = participants,
            initialTab = dialogTab,
            onDismiss = { showManageDialog = false },
            onAddUser = { user ->
                if (participants.none { it.id == user.id || it.userId == user.id }) {
                    participants = participants + TempParticipant(id = user.id, name = user.name, isUser = true, userId = user.id)
                    Toast.makeText(context, "${user.name} added", Toast.LENGTH_SHORT).show()
                }
                showManageDialog = false
            },
            onAddNonUser = { name ->
                participants = participants + TempParticipant(id = "n_${System.currentTimeMillis()}", name = name, isUser = false)
                Toast.makeText(context, "$name added", Toast.LENGTH_SHORT).show()
                showManageDialog = false
            },
            onRemoveParticipant = { id ->
                if (id != "m_$userId") {
                    participants = participants.filterNot { it.id == id }
                    Toast.makeText(context, "Participant removed", Toast.LENGTH_SHORT).show()
                    // drop removed person from item assignments
                    expenseItems = expenseItems.map { st ->
                        st.copy(
                            assignedMemberIds = st.assignedMemberIds - id,
                            percentages = st.percentages - id
                        )
                    }
                }
            }
        )
    }
}

// ==========================================
// MANAGE GROUP DIALOG
// ==========================================
@Composable
fun ManageGroupDialog(
    viewModel: SplitBillViewModel,
    currentUserId: String,
    currentParticipants: List<TempParticipant>,
    onDismiss: () -> Unit,
    onAddUser: (UserResult) -> Unit,
    onAddNonUser: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    initialTab: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    var tabIndex by remember { mutableIntStateOf(initialTab) }
    var searchUserId by remember { mutableStateOf("") }
    var newGroupName by remember { mutableStateOf("") }
    var selectedMembersForGroup by remember { mutableStateOf(setOf<String>()) }
    var nonUserName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MoneeCream),
            border = BorderStroke(1.dp, MoneeBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Manage Participants & Groups",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MoneeInk
                )
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MoneeInk
                ) {
                    Tab(
                        selected = tabIndex == 0, onClick = { tabIndex = 0 },
                        selectedContentColor = MoneeInk, unselectedContentColor = MoneeMuted,
                        text = { Text("Add People") }
                    )
                    Tab(
                        selected = tabIndex == 1, onClick = { tabIndex = 1 },
                        selectedContentColor = MoneeInk, unselectedContentColor = MoneeMuted,
                        text = { Text("Create Group") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (tabIndex) {
                    // ===== TAB 0: Search & Add User =====
                    0 -> {
                        Text("Search Registered User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchUserId,
                            onValueChange = { searchUserId = it },
                            label = { Text("Enter User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g., user_abc123") },
                            colors = moneeFieldColors()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        MoneeGradientButton(
                            onClick = { viewModel.searchUserById(searchUserId.trim()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            enabled = searchUserId.isNotBlank() && !uiState.isSearching
                        ) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MoneeInk)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (uiState.isSearching) "Searching..." else "Search User", fontWeight = FontWeight.SemiBold)
                        }

                        uiState.searchedUser?.let { user ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MoneeYellowSoft),
                                border = BorderStroke(1.dp, MoneeBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("User Found!", fontWeight = FontWeight.SemiBold, color = MoneeInk)
                                    Text("Name: ${user.name}", color = MoneeInk)
                                    Text("ID: ${user.id}", color = MoneeInk)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MoneeGradientButton(
                                        onClick = {
                                            onAddUser(user)
                                            viewModel.clearSearch()
                                            searchUserId = ""
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Text("+ Add to Bill", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        uiState.searchError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MoneeBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Add Guest (No Account)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nonUserName,
                            onValueChange = { nonUserName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Enter guest name") },
                            colors = moneeFieldColors()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        MoneeGradientButton(
                            onClick = {
                                if (nonUserName.isNotBlank()) {
                                    onAddNonUser(nonUserName.trim())
                                    nonUserName = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            enabled = nonUserName.isNotBlank()
                        ) {
                            Text("+ Add Guest", fontWeight = FontWeight.SemiBold)
                        }

                        // Current participants with remove buttons
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MoneeBorder)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Current Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        currentParticipants.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name)
                                if (p.id != "m_$currentUserId") {
                                    IconButton(onClick = { onRemoveParticipant(p.id) }) {
                                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // ===== TAB 1: Create & Save New Group =====
                    1 -> {
                        Text("Create New Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Group Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g., Roommates, Dinner Friends") },
                            colors = moneeFieldColors()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Members to Include:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))

                        currentParticipants.forEach { member ->
                            val selected = selectedMembersForGroup.contains(member.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMembersForGroup =
                                            if (selected) selectedMembersForGroup - member.id
                                            else selectedMembersForGroup + member.id
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        selectedMembersForGroup =
                                            if (it) selectedMembersForGroup + member.id
                                            else selectedMembersForGroup - member.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MoneeYellow,
                                        checkmarkColor = MoneeInk,
                                        uncheckedColor = MoneeMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(member.name)
                                if (member.userId == currentUserId) {
                                    Text(" (You)", color = MoneeAmber, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        MoneeGradientButton(
                            onClick = {
                                if (newGroupName.isNotBlank() && selectedMembersForGroup.any { it != "m_$currentUserId" }) {
                                    val newGroup = ParticipantGroup(
                                        groupId = UUID.randomUUID().toString(),
                                        userId = currentUserId,
                                        name = newGroupName
                                    )
                                    // You are the group owner, so only the other people are stored
                                    val membersToSave = currentParticipants
                                        .filter { it.id in selectedMembersForGroup && it.userId != currentUserId }
                                        .map {
                                            GroupMember(
                                                groupMemberId = UUID.randomUUID().toString(),
                                                groupId = newGroup.groupId,
                                                userId = if (it.isUser) it.userId else null,
                                                name = it.name
                                            )
                                        }
                                    viewModel.createGroup(
                                        group = newGroup,
                                        members = membersToSave,
                                        onSuccess = {
                                            Toast.makeText(context, "Group saved", Toast.LENGTH_SHORT).show()
                                            newGroupName = ""
                                            selectedMembersForGroup = emptySet()
                                            onDismiss()   // the new group now shows in the dropdown
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            enabled = newGroupName.isNotBlank() && selectedMembersForGroup.any { it != "m_$currentUserId" }
                        ) {
                            Text("💾 Save Group", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                // Show the real error (e.g. a Supabase/RLS message) instead of failing silently
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MoneeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneeInk)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapParticipantsChips(participants: List<TempParticipant>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        participants.forEach { member ->
            // You / registered users get the yellow gradient, guests the plain white chip
            MoneeChip(selected = member.isUser) {
                Text(member.name, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// EXPENSE ITEM CARD
// ==========================================
@SuppressLint("DefaultLocale")
@Composable
fun ExpenseItemCard(
    itemState: ExpenseItemState,
    participants: List<TempParticipant>,
    onRemove: () -> Unit,
    onUpdate: (ExpenseItemState) -> Unit
) {
    var showAssignDropdown by remember { mutableStateOf(false) }
    val itemId = itemState.item.splitItemId

    var amountText by remember(itemId) {
        mutableStateOf(if (itemState.item.amount == 0.0) "" else itemState.item.amount.toString())
    }
    var quantityText by remember(itemId) { mutableStateOf(itemState.item.quantity.toString()) }

    val assignees = itemState.assignedMemberIds
    val itemTotal = itemState.item.amount * itemState.item.quantity

    fun equalPercentages(ids: List<String>): Map<String, Double> =
        if (ids.isEmpty()) emptyMap() else ids.associateWith { 100.0 / ids.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = moneeCardColors(),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: total + remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RM %.2f".format(itemTotal), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Remove item", tint = MoneeMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editable description / amount / quantity
            OutlinedTextField(
                value = itemState.item.description,
                onValueChange = { onUpdate(itemState.copy(item = itemState.item.copy(description = it))) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = moneeFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        if (text.matches(DECIMAL_REGEX)) {
                            amountText = text
                            onUpdate(itemState.copy(item = itemState.item.copy(amount = text.toDoubleOrNull() ?: 0.0)))
                        }
                    },
                    label = { Text("Unit price") },
                    prefix = { Text("RM ") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    colors = moneeFieldColors()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { text ->
                        if (text.all { it.isDigit() } && text.length <= 3) {
                            quantityText = text
                            val q = text.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            onUpdate(itemState.copy(item = itemState.item.copy(quantity = q)))
                        }
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = moneeFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Assign to
            Text(
                "Assigned to",
                style = MaterialTheme.typography.labelMedium,
                color = MoneeMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedButton(
                    onClick = { showAssignDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MoneeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneeInk)
                ) {
                    val names = assignees.joinToString(", ") { id ->
                        participants.find { it.id == id }?.name ?: "Unknown"
                    }
                    Text(names.ifEmpty { "Select people" })
                }
                DropdownMenu(
                    expanded = showAssignDropdown,
                    onDismissRequest = { showAssignDropdown = false },
                    containerColor = MoneeCream
                ) {
                    participants.forEach { p ->
                        val selected = assignees.contains(p.id)
                        DropdownMenuItem(
                            text = { Text(if (selected) "✓ ${p.name}" else p.name) },
                            onClick = {
                                val newAssignees =
                                    if (selected) assignees - p.id else assignees + p.id
                                // Reset to an equal split whenever the assignees change
                                onUpdate(
                                    itemState.copy(
                                        assignedMemberIds = newAssignees,
                                        splitMode = "equal",
                                        percentages = equalPercentages(newAssignees)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            if (assignees.isEmpty()) return@Column

            Spacer(modifier = Modifier.height(12.dp))

            // Split mode toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MoneeChip(
                    selected = itemState.splitMode == "equal",
                    onClick = {
                        onUpdate(
                            itemState.copy(
                                splitMode = "equal",
                                percentages = equalPercentages(assignees)
                            )
                        )
                    }
                ) { Text("Equal Split", fontSize = 13.sp) }
                MoneeChip(
                    selected = itemState.splitMode == "custom",
                    onClick = {
                        val pcts =
                            if (assignees.all { itemState.percentages.containsKey(it) }) itemState.percentages
                            else equalPercentages(assignees)
                        onUpdate(itemState.copy(splitMode = "custom", percentages = pcts))
                    }
                ) { Text("Custom %", fontSize = 13.sp) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (itemState.splitMode == "equal") {
                val equalPct = 100.0 / assignees.size
                val share = itemTotal / assignees.size
                assignees.forEach { memberId ->
                    val member = participants.find { it.id == memberId }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(member?.name ?: "")
                        Text(
                            "%.1f%%  RM %.2f".format(equalPct, share),
                            fontWeight = FontWeight.Medium,
                            color = MoneeAmber
                        )
                    }
                }
            } else {
                val totalPct = assignees.sumOf { itemState.percentages[it] ?: 0.0 }

                assignees.forEach { memberId ->
                    val member = participants.find { it.id == memberId }
                    val pctValue = itemState.percentages[memberId] ?: 0.0
                    val amount = itemTotal * pctValue / 100.0
                    // Keyed on id only so typing "5." isn't overwritten by "5.0"
                    var textValue by remember(itemId, memberId) {
                        mutableStateOf(String.format("%.1f", pctValue))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(member?.name ?: "", modifier = Modifier.weight(1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { newText ->
                                    if (newText.matches(DECIMAL_REGEX)) {
                                        textValue = newText
                                        val newPct = newText.toDoubleOrNull() ?: 0.0
                                        onUpdate(
                                            itemState.copy(
                                                percentages = itemState.percentages + (memberId to newPct)
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.width(100.dp),
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 14.sp),
                                colors = moneeFieldColors()
                            )
                            Text(
                                "RM %.2f".format(amount),
                                modifier = Modifier.width(80.dp),
                                textAlign = TextAlign.End,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val ok = totalPct in 99.9..100.1
                Text(
                    "Total: %.1f%%".format(totalPct),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) MoneeMuted else MaterialTheme.colorScheme.error
                )
                if (!ok) {
                    Text(
                        "⚠️ Total should equal 100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEW (the full screen needs a ViewModel, so preview the pieces)
// ==========================================
@Preview(showBackground = true, name = "Participants chips")
@Composable
fun ParticipantsChipsPreview() {
    MiniProjectTheme {
        WrapParticipantsChips(
            listOf(
                TempParticipant("m_1", "You", true, "1"),
                TempParticipant("n_1", "Aisyah", false)
            )
        )
    }
}

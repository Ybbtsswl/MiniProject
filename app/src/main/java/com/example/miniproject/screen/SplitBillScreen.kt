package com.example.miniproject.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.miniproject.data.SplitBill
import com.example.miniproject.ui.theme.MiniProjectTheme
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeChip
import com.example.miniproject.ui.theme.MoneeCream
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeCardColors
import com.example.miniproject.ui.theme.moneeHero
import com.example.miniproject.ui.theme.moneeTopInsets
import com.example.miniproject.viewmodel.SplitBillViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// internal: also used by AllSplitBillsScreen. Named differently from the expense screen's helper
// because two top-level functions with the same name and signature in one package conflict.
// The language tag needs a hyphen ("ms-MY"): with an underscore Java ignores it and prints a
// generic currency sign instead of RM.
internal fun formatBillAmount(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ms-MY"))
        .apply { maximumFractionDigits = 2 }
        .format(amount)
        .replace("MYR", "RM")
}

private fun formatDateShort(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("dd MMM"))
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    userId: String,
    viewModel: SplitBillViewModel,
    onBillClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit,
    onSeeAllClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentMonth by remember { mutableStateOf(LocalDate.now()) }
    val monthDisplay = remember(currentMonth) {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    var showMonthPicker by remember { mutableStateOf(false) }
    val selectedYearMonth = remember(currentMonth) {
        YearMonth.from(currentMonth)
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var billToDelete by remember { mutableStateOf<SplitBill?>(null) }

    LaunchedEffect(userId, currentMonth) {
        viewModel.loadBills(userId, currentMonth)
    }

    Box(modifier = Modifier.fillMaxSize().moneeBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Split bills",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
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
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    MoneeGradientButton(
                        onClick = onCreateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Create split bill",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MoneeYellow)
                    }
                } else if (uiState.error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                viewModel.loadBills(userId, currentMonth)
                            }
                        ) {
                            Text("Retry", color = MoneeGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    MonthNavigationCard(
                        currentMonth = monthDisplay,
                        onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                        onMonthClick = { showMonthPicker = true },
                        youOwe = uiState.youOwe,
                        owedToYou = uiState.owedToYou
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SettlementOverviewCard(
                        openSplits = uiState.openSplits,
                        settledAmount = uiState.settledAmount
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Split bill history",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MoneeInk
                        )
                        TextButton(onClick = onSeeAllClick) {
                            Text(
                                "See all",
                                color = MoneeGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilterTabsRow(
                        selectedTab = selectedFilter,
                        onTabSelected = { selectedFilter = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val displayBills = remember(uiState.filteredBills, selectedFilter) {
                        when (selectedFilter) {
                            "SETTLED" -> uiState.filteredBills.filter {
                                it.status.equals("SETTLED", ignoreCase = true)
                            }
                            "ACTIVE" -> uiState.filteredBills.filter {
                                !it.status.equals("SETTLED", ignoreCase = true)
                            }
                            else -> uiState.filteredBills
                        }
                    }

                    if (displayBills.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedFilter) {
                                    "SETTLED" -> "No settled bills yet"
                                    "ACTIVE" -> "No active split bills"
                                    else -> "No split bills yet"
                                },
                                fontSize = 14.sp,
                                color = MoneeMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.heightIn(max = 500.dp)
                        ) {
                            items(displayBills) { bill ->
                                SplitBillItemRow(
                                    bill = bill,
                                    onClick = { onBillClick(bill.splitBillId) },
                                    onDelete = { billToDelete = bill }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initialSelection = selectedYearMonth,
            onDismissRequest = { showMonthPicker = false },
            onMonthSelected = { yearMonth ->
                currentMonth = yearMonth.atDay(1)
                showMonthPicker = false
            }
        )
    }

    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            containerColor = MoneeCream,
            title = { Text("Delete split bill?", fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${bill.title}\" will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBill(bill.splitBillId, userId, currentMonth)
                        billToDelete = null
                    }
                ) {
                    Text("Delete", color = MoneeRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("Cancel", color = MoneeMuted)
                }
            }
        )
    }
}

@Composable
fun MonthNavigationCard(
    currentMonth: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    youOwe: Double,
    owedToYou: Double
) {
    val netBalance = owedToYou - youOwe

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .moneeHero(RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowLeft,
                        contentDescription = "Previous",
                        tint = MoneeMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = currentMonth,
                    fontSize = 14.sp,
                    color = MoneeInk,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onMonthClick() }
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = "Next",
                        tint = MoneeMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "YOU OWE",
                        fontSize = 11.sp,
                        color = MoneeMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatBillAmount(youOwe),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeRed
                    )
                    Text(
                        text = if (youOwe > 0) "Based on your open bills" else "All settled",
                        fontSize = 11.sp,
                        color = MoneeMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OWED TO YOU",
                        fontSize = 11.sp,
                        color = MoneeMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatBillAmount(owedToYou),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneeGreen
                    )
                    Text(
                        text = if (owedToYou > 0) "Based on your open bills" else "All settled",
                        fontSize = 11.sp,
                        color = MoneeMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (netBalance >= 0) {
                    "Net balance · ${formatBillAmount(netBalance)} to receive"
                } else {
                    "Net balance · ${formatBillAmount(-netBalance)} to pay"
                },
                fontSize = 13.sp,
                color = MoneeInk,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MonthPickerDialog(
    initialSelection: YearMonth,
    onDismissRequest: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialSelection.year) }
    var selectedMonth by remember { mutableIntStateOf(initialSelection.monthValue) }

    val months = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val years = remember { (2024..2030).toList() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MoneeCream,
        confirmButton = {
            TextButton(
                onClick = {
                    onMonthSelected(YearMonth.of(selectedYear, selectedMonth))
                }
            ) {
                Text("OK", color = MoneeGreen, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = MoneeMuted)
            }
        },
        title = {
            Text(
                "Select Month",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Year", fontSize = 14.sp, color = MoneeMuted)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(years) { year ->
                        MoneeChip(
                            selected = year == selectedYear,
                            onClick = { selectedYear = year },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = year.toString(),
                                fontWeight = if (year == selectedYear) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Month", fontSize = 14.sp, color = MoneeMuted)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(months) { monthName ->
                        val monthNumber = months.indexOf(monthName) + 1
                        MoneeChip(
                            selected = monthNumber == selectedMonth,
                            onClick = { selectedMonth = monthNumber },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = monthName,
                                fontWeight = if (monthNumber == selectedMonth) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SettlementOverviewCard(openSplits: Int, settledAmount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = moneeCardColors(),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settlement overview",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$openSplits open splits",
                    fontSize = 13.sp,
                    color = MoneeMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatBillAmount(settledAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneeInk
                )
                Text(
                    text = "settled this month",
                    fontSize = 11.sp,
                    color = MoneeMuted
                )
            }
        }
    }
}

@Composable
fun FilterTabsRow(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        "ALL" to "All",
        "SETTLED" to "Settled",
        "ACTIVE" to "Active"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        tabs.forEach { (key, label) ->
            val isSelected = selectedTab == key
            MoneeChip(selected = isSelected, onClick = { onTabSelected(key) }) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// onDelete = null hides the delete button (used by the read-only "all bills" list)
@Composable
fun SplitBillItemRow(bill: SplitBill, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = moneeCardColors(),
        border = BorderStroke(1.dp, MoneeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .padding(start = 16.dp, end = if (onDelete != null) 6.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MoneeInk
                )
                Text(
                    text = "${formatDateShort(bill.billDate)} · ${bill.participantCount} people",
                    fontSize = 12.sp,
                    color = MoneeMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatBillAmount(bill.totalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk
                )
                val statusText = when (bill.status.lowercase()) {
                    "settled" -> "Settled"
                    else -> "Active"
                }
                val statusColor = when (bill.status.lowercase()) {
                    "settled" -> MoneeGreen
                    else -> MoneeMuted
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MoneeRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplitBillScreenPreview() {
    MiniProjectTheme {
        val mockBills = listOf(
            SplitBill(
                title = "Dinner with friends",
                totalAmount = 45.00,
                billDate = "2026-10-13",
                status = "ACTIVE",
                participantCount = 4
            ),
            SplitBill(
                title = "Weekly groceries",
                totalAmount = 56.00,
                billDate = "2026-10-12",
                status = "ACTIVE",
                participantCount = 3
            ),
            SplitBill(
                title = "Coffee catch-up",
                totalAmount = 32.00,
                billDate = "2026-10-11",
                status = "ACTIVE",
                participantCount = 2
            ),
            SplitBill(
                title = "Movie night",
                totalAmount = 40.00,
                billDate = "2026-10-10",
                status = "ACTIVE",
                participantCount = 3
            ),
            SplitBill(
                title = "Lunch at Kopitiam",
                totalAmount = 28.50,
                billDate = "2026-10-09",
                status = "SETTLED",
                participantCount = 2
            )
        )

        val youOwe = 25.50
        val owedToYou = 65.00
        val openSplits = mockBills.count { !it.status.equals("SETTLED", ignoreCase = true) }
        val settledAmount = mockBills.filter { it.status.equals("SETTLED", ignoreCase = true) }.sumOf { it.totalAmount }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .moneeBackground()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            MonthNavigationCard(
                currentMonth = "October 2026",
                onPrevMonth = { },
                onNextMonth = { },
                onMonthClick = { },
                youOwe = youOwe,
                owedToYou = owedToYou
            )
            Spacer(modifier = Modifier.height(20.dp))
            SettlementOverviewCard(
                openSplits = openSplits,
                settledAmount = settledAmount
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Split bill history",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            var selectedFilter by remember { mutableStateOf("ALL") }
            FilterTabsRow(
                selectedTab = selectedFilter,
                onTabSelected = { selectedFilter = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            val displayBills = when (selectedFilter) {
                "SETTLED" -> mockBills.filter { it.status.equals("SETTLED", ignoreCase = true) }
                "ACTIVE" -> mockBills.filter { !it.status.equals("SETTLED", ignoreCase = true) }
                else -> mockBills
            }
            displayBills.forEach { bill ->
                SplitBillItemRow(bill = bill, onClick = {}, onDelete = {})
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

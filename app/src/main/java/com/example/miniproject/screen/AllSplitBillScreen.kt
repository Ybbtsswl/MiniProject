package com.example.miniproject.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.miniproject.ui.theme.MoneeGreen
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.MoneeRed
import com.example.miniproject.ui.theme.MoneeYellow
import com.example.miniproject.ui.theme.moneeBackground
import com.example.miniproject.ui.theme.moneeFieldColors
import com.example.miniproject.viewmodel.SplitBillViewModel
import java.util.Locale

/**
 * Every split bill (all months), newest first, with a search box.
 * Tapping a bill opens its details; deleting stays on the Split bills screen.
 */
@Composable
fun AllSplitBillsScreen(
    userId: String,
    viewModel: SplitBillViewModel,
    onBillClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(userId) {
        viewModel.loadAllBills(userId)
    }

    val bills = uiState.bills   // every bill, filled by loadAllBills
    val results = remember(bills, query) {
        val q = query.trim()
        val matches = if (q.isEmpty()) {
            bills
        } else {
            bills.filter { b ->
                val statusLabel = if (b.status.equals("SETTLED", ignoreCase = true)) "Settled" else "Active"
                b.title.contains(q, ignoreCase = true) ||
                        b.billDate.contains(q) ||
                        statusLabel.contains(q, ignoreCase = true) ||
                        String.format(Locale.US, "%.2f", b.totalAmount).contains(q)
            }
        }
        matches.sortedByDescending { it.billDate }   // ISO dates sort correctly as text
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .moneeBackground()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MoneeInk
                    )
                }
                Text(
                    text = "All Split Bills",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoneeInk,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title, date, status or amount") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MoneeMuted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MoneeMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = moneeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MoneeYellow)
                    }
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading data",
                            color = MoneeRed,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                viewModel.loadAllBills(userId)
                            }
                        ) {
                            Text("Retry", color = MoneeGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                bills.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No split bills yet.", color = MoneeMuted, fontSize = 14.sp)
                    }
                }

                else -> {
                    Text(
                        text = "${results.size} bill${if (results.size == 1) "" else "s"} · " +
                                formatBillAmount(results.sumOf { it.totalAmount }),
                        fontSize = 12.sp,
                        color = MoneeMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No split bills match \"${query.trim()}\"",
                                color = MoneeMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(results) { bill ->
                                SplitBillItemRow(
                                    bill = bill,
                                    onClick = { onBillClick(bill.splitBillId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.tripexpensetracker.ui.settlement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.domain.Debt
import com.example.tripexpensetracker.ui.common.DebtGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: SettlementViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) {
        viewModel.calculateSettlements(tripId)
    }

    val debts by viewModel.debts.collectAsState()
    val personalBalances by viewModel.personalBalances.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    var showPaymentDialog by remember { mutableStateOf(false) }
    var payeeForDialog by remember { mutableStateOf<PersonalBalance?>(null) }

    if (showPaymentDialog && payeeForDialog != null) {
        RecordPaymentDialog(
            recipientName = payeeForDialog!!.otherPersonName,
            currencySymbol = currencySymbol,
            suggestedAmount = kotlin.math.abs(payeeForDialog!!.netAmount),
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount ->
                viewModel.recordPayment(payeeForDialog!!.otherPersonId, amount)
                showPaymentDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settlements") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        val summary = personalBalances.joinToString("\n") { balance ->
                             val label = if (balance.netAmount >= 0) "owes you" else "you owe"
                             "${balance.otherPersonName} $label: ${currencySymbol}${"%.2f".format(kotlin.math.abs(balance.netAmount))}"
                        }
                        if (summary.isNotBlank()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "My Trip Balances:\n$summary")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share My Balances")
                            context.startActivity(shareIntent)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (personalBalances.isEmpty() && debts.isEmpty()) {
                Text(
                    text = "No debts recorded!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (personalBalances.isNotEmpty()) {
                        item {
                            Text(
                                "Your Summary",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(personalBalances) { balance ->
                            PersonalBalanceItem(
                                balance = balance, 
                                currencySymbol = currencySymbol,
                                onSettleClick = {
                                    payeeForDialog = balance
                                    showPaymentDialog = true
                                }
                            )
                        }
                        item {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    item {
                        Text(
                            "Full Debt Graph",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                         Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                         ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val peopleMap = viewModel.peopleMap
                                DebtGraph(
                                    debts = debts,
                                    personNames = peopleMap,
                                    currencySymbol = currencySymbol,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                         }
                    }
                    
                    if (debts.isNotEmpty()) {
                        item {
                            Text(
                                "All Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(debts) { debt ->
                            DebtItem(debt, viewModel, currencySymbol)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalBalanceItem(
    balance: PersonalBalance, 
    currencySymbol: String,
    onSettleClick: () -> Unit
) {
    val amount = balance.netAmount
    val isPositive = amount > 0.01
    val isNegative = amount < -0.01
    val isSettled = !isPositive && !isNegative
    
    val absAmount = kotlin.math.abs(amount)
    val color = when {
        isPositive -> Color(0xFF2E7D32) // Dark Green
        isNegative -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant // Grey
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "You to ${balance.otherPersonName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            isPositive -> "They owe you"
                            isNegative -> "You owe them"
                            else -> "Settled up"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
                Text(
                    text = when {
                        isPositive -> "+${currencySymbol}${"%.2f".format(absAmount)}"
                        isNegative -> "-${currencySymbol}${"%.2f".format(absAmount)}"
                        else -> "${currencySymbol}${"%.2f".format(absAmount)}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = if (isSettled) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            // Show Settle button only if YOU OWE THEM
            if (isNegative) {
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSettleClick) {
                        Text("Settle", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItem(debt: Debt, viewModel: SettlementViewModel, currencySymbol: String) {
    val fromName = viewModel.getPersonName(debt.fromPersonId)
    val toName = viewModel.getPersonName(debt.toPersonId)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$fromName → $toName",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${currencySymbol}${"%.2f".format(debt.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

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
    val isLoading by viewModel.isLoading.collectAsState()

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
                        val summary = debts.joinToString("\n") { debt ->
                             "${viewModel.getPersonName(debt.fromPersonId)} owes ${viewModel.getPersonName(debt.toPersonId)}: $${"%.2f".format(debt.amount)}"
                        }
                        if (summary.isNotBlank()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Trip Settlements:\n$summary")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Settlements")
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
            } else if (debts.isEmpty()) {
                Text(
                    text = "No debts recorded!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                         Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                         ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Debt Graph", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                // Need people map for names
                                val peopleMap = viewModel.peopleMap // We need to expose this from VM
                                DebtGraph(
                                    debts = debts,
                                    personNames = peopleMap,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                         }
                    }
                    items(debts) { debt ->
                        DebtItem(debt, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItem(debt: Debt, viewModel: SettlementViewModel) {
    val fromName = viewModel.getPersonName(debt.fromPersonId)
    val toName = viewModel.getPersonName(debt.toPersonId)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$fromName owes $toName",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "$%.2f".format(debt.amount),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

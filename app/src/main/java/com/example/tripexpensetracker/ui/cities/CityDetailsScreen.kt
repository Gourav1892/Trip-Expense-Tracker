package com.example.tripexpensetracker.ui.cities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.data.model.TimelineItem
import com.example.tripexpensetracker.ui.common.ActivityTimelineCard
import com.example.tripexpensetracker.ui.common.ExpenseTimelineCard
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDetailsScreen(
    tripId: String,
    destinationId: String,
    onNavigateBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onAddActivity: (String) -> Unit,
    onEndCityVisit: () -> Unit = {},
    viewModel: CityDetailsViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    
    var showEndCityDialog by remember { mutableStateOf(false) }
    
    // End City Visit Confirmation Dialog
    if (showEndCityDialog) {
        com.example.tripexpensetracker.ui.common.EndCityVisitDialog(
            cityName = destination?.name ?: "this city",
            onConfirm = {
                showEndCityDialog = false
                viewModel.endCityVisit()
                onNavigateBack()
            },
            onDismiss = { showEndCityDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination?.name ?: "City") },
                navigationIcon = {
                    IconButton(onClick = { showEndCityDialog = true }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // End City Visit button
                    TextButton(
                        onClick = { showEndCityDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("End Visit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Activity FAB
                SmallFloatingActionButton(
                    onClick = { onAddActivity(destinationId) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Event, "Add Activity")
                }
                
                // Add Expense FAB (Primary)
                FloatingActionButton(
                    onClick = { onAddExpense(destinationId) }
                ) {
                    Icon(Icons.Default.AttachMoney, "Add Expense")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // City Summary Card
            item {
                CitySummaryCard(
                    stats = stats,
                    destination = destination,
                    currencySymbol = currencySymbol
                )
            }
            
            // Timeline Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Timeline",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${timelineItems.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (timelineItems.isEmpty()) {
                // Empty state
                item {
                    EmptyTimelineState(
                        onAddExpense = { onAddExpense(destinationId) },
                        onAddActivity = { onAddActivity(destinationId) }
                    )
                }
            } else {
                // Group items by date
                val itemsByDate = timelineItems.groupBy { 
                    viewModel.getDateString(it.timestamp)
                }
                
                itemsByDate.forEach { (date, items) ->
                    item {
                        Text(
                            date,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(items) { item ->
                        when (item) {
                            is TimelineItem.ExpenseItem -> 
                                ExpenseTimelineCard(expense = item.expense, currencySymbol = currencySymbol)
                            is TimelineItem.ActivityItem -> 
                                ActivityTimelineCard(activity = item.activity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CitySummaryCard(
    stats: com.example.tripexpensetracker.data.model.CityStats,
    destination: com.example.tripexpensetracker.data.model.Destination?,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Total Spending",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "${currencySymbol}${String.format("%.2f", stats.totalExpenses)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Text(
                    "🏙️",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "Expenses",
                    value = stats.expenseCount.toString(),
                    icon = "💸"
                )
                StatItem(
                    label = "Activities",
                    value = stats.activityCount.toString(),
                    icon = "📍"
                )
                if (stats.topCategory.isNotBlank()) {
                    StatItem(
                        label = "Top Category",
                        value = stats.topCategory,
                        icon = "📊"
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun EmptyTimelineState(
    onAddExpense: () -> Unit,
    onAddActivity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "🗓️",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            "No items yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Start planning by adding expenses or activities to this city",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddExpense) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
            OutlinedButton(onClick = onAddActivity) {
                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Activity")
            }
        }
    }
}

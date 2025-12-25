package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.Destination
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.model.Participant
import com.example.tripexpensetracker.data.model.ItineraryItem
import com.example.tripexpensetracker.ui.trips.TripDetailsViewModel
import com.example.tripexpensetracker.ui.common.ExpenseTimelineCard
import com.example.tripexpensetracker.ui.common.CityCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewContent(
    paddingValues: PaddingValues,
    trip: Trip?,
    expenses: List<Expense>,
    destinations: List<Destination>,
    itineraryItems: List<ItineraryItem>,
    activeCityId: String?,
    activeTripId: String?,
    activeCityName: String?,
    tripId: String,
    viewModel: TripDetailsViewModel,
    onNavigateToCityDetails: (String) -> Unit,
    onEditExpense: (String) -> Unit,
    onSwitchToAllExpenses: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onShowAddDestination: () -> Unit,
    onEditCity: (Destination) -> Unit,
    onDeleteCity: (Destination) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Trip Summary Card
        item {
            TripSummaryCard(
                expenses = expenses,
                destinations = destinations,
                budget = trip?.budget,
                budgetAlertThreshold = trip?.budgetAlertThreshold ?: 80.0,
                currencySymbol = trip?.currencySymbol ?: "₹"
            )
        }
        
        // Participants Section
        val currentTrip = trip
        if (currentTrip != null && currentTrip.participants.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Participants",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // Filter out declined participants
            val visibleParticipants = currentTrip.participants.filter { 
                it.status != Participant.STATUS_DECLINED 
            }
            
            items(visibleParticipants.size) { index ->
                val participant = visibleParticipants[index]
                val isPending = participant.status == Participant.STATUS_INVITED
                val isJoined = participant.status == Participant.STATUS_JOINED
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) 
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isPending) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = participant.name.ifBlank { participant.phoneNumber ?: "Unknown" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isPending) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (isPending) {
                            Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                Text("Pending", style = MaterialTheme.typography.labelSmall)
                            }
                        } else if (isJoined) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Joined",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        
        // Cities Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cities",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onShowAddDestination) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add City")
                }
            }
        }
        
        // City Cards
        if (destinations.isEmpty()) {
            item {
                EmptyCitiesState(onAddCity = onShowAddDestination)
            }
        } else {
            items(destinations.size) { index ->
                val destination = destinations[index]
                var cityStats by remember { mutableStateOf(com.example.tripexpensetracker.data.model.CityStats()) }
                
                LaunchedEffect(destination.id, expenses, itineraryItems) {
                    cityStats = viewModel.getCityStats(destination.id)
                }
                
                val isThisCityActive = activeCityId == destination.id
                val isAnotherCityActive = activeTripId == tripId && activeCityId != null && activeCityId != destination.id
                
                CityCard(
                    destination = destination,
                    stats = cityStats,
                    isActive = isThisCityActive,
                    isLocked = isAnotherCityActive,
                    activeCityName = activeCityName,
                    currencySymbol = trip?.currencySymbol ?: "₹",
                    onClick = {
                        when {
                            isThisCityActive -> {
                                // Already visiting this city, navigate to it
                                onNavigateToCityDetails(destination.id)
                            }
                            isAnotherCityActive -> {
                                // Show warning: need to end current visit first
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "End your visit to ${activeCityName ?: "another city"} first"
                                    )
                                }
                            }
                            else -> {
                                // Start visit and navigate
                                viewModel.startCityVisit(destination.id, destination.name)
                                onNavigateToCityDetails(destination.id)
                            }
                        }
                    },
                    onEditClick = { onEditCity(destination) },
                    onDeleteClick = { onDeleteCity(destination) }
                )
            }
        }
        
        // Expenses Overview Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Expenses",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "${expenses.size} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Inline expense items for Overview
        if (expenses.isEmpty()) {
            item {
                Text(
                    "No expenses yet. Tap + to add one!",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(expenses.take(5)) { expense -> // Show only top 5 in overview
                ExpenseTimelineCard(
                    expense = expense,
                    currencySymbol = trip?.currencySymbol ?: "₹",
                    onClick = { viewModel.selectExpense(expense) },
                    onEditClick = { onEditExpense(expense.id) },
                    onDeleteClick = { onDeleteExpense(expense) }
                )
            }
            if (expenses.size > 5) {
                item {
                     TextButton(
                         onClick = onSwitchToAllExpenses,
                         modifier = Modifier.fillMaxWidth().padding(8.dp)
                     ) {
                         Text("View All Expenses")
                     }
                }
            }
        }
    }
}

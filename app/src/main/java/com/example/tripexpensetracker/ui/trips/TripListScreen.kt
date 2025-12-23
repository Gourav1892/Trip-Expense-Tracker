package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.data.model.Trip
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun TripListScreen(
    onAddTripClick: () -> Unit,
    onProfileClick: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: TripListViewModel = hiltViewModel()
) {

    val trips by viewModel.trips.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    @OptIn(ExperimentalMaterialApi::class)
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { viewModel.refresh() })
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    
    if (showDeleteDialog && tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("Are you sure you want to delete '${tripToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip(tripToDelete!!)
                        showDeleteDialog = false
                        tripToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Trips") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),

                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }

            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTripClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).pullRefresh(pullRefreshState)) {
            if (trips.isEmpty()) {
                com.example.tripexpensetracker.ui.common.EmptyState(
                    icon = androidx.compose.material.icons.Icons.Default.Add, // Or a better icon like BeachAccess if available, using Add for now or generic
                    message = "No trips yet.\nTap the + button to create your first trip!"
                )

            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(trips, key = { it.id }) { trip ->
                        TripItem(
                            trip = trip, 
                            onClick = { onTripClick(trip.id) },
                            onDeleteClick = {
                                tripToDelete = trip
                                showDeleteDialog = true
                            },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }

                }
            }
            
            PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(androidx.compose.ui.Alignment.TopCenter))
        }
    }
}

@Composable
fun TripItem(trip: Trip, onClick: () -> Unit, onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = trip.name, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(
                    text = dateFormat.format(trip.startDate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

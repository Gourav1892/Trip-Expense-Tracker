package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications

import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
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
    onNotificationsClick: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: TripListViewModel = hiltViewModel()
) {

    val trips by viewModel.trips.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val invitationCount by viewModel.invitationCount.collectAsState()
    
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
                    IconButton(onClick = onNotificationsClick) {
                        if (invitationCount > 0) {
                            BadgedBox(badge = { Badge { Text(invitationCount.toString()) } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        } else {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
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
    val gradient = remember(trip.id) {
        val colors = listOf(
            listOf(Color(0xFF4CA1AF), Color(0xFFC4E0E5)), // Blue/Green
            listOf(Color(0xFFff9966), Color(0xFFff5e62)), // Orange/Red
            listOf(Color(0xFF00c6ff), Color(0xFF0072ff)), // Blue
            listOf(Color(0xFF11998e), Color(0xFF38ef7d)), // Green
            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Purple
        )
        // Pick a stable color based on trip ID hash
        val colorPair = colors[kotlin.math.abs(trip.id.hashCode()) % colors.size]
        Brush.linearGradient(
            colors = colorPair,
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f) // Approximate diagonal
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier.background(gradient)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header: Name and Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trip.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    blurRadius = 4f
                                )
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Started ${dateFormat.format(trip.startDate)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer: Budget Info
                if (trip.budget != null && trip.budget > 0) {
                   Column {
                       Row(
                           modifier = Modifier.fillMaxWidth(),
                           horizontalArrangement = Arrangement.SpaceBetween
                       ) {
                           Text(
                               text = "Budget",
                               style = MaterialTheme.typography.labelSmall,
                               color = Color.White.copy(alpha = 0.8f)
                           )
                           Text(
                               text = java.text.NumberFormat.getCurrencyInstance().format(trip.budget),
                               style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                               color = Color.White
                           )
                       }
                       Spacer(modifier = Modifier.height(6.dp))
                       LinearProgressIndicator(
                           progress = 0f, // Placeholder until joined data
                           modifier = Modifier.fillMaxWidth().height(4.dp),
                           color = Color.White,
                           trackColor = Color.White.copy(alpha = 0.3f)
                       )
                   }
                } else {
                     Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

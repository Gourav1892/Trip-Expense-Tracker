package com.example.tripexpensetracker.ui.trips

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.ui.common.PieChart
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onSettleClick: (String) -> Unit,
    onEditTripClick: (String) -> Unit,

    onNavigateToAnalytics: (String) -> Unit, // New callback
    onNavigateToCityDetails: (String) -> Unit = {}, // Navigate to city details
    viewModel: TripDetailsViewModel = hiltViewModel()
) {
    val trip by viewModel.trip.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val approved by viewModel.approvedParticipants.collectAsState()
    val pending by viewModel.pendingParticipants.collectAsState()
    val destinations by viewModel.destinations.collectAsState()
    val itineraryItems by viewModel.itineraryItems.collectAsState()
    val people by viewModel.people.collectAsState()
    
    // Active city tracking
    val activeCityId by viewModel.activeCityId.collectAsState()
    val activeCityName by viewModel.activeCityName.collectAsState()
    val activeTripId by viewModel.activeTripId.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Expenses", "Itinerary")

    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to delete '${expenseToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!)
                        showDeleteDialog = false
                        expenseToDelete = null
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

    if (showDeleteTripDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTripDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("Are you sure you want to delete '${trip?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip {
                            showDeleteTripDialog = false
                            onBackClick()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTripDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var showAddChoice by remember { mutableStateOf(false) }
    var showAddDestination by remember { mutableStateOf(false) }
    var showAddActivity by remember { mutableStateOf(false) }

    if (showAddChoice) {
        AlertDialog(
            onDismissRequest = { showAddChoice = false },
            title = { Text("Add to Itinerary") },
            text = { Text("What would you like to add?") },
            confirmButton = {
                TextButton(onClick = { 
                    showAddChoice = false
                    showAddDestination = true 
                }) { Text("Destination (City)") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddChoice = false
                    showAddActivity = true 
                }) { Text("Activity") }
            }
        )
    }

    if (showAddDestination) {
        AddDestinationDialog(
            onDismiss = { showAddDestination = false },
            onConfirm = { name, start, end ->
                viewModel.addDestination(com.example.tripexpensetracker.data.model.Destination(
                    name = name,
                    startDate = start,
                    endDate = end
                ))
                showAddDestination = false
            }
        )
    }

    if (showAddActivity) {
        AddActivityDialog(
            destinations = destinations,
            onDismiss = { showAddActivity = false },
            onConfirm = { title, desc, time, type ->
                viewModel.addItineraryItem(com.example.tripexpensetracker.data.model.ItineraryItem(
                    title = title,
                    description = desc,
                    startTime = time,
                    type = type
                ))
                showAddActivity = false
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(trip?.name ?: "Trip Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Join my trip on Trip Expense Tracker: tripexpensetracker://invite/$tripId")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite Friends")
                    }
                    IconButton(onClick = { onEditTripClick(tripId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip")
                    }
                    IconButton(onClick = { onNavigateToAnalytics(tripId) }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Analytics")
                    }
                    IconButton(onClick = { showDeleteTripDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Trip")
                    }
                    TextButton(onClick = { onSettleClick(tripId) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settle")
                    }
                    
                    IconButton(onClick = {
                         val csvData = viewModel.generateCsvExport()
                         val sendIntent = android.content.Intent().apply {
                             action = android.content.Intent.ACTION_SEND
                             putExtra(android.content.Intent.EXTRA_TEXT, csvData)
                             type = "text/csv"
                             putExtra(android.content.Intent.EXTRA_SUBJECT, "Trip Expenses: ${trip?.name}")
                         }
                         val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Expenses")
                         context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { onAddExpenseClick(tripId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            } else {
                 FloatingActionButton(onClick = { showAddChoice = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { paddingValues ->
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
                    budgetAlertThreshold = trip?.budgetAlertThreshold ?: 80.0
                )
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
                    TextButton(onClick = { showAddDestination = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add City")
                    }
                }
            }
            
            // City Cards
            if (destinations.isEmpty()) {
                item {
                    EmptyCitiesState(onAddCity = { showAddDestination = true })
                }
            } else {
                items(destinations.size) { index ->
                    val destination = destinations[index]
                    var cityStats by remember { mutableStateOf(com.example.tripexpensetracker.data.model.CityStats()) }
                    
                    LaunchedEffect(destination.id, expenses, itineraryItems) {
                        cityStats = viewModel.getCityStats(destination.id)
                    }
                    
                    val isThisCityActive = activeCityId == destination.id
                    val isAnotherCityActive = activeCityId != null && activeCityId != destination.id
                    
                    com.example.tripexpensetracker.ui.common.CityCard(
                        destination = destination,
                        stats = cityStats,
                        isActive = isThisCityActive,
                        isLocked = isAnotherCityActive,
                        activeCityName = activeCityName,
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
                                            "End your visit to $activeCityName first"
                                        )
                                    }
                                }
                                else -> {
                                    // Start visit and navigate
                                    viewModel.startCityVisit(destination.id, destination.name)
                                    onNavigateToCityDetails(destination.id)
                                }
                            }
                        }
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
            
            // Inline expense items (not nested scrollable)
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
                items(expenses.size) { index ->
                    val expense = expenses[index]
                    ExpenseItem(
                        expense = expense,
                        onDeleteClick = { 
                            expenseToDelete = expense
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesTabContent(
    expenses: List<Expense>,
    people: List<com.example.tripexpensetracker.data.model.Person>,
    approvedParticipants: List<com.example.tripexpensetracker.data.model.Participant>,
    pendingParticipants: List<com.example.tripexpensetracker.data.model.Participant>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onResendInvite: (com.example.tripexpensetracker.data.model.Participant) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    var showChart by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val total = expenses.sumOf { it.amount }
                    Text("Total Expenses", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(total),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
             OutlinedButton(
                onClick = { showChart = !showChart },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(if (showChart) "Hide Insights" else "Show Spending Insights")
            }

            AnimatedVisibility(visible = showChart) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text("Expenses by Category", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val chartData = expenses.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                        
                        if (chartData.isNotEmpty()) {
                            PieChart(
                                data = chartData,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("No expenses to show.")
                        }
                        
                        // Member Spending Section
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Expenses by Member", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        val memberSpending = expenses.groupBy { it.paidByPersonId }
                            .mapKeys { (personId, _) -> 
                                people.find { it.id == personId }?.name ?: "Unknown"
                            }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                        
                        if (memberSpending.isNotEmpty()) {
                            com.example.tripexpensetracker.ui.common.BarChart(
                                data = memberSpending,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("No member data available.")
                        }
                    }
                }
            }
        }

        item {
            // Participants Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                 Column(modifier = Modifier.padding(16.dp)) {
                     Text("Participants", style = MaterialTheme.typography.titleMedium)
                     Spacer(modifier = Modifier.height(8.dp))
                     
                     if (pendingParticipants.isNotEmpty()) {
                         Text("Pending Approval", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                         pendingParticipants.forEach { p ->
                             Row(
                                 modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                             ) {
                                 Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                 TextButton(onClick = { onResendInvite(p) }) {
                                     Text("Resend")
                                 }
                             }
                         }
                         Divider(modifier = Modifier.padding(vertical = 8.dp))
                     }
                     
                     if (approvedParticipants.isNotEmpty()) {
                         Text("Going", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                         approvedParticipants.forEach { p ->
                             Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                 Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text(p.name, style = MaterialTheme.typography.bodyMedium)
                             }
                         }
                     }
                 }
            }
        }

        item {
            Text(
                "Expenses",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
             val categories = listOf("All", "Food", "Transport", "Lodging", "Entertainment", "General")
             androidx.compose.foundation.lazy.LazyRow(
                 horizontalArrangement = Arrangement.spacedBy(8.dp),
                 modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
             ) {
                  items(categories.size) { index ->
                      val cat = categories[index]
                      FilterChip(
                          selected = selectedCategory == cat,
                          onClick = { onCategorySelected(cat) },
                          label = { Text(cat) }
                      )
                  }
             }
        }

        items(expenses) { expense ->
             ExpenseItem(
                 expense = expense,
                 onDeleteClick = { onDeleteExpense(expense) }
             )
        }
    }
}

@Composable
fun ItineraryTabContent(
    destinations: List<com.example.tripexpensetracker.data.model.Destination>,
    itineraryItems: List<com.example.tripexpensetracker.data.model.ItineraryItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (destinations.isEmpty() && itineraryItems.isEmpty()) {
             item {
                 Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                     Text("No itinerary items yet. Start planning!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 }
             }
        } else {
            item { Text("Destinations", style = MaterialTheme.typography.titleLarge) }
            items(destinations) { dest ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(dest.name, style = MaterialTheme.typography.titleMedium)
                        Text("Start: ${java.text.DateFormat.getDateInstance().format(java.util.Date(dest.startDate))}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            item { 
                Spacer(modifier = Modifier.height(16.dp))
                Text("Activities", style = MaterialTheme.typography.titleLarge) 
            }
            items(itineraryItems) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                     ListItem(
                         headlineContent = { Text(item.title) },
                         supportingContent = { Text(item.description) },
                         leadingContent = { 
                             val icon = when(item.type) {
                                 com.example.tripexpensetracker.data.model.ItineraryItem.Companion.TYPE_FLIGHT -> "✈️"
                                 else -> "📍"
                             }
                             Text(icon)
                         }
                     )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDeleteClick: () -> Unit) {
    ListItem(
        leadingContent = {
            com.example.tripexpensetracker.ui.common.CategoryIcon(category = expense.category)
        },
        headlineContent = { Text(expense.title) },
        supportingContent = { Text(java.text.DateFormat.getDateInstance().format(expense.date)) },
        trailingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = NumberFormat.getCurrencyInstance().format(expense.amount),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick) {
                     Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    )
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun TripSummaryCard(
    expenses: List<Expense>,
    destinations: List<com.example.tripexpensetracker.data.model.Destination>,
    budget: Double? = null,
    budgetAlertThreshold: Double = 80.0
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
            val totalSpent = expenses.sumOf { it.amount }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Total Trip Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        NumberFormat.getCurrencyInstance().format(totalSpent),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            if (budget != null && budget > 0) {
                 Spacer(modifier = Modifier.height(8.dp))
                 val progress = (totalSpent / budget).toFloat().coerceIn(0f, 1f)
                 val isOverBudget = totalSpent > budget

                 val isAlertZone = (progress * 100) >= budgetAlertThreshold

                 LinearProgressIndicator(
                     progress = progress,
                     modifier = Modifier.fillMaxWidth().height(8.dp),
                     color = when {
                         isOverBudget -> MaterialTheme.colorScheme.error
                         isAlertZone -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange for warning
                         else -> MaterialTheme.colorScheme.primary
                     },
                     trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                 )
                 
                 Spacer(modifier = Modifier.height(4.dp))
                 
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                     Text(
                         text = "${(progress * 100).toInt()}% used",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSecondaryContainer
                     )
                     Text(
                         text = "Budget: ${NumberFormat.getCurrencyInstance().format(budget)}",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSecondaryContainer
                     )
                 }
                 
                 if (isOverBudget) {
                      Text(
                         text = "Over Budget by ${NumberFormat.getCurrencyInstance().format(totalSpent - budget)}",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error
                     )
                 } else if (isAlertZone) {
                      Text(
                         text = "⚠️ You've used ${ (progress * 100).toInt()}% of your budget (Alert at $budgetAlertThreshold%)",
                         style = MaterialTheme.typography.labelSmall,
                         color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                     )
                 }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${destinations.size} ${if (destinations.size == 1) "city" else "cities"} • ${expenses.size} expenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EmptyCitiesState(onAddCity: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🗺️", style = MaterialTheme.typography.displayMedium)
        Text(
            "No cities added yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Start planning by adding cities to your trip",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onAddCity) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Your First City")
        }
    }
}

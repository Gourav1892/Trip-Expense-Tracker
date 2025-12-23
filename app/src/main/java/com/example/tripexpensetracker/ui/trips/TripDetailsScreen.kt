package com.example.tripexpensetracker.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.data.model.Expense
import java.text.NumberFormat
import androidx.compose.animation.AnimatedVisibility
import com.example.tripexpensetracker.ui.common.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onSettleClick: (String) -> Unit,
    onEditTripClick: (String) -> Unit,
    viewModel: TripDetailsViewModel = hiltViewModel()
) {
    // LaunchedEffect removed as ViewModel handles init via SavedStateHandle

    val trip by viewModel.trip.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
    var showDeleteTripDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Trip Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditTripClick(tripId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip")
                    }
                    IconButton(onClick = { showDeleteTripDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Trip")
                    }
                    TextButton(onClick = { onSettleClick(tripId) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddExpenseClick(tripId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
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

            // Insights Section
            var showChart by remember { mutableStateOf(false) }
            
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
                    }
                }
            }
            }

            Text(
                "Expenses",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Filter
            val categories = listOf("All", "Food", "Transport", "Lodging", "Entertainment", "General")
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                 items(categories.size) { index ->
                     val cat = categories[index]
                     FilterChip(
                         selected = selectedCategory == cat,
                         onClick = { viewModel.onCategorySelected(cat) },
                         label = { Text(cat) }
                     )
                 }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(expenses) { expense ->
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

@Composable
fun ExpenseItem(expense: Expense, onDeleteClick: () -> Unit) {
    val emoji = when (expense.category) {
        "Food" -> "🍔"
        "Transport" -> "🚗"
        "Lodging" -> "🏨"
        "Entertainment" -> "🎬"
        else -> "📦"
    }
    
    ListItem(
        leadingContent = {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )
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

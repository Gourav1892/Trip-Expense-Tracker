package com.example.tripexpensetracker.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripexpensetracker.ui.common.PieChart
import com.example.tripexpensetracker.ui.common.LineChart
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val timeRange by viewModel.timeRange.collectAsState()
    val categoryData by viewModel.categoryData.collectAsState()
    val dailyTrend by viewModel.dailyTrend.collectAsState()
    val cityData by viewModel.cityData.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Date Wise", "City Wise")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Simple Toggle
                    TextButton(onClick = { 
                        val newRange = if (timeRange == AnalyticsViewModel.TimeRange.ALL_TIME) 
                            AnalyticsViewModel.TimeRange.LAST_7_DAYS 
                        else 
                            AnalyticsViewModel.TimeRange.ALL_TIME
                        viewModel.setTimeRange(newRange)
                    }) {
                        Text(if (timeRange == AnalyticsViewModel.TimeRange.ALL_TIME) "All Time" else "Last 7 Days")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Spent", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${currencySymbol}${String.format("%.2f", totalSpent)}",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> { // Overview (Category)
                    if (categoryData.isNotEmpty()) {
                        Text("Expenses by Category", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(
                            data = categoryData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    } else {
                         Text("No category data", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                1 -> { // Date Wise
                    if (dailyTrend.isNotEmpty()) {
                        Text("Daily Trend", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        LineChart(
                            data = dailyTrend,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("No data available for this range", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                2 -> { // City Wise
                    if (cityData.isNotEmpty()) {
                        Text("Expenses by City", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(
                            data = cityData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                        // List breakdown
                        Spacer(modifier = Modifier.height(16.dp))
                        cityData.forEach { (city, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(city, style = MaterialTheme.typography.bodyMedium)
                                Text("${currencySymbol}${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text("No city data available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

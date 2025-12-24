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
    val totalSpent by viewModel.totalSpent.collectAsState()

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
                        NumberFormat.getCurrencyInstance().format(totalSpent),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Chart
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
                 Text("No data to display", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Daily Trend (Simple List for now as LineChart is complex to build from scratch without canvas utils)
            // Or we can try a simple BarChart style for daily trend
            if (dailyTrend.isNotEmpty()) {
                Text("Daily Trend", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                LineChart(
                    data = dailyTrend,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

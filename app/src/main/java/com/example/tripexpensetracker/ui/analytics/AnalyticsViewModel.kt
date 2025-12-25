package com.example.tripexpensetracker.ui.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    
    private val _currencySymbol = kotlinx.coroutines.flow.MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    init {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
             _currencySymbol.value = trip?.currencySymbol ?: "₹"
        }
    }

    private val allExpenses = repository.getExpensesForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val destinations = repository.getDestinationsFlow(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date Range Filter
    private val _timeRange = kotlinx.coroutines.flow.MutableStateFlow(TimeRange.ALL_TIME)
    val timeRange: StateFlow<TimeRange> = _timeRange

    val filteredExpenses = kotlinx.coroutines.flow.combine(allExpenses, _timeRange) { expenses, range ->
        if (range == TimeRange.ALL_TIME) {
            expenses
        } else {
            val cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
            expenses.filter { it.date.time >= cutoff }
        }
    }

    val categoryData = filteredExpenses.map { expenses ->
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Daily Trend Data: Map<DateString, TotalAmount>
    val dailyTrend = filteredExpenses.map { expenses ->
        expenses.groupBy { 
            // Simple date formatting for grouping: yyyy-MM-dd
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it.date)
        }.mapValues { entry -> entry.value.sumOf { it.amount } }
         .toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // City Wise Data: Map<CityName, TotalAmount>
    val cityData = kotlinx.coroutines.flow.combine(filteredExpenses, destinations) { expenses, dests ->
        expenses.groupBy { it.destinationId }
            .mapKeys { entry -> 
                dests.find { it.id == entry.key }?.name ?: "Unknown City"
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    // Total Spent
    val totalSpent = filteredExpenses.map { it.sumOf { exp -> exp.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    enum class TimeRange {
        ALL_TIME, LAST_7_DAYS
    }
}

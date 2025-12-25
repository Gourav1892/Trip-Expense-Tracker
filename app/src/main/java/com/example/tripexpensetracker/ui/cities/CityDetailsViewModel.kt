package com.example.tripexpensetracker.ui.cities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.CityStats
import com.example.tripexpensetracker.data.model.Destination
import com.example.tripexpensetracker.data.model.TimelineItem
import com.example.tripexpensetracker.data.repository.TripRepository
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.ExpenseShare
import com.example.tripexpensetracker.data.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityDetailsViewModel @Inject constructor(
    private val repository: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val tripId: String = savedStateHandle["tripId"] ?: ""
    private val destinationId: String = savedStateHandle["destinationId"] ?: ""
    
    /**
     * End the current city visit session
     */
    fun endCityVisit() {
        viewModelScope.launch {
            // Auto-update end date to NOW
            val currentCity = destination.value
            if (currentCity != null) {
                repository.updateDestination(tripId, currentCity.copy(endDate = System.currentTimeMillis()))
            }
            repository.clearActiveDestination(tripId)
        }
    }
    
    // Current destination/city
    val destination = repository.getDestinationsFlow(tripId)
        .map { destinations -> destinations.find { it.id == destinationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Unified timeline of expenses and activities
    val timelineItems = repository.getCityTimelineItems(tripId, destinationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // City statistics
    private val _stats = MutableStateFlow(CityStats())
    val stats: StateFlow<CityStats> = _stats
    
    // People in Trip (for mapping names)
    // People in Trip (for mapping names)
    val people = repository.getPeopleForTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Expense for Details
    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense

    private val _selectedExpenseShares = MutableStateFlow<List<ExpenseShare>>(emptyList())
    val selectedExpenseShares: StateFlow<List<ExpenseShare>> = _selectedExpenseShares

    fun selectExpense(expense: Expense) {
        _selectedExpense.value = expense
        viewModelScope.launch {
            _selectedExpenseShares.value = repository.getSharesForExpense(tripId, expense.id)
        }
    }

    fun dismissExpenseDetails() {
        _selectedExpense.value = null
        _selectedExpenseShares.value = emptyList()
    }
    
    // Currency Support
    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol
    
    init {
        // Calculate stats whenever timeline changes
        viewModelScope.launch {
            timelineItems.collect { items ->
                _stats.value = repository.getCityStats(tripId, destinationId)
            }
        }
        
        // Load currency
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
            _currencySymbol.value = trip?.currencySymbol ?: "₹"
        }
    }
    
    fun getTimeString(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun getDateString(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }
}

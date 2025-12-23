package com.example.tripexpensetracker.ui.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    private val repository: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    
    val trip: StateFlow<Trip?> = repository.getAllTrips() // This is suboptimal, should get single flow from ID
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .let { tripsFlow ->
             // A quick hack to reuse existing flow or we should add getTripByIdFlow to repository
             // Better: just load it once manually or add flow support for single trip in DAO
             // For now, let's keep the manual load pattern but safe
             val flow = kotlinx.coroutines.flow.MutableStateFlow<Trip?>(null)
             viewModelScope.launch {
                 flow.value = repository.getTripById(tripId)
             }
             flow
        }

    // Safely initialized flow
    private val _selectedCategory = kotlinx.coroutines.flow.MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val expenses: StateFlow<List<Expense>> = repository.getExpensesForTrip(tripId)
        .combine(_selectedCategory) { expenses: List<Expense>, category: String ->
            if (category == "All") {
                expenses
            } else {
                expenses.filter { it.category == category }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteTrip(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentTrip = trip.value
            if (currentTrip != null) {
                repository.deleteTrip(currentTrip)
                onSuccess()
            }
        }
    }
}

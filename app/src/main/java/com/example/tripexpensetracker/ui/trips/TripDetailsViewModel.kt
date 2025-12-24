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
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    private val repository: TripRepository,
    private val activeCityManager: com.example.tripexpensetracker.data.repository.ActiveCityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _tripId: String = checkNotNull(savedStateHandle["tripId"])
    
    // Active city tracking
    val activeCityId = activeCityManager.activeCityId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeCityName = activeCityManager.activeCityName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeTripId = activeCityManager.activeTripId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    fun startCityVisit(cityId: String, cityName: String) {
        viewModelScope.launch {
            activeCityManager.startCityVisit(_tripId, cityId, cityName)
        }
    }
    
    fun endCityVisit() {
        viewModelScope.launch {
            activeCityManager.endCityVisit()
        }
    }
    
    // Reactive flow for the specific trip
    val trip: StateFlow<Trip?> = repository.getAllTrips()
        .map { trips -> trips.find { it.id == _tripId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val approvedParticipants = trip.map { it?.participants?.filter { p -> 
        p.status == com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED 
    } ?: emptyList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingParticipants = trip.map { it?.participants?.filter { p -> 
        p.status == com.example.tripexpensetracker.data.model.Participant.STATUS_INVITED 
    } ?: emptyList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Safely initialized flow
    private val _selectedCategory = kotlinx.coroutines.flow.MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // New Itinerary Streams
    val destinations = repository.getDestinationsFlow(_tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val itineraryItems = repository.getItineraryItemsFlow(_tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val people = repository.getPeopleForTrip(_tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.getExpensesForTrip(_tripId)
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

    fun addDestination(destination: com.example.tripexpensetracker.data.model.Destination) {
        viewModelScope.launch {
            repository.addDestination(_tripId, destination)
        }
    }

    fun addItineraryItem(item: com.example.tripexpensetracker.data.model.ItineraryItem) {
        viewModelScope.launch {
            repository.addItineraryItem(_tripId, item)
        }
    }

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
    
    fun resendInvite(participant: com.example.tripexpensetracker.data.model.Participant) {
        viewModelScope.launch {
            val currentTrip = trip.value ?: return@launch
            // TODO: Implement resend invitation logic
            // This would require adding the function to TripRepository
            android.util.Log.d("TripDetails", "Resend invite requested for ${participant.name}")
        }
    }
    
    /**
     * Get statistics for a specific city/destination
     */
    suspend fun getCityStats(destinationId: String): com.example.tripexpensetracker.data.model.CityStats {
        return repository.getCityStats(_tripId, destinationId)
    }
}

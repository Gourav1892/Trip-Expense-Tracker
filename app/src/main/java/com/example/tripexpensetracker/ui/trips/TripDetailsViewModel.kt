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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    private val repository: TripRepository,
    private val userRepository: com.example.tripexpensetracker.data.repository.UserRepository,
    private val activeCityManager: com.example.tripexpensetracker.data.repository.ActiveCityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _tripId: String = checkNotNull(savedStateHandle["tripId"])
    
    // Reactive flow for the specific trip
    val trip: StateFlow<Trip?> = repository.getAllTrips()
        .map { trips -> trips.find { it.id == _tripId } }
        .flatMapLatest { trip ->
            if (trip == null) kotlinx.coroutines.flow.flowOf(null)
            else {
                val uids = trip.participants.mapNotNull { it.userId }
                userRepository.getUsersFlow(uids).map { userMap ->
                    val hydratedParticipants = trip.participants.map { p ->
                        val user = userMap[p.userId]
                        if (user != null && !user.displayName.isNullOrBlank()) {
                            p.copy(name = user.displayName!!)
                        } else {
                            p
                        }
                    }
                    trip.copy(participants = hydratedParticipants)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active city tracking
    // Derived from the shared Trip object
    val activeCityId = trip.map { it?.activeDestinationId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeCityName = combine(activeCityId, repository.getDestinationsFlow(_tripId)) { activeId, dests ->
        dests.find { it.id == activeId }?.name
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTripId = trip.map { if (it?.activeDestinationId != null) it.id else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    fun startCityVisit(cityId: String, cityName: String) {
        viewModelScope.launch {
            // Auto-update start date to NOW
            val currentDests = destinations.value
            val targetCity = currentDests.find { it.id == cityId }
            if (targetCity != null) {
                repository.updateDestination(_tripId, targetCity.copy(startDate = System.currentTimeMillis()))
            }
            repository.setActiveDestination(_tripId, cityId)
        }
    }
    
    fun endCityVisit() {
        viewModelScope.launch {
            // Auto-update end date to NOW
            val activeId = activeCityId.value
            if (activeId != null) {
                 val currentDests = destinations.value
                 val activeCity = currentDests.find { it.id == activeId }
                 if (activeCity != null) {
                      repository.updateDestination(_tripId, activeCity.copy(endDate = System.currentTimeMillis()))
                 }
            }
            repository.clearActiveDestination(_tripId)
        }
    }
    

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

    // Selected Expense for Details
    private val _selectedExpense = kotlinx.coroutines.flow.MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense

    private val _selectedExpenseShares = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.tripexpensetracker.data.model.ExpenseShare>>(emptyList())
    val selectedExpenseShares: StateFlow<List<com.example.tripexpensetracker.data.model.ExpenseShare>> = _selectedExpenseShares

    fun selectExpense(expense: Expense) {
        _selectedExpense.value = expense
        viewModelScope.launch {
            _selectedExpenseShares.value = repository.getSharesForExpense(expense.tripId, expense.id)
        }
    }

    fun dismissExpenseDetails() {
        _selectedExpense.value = null
        _selectedExpenseShares.value = emptyList()
    }

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

    fun updateDestination(destination: com.example.tripexpensetracker.data.model.Destination) {
        viewModelScope.launch {
            repository.updateDestination(_tripId, destination)
        }
    }

    fun deleteDestination(destinationId: String) {
        viewModelScope.launch {
            repository.deleteDestination(_tripId, destinationId)
            // If deleting active city, end visit
            if (activeCityId.value == destinationId) {
                endCityVisit()
            }
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
            // Call repository to resend invitation
            // Note: inviteeId is the userId of the participant
             if (participant.userId != null) {
                repository.resendInvitation(currentTrip.id, currentTrip.name, participant.userId)
                android.util.Log.d("TripDetails", "Resent invite to ${participant.name}")
            } else {
                 android.util.Log.e("TripDetails", "Cannot resend invite: Participant userId is null")
            }
        }
    }
    
    /**
     * Get statistics for a specific city/destination
     */
    suspend fun getCityStats(destinationId: String): com.example.tripexpensetracker.data.model.CityStats {
        return repository.getCityStats(_tripId, destinationId)
    }

    fun updateBudget(amount: Double) {
        viewModelScope.launch {
            repository.updateTripBudget(_tripId, amount)
        }
    }

    fun generateCsvExport(): String {
        val currentTrip = trip.value ?: return ""
        val currentExpenses = expenses.value

        val sb = StringBuilder()
        sb.append("Date,Title,Category,Amount,Payer\n")
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        currentExpenses.forEach { expense ->
            val date = dateFormat.format(expense.date)
            // Escape commas in title
            val title = expense.title.replace(",", " ")
            val category = expense.category
            val amount = expense.amount
            
            // Resolve payer name
            // We need the people list. It's in 'people' flow.
            // Accessing current value of flow roughly
            val payerName = people.value.find { it.id == expense.paidByPersonId }?.name ?: "Unknown"
            
            sb.append("$date,$title,$category,$amount,$payerName\n")
        }
        
        return sb.toString()
    }
}

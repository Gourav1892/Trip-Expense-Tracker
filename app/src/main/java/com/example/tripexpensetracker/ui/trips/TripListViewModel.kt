package com.example.tripexpensetracker.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.example.tripexpensetracker.data.repository.AuthRepository

@HiltViewModel
class TripListViewModel @Inject constructor(
    private val repository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    fun signOut() {
        authRepository.signOut()
    }

    private val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

    val trips: StateFlow<List<Trip>> = kotlinx.coroutines.flow.flow {
        // Emit trigger immediately to start the flow
        refreshTrigger.trySend(Unit)
        for (trigger in refreshTrigger) {
            emit(repository.getAllTrips())
        }
    }
    .flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshTrigger.send(Unit)
            // Minimum spinner duration for UX
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }
        
    init {
        viewModelScope.launch {
            trips.collect { tripList ->
                tripList.forEach { trip ->
                    repository.subscribeToTripTopic(trip.id)
                }
            }
        }
    }

    fun createTrip(name: String) {
        viewModelScope.launch {
            val newTrip = Trip(
                name = name,
                startDate = Date()
            )
            repository.insertTrip(newTrip)
        }
    }
    
    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }
}

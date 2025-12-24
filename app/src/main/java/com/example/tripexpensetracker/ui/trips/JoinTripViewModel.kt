package com.example.tripexpensetracker.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinTripViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JoinUiState>(JoinUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _tripDetails = MutableStateFlow<Trip?>(null)
    val tripDetails = _tripDetails.asStateFlow()

    fun loadTripDetails(tripId: String) {
        viewModelScope.launch {
            _uiState.value = JoinUiState.Loading
            try {
                val trip = repository.getTripById(tripId)
                if (trip != null) {
                    _tripDetails.value = trip
                    _uiState.value = JoinUiState.Loaded(trip)
                } else {
                    _uiState.value = JoinUiState.Error("Trip not found")
                }
            } catch (e: Exception) {
                _uiState.value = JoinUiState.Error("Failed to load trip")
            }
        }
    }

    fun joinTrip(tripId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = JoinUiState.Joining
            try {
                repository.joinTrip(tripId)
                _uiState.value = JoinUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = JoinUiState.Error("Failed to join trip")
            }
        }
    }

    fun reset() {
        _uiState.value = JoinUiState.Idle
        _tripDetails.value = null
    }

    sealed class JoinUiState {
        object Idle : JoinUiState()
        object Loading : JoinUiState()
        data class Loaded(val trip: Trip) : JoinUiState()
        object Joining : JoinUiState()
        object Success : JoinUiState()
        data class Error(val message: String) : JoinUiState()
    }
}

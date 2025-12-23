package com.example.tripexpensetracker.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Person
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.model.Participant
import com.example.tripexpensetracker.data.repository.TripRepository
import com.example.tripexpensetracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class AddEditTripViewModel @Inject constructor(
    private val repository: TripRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    var tripId: String? = savedStateHandle["tripId"]
    // If tripId is "null" (string literal) or empty, treat as new trip
    init {
        if (tripId == "null") tripId = null
        if (tripId != null) {
            loadTrip(tripId!!)
        }
    }

    private val _tripName = kotlinx.coroutines.flow.MutableStateFlow("")
    val tripName = _tripName.asStateFlow()

    private val _participants = kotlinx.coroutines.flow.MutableStateFlow<List<Participant>>(emptyList())
    val participants = _participants.asStateFlow()
    
    fun onTripNameChanged(name: String) { _tripName.value = name }
    fun onAddParticipant(participant: Participant) { _participants.value += participant }
    fun onRemoveParticipant(participant: Participant) { _participants.value -= participant }
    fun onUpdateParticipant(index: Int, participant: Participant) {
         val list = _participants.value.toMutableList()
         if (index in list.indices) {
             list[index] = participant
             _participants.value = list
         }
    }

    private fun loadTrip(id: String) {
        viewModelScope.launch {
            val trip = repository.getTripById(id)
            if (trip != null) {
                _tripName.value = trip.name
                _participants.value = trip.participants
            }
        }
    }

    fun checkUserRegistration(participant: Participant, onResult: (Participant) -> Unit) {
        viewModelScope.launch {
            if (participant.phoneNumber != null) {
                val users = userRepository.getUsersByPhones(listOf(participant.phoneNumber))
                val user = users.find { it["phone"] == participant.phoneNumber }
                if (user != null) {
                    onResult(participant.copy(userId = user["uid"] as? String))
                } else {
                    onResult(participant)
                }
            } else {
                onResult(participant)
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        object Success : UiState()
    }

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun saveTrip(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val currentParticipants = _participants.value
                val currentName = _tripName.value
                
                // 1. Identify which participants have phone numbers but no userId
                val participantsWithPhones = currentParticipants.filter { !it.phoneNumber.isNullOrBlank() && it.userId == null }
                val phones = participantsWithPhones.mapNotNull { it.phoneNumber }
                
                // 2. Lookup users by phone
                val registeredUsers = if (phones.isNotEmpty()) {
                    userRepository.getUsersByPhones(phones)
                } else {
                    emptyList()
                }
                
                // 3. Update participants list with found userIds
                val updatedParticipants = currentParticipants.map { p ->
                    if (p.phoneNumber != null) {
                        val user = registeredUsers.find { it["phone"] == p.phoneNumber }
                        if (user != null) {
                            p.copy(userId = user["uid"] as? String)
                        } else {
                            p
                        }
                    } else {
                        p
                    }
                }
                
                // 4. Create or Update Trip
                if (tripId != null) {
                     val existingTrip = repository.getTripById(tripId!!)
                     if (existingTrip != null) {
                         val trip = existingTrip.copy(name = currentName, participants = updatedParticipants)
                         repository.updateTrip(trip)
                         
                         val existingPeople = repository.getPeopleForTrip(tripId!!).first()
                         val existingNames = existingPeople.map { it.name }.toSet()
                         
                         updatedParticipants.forEach { p ->
                             if (!existingNames.contains(p.name)) {
                                  repository.insertPerson(Person(
                                    tripId = tripId!!, 
                                    name = p.name,
                                    userId = p.userId,
                                    phoneNumber = p.phoneNumber
                                ))
                             }
                         }
                     }
                } else {
                    // INSERT NEW
                    val trip = Trip(name = currentName, participants = updatedParticipants)
                    val newTripId = repository.insertTrip(trip)
                    
                    updatedParticipants.forEach { participant ->
                        repository.insertPerson(Person(
                            tripId = newTripId, 
                            name = participant.name,
                            userId = participant.userId,
                            phoneNumber = participant.phoneNumber
                        ))
                    }
                }
                _uiState.value = UiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}

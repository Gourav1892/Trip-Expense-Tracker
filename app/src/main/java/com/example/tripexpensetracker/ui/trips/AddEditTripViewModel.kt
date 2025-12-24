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
    private val friendRepository: com.example.tripexpensetracker.data.repository.FriendRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    var tripId: String? = savedStateHandle["tripId"]
    // If tripId is "null" (string literal) or empty, treat as new trip
    init {
        if (tripId == "null") tripId = null
        if (tripId != null) {
            loadTrip(tripId!!)
        }
        loadFriends()
    }

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _friends = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.tripexpensetracker.data.model.Friend>>(emptyList())
    // Raw friends list is not exposed directly for search anymore, we use filteredFriends

    private val _filteredFriends = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.tripexpensetracker.data.model.Friend>>(emptyList())
    val filteredFriends = _filteredFriends.asStateFlow()

    private fun loadFriends() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            friendRepository.getFriends(userId).collect {
                _friends.value = it
                 // Initial filter (show all or none? Let's show all initally or when query is empty)
                filterFriends(_searchQuery.value)
            }
        }
    }

    // User Search Logic
    private val _userSearchResults = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.tripexpensetracker.data.model.User>>(emptyList())
    val userSearchResults = _userSearchResults.asStateFlow()

    private val _isSearching = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterFriends(query)
        searchGlobalUsers(query)
    }

    private fun filterFriends(query: String) {
        if (query.isBlank()) {
            _filteredFriends.value = _friends.value
        } else {
            _filteredFriends.value = _friends.value.filter {
                it.name.contains(query, ignoreCase = true) || 
                (it.phoneNumber != null && it.phoneNumber.contains(query))
            }
        }
    }

    private fun searchGlobalUsers(query: String) {
        viewModelScope.launch {
            if (query.length >= 3) {
                _isSearching.value = true
                try {
                    val results = userRepository.searchUsers(query)
                    // Filter out users who are already friends to avoid duplicates in UI if we wanted, 
                    // but for now let's just show them. Ideally we deduplicate.
                    // Let's remove results that are already in the friend list (linkedUserId)
                    val friendUserIds = _friends.value.mapNotNull { it.linkedUserId }.toSet()
                    _userSearchResults.value = results.filter { !friendUserIds.contains(it.uid) }
                } catch (e: Exception) {
                    _userSearchResults.value = emptyList()
                } finally {
                    _isSearching.value = false
                }
            } else {
                _userSearchResults.value = emptyList()
            }
        }
    }

    // Suggested Contacts (from Device)
    private val _suggestedContacts = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.tripexpensetracker.data.model.User>>(emptyList())
    val suggestedContacts = _suggestedContacts.asStateFlow()

    private val _isMatchingContacts = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isMatchingContacts = _isMatchingContacts.asStateFlow()

    fun matchContacts(contacts: List<com.example.tripexpensetracker.ui.common.ContactData>) {
        viewModelScope.launch {
            _isMatchingContacts.value = true
            try {
                val phones = contacts.map { it.phoneNumber }
                if (phones.isNotEmpty()) {
                    val matches = userRepository.getUsersByPhones(phones)
                    val currentFriendIds = _friends.value.mapNotNull { it.linkedUserId }.toSet()
                    _suggestedContacts.value = matches.filter { !currentFriendIds.contains(it.uid) }
                } else {
                    _suggestedContacts.value = emptyList()
                }
            } catch (e: Exception) {
                // Log error
            } finally {
                _isMatchingContacts.value = false
            }
        }
    }

    fun onAddFriend(friend: com.example.tripexpensetracker.data.model.Friend) {
        val newParticipant = Participant(
            name = friend.name,
            userId = friend.linkedUserId,
            phoneNumber = friend.phoneNumber,
            status = Participant.STATUS_INVITED
        )
        addParticipantIfNotExists(newParticipant)
    }

    fun onAddUser(user: com.example.tripexpensetracker.data.model.User) {
        val newParticipant = Participant(
            name = user.displayName ?: user.phone,
            userId = user.uid,
            phoneNumber = user.phone,
            status = Participant.STATUS_INVITED
        )
        addParticipantIfNotExists(newParticipant)
    }

    fun onAddManualParticipant(name: String, phone: String?) {
        val newParticipant = Participant(
            name = name.trim(),
            phoneNumber = phone?.ifBlank { null }
        )
        addParticipantIfNotExists(newParticipant)
    }

    private val _tripName = kotlinx.coroutines.flow.MutableStateFlow("")
    val tripName = _tripName.asStateFlow()

    private val _budget = kotlinx.coroutines.flow.MutableStateFlow("")
    val budget = _budget.asStateFlow()

    private val _alertThreshold = kotlinx.coroutines.flow.MutableStateFlow(80f)
    val alertThreshold = _alertThreshold.asStateFlow()

    private val _participants = kotlinx.coroutines.flow.MutableStateFlow<List<Participant>>(emptyList())
    val participants = _participants.asStateFlow()
    
    fun onTripNameChanged(name: String) { _tripName.value = name }

    fun onBudgetChanged(amount: String) { _budget.value = amount }
    fun onAlertThresholdChanged(value: Float) { _alertThreshold.value = value }
    fun onAddParticipant(participant: Participant) { _participants.value += participant }
    fun onRemoveParticipant(participant: Participant) { _participants.value -= participant }
    fun onUpdateParticipant(index: Int, participant: Participant) {
         val list = _participants.value.toMutableList()
         if (index in list.indices) {
             list[index] = participant
             _participants.value = list
         }
    }

    private fun addParticipantIfNotExists(participant: Participant) {
        // Avoid duplicates checking name+phone OR userId
        val exists = _participants.value.any { 
            (it.userId != null && it.userId == participant.userId) || 
            (it.name.equals(participant.name, ignoreCase = true) && it.phoneNumber == participant.phoneNumber) 
        }
        if (!exists) {
            _participants.value += participant
        }
    }

    private fun loadTrip(id: String) {
        viewModelScope.launch {
            val trip = repository.getTripById(id)
            if (trip != null) {
                _tripName.value = trip.name
                _participants.value = trip.participants
                _budget.value = trip.budget?.toString() ?: ""
                _alertThreshold.value = trip.budgetAlertThreshold?.toFloat() ?: 80f
            }
        }
    }

    fun checkUserRegistration(participant: Participant, onResult: (Participant) -> Unit) {
        viewModelScope.launch {
            if (participant.phoneNumber != null) {
                val users = userRepository.getUsersByPhones(listOf(participant.phoneNumber))
                val user = users.find { it.phone == participant.phoneNumber }
                if (user != null) {
                    onResult(participant.copy(userId = user.uid))
                } else {
                    onResult(participant)
                }
            } else {
                onResult(participant)
            }
        }
    }

    fun onSaveUserAsFriend(user: com.example.tripexpensetracker.data.model.User) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val friend = com.example.tripexpensetracker.data.model.Friend(
                ownerId = userId,
                name = user.displayName ?: "Unknown",
                phoneNumber = user.phone,
                linkedUserId = user.uid
            )
            friendRepository.addFriend(userId, friend)
            loadFriends() // Refresh friends list
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
                val currentParticipants: List<Participant> = _participants.value
                val currentName = _tripName.value
                
                // 1. Identify which participants have phone numbers but no userId
                val participantsWithPhones: List<Participant> = currentParticipants.filter { p -> !p.phoneNumber.isNullOrBlank() && p.userId == null }
                val phones = participantsWithPhones.mapNotNull { p -> p.phoneNumber }
                
                // 2. Lookup users by phone
                val registeredUsers = if (phones.isNotEmpty()) {
                    userRepository.getUsersByPhones(phones)
                } else {
                    emptyList()
                }
                
                // 3. Update participants list with found userIds
                // 3. Update participants list with found userIds
                // AND Set Status
                val currentCreatorId = auth.currentUser?.uid
                
                val updatedParticipants: List<Participant> = currentParticipants.map { p: Participant ->
                    var updatedP = p
                    if (p.phoneNumber != null) {
                        val user = registeredUsers.find { it.phone == p.phoneNumber }
                        if (user != null) {
                            updatedP = p.copy(userId = user.uid)
                        }
                    }
                    
                    // Set Status
                    // If it's me (creator), JOINED.
                    // If it's another user with ID, INVITED (unless already joined/declined? For new additions, INVITED)
                    // If it's manual (no ID), JOINED (implicitly managed by creator)
                    
                    if (updatedP.userId == currentCreatorId) {
                         updatedP.copy(status = Participant.STATUS_JOINED)
                    } else if (updatedP.userId != null) {
                        // If it's a new trip or they were not previously in it, set to INVITED.
                        // Ideally we check if they were already there.
                        // For simplicity in this step: If status is JOINED, keep it. If default/new, set INVITED.
                         if (updatedP.status == Participant.STATUS_JOINED) {
                             updatedP
                         } else {
                             updatedP.copy(status = Participant.STATUS_INVITED)
                         }
                    } else {
                        updatedP // Manual entry, status JOINED by default or irrelevant
                    }
                }
                
                // 3.5 Prepare Participant IDs list
                // Only include JOINED participants in the queryable IDs list.
                // INVITED participants should not see the trip in "My Trips" until they accept.
                val participantIds = updatedParticipants
                    .filter { it.status == Participant.STATUS_JOINED }
                    .mapNotNull { it.userId }
                    .toMutableList()
                
                if (currentCreatorId != null && !participantIds.contains(currentCreatorId)) {
                    participantIds.add(currentCreatorId)
                }

                // 4. Create or Update Trip
                val finalTripId: String
                if (tripId != null) {
                     finalTripId = tripId!!
                     val existingTrip = repository.getTripById(tripId!!)
                     if (existingTrip != null) {
                         val trip = existingTrip.copy(
                             name = currentName, 
                             participants = updatedParticipants,
                             participantIds = participantIds,
                             budget = _budget.value.toDoubleOrNull(),
                             budgetAlertThreshold = _alertThreshold.value.toDouble()
                         )
                         repository.updateTrip(trip)
                         
                         val existingPeople = repository.getPeopleForTrip(tripId!!).first()
                         val existingNames = existingPeople.map { it.name }.toSet()
                         
                          for (p in updatedParticipants) {
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
                    val trip = Trip(
                        name = currentName, 
                        participants = updatedParticipants,
                        participantIds = participantIds,
                        budget = _budget.value.toDoubleOrNull(),
                        budgetAlertThreshold = _alertThreshold.value.toDouble()
                    )
                    finalTripId = repository.insertTrip(trip)
                    
                    // Always add the creator as a Person first (payer option)
                    if (currentCreatorId != null) {
                        val creatorUser = userRepository.getUser(currentCreatorId).first()
                        repository.insertPerson(Person(
                            tripId = finalTripId, 
                            name = creatorUser?.displayName ?: "Me",
                            userId = currentCreatorId,
                            phoneNumber = creatorUser?.phone
                        ))
                    }
                    
                    // Add other participants as people (skip if same as creator)
                    for (participant in updatedParticipants) {
                        if (participant.userId != currentCreatorId) {
                            repository.insertPerson(Person(
                                tripId = finalTripId, 
                                name = participant.name,
                                userId = participant.userId,
                                phoneNumber = participant.phoneNumber
                            ))
                        }
                    }
                }
                
                // 5. Send Invitations
                // Iterate through updatedParticipants. If status is INVITED and userId != null, send invite.
                // Optimally we only send if they weren't invited before. 
                // For MVP, sending duplicate invites is handled by Firestore (new doc). Ideally we check.
                // Let's send invite.
                updatedParticipants.forEach { p ->
                    if (p.status == Participant.STATUS_INVITED && p.userId != null && p.userId != currentCreatorId) {
                         repository.inviteUserToTrip(finalTripId, currentName, p.userId)
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

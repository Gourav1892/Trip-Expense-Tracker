package com.example.tripexpensetracker.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.FriendRequest
import com.example.tripexpensetracker.data.model.Invitation
import com.example.tripexpensetracker.data.repository.FriendRepository
import com.example.tripexpensetracker.data.repository.TripRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val friendRepository: FriendRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _invitations = MutableStateFlow<List<Invitation>>(emptyList())
    val invitations = _invitations.asStateFlow()
    
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()
    
    // Simple state to show loading or error during action
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.getInvitations().collect {
                _invitations.value = it
            }
        }
        
        // Load friend requests
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                friendRepository.getFriendRequests(userId).collect {
                    _friendRequests.value = it
                }
            }
        }
    }

    fun onAccept(invitation: Invitation) {
        respond(invitation, true)
    }

    fun onRefuse(invitation: Invitation) {
        respond(invitation, false)
    }

    private fun respond(invitation: Invitation, accept: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tripRepository.respondToInvitation(invitation, accept)
            } catch(e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== FRIEND REQUEST HANDLERS ==========

    fun onAcceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendRepository.respondToFriendRequest(request, true)
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDeclineFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendRepository.respondToFriendRequest(request, false)
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}


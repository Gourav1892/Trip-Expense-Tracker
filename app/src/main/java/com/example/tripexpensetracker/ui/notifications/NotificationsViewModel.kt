package com.example.tripexpensetracker.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Invitation
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _invitations = MutableStateFlow<List<Invitation>>(emptyList())
    val invitations = _invitations.asStateFlow()
    
    // Simple state to show loading or error during action
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.getInvitations().collect {
                _invitations.value = it
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
}

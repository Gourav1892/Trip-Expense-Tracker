package com.example.tripexpensetracker.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.manager.ContactManager
import com.example.tripexpensetracker.data.model.User
import com.example.tripexpensetracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestedContactsViewModel @Inject constructor(
    private val contactManager: ContactManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _suggestedUsers = MutableStateFlow<List<User>>(emptyList())
    val suggestedUsers = _suggestedUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    // We can also store contacts that are NOT on the app if we want to invite them later
    // private val _inviteList = MutableStateFlow<List<ContactInfo>>(emptyList())

    fun fetchSuggestedContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get Local Contacts
                val localContacts = contactManager.getLocalContacts()
                val phoneNumbers = localContacts.map { it.phoneNumber }
                
                // 2. Check Match in Firestore
                if (phoneNumbers.isNotEmpty()) {
                    val matchedUsers = userRepository.getUsersByPhones(phoneNumbers)
                    _suggestedUsers.value = matchedUsers
                } else {
                    _suggestedUsers.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

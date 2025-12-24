package com.example.tripexpensetracker.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Friend
import com.example.tripexpensetracker.data.model.User
import com.example.tripexpensetracker.data.repository.FriendRepository
import com.example.tripexpensetracker.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults = _userSearchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            friendRepository.getFriends(userId).collectLatest {
                _friends.value = it
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _userSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            // Support search by Name OR Phone
            // Ideally we run parallel queries or try one then the other.
            // For simplicity, if it looks like a phone, search phone, else name.
            val isPhone = query.all { it.isDigit() || it == '+' }
            
            val results = if (isPhone) {
                userRepository.searchUsers(query)
            } else {
                userRepository.searchUsersByName(query)
            }
            // Filter out self
            val currentUid = auth.currentUser?.uid
            _userSearchResults.value = results.filter { it.uid != currentUid }
            _isLoading.value = false
        }
    }

    fun addFriendFromUser(user: User, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val friend = Friend(
                ownerId = userId,
                name = user.displayName ?: user.phone,
                phoneNumber = user.phone,
                linkedUserId = user.uid,
                email = null // User object doesn't have email exposed currently
            )
            // Check existence
            if (_friends.value.none { it.linkedUserId == user.uid }) {
                friendRepository.addFriend(userId, friend)
            }
            _isLoading.value = false
            onSuccess()
        }
    }

    fun addManualFriend(name: String, phone: String?, email: String?, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val friend = Friend(
                ownerId = userId,
                name = name,
                phoneNumber = phone,
                email = email,
                linkedUserId = null
            )
            friendRepository.addFriend(userId, friend)
            _isLoading.value = false
            onSuccess()
        }
    }

    fun deleteFriend(friendId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            friendRepository.deleteFriend(userId, friendId)
        }
    }
}

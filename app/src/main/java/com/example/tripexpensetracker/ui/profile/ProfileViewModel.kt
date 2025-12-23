package com.example.tripexpensetracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.repository.AuthRepository
import com.example.tripexpensetracker.data.repository.UserRepository
import com.example.tripexpensetracker.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val user: Flow<User?> = if (authRepository.userId() != null) {
        userRepository.getUser(authRepository.userId()!!)
    } else {
        flowOf(null)
    }

    fun signOut() {
        authRepository.signOut()
    }
}

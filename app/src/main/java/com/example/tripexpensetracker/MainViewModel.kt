package com.example.tripexpensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.util.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.content.Context
import com.example.tripexpensetracker.util.OnboardingManager
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val onboardingManager: OnboardingManager
) : ViewModel() {

    private val connectivityObserver = NetworkConnectivityObserver(context)

    val isOnboardingCompleted = onboardingManager.isOnboardingCompleted()
    
    fun completeOnboarding() {
        onboardingManager.saveOnboardingCompleted()
    }

    val isConnected: StateFlow<Boolean> = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Assume connected initially to avoid flashing offline on start if checking?
            // Or assume offline? Better to assume true to be less intrusive, or false?
            // Let's assume true, the observer emits the current state immediately anyway.
            initialValue = true 
        )
}

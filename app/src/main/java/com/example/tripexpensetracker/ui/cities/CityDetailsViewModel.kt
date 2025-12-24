package com.example.tripexpensetracker.ui.cities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.CityStats
import com.example.tripexpensetracker.data.model.Destination
import com.example.tripexpensetracker.data.model.TimelineItem
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityDetailsViewModel @Inject constructor(
    private val repository: TripRepository,
    private val activeCityManager: com.example.tripexpensetracker.data.repository.ActiveCityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val tripId: String = savedStateHandle["tripId"] ?: ""
    private val destinationId: String = savedStateHandle["destinationId"] ?: ""
    
    /**
     * End the current city visit session
     */
    fun endCityVisit() {
        viewModelScope.launch {
            activeCityManager.endCityVisit()
        }
    }
    
    // Current destination/city
    val destination = repository.getDestinationsFlow(tripId)
        .map { destinations -> destinations.find { it.id == destinationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Unified timeline of expenses and activities
    val timelineItems = repository.getCityTimelineItems(tripId, destinationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // City statistics
    private val _stats = MutableStateFlow(CityStats())
    val stats: StateFlow<CityStats> = _stats
    
    init {
        // Calculate stats whenever timeline changes
        viewModelScope.launch {
            timelineItems.collect { items ->
                _stats.value = repository.getCityStats(tripId, destinationId)
            }
        }
    }
    
    fun getTimeString(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun getDateString(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }
}

package com.example.tripexpensetracker.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Person
import com.example.tripexpensetracker.data.repository.TripRepository
import com.example.tripexpensetracker.domain.Debt
import com.example.tripexpensetracker.domain.SettlementCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var peopleMap: Map<String, String> = emptyMap()

    fun calculateSettlements(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val expenses = repository.getExpensesForTrip(tripId).first() // Get current expenses once
            val people = repository.getPeopleForTrip(tripId).first() // Get current people once
            val shares = repository.getSharesForTrip(tripId).first()
            
            peopleMap = people.associate { it.id to it.name }

            val calculatedDebts = SettlementCalculator.calculateSettlements(expenses, people, shares)
            _debts.value = calculatedDebts
            _isLoading.value = false
        }
    }

    fun getPersonName(personId: String): String {
        return peopleMap[personId] ?: "Unknown"
    }
}

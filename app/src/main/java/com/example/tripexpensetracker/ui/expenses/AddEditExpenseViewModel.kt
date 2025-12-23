package com.example.tripexpensetracker.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.Person
import com.example.tripexpensetracker.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val repository: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    val people: StateFlow<List<Person>> = repository.getPeopleForTrip(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        object Success : UiState()
    }

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun saveExpense(
        tripId: String, 
        title: String, 
        amount: Double, 
        paidByPersonId: String,
        splitType: SplitType,
        shares: Map<String, Double>,
        selectedPersonIds: Set<String>,
        category: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val expenseShares = if (splitType == SplitType.UNEQUAL) {
                    shares.map { (personId, shareAmount) ->
                        com.example.tripexpensetracker.data.model.ExpenseShare(
                            expenseId = "", // Will be set by repository
                            personId = personId,
                            amountOwed = shareAmount
                        )
                    }
                } else if (splitType == SplitType.PERCENTAGE) {
                    // PERCENTAGE Split
                    // shares map contains "personId" -> "percentage value (e.g. 50.0)"
                     shares.map { (personId, percentage) ->
                         com.example.tripexpensetracker.data.model.ExpenseShare(
                             expenseId = "",
                             personId = personId,
                             amountOwed = (percentage / 100.0) * amount
                         )
                     }
                } else if (splitType == SplitType.SHARES) {
                    // SHARES Split
                    // shares map contains "personId" -> "share count (e.g. 1.0, 2.0)"
                    val totalShares = shares.values.sum()
                    if (totalShares > 0) {
                         val amountPerShare = amount / totalShares
                         shares.map { (personId, shareCount) ->
                             com.example.tripexpensetracker.data.model.ExpenseShare(
                                 expenseId = "",
                                 personId = personId,
                                 amountOwed = shareCount * amountPerShare
                             )
                         }
                    } else {
                        emptyList()
                    }
                } else {
                    // EQUAL Split
                    if (selectedPersonIds.isNotEmpty()) {
                        val splitAmount = amount / selectedPersonIds.size
                        selectedPersonIds.map { personId ->
                            com.example.tripexpensetracker.data.model.ExpenseShare(
                                expenseId = "",
                                personId = personId,
                                amountOwed = splitAmount
                            )
                        }
                    } else {
                         emptyList()
                    }
                }

                repository.insertExpense(
                    Expense(
                        tripId = tripId,
                        paidByPersonId = paidByPersonId,
                        title = title,
                        amount = amount,
                        category = category
                    ),
                    expenseShares
                )
                _uiState.value = UiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}

enum class SplitType {
    EQUAL, UNEQUAL, PERCENTAGE, SHARES
}

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val repository: TripRepository,
    private val userRepository: com.example.tripexpensetracker.data.repository.UserRepository,
    private val activeCityManager: com.example.tripexpensetracker.data.repository.ActiveCityManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val rawPeople = repository.getPeopleForTrip(tripId)
    
    val people: StateFlow<List<Person>> = rawPeople
        .flatMapLatest { peopleList ->
            val uids = peopleList.mapNotNull { it.userId }
            userRepository.getUsersFlow(uids).map { userMap ->
                peopleList.map { person ->
                    val user = userMap[person.userId]
                    if (user != null && !user.displayName.isNullOrBlank()) {
                        person.copy(name = user.displayName!!)
                    } else {
                        person
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val destinations: StateFlow<List<com.example.tripexpensetracker.data.model.Destination>> = 
        repository.getDestinationsFlow(tripId)
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

    // Smart Suggestions state
    private val _category = kotlinx.coroutines.flow.MutableStateFlow("General")
    val category = _category.asStateFlow()

    private val _currencySymbol = kotlinx.coroutines.flow.MutableStateFlow("₹")
    val currencySymbol = _currencySymbol.asStateFlow()

    private val _suggestedDestinationId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val suggestedDestinationId = _suggestedDestinationId.asStateFlow()

    private val _editState = kotlinx.coroutines.flow.MutableStateFlow<EditState?>(null)
    val editState = _editState.asStateFlow()

    data class EditState(
        val title: String,
        val amount: Double,
        val category: String,
        val paidBy: String,
        val destinationId: String,
        val shares: List<com.example.tripexpensetracker.data.model.ExpenseShare>
    )

    init {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
            _currencySymbol.value = trip?.currencySymbol ?: "₹"
            
            if (expenseId != null) {
                // Load existing expense
                val expenses = repository.getExpensesForTrip(tripId).firstOrNull() ?: emptyList() 
                // Note: getExpensesForTrip returns a Flow. We need to find the specific expense.
                // Ideally repository should have getExpenseById.
                // Let's iterate found expenses or fetch single? 
                // Repository doesn't expose getExpenseById directly except via looking at the list.
                // Let's assume we can find it in the list for now or we added getSharesForExpense logic.
                // Wait, I can use the new getSharesForExpense!
                
                // Better: find expense from list.
                // Flow collection is tricky here if we want one-shot.
                // Let's just collect first emission of list and find it.
                val expense = expenses.find { it.id == expenseId }
                
                if (expense != null) {
                     val shares = repository.getSharesForExpense(tripId, expenseId)
                     _editState.value = EditState(
                         title = expense.title,
                         amount = expense.amount,
                         category = expense.category,
                         paidBy = expense.paidByPersonId,
                         destinationId = expense.destinationId,
                         shares = shares
                     )
                     _category.value = expense.category
                     _suggestedDestinationId.value = expense.destinationId // Use expense's destination
                }
            } else {
                 // Check if there's an active city visit for THIS trip (shared state)
                val activeDestId = trip?.activeDestinationId
                if (activeDestId != null) {
                    _suggestedDestinationId.value = activeDestId
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        // If the current category is "General" (default), try to find a better one
        if (_category.value == "General" && expenseId == null) { // Only suggest on create
            val suggestion = CategorySuggester.suggestCategory(title)
            if (suggestion != null) {
                _category.value = suggestion
            }
        }
    }

    fun onCategorySelected(newCategory: String) {
        _category.value = newCategory
    }

    fun saveExpense(
        tripId: String,
        title: String,
        amount: Double,
        paidByPersonId: String,
        splitType: SplitType,
        shares: Map<String, Double>,
        selectedPersonIds: Set<String>,

        destinationId: String = "", // City/destination association
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Use current category state
                val finalCategory = _category.value
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

                val expenseToSave = Expense(
                        id = expenseId ?: "",
                        tripId = tripId,
                        destinationId = destinationId,
                        paidByPersonId = paidByPersonId,
                        title = title,
                        amount = amount,
                        category = finalCategory
                    )

                if (expenseId != null) {
                    repository.updateExpenseWithShares(expenseToSave, expenseShares)
                } else {
                    repository.insertExpense(expenseToSave, expenseShares)
                }
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

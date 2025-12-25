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
    private val repository: TripRepository,
    private val userRepository: com.example.tripexpensetracker.data.repository.UserRepository
) : ViewModel() {

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts

    private val _personalBalances = MutableStateFlow<List<PersonalBalance>>(emptyList())
    val personalBalances: StateFlow<List<PersonalBalance>> = _personalBalances

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol

    var peopleMap: Map<String, String> = emptyMap()
    private var myPersonId: String = ""

    fun recordPayment(toPersonId: String, amount: Double) {
        viewModelScope.launch {
            if (myPersonId.isBlank()) return@launch
            
            val toName = peopleMap[toPersonId] ?: "Unknown"
            val newExpense = com.example.tripexpensetracker.data.model.Expense(
                tripId = currentTripId,
                title = "Payment to $toName",
                amount = amount,
                category = "Settlement", // Special category
                paidByPersonId = myPersonId,
                date = java.util.Date()
            )
            
            val shares = listOf(
                com.example.tripexpensetracker.data.model.ExpenseShare(
                    personId = toPersonId,
                    amountOwed = amount
                )
            )
            
            // Insert expense and shares
            repository.insertExpense(newExpense, shares)
            
            // Recalculate will happen automatically via flows? 
            // Actually calculateSettlements is manual. We should re-trigger it or rely on Flow.
            // The ViewModel observes flows in calculateSettlements, but it uses .first().
            // We need to switch calculateSettlements to collect flows continuously or manually re-call.
            // For now, let's manually re-call calculateSettlements(currentTripId).
            calculateSettlements(currentTripId)
        }
    }

    private var currentTripId: String = ""

    fun calculateSettlements(tripId: String) {
        currentTripId = tripId
        viewModelScope.launch {
            _isLoading.value = true
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUid = auth.currentUser?.uid ?: ""
            
            // Collect dependencies once
            val trip = repository.getTripById(tripId)
            _currencySymbol.value = trip?.currencySymbol ?: "₹"
            
            val expenses = repository.getExpensesForTrip(tripId).first()
            val rawPeople = repository.getPeopleForTrip(tripId).first()
            val shares = repository.getSharesForTrip(tripId).first()
            
            // Hydrate names reactively
            val uids = rawPeople.mapNotNull { it.userId }
            userRepository.getUsersFlow(uids).collect { userMap ->
                val hydratedPeople = rawPeople.map { p ->
                    val user = userMap[p.userId]
                    if (user != null && !user.displayName.isNullOrBlank()) {
                        p.copy(name = user.displayName!!)
                    } else {
                        p
                    }
                }
                
                peopleMap = hydratedPeople.associate { it.id to it.name }
                myPersonId = hydratedPeople.find { it.userId == currentUid }?.id ?: "" // Store myPersonId

                val calculatedDebts = SettlementCalculator.calculateSettlements(expenses, hydratedPeople, shares)
                _debts.value = calculatedDebts
                
                // Calculate personal perspective
                val balanceMap = mutableMapOf<String, Double>()
                
                // Initialize balanceMap with ALL hydrated participants (except me) to show $0 balances
                hydratedPeople.forEach { person ->
                    if (person.id != myPersonId) {
                        balanceMap[person.id] = 0.0
                    }
                }
                
                calculatedDebts.forEach { debt ->
                    if (debt.fromPersonId == myPersonId) {
                        balanceMap[debt.toPersonId] = (balanceMap[debt.toPersonId] ?: 0.0) - debt.amount
                    } else if (debt.toPersonId == myPersonId) {
                        balanceMap[debt.fromPersonId] = (balanceMap[debt.fromPersonId] ?: 0.0) + debt.amount
                    }
                }
                
                _personalBalances.value = balanceMap.map { (otherId, amount) ->
                    PersonalBalance(otherId, peopleMap[otherId] ?: "Unknown", amount)
                }.sortedWith(compareByDescending<PersonalBalance> { kotlin.math.abs(it.netAmount) }.thenBy { it.otherPersonName })
                
                _isLoading.value = false
            }
        }
    }

    fun getPersonName(personId: String): String {
        return peopleMap[personId] ?: "Unknown"
    }
}

data class PersonalBalance(
    val otherPersonId: String,
    val otherPersonName: String,
    val netAmount: Double
)

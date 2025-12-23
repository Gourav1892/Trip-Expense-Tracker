package com.example.tripexpensetracker.domain

import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.ExpenseShare
import com.example.tripexpensetracker.data.model.Person
import kotlin.math.abs

data class Debt(
    val fromPersonId: String,
    val toPersonId: String,
    val amount: Double
)

object SettlementCalculator {

    fun calculateSettlements(
        expenses: List<Expense>,
        people: List<Person>,
        allShares: List<ExpenseShare> // New parameter
    ): List<Debt> {
        val balances = mutableMapOf<String, Double>()
        people.forEach { balances[it.id] = 0.0 }

        val sharesByExpense = allShares.groupBy { it.expenseId }

        expenses.forEach { expense ->
            val paidBy = expense.paidByPersonId
            val amount = expense.amount
            val shares = sharesByExpense[expense.id]

            // Credit the payer
            balances[paidBy] = (balances[paidBy] ?: 0.0) + amount

            if (shares != null && shares.isNotEmpty()) {
                // Use custom split
                shares.forEach { share ->
                    balances[share.personId] = (balances[share.personId] ?: 0.0) - share.amountOwed
                }
            } else {
                // Use equal split (default)
                if (people.isNotEmpty()) {
                    val splitAmount = amount / people.size
                    people.forEach { person ->
                        balances[person.id] = (balances[person.id] ?: 0.0) - splitAmount
                    }
                }
            }
        }

        // Minimize transactions (greedy approach)
        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        balances.forEach { (personId, balance) ->
            if (balance < -0.01) debtors.add(personId to balance)
            if (balance > 0.01) creditors.add(personId to balance)
        }

        debtors.sortBy { it.second } // Sort by most debt (most negative)
        creditors.sortByDescending { it.second } // Sort by most credit (most positive)

        val debts = mutableListOf<Debt>()
        var i = 0 // iterator for debtors
        var j = 0 // iterator for creditors

        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            // amount to settle is min(debt, credit)
            val amount = minOf(abs(debtor.second), creditor.second)
            
            if (amount > 0.01) {
                debts.add(Debt(debtor.first, creditor.first, amount))
            }

            val remainingDebt = debtor.second + amount
            val remainingCredit = creditor.second - amount
            
            // Update values for next iteration
            if (abs(remainingDebt) < 0.01) {
                i++ // Debtor settled
            } else {
                debtors[i] = debtor.first to remainingDebt
            }

            if (remainingCredit < 0.01) {
                j++ // Creditor settled
            } else {
                creditors[j] = creditor.first to remainingCredit
            }
        }

        return debts
    }
}

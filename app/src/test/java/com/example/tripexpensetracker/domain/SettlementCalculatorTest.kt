package com.example.tripexpensetracker.domain

import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.Person
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementCalculatorTest {

    @Test
    fun `calculateSettlements should return correct debts for simple case`() {
        val people = listOf(
            Person(id = "1", tripId = "1", name = "Alice"),
            Person(id = "2", tripId = "1", name = "Bob")
        )
        val expenses = listOf(
            Expense(id = "1", tripId = "1", paidByPersonId = "1", title = "Lunch", amount = 20.0)
        )

        // Alice paid 20 (Split: 10 each). Alice +10, Bob -10.
        // Expect: Bob pays Alice 10.

        val debts = SettlementCalculator.calculateSettlements(expenses, people, emptyList())

        assertEquals(1, debts.size)
        assertEquals("2", debts[0].fromPersonId)
        assertEquals("1", debts[0].toPersonId)
        assertEquals(10.0, debts[0].amount, 0.01)
    }

    @Test
    fun `calculateSettlements should return empty list if everyone paid equally`() {
        val people = listOf(
            Person(id = "1", tripId = "1", name = "Alice"),
            Person(id = "2", tripId = "1", name = "Bob")
        )
        val expenses = listOf(
            Expense(id = "1", tripId = "1", paidByPersonId = "1", title = "Lunch", amount = 10.0),
            Expense(id = "2", tripId = "1", paidByPersonId = "2", title = "Dinner", amount = 10.0)
        )

        val debts = SettlementCalculator.calculateSettlements(expenses, people, emptyList())

        assertEquals(0, debts.size)
    }
    
    @Test
    fun `calculateSettlements complex 3 people case`() {
        val people = listOf(
            Person(id = "1", tripId = "1", name = "Alice"),
            Person(id = "2", tripId = "1", name = "Bob"),
            Person(id = "3", tripId = "1", name = "Charlie")
        )
        // Alice pays 30. Split 10 each. A:+20, B:-10, C:-10.
        val expenses = listOf(
             Expense(id = "1", tripId = "1", paidByPersonId = "1", title = "Taxi", amount = 30.0)
        )
        
        val debts = SettlementCalculator.calculateSettlements(expenses, people, emptyList())
        
        // Bob pays Alice 10, Charlie pays Alice 10.
        assertEquals(2, debts.size)
        val totalToAlice = debts.filter { it.toPersonId == "1" }.sumOf { it.amount }
        assertEquals(20.0, totalToAlice, 0.01)
    }

    @Test
    fun `calculateSettlements with unequal split`() {
        val people = listOf(
            Person(id = "1", tripId = "1", name = "Alice"),
            Person(id = "2", tripId = "1", name = "Bob"),
            Person(id = "3", tripId = "1", name = "Charlie")
        )
        val expenses = listOf(
            Expense(id = "1", tripId = "1", paidByPersonId = "1", title = "Dinner", amount = 100.0)
        )
        // Unequal split: Alice owes 50, Bob owes 25, Charlie owes 25.
        // Alice paid 100.
        // Net:
        // Alice: Paid 100, Owes 50 -> +50
        // Bob: Paid 0, Owes 25 -> -25
        // Charlie: Paid 0, Owes 25 -> -25
        
        val shares = listOf(
            com.example.tripexpensetracker.data.model.ExpenseShare(expenseId = "1", personId = "1", amountOwed = 50.0),
            com.example.tripexpensetracker.data.model.ExpenseShare(expenseId = "1", personId = "2", amountOwed = 25.0),
            com.example.tripexpensetracker.data.model.ExpenseShare(expenseId = "1", personId = "3", amountOwed = 25.0)
        )

        val debts = SettlementCalculator.calculateSettlements(expenses, people, shares)

        // Result: Bob pays Alice 25, Charlie pays Alice 25.
        assertEquals(2, debts.size)
        val totalToAlice = debts.filter { it.toPersonId == "1" }.sumOf { it.amount }
        assertEquals(50.0, totalToAlice, 0.01)
    }
}

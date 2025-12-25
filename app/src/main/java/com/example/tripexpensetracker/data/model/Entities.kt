package com.example.tripexpensetracker.data.model

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class Trip(
    val id: String = "",
    val name: String = "",
    val startDate: Date = Date(),
    val createdBy: String = "",
    val participants: List<Participant> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val budget: Double? = null,
    val budgetAlertThreshold: Double? = null,
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹"
)

@Stable
data class Person(
    val id: String = "",
    val tripId: String = "",
    val name: String = "",
    val receiptPath: String? = null,
    val userId: String? = null,
    val phoneNumber: String? = null
)

@Stable
data class Expense(
    val id: String = "",
    val tripId: String = "",
    val destinationId: String = "", // Link to city/destination
    val paidByPersonId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "General",
    val date: Date = Date()
)

@Stable
data class ExpenseShare(
    val id: String = "",
    val tripId: String = "",
    val expenseId: String = "",
    val personId: String = "",
    val amountOwed: Double = 0.0
)

@Stable
data class User(
    val uid: String = "",
    val phone: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null
)

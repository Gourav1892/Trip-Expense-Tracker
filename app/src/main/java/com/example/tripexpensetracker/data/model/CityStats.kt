package com.example.tripexpensetracker.data.model

import java.util.Date

/**
 * Statistics and summary data for a city/destination within a trip.
 */
data class CityStats(
    val totalExpenses: Double = 0.0,
    val expenseCount: Int = 0,
    val activityCount: Int = 0,
    val topCategory: String = "",
    val dateRange: Pair<Date?, Date?> = Pair(null, null)
)

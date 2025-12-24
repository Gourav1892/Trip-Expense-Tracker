package com.example.tripexpensetracker.data.model

import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.ItineraryItem

/**
 * Unified timeline item for displaying expenses and activities together
 * in chronological order within a city.
 */
sealed class TimelineItem {
    abstract val id: String
    abstract val timestamp: Long
    
    data class ExpenseItem(
        override val id: String,
        override val timestamp: Long,
        val expense: Expense
    ) : TimelineItem()
    
    data class ActivityItem(
        override val id: String,
        override val timestamp: Long,
        val activity: ItineraryItem
    ) : TimelineItem()
}

package com.example.tripexpensetracker.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for date and time formatting throughout the app.
 */
object DateTimeUtils {
    
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    
    /**
     * Format time only (e.g., "3:30 PM")
     */
    fun formatTime(date: Date): String = timeFormat.format(date)
    fun formatTime(timestamp: Long): String = formatTime(Date(timestamp))
    
    /**
     * Format date only (e.g., "Dec 24, 2024")
     */
    fun formatDate(date: Date): String = dateFormat.format(date)
    fun formatDate(timestamp: Long): String = formatDate(Date(timestamp))
    
    /**
     * Format short date (e.g., "Dec 24")
     */
    fun formatShortDate(date: Date): String = shortDateFormat.format(date)
    fun formatShortDate(timestamp: Long): String = formatShortDate(Date(timestamp))
    
    /**
     * Format full date and time (e.g., "Dec 24, 2024 • 3:30 PM")
     */
    fun formatDateTime(date: Date): String = fullDateTimeFormat.format(date)
    fun formatDateTime(timestamp: Long): String = formatDateTime(Date(timestamp))
    
    /**
     * Format date range (e.g., "Dec 24 - Dec 28" or "Dec 24" if same day)
     */
    fun formatDateRange(start: Date?, end: Date?): String {
        if (start == null) return ""
        if (end == null) return formatShortDate(start)
        
        val startStr = formatShortDate(start)
        val endStr = formatShortDate(end)
        
        return if (startStr == endStr) startStr else "$startStr - $endStr"
    }
    
    /**
     * Get relative date string (e.g., "Today", "Yesterday", "Tomorrow")
     */
    fun getRelativeDateString(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        calendar.time = date
        val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
        val dateYear = calendar.get(Calendar.YEAR)
        
        return when {
            year == dateYear && dateDay == today -> "Today"
            year == dateYear && dateDay == today - 1 -> "Yesterday"
            year == dateYear && dateDay == today + 1 -> "Tomorrow"
            else -> formatDate(date)
        }
    }
}

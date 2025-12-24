package com.example.tripexpensetracker.data.model

data class ItineraryItem(
    val id: String = "",
    val tripId: String = "",
    val destinationId: String? = null,
    val title: String = "",
    val description: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val locationName: String = "",
    val type: String = TYPE_ACTIVITY
) {
    companion object {
        const val TYPE_ACTIVITY = "ACTIVITY"
        const val TYPE_FLIGHT = "FLIGHT"
        const val TYPE_LODGING = "LODGING"
        const val TYPE_TRAVEL = "TRAVEL"
        const val TYPE_FOOD = "FOOD"
    }
}

package com.example.tripexpensetracker.data.model

data class Destination(
    val id: String = "",
    val tripId: String = "",
    val name: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val notes: String = ""
)

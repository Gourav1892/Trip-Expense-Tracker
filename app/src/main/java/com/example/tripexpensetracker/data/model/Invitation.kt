package com.example.tripexpensetracker.data.model

import java.util.Date

data class Invitation(
    val id: String = "",
    val tripId: String = "",
    val tripName: String = "",
    val inviterName: String = "",
    val inviterId: String = "",
    val inviteeId: String = "", // User ID of the person being invited
    val timestamp: Date = Date(),
    val type: String = TYPE_INVITE,
    val message: String = "" // For informational notifications
) {
    companion object {
        const val TYPE_INVITE = "INVITE"
        const val TYPE_ACCEPTANCE_INFO = "ACCEPTANCE_INFO"
        const val TYPE_DECLINE_INFO = "DECLINE_INFO"
    }
}

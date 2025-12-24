package com.example.tripexpensetracker.data.model

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhone: String? = null,
    val receiverId: String = "",
    val timestamp: Date = Date(),
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_DECLINED = "DECLINED"
    }
}

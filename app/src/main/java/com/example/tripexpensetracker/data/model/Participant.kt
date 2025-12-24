package com.example.tripexpensetracker.data.model

data class Participant(
    val name: String = "",
    val userId: String? = null,
    val phoneNumber: String? = null,
    val status: String = STATUS_JOINED
) {
    companion object {
        const val STATUS_JOINED = "JOINED"
        const val STATUS_INVITED = "INVITED"
        const val STATUS_DECLINED = "DECLINED"
    }
}

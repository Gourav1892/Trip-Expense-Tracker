package com.example.tripexpensetracker.data.model

data class Friend(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val phoneNumber: String? = null,
    val email: String? = null,
    val linkedUserId: String? = null // If they are a registered user
)

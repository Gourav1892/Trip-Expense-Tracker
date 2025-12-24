package com.example.tripexpensetracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class FriendIdentificationTest {

    @Test
    fun `test creating friend from registered user`() {
        // Given a registered user
        val user = User(
            uid = "user123",
            phone = "+15551234567",
            displayName = "Alice App User"
        )
        val ownerId = "owner999"

        // When creating a Friend object
        val friend = Friend(
            ownerId = ownerId,
            name = user.displayName ?: user.phone,
            phoneNumber = user.phone,
            linkedUserId = user.uid
        )

        // Then it should be identified as a registered user
        assertEquals("user123", friend.linkedUserId)
        assertEquals("Alice App User", friend.name)
        assertNotNull(friend.linkedUserId)
    }

    @Test
    fun `test creating manual friend`() {
        // Given manual details
        val name = "Bob Offline"
        val phone = "0987654321"
        val ownerId = "owner999"

        // When creating a Friend object
        val friend = Friend(
            ownerId = ownerId,
            name = name,
            phoneNumber = phone,
            linkedUserId = null
        )

        // Then it should NOT have a linked user ID
        assertNull(friend.linkedUserId)
        assertEquals("Bob Offline", friend.name)
        assertEquals("0987654321", friend.phoneNumber)
    }

    @Test
    fun `test participant conversion retains user id`() {
        // Given a friend with a linked user ID
        val friend = Friend(
            ownerId = "owner999",
            name = "Charlie App User",
            linkedUserId = "user456",
            phoneNumber = "+15559876543"
        )

        // When converting to Participant (simulating Add logic)
        val participant = Participant(
            name = friend.name,
            userId = friend.linkedUserId,
            phoneNumber = friend.phoneNumber
        )

        // Then the participant should still have the userId
        assertEquals("user456", participant.userId)
        assertEquals("Charlie App User", participant.name)
    }
}

package com.example.tripexpensetracker.data.repository

import com.example.tripexpensetracker.data.model.Friend
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Friends are stored in a subcollection of the user: /users/{userId}/friends/{friendId}
    // This ensures privacy and easy access control.

    fun getFriends(userId: String): Flow<List<Friend>> {
        return firestore.collection("users").document(userId).collection("friends")
            .orderBy("name")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friend::class.java)
            }
    }

    suspend fun addFriend(userId: String, friend: Friend): String {
        val docRef = firestore.collection("users").document(userId).collection("friends").document()
        val friendWithId = friend.copy(id = docRef.id, ownerId = userId)
        docRef.set(friendWithId).await()
        return docRef.id
    }

    suspend fun updateFriend(userId: String, friend: Friend) {
        if (friend.id.isNotEmpty()) {
            firestore.collection("users").document(userId).collection("friends")
                .document(friend.id)
                .set(friend)
                .await()
        }
    }

    suspend fun deleteFriend(userId: String, friendId: String) {
        firestore.collection("users").document(userId).collection("friends")
            .document(friendId)
            .delete()
            .await()
    }
}

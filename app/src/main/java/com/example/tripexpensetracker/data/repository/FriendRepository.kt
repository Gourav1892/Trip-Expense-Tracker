package com.example.tripexpensetracker.data.repository

import com.example.tripexpensetracker.data.model.Friend
import com.example.tripexpensetracker.data.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
        // Get the friend document to find their linkedUserId
        val friendDoc = firestore.collection("users").document(userId).collection("friends")
            .document(friendId)
            .get()
            .await()
        
        val friend = friendDoc.toObject(Friend::class.java)
        
        // Delete from current user's list
        firestore.collection("users").document(userId).collection("friends")
            .document(friendId)
            .delete()
            .await()
        
        // Also delete from the other user's friends list (mutual unfriend)
        if (friend?.linkedUserId != null) {
            val otherUserId = friend.linkedUserId
            // Find and delete the friend entry in the other user's collection
            val otherUserFriends = firestore.collection("users")
                .document(otherUserId)
                .collection("friends")
                .whereEqualTo("linkedUserId", userId)
                .get()
                .await()
            
            for (doc in otherUserFriends.documents) {
                doc.reference.delete().await()
            }
        }
    }

    // ========== FRIEND REQUEST METHODS ==========

    // Friend requests are stored in receiver's subcollection: /users/{receiverId}/friendRequests/{requestId}
    
    suspend fun sendFriendRequest(request: FriendRequest): String {
        val docRef = firestore.collection("users")
            .document(request.receiverId)
            .collection("friendRequests")
            .document()
        val requestWithId = request.copy(id = docRef.id)
        docRef.set(requestWithId).await()
        return docRef.id
    }

    fun getFriendRequests(userId: String): Flow<List<FriendRequest>> {
        return firestore.collection("users")
            .document(userId)
            .collection("friendRequests")
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FriendRequest::class.java)
            }
    }

    suspend fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        val receiverId = request.receiverId
        val senderId = request.senderId
        
        if (accept) {
            // Get receiver's (current user's) info
            val currentUser = auth.currentUser
            var receiverName = currentUser?.displayName
            val receiverPhone = currentUser?.phoneNumber
            
            // Fallback: Fetch from Firestore user profile if displayName is null
            if (receiverName.isNullOrBlank() && receiverId.isNotEmpty()) {
                val userDoc = firestore.collection("users").document(receiverId).get().await()
                receiverName = userDoc.getString("displayName") ?: userDoc.getString("phone") ?: "Friend"
            }
            
            // Add sender to receiver's friends list
            val friendForReceiver = Friend(
                ownerId = receiverId,
                name = request.senderName,
                phoneNumber = request.senderPhone,
                linkedUserId = senderId
            )
            addFriend(receiverId, friendForReceiver)
            
            // Add receiver to sender's friends list (mutual!)
            val friendForSender = Friend(
                ownerId = senderId,
                name = receiverName ?: "Friend",
                phoneNumber = receiverPhone,
                linkedUserId = receiverId
            )
            addFriend(senderId, friendForSender)
        }
        
        // Delete the request after processing
        firestore.collection("users")
            .document(receiverId)
            .collection("friendRequests")
            .document(request.id)
            .delete()
            .await()
    }
}


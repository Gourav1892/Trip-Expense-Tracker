package com.example.tripexpensetracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.tripexpensetracker.data.model.User
import com.google.firebase.firestore.snapshots

class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun saveUser(uid: String, phone: String, displayName: String? = null, photoUrl: String? = null) {
        val userMap = mutableMapOf(
            "uid" to uid,
            "phone" to phone
        )
        if (displayName != null) userMap["displayName"] = displayName
        if (photoUrl != null) userMap["photoUrl"] = photoUrl

        try {
            firestore.collection("users").document(uid).set(userMap).await()
        } catch (e: Exception) {
            // Handle or log error
            e.printStackTrace()
        }
    }

    fun getUser(uid: String): Flow<User?> {
        return firestore.collection("users").document(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) {
                User(
                    uid = snapshot.getString("uid") ?: "",
                    phone = snapshot.getString("phone") ?: "",
                    displayName = snapshot.getString("displayName"),
                    photoUrl = snapshot.getString("photoUrl")
                )
            } else {
                null
            }
        }
    }

    suspend fun getUsersByPhones(phones: List<String>): List<User> {
        if (phones.isEmpty()) return emptyList()
        val allUsers = mutableListOf<User>()
        
        // Firestore 'in' query supports up to 10 values.
        val chunks = phones.chunked(10)
        
        for (chunk in chunks) {
            try {
                val snapshot = firestore.collection("users")
                    .whereIn("phone", chunk)
                    .get()
                    .await()
                allUsers.addAll(snapshot.toObjects(User::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue to next chunk even if one fails
            }
        }
        return allUsers
    }

    /**
     * Search users by phone number prefix.
     * Note: Firestore does not support native fuzzy search.
     * We use startAt/endAt for prefix matching.
     */
    suspend fun searchUsers(query: String): List<User> {
        if (query.length < 3) return emptyList()

        val endQuery = query + "\uf8ff"

        return try {
            val snapshot = firestore.collection("users")
                .orderBy("phone")
                .startAt(query)
                .endAt(endQuery)
                .limit(20)
                .get()
                .await()
            
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchUsersByName(query: String): List<User> {
        if (query.length < 3) return emptyList()

        // Normalize casing if needed, but Firestore is case-sensitive by default.
        // For simple search, we assume exact case or user matches case.
        val endQuery = query + "\uf8ff"

        return try {
            val snapshot = firestore.collection("users")
                .orderBy("displayName")
                .startAt(query)
                .endAt(endQuery)
                .limit(20)
                .get()
                .await()
            
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun uploadProfilePicture(uid: String, uri: android.net.Uri): String {
        return try {
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val profileRef = storageRef.child("profile_images/$uid.jpg")
            profileRef.putFile(uri).await()
            profileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

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

    suspend fun getUsersByPhones(phones: List<String>): List<Map<String, Any>> {
        if (phones.isEmpty()) return emptyList()
        // Note: Firestore 'in' query supports up to 10 values. 
        // For production, we would need to batch this.
        // For this demo, we'll take the first 10 if there are more.
        val searchChunk = phones.take(10)
        
        return try {
            val snapshot = firestore.collection("users")
                .whereIn("phone", searchChunk)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
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

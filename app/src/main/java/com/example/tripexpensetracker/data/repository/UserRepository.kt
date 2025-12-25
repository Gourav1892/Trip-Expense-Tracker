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
            firestore.collection("users").document(uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .await()
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
            }
        }
        return allUsers
    }

    suspend fun getUsersByIds(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()
        val allUsers = mutableListOf<User>()
        val chunks = uids.chunked(10)
        for (chunk in chunks) {
            try {
                val snapshot = firestore.collection("users")
                    .whereIn("uid", chunk)
                    .get()
                    .await()
                allUsers.addAll(snapshot.toObjects(User::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return allUsers
    }

    /**
     * Search users by phone number prefix.
     * Note: Firestore does not support native fuzzy search.
     * We use startAt/endAt for prefix matching.
     * Also supports searching by last N digits (without country code)
     */
    suspend fun searchUsers(query: String): List<User> {
        if (query.length < 3) return emptyList()

        val endQuery = query + "\uf8ff"
        
        // Clean the query - remove spaces, dashes, and non-digit chars for phone matching
        val cleanQuery = query.replace(Regex("[^0-9]"), "").trimStart('0')

        return try {
            // First try exact prefix match (for full numbers with country code like +91...)
            val prefixResults = firestore.collection("users")
                .orderBy("phone")
                .startAt(query)
                .endAt(endQuery)
                .limit(20)
                .get()
                .await()
                .toObjects(User::class.java)
            
            // If no results from prefix match, search by phone suffix (last N digits)
            // This handles searching without country code
            if (prefixResults.isEmpty() && cleanQuery.length >= 3) {
                val allUsers = firestore.collection("users")
                    .limit(200) // Increase limit for better coverage
                    .get()
                    .await()
                    .toObjects(User::class.java)
                
                allUsers.filter { user ->
                    // Clean phone number and check if it ends with the search query
                    val cleanPhone = user.phone?.replace(Regex("[^0-9]"), "") ?: ""
                    cleanPhone.endsWith(cleanQuery) || cleanPhone.contains(cleanQuery)
                }.take(20)
            } else {
                prefixResults
            }
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
    
    suspend fun updateDisplayName(userId: String, newName: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .update("displayName", newName)
                .await()
            
            // Also update Firebase Auth profile
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null && user.uid == userId) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUsersFlow(uids: List<String>): Flow<Map<String, User>> {
        if (uids.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyMap())
        
        // Split into chunks of 10 for Firestore 'in' query
        val chunks = uids.chunked(10)
        val flows = chunks.map { chunk ->
            firestore.collection("users")
                .whereIn("uid", chunk)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(User::class.java).associateBy { it.uid }
                }
        }
        
        return kotlinx.coroutines.flow.combine(flows) { maps ->
            val result = mutableMapOf<String, User>()
            maps.forEach { result.putAll(it) }
            result
        }
    }
}

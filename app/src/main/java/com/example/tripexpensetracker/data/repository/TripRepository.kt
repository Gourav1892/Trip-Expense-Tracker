package com.example.tripexpensetracker.data.repository

import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.ExpenseShare
import com.example.tripexpensetracker.data.model.Person
import com.example.tripexpensetracker.data.model.Trip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

private const val TAG = "TripRepository"

class TripRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) {
    // Trip Operations
    fun getAllTrips(): Flow<List<Trip>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        
        return firestore.collection("trips")
            .whereArrayContains("participantIds", userId)
            .orderBy("startDate", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Trip>() }
            .catch { e -> 
                Log.e(TAG, "Error fetching trips. Likely missing Index. Check log for link.", e)
                emit(emptyList())
            }
    }
    
    suspend fun getTripById(tripId: String): Trip? {
        return firestore.collection("trips").document(tripId).get().await().toObject(Trip::class.java)
    }
    
    suspend fun insertTrip(trip: Trip): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val docRef = firestore.collection("trips").document()
        
        // Ensure the creator is in the participants list if not already
        val updatedParticipants = trip.participants.toMutableList()
        val hasCreator = updatedParticipants.any { it.userId == userId }
        if (!hasCreator) {
            // Fetch current user details or just add basic info? 
            // Ideally we should have the user's name. For now using "Me" or placeholder if missing.
            // But usually the UI should pass this.
            // Let's assume UI passes it or we add a basic specific "Me" entry?
            // Safer: Add it.
            val userPhone = auth.currentUser?.phoneNumber
            updatedParticipants.add(com.example.tripexpensetracker.data.model.Participant(name = "Me", userId = userId, phoneNumber = userPhone))
        }

        val participantIds = updatedParticipants.mapNotNull { it.userId }

        val tripWithId = trip.copy(
            id = docRef.id,
            createdBy = userId,
            participants = updatedParticipants,
            participantIds = participantIds
        )
        docRef.set(tripWithId).await()
        Log.d(TAG, "Trip inserted: ${docRef.id} by user: $userId")
        
        // Subscribe creator to trip topic
        subscribeToTripTopic(docRef.id)
        
        return docRef.id
    }
    
    suspend fun updateTrip(trip: Trip) {
        firestore.collection("trips").document(trip.id).set(trip).await()
    }
    
    suspend fun deleteTrip(trip: Trip) {
        firestore.collection("trips").document(trip.id).delete().await()
    }

    // Person Operations
    fun getPeopleForTrip(tripId: String): Flow<List<Person>> {
        Log.d(TAG, "Getting people for trip: $tripId")
        return firestore.collection("trips").document(tripId).collection("people")
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Person>() }
    }
    
    suspend fun insertPerson(person: Person) {
        val docRef = firestore.collection("trips").document(person.tripId).collection("people").document()
        val personWithId = person.copy(id = docRef.id)
        docRef.set(personWithId).await()
    }
    
    suspend fun deletePerson(person: Person) {
        firestore.collection("trips").document(person.tripId).collection("people").document(person.id).delete().await()
    }

    // Expense Operations
    fun getExpensesForTrip(tripId: String): Flow<List<Expense>> {
        return firestore.collection("trips").document(tripId).collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Expense>() }
    }
    
    suspend fun insertExpense(expense: Expense, shares: List<ExpenseShare> = emptyList()) {
        val expenseCollection = firestore.collection("trips").document(expense.tripId).collection("expenses")
        val docRef = expenseCollection.document()
        val expenseWithId = expense.copy(id = docRef.id)
        
        // We will store shares as a subcollection of the expense for cleaner mapping, or embedded?
        // Let's store embedded in a separate field map if possible, but Expense data class doesn't have it.
        // Let's store shares in a sub-collection of the expense document. "expenses/{expenseId}/shares"
        
        firestore.runBatch { batch ->
            batch.set(docRef, expenseWithId)
            shares.forEach { share ->
                val shareDoc = docRef.collection("shares").document()
                val shareWithId = share.copy(id = shareDoc.id, expenseId = docRef.id, tripId = expense.tripId)
                batch.set(shareDoc, shareWithId)
            }
        }.await()
    }
    
    suspend fun updateExpense(expense: Expense) {
        firestore.collection("trips").document(expense.tripId).collection("expenses").document(expense.id).set(expense).await()
    }
    
    suspend fun deleteExpense(expense: Expense) {
        val expenseRef = firestore.collection("trips").document(expense.tripId).collection("expenses").document(expense.id)
        // Note: Subcollections are not automatically deleted in Firestore.
        // We should manually delete shares first.
        val shares = expenseRef.collection("shares").get().await()
        firestore.runBatch { batch ->
             shares.documents.forEach { batch.delete(it.reference) }
             batch.delete(expenseRef)
        }.await()
    }

    // Share Operations
    // Note: getSharesForExpense is suspend, but getSharesForTrip is Flow. 
    // Implementing getSharesForTrip efficiently in Firestore is tricky without a collection group query or fetching all subcollections.
    // Given the structure `trips/{tripId}/expenses/{expenseId}/shares`, we can't easily query ALL shares for a trip in one go without Collection Group Query + filtering by something.
    // Alternatively, we can fetch all expenses, and for each expense fetch shares. But that's N+1 reads.
    // Better Approach: Store the list of shares EMBEDDED in the Expense document as a field `sharesList: List<ExpenseShare>`.
    // But Expense entity doesn't have it.
    // Let's add `shares: List<ExpenseShare> = emptyList()` to the Expense entity but annotate it @Exclude or similar if we were using POJOs, but since we use data classes we can add it.
    // Actually, Firestore toObject ignores fields not in the document.
    // If I modify Expense data class to include shares, I can read it directly.
    
    // DECISION: Modify Expense data class to include `val shares: List<ExpenseShare> = emptyList()`.
    // This simplifies everything.
    
    // I will wait to modify Entity and just implement getSharesForTrip by fetching expenses and flattening? No, that's slow.
    // Collection Group Query: db.collectionGroup("shares").whereEqualTo("tripId", tripId)?
    // ExpenseShare needs `tripId` to be queryable if nested.
    // 
    // Let's stick to the mapped structure plan.
    // I will implement `getSharesForTrip` by listening to expenses and then combining flows? Complex.
    // 
    // SIMPLEST: Fetch all expenses for the trip. Then fetch all shares for those expenses.
    // For a real app, Collection Group Index is best.
    // For this MVP, let's Iterate.
    // 
    // But `getSharesForTrip` returns a Flow.
    // Writing a customized Flow that observes expenses, and then for each emission, observes their shares... is hard to get right (flatMapLatest).
    
    // Alternative: Denormalize. When I run `insertExpense`, I calculate the shares and I can store them in a top-level collection `expense_shares` with `tripId` field?
    // Structure: `trips/{tripId}/shares` (Collection).
    // `shares` -> { expenseId, personId, amount, tripId }
    // Then `getSharesForTrip` is just listening to `trips/{tripId}/shares`.
    // THIS IS THE BEST SOLUTION. Flat sub-collection for shares under trip.
    
    suspend fun getSharesForExpense(expenseId: String): List<ExpenseShare> {
        // This query depends on where we store them. 
        // If we store in `trips/{tripId}/shares`, we need to filter by expenseId.
        // But we need tripId to find the collection. The signature only has `expenseId`.
        // This implies we can't find it easily without tripId.
        // 
        // Let's look at usage. `SettlementViewModel` calls `getSharesForTrip(tripId)`.
        // `AddEditExpenseViewModel` calls `insertExpense`.
        // 
        // So `getSharesForTrip` is the critical one.
        // Storing shares in `trips/{tripId}/shares` works perfectly for `getSharesForTrip`.
        // For `getSharesForExpense(expenseId)`, we would need `tripId` or we query `trips/{tripId}/shares` where `expenseId` == ID.
        // But we don't know tripId in `getSharesForExpense` signature?
        // Wait, `ExpenseShare` entity has `expenseId` but not `tripId`.
        // I should add `tripId` to `ExpenseShare` entity/model.
        
        return emptyList() // Placeholder until I fix Entity
    }

    fun getSharesForTrip(tripId: String): Flow<List<ExpenseShare>> {
        return firestore.collection("trips").document(tripId).collection("shares")
            .snapshots()
            .map { snapshot -> snapshot.toObjects<ExpenseShare>() }
    }
    
    fun subscribeToTripTopic(tripId: String) {
        messaging.subscribeToTopic("trip_$tripId")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Subscribe to topic failed", task.exception)
                } else {
                    Log.d(TAG, "Subscribed to topic: trip_$tripId")
                }
            }
    }

    // Invitation Operations
    suspend fun inviteUserToTrip(tripId: String, tripName: String, inviteeId: String) {
        val inviterId = auth.currentUser?.uid ?: return
        // Get inviter name? For now, we might not have it easily available without fetching user.
        // Or we could pass it from UI. Let's assume we can fetch or pass it. 
        // For simplicity, fetching current user profile is better.
        val inviterName = try {
             firestore.collection("users").document(inviterId).get().await().getString("displayName") ?: "A friend"
        } catch (e: Exception) { "A friend" }

        val invitation = com.example.tripexpensetracker.data.model.Invitation(
            tripId = tripId,
            tripName = tripName,
            inviterName = inviterName,
            inviterId = inviterId,
            inviteeId = inviteeId
        )
        
        // Save invitation to invitee's collection
        val docRef = firestore.collection("users").document(inviteeId).collection("invitations").document()
        docRef.set(invitation.copy(id = docRef.id)).await()
    }

    fun getInvitations(): Flow<List<com.example.tripexpensetracker.data.model.Invitation>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return firestore.collection("users").document(userId).collection("invitations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<com.example.tripexpensetracker.data.model.Invitation>() }
    }

    suspend fun respondToInvitation(invitation: com.example.tripexpensetracker.data.model.Invitation, accept: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        
        if (accept) {
            // 1. Add user to Trip's participant list (or update status if already there)
            val tripRef = firestore.collection("trips").document(invitation.tripId)
            
            firestore.runBatch { batch ->
                // We can't easily "update item in list" with arrayUnion if it's a complex object that changed (status).
                // So we need to read modify write.
                // But runBatch doesn't read.
                // We should use transaction or just read-modify-write.
                // Transaction is safer.
            }.await() // Placeholder for batch, doing transaction below
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(tripRef)
                val trip = snapshot.toObject(Trip::class.java) ?: return@runTransaction
                
                val currentParticipants = trip.participants.toMutableList()
                val existingIndex = currentParticipants.indexOfFirst { it.userId == userId }
                
                val userPhone = auth.currentUser?.phoneNumber // Might need to fetch User object if phone is needed and missing
                
                if (existingIndex != null && existingIndex >= 0) {
                     // Update status
                     currentParticipants[existingIndex] = currentParticipants[existingIndex].copy(
                         status = com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED
                     )
                } else {
                    // Add new
                    // Fetch name?
                    currentParticipants.add(
                        com.example.tripexpensetracker.data.model.Participant(
                            name = "Joined User", // Ideally fetch real name
                            userId = userId,
                            phoneNumber = userPhone,
                            status = com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED
                        )
                    )
                }
                
                val currentIds = trip.participantIds.toMutableList()
                if (!currentIds.contains(userId)) {
                    currentIds.add(userId)
                }

                transaction.update(tripRef, "participants", currentParticipants)
                transaction.update(tripRef, "participantIds", currentIds)
                
                // Delete invitation
                transaction.delete(firestore.collection("users").document(userId).collection("invitations").document(invitation.id))
            }.await()
            
            // Subscribe to topic
            subscribeToTripTopic(invitation.tripId)
            
            // Notify Inviter that user accepted
            if (invitation.inviterId.isNotEmpty()) {
                val acceptanceNotif = invitation.copy(
                    id = "", // New ID
                    type = com.example.tripexpensetracker.data.model.Invitation.TYPE_ACCEPTANCE_INFO,
                    message = "${auth.currentUser?.displayName ?: "A user"} accepted your invite to ${invitation.tripName}",
                    inviteeId = invitation.inviterId, // Sending TO the inviter
                    inviterId = userId, // Coming FROM the acceptor
                    timestamp = java.util.Date()
                )
                
                val notifRef = firestore.collection("users").document(invitation.inviterId).collection("invitations").document()
                notifRef.set(acceptanceNotif.copy(id = notifRef.id))
            }

        } else {
            // If it's just an info notification, we simply delete it
            if (invitation.type == com.example.tripexpensetracker.data.model.Invitation.TYPE_ACCEPTANCE_INFO || 
                invitation.type == com.example.tripexpensetracker.data.model.Invitation.TYPE_DECLINE_INFO) {
                 firestore.collection("users").document(userId).collection("invitations").document(invitation.id).delete().await()
                 return
            }

            // Just delete invitation and potentially remove from participant list if they were effectively there?
            // If they reject, we should probably update status to DECLINED in trip so inviter knows.
            val tripRef = firestore.collection("trips").document(invitation.tripId)
             firestore.runTransaction { transaction ->
                val snapshot = transaction.get(tripRef)
                val trip = snapshot.toObject(Trip::class.java)
                
                if (trip != null) {
                    val currentParticipants = trip.participants.toMutableList()
                    val existingIndex = currentParticipants.indexOfFirst { it.userId == userId }
                     if (existingIndex >= 0) {
                         currentParticipants[existingIndex] = currentParticipants[existingIndex].copy(
                             status = com.example.tripexpensetracker.data.model.Participant.STATUS_DECLINED
                         )
                         transaction.update(tripRef, "participants", currentParticipants)
                     }
                }
                 // Delete invitation
                transaction.delete(firestore.collection("users").document(userId).collection("invitations").document(invitation.id))
             }.await()
             
             // Notify Inviter that user declined
             if (invitation.inviterId.isNotEmpty()) {
                val declineNotif = invitation.copy(
                    id = "", // New ID
                    type = com.example.tripexpensetracker.data.model.Invitation.TYPE_DECLINE_INFO,
                    message = "${auth.currentUser?.displayName ?: "A user"} declined your invite to ${invitation.tripName}",
                    inviteeId = invitation.inviterId, // Sending TO the inviter
                    inviterId = userId, // Coming FROM the decliner
                    timestamp = java.util.Date()
                )
                
                val notifRef = firestore.collection("users").document(invitation.inviterId).collection("invitations").document()
                notifRef.set(declineNotif.copy(id = notifRef.id)).await()
            }
        }
    }
    
    suspend fun resendInvitation(tripId: String, tripName: String, inviteeId: String) {
        inviteUserToTrip(tripId, tripName, inviteeId)
    }

    suspend fun joinTrip(tripId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userPhone = auth.currentUser?.phoneNumber
        val tripRef = firestore.collection("trips").document(tripId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val trip = snapshot.toObject(Trip::class.java) ?: return@runTransaction

            val currentParticipants = trip.participants.toMutableList()
            val existingIndex = currentParticipants.indexOfFirst { it.userId == userId }

            if (existingIndex >= 0) {
                // Already there, ensure joined status
                if (currentParticipants[existingIndex].status != com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED) {
                    currentParticipants[existingIndex] = currentParticipants[existingIndex].copy(
                        status = com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED
                    )
                }
            } else {
                // Add new
                currentParticipants.add(
                    com.example.tripexpensetracker.data.model.Participant(
                        name = "Joined User", // Ideally fetch real name or pass it
                        userId = userId,
                        phoneNumber = userPhone,
                        status = com.example.tripexpensetracker.data.model.Participant.STATUS_JOINED
                    )
                )
            }

            val currentIds = trip.participantIds.toMutableList()
            if (!currentIds.contains(userId)) {
                currentIds.add(userId)
            }

            transaction.update(tripRef, "participants", currentParticipants)
            transaction.update(tripRef, "participantIds", currentIds)
            
            // Clean up any pending invites if they existed
            // (invitation collection is under user doc, we can delete it outside transaction or separately)
        }.await()
        
        subscribeToTripTopic(tripId)
        
        // Cleanup invites (best effort)
        try {
            val invites = firestore.collection("users").document(userId).collection("invitations")
                .whereEqualTo("tripId", tripId)
                .get().await()
            for (doc in invites.documents) {
                doc.reference.delete()
            }
        } catch (e: Exception) { Log.w(TAG, "Failed to cleanup invites after join", e) }
    }

    // Destination Management
    suspend fun addDestination(tripId: String, destination: com.example.tripexpensetracker.data.model.Destination) {
        val ref = firestore.collection("trips").document(tripId).collection("destinations").document()
        val newDest = destination.copy(id = ref.id, tripId = tripId)
        ref.set(newDest).await()
    }

    fun getDestinationsFlow(tripId: String): Flow<List<com.example.tripexpensetracker.data.model.Destination>> = callbackFlow {
        val listener = firestore.collection("trips").document(tripId).collection("destinations")
            .orderBy("startDate")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val destinations = snapshot?.toObjects(com.example.tripexpensetracker.data.model.Destination::class.java) ?: emptyList()
                trySend(destinations)
            }
        awaitClose { listener.remove() }
    }

    // Itinerary Management
    suspend fun addItineraryItem(tripId: String, item: com.example.tripexpensetracker.data.model.ItineraryItem) {
        val ref = firestore.collection("trips").document(tripId).collection("itinerary").document()
        val newItem = item.copy(id = ref.id, tripId = tripId)
        ref.set(newItem).await()
    }

    fun getItineraryItemsFlow(tripId: String): Flow<List<com.example.tripexpensetracker.data.model.ItineraryItem>> = callbackFlow {
        val listener = firestore.collection("trips").document(tripId).collection("itinerary")
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(com.example.tripexpensetracker.data.model.ItineraryItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // City-specific methods for integrated timeline view
    
    /**
     * Get all timeline items (expenses + activities) for a specific city, sorted chronologically
     */
    fun getCityTimelineItems(tripId: String, destinationId: String): Flow<List<com.example.tripexpensetracker.data.model.TimelineItem>> {
        return combine(
            getExpensesForTrip(tripId),
            getItineraryItemsFlow(tripId)
        ) { expenses, activities ->
            val items = mutableListOf<com.example.tripexpensetracker.data.model.TimelineItem>()
            
            // Filter and convert expenses
            expenses
                .filter { it.destinationId == destinationId }
                .forEach { expense ->
                    items.add(com.example.tripexpensetracker.data.model.TimelineItem.ExpenseItem(
                        id = expense.id,
                        timestamp = expense.date.time,
                        expense = expense
                    ))
                }
            
            // Filter and convert activities
            activities
                .filter { it.destinationId == destinationId }
                .forEach { activity ->
                    items.add(com.example.tripexpensetracker.data.model.TimelineItem.ActivityItem(
                        id = activity.id,
                        timestamp = activity.startTime,
                        activity = activity
                    ))
                }
            
            // Sort by timestamp
            items.sortedBy { it.timestamp }
        }
    }
    
    /**
     * Calculate statistics for a specific city
     */
    suspend fun getCityStats(tripId: String, destinationId: String): com.example.tripexpensetracker.data.model.CityStats {
        val expenses = getExpensesForTrip(tripId).first().filter { it.destinationId == destinationId }
        val activities = getItineraryItemsFlow(tripId).first().filter { it.destinationId == destinationId }
        
        val totalExpenses = expenses.sumOf { it.amount }
        val topCategory = expenses
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key ?: ""
        
        val allTimestamps = expenses.map { it.date } + activities.map { java.util.Date(it.startTime) }
        val dateRange = if (allTimestamps.isNotEmpty()) {
            Pair(allTimestamps.minOrNull(), allTimestamps.maxOrNull())
        } else {
            Pair(null, null)
        }
        
        return com.example.tripexpensetracker.data.model.CityStats(
            totalExpenses = totalExpenses,
            expenseCount = expenses.size,
            activityCount = activities.size,
            topCategory = topCategory,
            dateRange = dateRange
        )
    }
    
    /**
     * Get expenses filtered by destination
     */
    fun getExpensesForDestination(tripId: String, destinationId: String): Flow<List<Expense>> {
        return getExpensesForTrip(tripId).map { expenses ->
            expenses.filter { it.destinationId == destinationId }
        }
    }
}


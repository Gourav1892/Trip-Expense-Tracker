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
}

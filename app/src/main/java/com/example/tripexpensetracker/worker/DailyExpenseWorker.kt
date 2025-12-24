package com.example.tripexpensetracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tripexpensetracker.util.NotificationHelper
import com.example.tripexpensetracker.data.model.Trip
import com.example.tripexpensetracker.data.model.Expense
import com.example.tripexpensetracker.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

@HiltWorker
class DailyExpenseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()
        
        try {
            // 1. Get all trips where user is a participant
            val tripsSnapshot = firestore.collection("trips")
                .whereArrayContains("participantIds", userId)
                .get()
                .await()
            
            val trips = tripsSnapshot.toObjects(Trip::class.java)
            if (trips.isEmpty()) return Result.success()

            var totalSpentToday = 0.0
            val tripNames = mutableListOf<String>()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val joyStartOfDay = calendar.time
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.time

            for (trip in trips) {
                // 2. Find My Person ID in this trip
                // We need to query the 'people' collection to find the person document related to this userId
                val peopleSnapshot = firestore.collection("trips").document(trip.id).collection("people")
                    .whereEqualTo("userId", userId)
                    .get().await()
                
                // If using old schema where userId might not be on Person, we fall back? 
                // Assumes Person has userId set (which we firmly established in Join/Add logic).
                val myPerson = peopleSnapshot.toObjects(Person::class.java).firstOrNull() ?: continue

                // 3. Query expenses for this trip for today AND paidBy this person
                val expensesSnapshot = firestore.collection("trips").document(trip.id).collection("expenses")
                    .whereGreaterThanOrEqualTo("date", joyStartOfDay)
                    .whereLessThan("date", endOfDay)
                    .whereEqualTo("paidByPersonId", myPerson.id)
                    .get()
                    .await()
                
                val expenses = expensesSnapshot.toObjects(Expense::class.java)
                val tripSum = expenses.sumOf { it.amount }
                
                if (tripSum > 0) {
                    totalSpentToday += tripSum
                    tripNames.add(trip.name)
                }
            }

            if (totalSpentToday > 0) {
                NotificationHelper.showNotification(
                    applicationContext, 
                    "Daily Expense Summary", 
                    "You spent $${String.format("%.2f", totalSpentToday)} today across ${tripNames.size} trip(s)."
                )
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

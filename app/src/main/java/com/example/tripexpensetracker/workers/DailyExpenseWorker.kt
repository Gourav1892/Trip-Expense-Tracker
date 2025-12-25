package com.example.tripexpensetracker.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tripexpensetracker.data.repository.TripRepository
import com.example.tripexpensetracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class DailyExpenseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TripRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val trips = repository.getAllTrips().first()
            if (trips.isEmpty()) return Result.success()

            var totalDailyExpense = 0.0
            var activeTripCount = 0
            var currencySymbol = "₹" // Default or take from first trip

            val calendar = Calendar.getInstance()
            val todayYear = calendar.get(Calendar.YEAR)
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

            for (trip in trips) {
                // Determine if trip is "active" or just relevant?
                // We should check expenses for ALL trips the user has join.
                val expenses = repository.getExpensesForTrip(trip.id).first()
                
                val dailyExpenses = expenses.filter { expense ->
                    calendar.time = expense.date
                    calendar.get(Calendar.YEAR) == todayYear &&
                    calendar.get(Calendar.DAY_OF_YEAR) == todayDay
                }

                if (dailyExpenses.isNotEmpty()) {
                    val sum = dailyExpenses.sumOf { it.amount }
                    totalDailyExpense += sum
                    activeTripCount++
                    currencySymbol = trip.currencySymbol // Use symbol from one of the active trips
                }
            }

            if (totalDailyExpense > 0) {
                NotificationHelper.showDailySummaryNotification(
                    applicationContext,
                    totalDailyExpense,
                    currencySymbol,
                    activeTripCount
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

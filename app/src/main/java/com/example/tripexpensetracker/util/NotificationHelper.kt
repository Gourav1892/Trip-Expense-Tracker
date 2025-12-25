package com.example.tripexpensetracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tripexpensetracker.MainActivity
import com.example.tripexpensetracker.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "trip_updates"
    private const val CHANNEL_NAME = "Trip Updates"
    private const val CHANNEL_DESCRIPTION = "Notifications for new expenses and trip updates"
    
    // New Channel for Daily Summaries
    private const val DAILY_CHANNEL_ID = "daily_summary"
    private const val DAILY_CHANNEL_NAME = "Daily Expense Summary"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val dailyChannel = NotificationChannel(DAILY_CHANNEL_ID, DAILY_CHANNEL_NAME, importance).apply {
                description = "Daily summary of your spending"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(channel, dailyChannel))
        }
    }

    fun showNotification(context: Context, title: String?, body: String?) {
        // Basic permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, cannot show notification.
                // ideally request it, but we can't request from a Service easily.
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Use a unique ID (e.g., current time) to not overwrite previous notifications immediately
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun showDailySummaryNotification(context: Context, totalAmount: Double, currencySymbol: String, tripCount: Int) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (tripCount > 1) {
            "You spent $currencySymbol${String.format("%.2f", totalAmount)} across $tripCount trips today."
        } else {
            "You spent $currencySymbol${String.format("%.2f", totalAmount)} today."
        }

        val builder = NotificationCompat.Builder(context, DAILY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Daily Spending Summary")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Fixed ID for summary so it updates instead of stacking? Or unique? 
        // Let's use unique to see history, or fixed 2001 to update. Fixed is better for "Summary".
        notificationManager.notify(2001, builder.build())
    }
}

package com.example.medicationtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra("medication_name") ?: "Your medication"
        val dosage = intent.getStringExtra("dosage") ?: "Unknown dosage"

        val notificationMessage = "Time to take your medication: $medicationName, Dosage: $dosage"

        val builder = NotificationCompat.Builder(context, "medicationChannel")
            .setSmallIcon(R.drawable.ic_medication_notification) // Use your own notification icon
            .setContentTitle("$medicationName Reminder")
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage)) // Ensure longer text is displayed properly

        with(NotificationManagerCompat.from(context)) {
            val medicationId = intent.getIntExtra("medication_id", 0) // Example default
            notify(medicationId, builder.build())
        }
    }
}
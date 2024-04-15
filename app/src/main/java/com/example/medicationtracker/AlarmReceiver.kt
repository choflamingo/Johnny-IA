package com.example.medicationtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * BroadcastReceiver that handles the event of an alarm triggering.
 * It constructs and displays a notification reminding the user to take their medication.
 */
class AlarmReceiver : BroadcastReceiver() {
    /**
     * This method is called when the BroadcastReceiver receives an Intent broadcast.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Retrieve medication name and dosage from the intent; provide defaults if not found
        val medicationName = intent.getStringExtra("medication_name") ?: "Your medication"
        val dosage = intent.getStringExtra("dosage") ?: "Unknown dosage"

        // Construct the notification message content
        val notificationMessage = "Time to take your medication: $medicationName, Dosage: $dosage"

        // Setup the notification builder, specifying the channel and settings for the notification
        val builder = NotificationCompat.Builder(context, "medicationChannel")
            .setSmallIcon(R.drawable.ic_medication_notification) // Set an icon for the notification
            .setContentTitle("$medicationName Reminder") // Set the title of the notification
            .setContentText(notificationMessage) // Set the content text for the notification
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority to high
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage)) // Use big text style for longer text

        // Issue the notification with a unique ID derived from the medication
        with(NotificationManagerCompat.from(context)) {
            val medicationId = intent.getIntExtra("medication_id", 0) // Use 0 as default medication ID if not found
            notify(medicationId, builder.build()) // Notify user with the built notification
        }
    }
}
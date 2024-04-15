package com.example.medicationtracker

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import com.google.gson.Gson

/**
 * Activity for adding or editing medication details. Users can set the name, dosage, frequency,
 * start date, and time for alarms associated with the medication.
 */
class MedicationDetailsActivity : AppCompatActivity() {

    // UI components that are initialized post-creation
    private lateinit var editTextMedicationName: EditText
    private lateinit var editTextDosage: EditText
    private lateinit var editTextFrequency: EditText
    private lateinit var buttonPickDate: Button
    private lateinit var buttonPickTime: Button
    private lateinit var buttonSubmitMedication: Button

    // Variables to store user input for date and time
    private var selectedDate = ""
    private var selectedTime = ""

    // Holds the medication object when editing
    private var medication: Medication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_details)

        initializeUI()
        loadExistingMedication()
    }

    /**
     * Initializes UI components and sets up event handlers.
     */
    private fun initializeUI() {
        // Assign each UI component to its corresponding view
        editTextMedicationName = findViewById(R.id.editTextMedicationName)
        editTextDosage = findViewById(R.id.editTextDosage)
        editTextFrequency = findViewById(R.id.editTextFrequency)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonPickTime = findViewById(R.id.buttonPickTime)
        buttonSubmitMedication = findViewById(R.id.buttonSubmitMedication)

        // Initialize handlers for date and time selection
        setupDateAndTimePickers()
        // Initialize the handler for submitting medication data
        setupSubmitButton()
        // Add a simple back button listener
        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }
    }

    /**
     * Loads medication data into the form if existing data is passed through the intent.
     */
    private fun loadExistingMedication() {
        // Attempt to deserialize the medication JSON passed in the intent
        intent.getStringExtra("medication")?.let {
            medication = Gson().fromJson(it, Medication::class.java)
            medication?.let { med ->
                // Set the text fields and selected date/time if editing an existing medication
                editTextMedicationName.setText(med.name)
                editTextDosage.setText(med.dosage)
                editTextFrequency.setText(med.frequency)
                selectedDate = med.startDate
                selectedTime = med.startTime
            }
        }
    }

    /**
     * Sets up the date and time pickers.
     */
    private fun setupDateAndTimePickers() {
        buttonPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Format the date as day/month/year and update the UI
                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                    Toast.makeText(this, "Date set: $selectedDate", Toast.LENGTH_SHORT).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonPickTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    // Format the time as hour:minute and update the UI
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    Toast.makeText(this, "Time set: $selectedTime", Toast.LENGTH_SHORT).show()
                },
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    /**
     * Schedules an alarm for the medication based on the details provided.
     * @param medication The medication object with complete details.
     */
    private fun scheduleAlarm(medication: Medication) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            // Pass essential medication details to the alarm receiver
            putExtra("medication_name", medication.name)
            putExtra("dosage", medication.dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medication.medicationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate the exact time when the alarm should start
        val scheduledTime = Calendar.getInstance().apply {
            val dateParts = medication.startDate.split("/").map { it.toInt() }
            val timeParts = medication.startTime.split(":").map { it.toInt() }
            set(Calendar.YEAR, dateParts[2])
            set(Calendar.MONTH, dateParts[1] - 1)
            set(Calendar.DAY_OF_MONTH, dateParts[0])
            set(Calendar.HOUR_OF_DAY, timeParts[0])
            set(Calendar.MINUTE, timeParts[1])
            set(Calendar.SECOND, 0)
        }.timeInMillis

        if (scheduledTime <= System.currentTimeMillis()) {
            // Prevent setting an alarm in the past
            Toast.makeText(this, "Cannot set an alarm in the past", Toast.LENGTH_LONG).show()
            return
        }

        // Check if the frequency indicates a repeating alarm
        if (medication.frequency == "00:00:00") {
            // Set a one-time alarm
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
        } else {
            // Calculate repeat interval and set a repeating alarm
            val (days, hours, minutes) = medication.frequency.split(":").map { it.toInt() }
            val repeatIntervalMillis = days * 24 * 3600000L + hours * 3600000L + minutes * 60000L
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                scheduledTime,
                repeatIntervalMillis,
                pendingIntent
            )
        }
        Toast.makeText(this, "Alarm set for ${medication.name}", Toast.LENGTH_SHORT).show()
        Log.d(
            "MedicationTracker",
            "Scheduling alarm for medication ID: ${medication.medicationId}, Name: ${medication.name}, Time: ${medication.startTime}, Date: ${medication.startDate}, Frequency: ${medication.frequency}"
        )
        // Debug log for tracking alarm scheduling
    }

    /**
     * Sets up the submit button with validation and submission logic.
     */
    private fun setupSubmitButton() {
        buttonSubmitMedication.setOnClickListener {
            if (validateInputs()) {
                val isEditing = medication != null
                val medId =
                    medication?.medicationId ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val newMedication = Medication(
                    medId,
                    editTextMedicationName.text.toString(),
                    editTextDosage.text.toString(),
                    editTextFrequency.text.toString(),
                    selectedDate,
                    selectedTime
                )

                scheduleAlarm(newMedication)

                val data = Intent().apply {
                    putExtra("medication", Gson().toJson(newMedication))
                    putExtra("isEditing", isEditing)
                }
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    /**
     * Validates all input fields to ensure they are appropriately filled.
     */
    private fun validateInputs(): Boolean {
        if (editTextMedicationName.text.isBlank()) {
            toast("Please enter the medication name.")
            return false
        }
        if (editTextDosage.text.isBlank()) {
            toast("Please enter the dosage.")
            return false
        }
        if (editTextFrequency.text.isBlank()) {
            toast("Please enter the frequency.")
            return false
        }
        if (selectedDate.isBlank()) {
            toast("Please pick a start date.")
            return false
        }
        if (selectedTime.isBlank()) {
            toast("Please pick a start time.")
            return false
        }
        return true  // All validations passed
    }

    /**
     * Displays a toast message.
     * @param message The message to display in the toast.
     */
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
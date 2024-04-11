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

class MedicationDetailsActivity : AppCompatActivity() {

    private lateinit var editTextMedicationName: EditText
    private lateinit var editTextDosage: EditText
    private lateinit var editTextFrequency: EditText
    private lateinit var buttonPickDate: Button
    private lateinit var buttonPickTime: Button
    private lateinit var buttonSubmitMedication: Button

    private var selectedDate = ""
    private var selectedTime = ""
    private var medication: Medication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_details)

        initializeUI()
        loadExistingMedication()
    }

    private fun initializeUI() {
        editTextMedicationName = findViewById(R.id.editTextMedicationName)
        editTextDosage = findViewById(R.id.editTextDosage)
        editTextFrequency = findViewById(R.id.editTextFrequency)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonPickTime = findViewById(R.id.buttonPickTime)
        buttonSubmitMedication = findViewById(R.id.buttonSubmitMedication)

        setupDateAndTimePickers()
        setupSubmitButton()
        findViewById<Button>(R.id.buttonBack).setOnClickListener { finish() }
    }

    private fun loadExistingMedication() {
        // Check if we're editing an existing medication
        intent.getStringExtra("medication")?.let {
            medication = Gson().fromJson(it, Medication::class.java)
            medication?.let { med ->
                editTextMedicationName.setText(med.name)
                editTextDosage.setText(med.dosage)
                editTextFrequency.setText(med.frequency)
                selectedDate = med.startDate
                selectedTime = med.startTime
            }
        }
    }

    private fun setupDateAndTimePickers() {
        buttonPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate = "$dayOfMonth/${month + 1}/$year"
                Toast.makeText(this, "Date set: $selectedDate", Toast.LENGTH_SHORT).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                Toast.makeText(this, "Time set: $selectedTime", Toast.LENGTH_SHORT).show()
            }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true).show()
        }
    }

    private fun scheduleAlarm(medication: Medication) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medication_name", medication.name)
            putExtra("dosage", medication.dosage)
            // Include any other data that the receiver might need
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, medication.medicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ensure that the scheduled time is in the future
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
            Toast.makeText(this, "Cannot set an alarm in the past", Toast.LENGTH_LONG).show()
            return
        }
        if (medication.frequency == "00:00:00") {
            // For a one-time alarm
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
        } else {
            // For repeating alarms, parse the frequency to determine repeat interval
            val (days, hours, minutes) = medication.frequency.split(":").map { it.toInt() }
            val repeatIntervalMillis = days * 24 * 3600000L + hours * 3600000L + minutes * 60000L // Convert hours and minutes to milliseconds
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, scheduledTime, repeatIntervalMillis, pendingIntent)
        }
        Toast.makeText(this, "Alarm set for ${medication.name}", Toast.LENGTH_SHORT).show()
        Log.d("MedicationTracker", "Scheduling alarm for medication ID: ${medication.medicationId}, Name: ${medication.name}, Time: ${medication.startTime}, Date: ${medication.startDate}, Frequency: ${medication.frequency}")

    }

    private fun setupSubmitButton() {
        buttonSubmitMedication.setOnClickListener {
            if (validateInputs()) {
                val isEditing = medication != null
                val medId = medication?.medicationId ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val newMedication = Medication(
                    medId,
                    editTextMedicationName.text.toString(),
                    editTextDosage.text.toString(),
                    editTextFrequency.text.toString(),
                    selectedDate,
                    selectedTime
                )

                // Schedule the alarm before finishing the activity
                scheduleAlarm(newMedication)

                // Prepare data to send back to MainActivity
                val data = Intent().apply {
                    putExtra("medication", Gson().toJson(newMedication))
                    putExtra("isEditing", isEditing)
                }
                setResult(RESULT_OK, data)

                // Finish should be called after scheduling the alarm
                finish()
            }
        }
    }


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
                // All validations passed
                return true
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

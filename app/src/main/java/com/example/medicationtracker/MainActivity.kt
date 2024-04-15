package com.example.medicationtracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Main Activity class that manages the display and manipulation of a list of medications.
 * It supports adding, editing, and deleting medication entries and managing their notifications.
 *
 * @property recyclerView Displays the list of medications.
 * @property addMedicationFab Button to trigger the addition of a new medication.
 * @property medicationList List containing all current medications.
 * @property adapter Adapter for the recyclerView to manage displaying medication items.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addMedicationFab: FloatingActionButton
    private val medicationList = mutableListOf<Medication>()
    private lateinit var adapter: MedicationAdapter

    // Result handler that updates the medication list after adding or editing
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val isEditing = data.getBooleanExtra("isEditing", false)
                val medication = Gson().fromJson(data.getStringExtra("medication"), Medication::class.java)

                if (isEditing) {
                    val index = medicationList.indexOfFirst { it.medicationId == medication.medicationId }
                    if (index != -1) {
                        medicationList[index] = medication
                        adapter.notifyItemChanged(index)
                    }
                } else {
                    medicationList.add(medication)
                    adapter.notifyItemInserted(medicationList.size - 1)
                }
                saveMedicationList(medicationList)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
        loadMedications()
        createNotificationChannel()
    }

    /** Sets up the RecyclerView, FloatingActionButton, and their related actions. */
    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerViewMedications)
        addMedicationFab = findViewById(R.id.addMedicationFab)
        adapter = MedicationAdapter(medicationList, onEditClick = this::editMedication, onDeleteClick = this::deleteMedication)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        addMedicationFab.setOnClickListener { startForResult.launch(Intent(this, MedicationDetailsActivity::class.java)) }
    }

    /** Launches the MedicationDetailsActivity for editing an existing medication.
     * @param position The position in the list of the medication to edit.
     */
    private fun editMedication(position: Int) {
        val medication = medicationList[position]
        val intent = Intent(this, MedicationDetailsActivity::class.java).apply {
            putExtra("isEditing", true)
            putExtra("medication", Gson().toJson(medication))
        }
        startForResult.launch(intent)
    }

    /** Deletes a medication entry from the list and cancels its alarm.
     * @param position The position in the list of the medication to delete.
     */
    private fun deleteMedication(position: Int) {
        if (position in medicationList.indices) {
            val medicationId = medicationList[position].medicationId
            cancelAlarm(medicationId)
            medicationList.removeAt(position)
            adapter.notifyItemRemoved(position)
            saveMedicationList(medicationList)
        } else {
            Log.e("MainActivity", "Attempt to delete item at invalid position: $position") // Log error for invalid position attempt
        }
    }

    /** Loads medication entries from SharedPreferences. */
    private fun loadMedications() {
        val sharedPrefs = getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE)
        medicationList.clear()
        medicationList.addAll(Gson().fromJson(sharedPrefs.getString("medications", "[]"), object : TypeToken<List<Medication>>() {}.type))
        adapter.notifyDataSetChanged()
    }

    /** Saves the current list of medications to SharedPreferences.
     * @param medications The list of medications to save
     */
    private fun saveMedicationList(medications: List<Medication>) {
        getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE).edit().putString("medications", Gson().toJson(medications)).apply()
    }

    /** Creates a notification channel for Android Oreo and above, required for notifications. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("medicationChannel", getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    /** Cancels a scheduled alarm associated with a medication ID.
     * @param medicationId The unique ID of the medication whose alarm should be canceled.
     */
    private fun cancelAlarm(medicationId: Int) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, medicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        Log.d("MedicationTracker", "Canceled alarm for medication ID: $medicationId") // Debug log for tracking alarm cancellation
    }
}
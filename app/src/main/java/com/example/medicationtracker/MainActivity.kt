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

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addMedicationFab: FloatingActionButton
    private val medicationList = mutableListOf<Medication>()
    private lateinit var adapter: MedicationAdapter

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

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerViewMedications)
        addMedicationFab = findViewById(R.id.addMedicationFab)
        adapter = MedicationAdapter(medicationList, onEditClick = this::editMedication, onDeleteClick = this::deleteMedication)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        addMedicationFab.setOnClickListener { startForResult.launch(Intent(this, MedicationDetailsActivity::class.java)) }
    }

    private fun editMedication(position: Int) {
        val medication = medicationList[position]
        val intent = Intent(this, MedicationDetailsActivity::class.java).apply {
            putExtra("isEditing", true)
            putExtra("medication", Gson().toJson(medication))
        }
        startForResult.launch(intent)
    }

    private fun deleteMedication(position: Int) {
        if (position in medicationList.indices) {
            val medicationId = medicationList[position].medicationId
            cancelAlarm(medicationId)
            medicationList.removeAt(position)
            adapter.notifyItemRemoved(position)
            saveMedicationList(medicationList)
        } else {
            Log.e("MainActivity", "Attempt to delete item at invalid position: $position")
        }
    }

    private fun loadMedications() {
        val sharedPrefs = getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE)
        medicationList.clear()
        medicationList.addAll(Gson().fromJson(sharedPrefs.getString("medications", "[]"), object : TypeToken<List<Medication>>() {}.type))
        adapter.notifyDataSetChanged()
    }

    private fun saveMedicationList(medications: List<Medication>) {
        getSharedPreferences("MedicationPrefs", Context.MODE_PRIVATE).edit().putString("medications", Gson().toJson(medications)).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("medicationChannel", getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun cancelAlarm(medicationId: Int) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, medicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        Log.d("MedicationTracker", "Canceled alarm for medication ID: $medicationId")
    }
}

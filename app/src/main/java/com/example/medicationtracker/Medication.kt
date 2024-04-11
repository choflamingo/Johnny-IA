package com.example.medicationtracker

data class Medication(
    val medicationId: Int,
    val name: String,
    val dosage: String,
    val frequency: String,
    val startDate: String,
    val startTime: String
)
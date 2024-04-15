package com.example.medicationtracker

/**
 * Data class representing a medication.
 *
 * @property medicationId Unique identifier for the medication.
 * @property name Name of the medication.
 * @property dosage Information about how much of the medication is to be taken each time.
 * @property frequency Describes how often the medication should be taken.
 * @property startDate The date when the medication is to be started.
 * @property startTime The time of day the medication is first to be taken.
 */
data class Medication(
    val medicationId: Int,
    val name: String,
    val dosage: String,
    val frequency: String,
    val startDate: String,
    val startTime: String
)
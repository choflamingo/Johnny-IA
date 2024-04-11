package com.example.medicationtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicationAdapter(
    private val medicationList: MutableList<Medication>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textViewMedicationName)
        val textViewDosage: TextView = view.findViewById(R.id.textViewMedicationDosage)
        val textViewTime: TextView = view.findViewById(R.id.textViewMedicationTime)
        val textViewDate: TextView = view.findViewById(R.id.textViewMedicationDate)
        val buttonDelete: Button = view.findViewById(R.id.buttonDeleteMedication)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medication = medicationList[position]
        holder.textViewName.text = medication.name
        holder.textViewDosage.text = medication.dosage
        holder.textViewTime.text = medication.startTime
        holder.textViewDate.text = medication.startDate
        holder.itemView.setOnClickListener { onEditClick(medication.medicationId) }
        holder.buttonDelete.setOnClickListener {
            // Use holder.getAdapterPosition() to ensure you get the correct position
            val currentPosition = holder.adapterPosition
            if(currentPosition != RecyclerView.NO_POSITION) {
                onDeleteClick(currentPosition)
            }
        }    }

    override fun getItemCount(): Int = medicationList.size
}

package com.example.medicationtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying medication items in a RecyclerView.
 * @param medicationList List of Medication objects to display.
 * @param onEditClick Function to execute when an item's edit action is triggered.
 * @param onDeleteClick Function to execute when an item's delete action is triggered.
 */
class MedicationAdapter(
    private val medicationList: MutableList<Medication>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.ViewHolder>() {

    /**
     * Provides a reference to the views for each data item.
     * Complex data items may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewName: TextView = view.findViewById(R.id.textViewMedicationName)
        val textViewDosage: TextView = view.findViewById(R.id.textViewMedicationDosage)
        val textViewTime: TextView = view.findViewById(R.id.textViewMedicationTime)
        val textViewDate: TextView = view.findViewById(R.id.textViewMedicationDate)
        val buttonDelete: Button = view.findViewById(R.id.buttonDeleteMedication)

    }

    /**
     * Create new views (invoked by the layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return ViewHolder(view)
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medication = medicationList[position]
        holder.textViewName.text = medication.name
        holder.textViewDosage.text = medication.dosage
        holder.textViewTime.text = medication.startTime
        holder.textViewDate.text = medication.startDate
        holder.itemView.setOnClickListener { onEditClick(medication.medicationId) } // Set click listener for the whole item view for edit action
        // Set click listener for the delete button
        holder.buttonDelete.setOnClickListener {
            // Use holder.adapterPosition to ensure you get the correct position, considering item animations or changes
            val currentPosition = holder.adapterPosition
            if(currentPosition != RecyclerView.NO_POSITION) {
                onDeleteClick(currentPosition)
            }
        }    }

    /**
     * Return the size of your dataset (invoked by the layout manager).
     */
    override fun getItemCount(): Int = medicationList.size
}

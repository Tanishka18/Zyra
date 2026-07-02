package com.example.zyra.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.zyra.R

class EventAdapter(
    private val context: Context,
    private val events: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        val tvEventDetails: TextView = itemView.findViewById(R.id.tvEventDetails)
        val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        val chipRisk: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipRisk)
        val viewRiskBar: View = itemView.findViewById(R.id.viewRiskBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.tvEventType.text = "${event.type} • ${event.modality}"
        holder.tvEventDetails.text = event.details
        holder.tvEventTime.text = event.timestamp
        holder.chipRisk.text = event.risk

        // Risk-based color mapping
        val colorInt = when (event.risk.lowercase()) {
            "safe", "low" -> context.getColor(R.color.colorSafe)
            "medium" -> context.getColor(R.color.colorMediumRisk)
            "high", "danger" -> context.getColor(R.color.colorHighRisk)
            else -> context.getColor(R.color.textSecondaryLight)
        }

        // Apply to chip + left bar
        holder.chipRisk.chipBackgroundColor = android.content.res.ColorStateList.valueOf(colorInt)
        holder.viewRiskBar.setBackgroundColor(colorInt)

        // Click handling
        holder.itemView.setOnClickListener {
            onClick(event)
        }
    }

    override fun getItemCount(): Int = events.size
}

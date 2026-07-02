package com.example.zyra.ui.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.zyra.R
import com.example.zyra.model.Event
import com.example.zyra.model.EventAdapter.EventViewHolder
import com.google.android.material.chip.Chip

class TimelineAdapter(
    private val events: List<Event>,
    private val onClick: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {

    inner class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        val tvEventDetails: TextView = itemView.findViewById(R.id.tvEventDetails)
        val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        val viewRiskBar: View = itemView.findViewById(R.id.viewRiskBar)
        val chipRisk: Chip = itemView.findViewById(R.id.chipRisk)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val e = events[position]

        // Bind texts
        holder.tvEventType.text = "${e.type} • ${e.modality}"
        holder.tvEventDetails.text = e.details
        holder.tvEventTime.text = e.timestamp

        // Risk color logic
        val ctx = holder.itemView.context
        val color = when (e.risk.lowercase()) {
            "safe", "low" -> ctx.getColor(R.color.colorSafe)
            "medium" -> ctx.getColor(R.color.colorMediumRisk)
            "high", "danger" -> ctx.getColor(R.color.colorHighRisk)
            else -> ctx.getColor(R.color.textSecondaryLight)
        }

        holder.tvEventType.setTextColor(color)
        holder.viewRiskBar.setBackgroundColor(color)
        holder.chipRisk.text = e.risk
        holder.chipRisk.setChipBackgroundColorResource(
            when (e.risk.lowercase()) {
                "safe", "low" -> R.color.colorSafe
                "medium" -> R.color.colorMediumRisk
                "high", "danger" -> R.color.colorHighRisk
                else -> R.color.textSecondaryLight
            }
        )

        // Single click listener
        holder.itemView.setOnClickListener {
            onClick?.invoke(e)
        }
    }

    override fun getItemCount(): Int = events.size
}

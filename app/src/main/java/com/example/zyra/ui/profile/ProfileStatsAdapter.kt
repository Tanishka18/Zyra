package com.example.zyra.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.zyra.R
import com.example.zyra.model.Event
import com.google.android.material.chip.Chip

class ProfileStatsAdapter(private val stats: List<Event>) :
    RecyclerView.Adapter<ProfileStatsAdapter.StatViewHolder>() {

    inner class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvStatTitle)
        val tvDetails: TextView = itemView.findViewById(R.id.tvStatDetails)
        val tvTime: TextView = itemView.findViewById(R.id.tvStatTime)
        val chipRisk: Chip = itemView.findViewById(R.id.chipRisk)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        val stat = stats[position]
        holder.tvTitle.text = stat.type
        holder.tvDetails.text = stat.details
        holder.tvTime.text = stat.timestamp

        // Set chip text & background color
        holder.chipRisk.text = stat.risk
        val colorRes = when (stat.risk.lowercase()) {
            "safe", "low" -> R.color.colorSafe
            "medium" -> R.color.colorMediumRisk
            "high", "danger" -> R.color.colorHighRisk
            else -> R.color.textSecondaryLight
        }
        holder.chipRisk.setChipBackgroundColorResource(colorRes)
    }

    override fun getItemCount(): Int = stats.size
}

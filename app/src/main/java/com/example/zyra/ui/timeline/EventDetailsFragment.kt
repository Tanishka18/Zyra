package com.example.zyra.ui.timeline

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.zyra.R
import com.example.zyra.model.Event
import com.google.android.material.chip.Chip
import android.widget.TextView

class EventDetailsFragment : Fragment() {

    private lateinit var tvType: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvTime: TextView
    private lateinit var chipRisk: Chip

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event_details, container, false)

        tvType = view.findViewById(R.id.tvEventDetailType)
        tvDescription = view.findViewById(R.id.tvEventDetailDescription)
        tvTime = view.findViewById(R.id.tvEventDetailTime)
        chipRisk = view.findViewById(R.id.chipRisk)

        // Get Event from bundle
        val event: Event? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("event", Event::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Event>("event")
        }

        event?.let { e ->
            tvType.text = "${e.type} • ${e.modality}"
            tvDescription.text = e.details
            tvTime.text = e.timestamp
            chipRisk.text = e.risk

            val colorInt = when (e.risk.lowercase()) {
                "safe", "low" -> ContextCompat.getColor(requireContext(), R.color.colorSafe)
                "medium" -> ContextCompat.getColor(requireContext(), R.color.colorMediumRisk)
                "high", "danger" -> ContextCompat.getColor(requireContext(), R.color.colorHighRisk)
                else -> ContextCompat.getColor(requireContext(), R.color.textSecondaryLight)
            }

            chipRisk.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(colorInt)
        }

        return view
    }
}

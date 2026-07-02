package com.example.zyra.ui.timeline

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.zyra.R
import com.example.zyra.model.Event

class EventDetailActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("event", Event::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Event>("event")
        }

        if (event != null) {
            findViewById<TextView>(R.id.tvType).text = "${event.type} (${event.risk})"
            findViewById<TextView>(R.id.tvDetails).text = event.details
            findViewById<TextView>(R.id.tvTime).text = event.timestamp
            findViewById<TextView>(R.id.tvConfidence).text = "Confidence: ${event.confidence}%"
            findViewById<TextView>(R.id.tvRiskScore).text = "Risk Score: ${event.riskScore}"
            findViewById<TextView>(R.id.tvDescription).text = event.description
            findViewById<TextView>(R.id.tvModality).text = "Modality: ${event.modality}"
        }
    }
}

package com.example.zyra.ui.details

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.zyra.R
import com.example.zyra.model.Event

class DetailsActivity : AppCompatActivity() {

    private lateinit var tvType: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvRiskScore: TextView
    private lateinit var pbRisk: ProgressBar
    private lateinit var tvConfidence: TextView
    private lateinit var tvDetails: TextView
    private lateinit var btnMarkMe: Button
    private lateinit var btnReAuth: Button
    private lateinit var btnReport: Button
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        tvType = findViewById(R.id.tvType)
        tvTime = findViewById(R.id.tvTime)
        tvRiskScore = findViewById(R.id.tvRiskScore)
        pbRisk = findViewById(R.id.pbRisk)
        tvConfidence = findViewById(R.id.tvConfidence)
        tvDetails = findViewById(R.id.tvDetails)
        btnMarkMe = findViewById(R.id.btnMarkMe)
        btnReAuth = findViewById(R.id.btnReAuth)
        btnReport = findViewById(R.id.btnReport)

        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("event", Event::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Event>("event")
        }

        if (event != null) bind(event)
    }


    private fun bind(e: Event) {
        tvType.text = "${e.type} • ${e.modality}"
        tvTime.text = e.timestamp
        tvRiskScore.text = "Risk Score: ${e.riskScore}/100"
        pbRisk.progress = e.riskScore
        tvConfidence.text = "Confidence: ${e.confidence}%"
        tvDetails.text = e.details

        val color = when (e.type) {
            "Safe" -> getColor(R.color.colorSafe)
            "Suspicious" -> getColor(R.color.colorSuspicious)
            else -> getColor(R.color.colorDanger)
        }
        tvType.setTextColor(color)

        btnMarkMe.setOnClickListener {
            // TODO: mark feedback locally; lower score next time or tag as user-verified
            finish()
        }
        btnReAuth.setOnClickListener {
            // TODO: call biometric prompt here (next step)
        }
        btnReport.setOnClickListener {
            // TODO: open an in-app report sheet or email intent
        }
    }
}

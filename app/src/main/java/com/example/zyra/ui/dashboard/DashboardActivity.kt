package com.example.zyra.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.zyra.R
import com.example.zyra.model.Event
import com.example.zyra.service.BehavioralDataService
import com.example.zyra.service.EventRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*

class DashboardFragment : Fragment(R.layout.activity_dashboard) {

    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart

    private val riskHistory = mutableListOf<Entry>()
    private var xCounter = 1f
    private val riskCounts = mutableMapOf("Safe" to 0f, "Medium" to 0f, "High" to 0f)

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BehavioralDataService.ACTION_EVENT_UPDATE) {
                val event: Event? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BehavioralDataService.EXTRA_EVENT, Event::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Event>(BehavioralDataService.EXTRA_EVENT)
                }
                event?.let {
                    EventRepository.addEvent(requireContext(), it)
                    addEventToCharts(it)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)

        setupPieChart()
        setupLineChart()
        loadSavedEvents()
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(
            eventReceiver,
            IntentFilter(BehavioralDataService.ACTION_EVENT_UPDATE), Context.RECEIVER_NOT_EXPORTED
        )
        loadSavedEvents() // reload in case new events were added
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(eventReceiver)
    }

    private fun setupPieChart() {
        pieChart.data = PieData(PieDataSet(listOf(), "Risk Distribution"))
        pieChart.description.isEnabled = false
        pieChart.invalidate()
    }

    private fun setupLineChart() {
        lineChart.data = LineData(LineDataSet(listOf(), "Risk Over Time"))
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }

    private fun loadSavedEvents() {
        val savedEvents = EventRepository.getEvents(requireContext())
        Log.d("DashboardFragment", "Loaded ${savedEvents.size} events")

        riskCounts["Safe"] = 0f
        riskCounts["Medium"] = 0f
        riskCounts["High"] = 0f
        riskHistory.clear()
        xCounter = 1f

        savedEvents.forEach { addEventToCharts(it) }
    }

    private fun addEventToCharts(event: Event) {
        riskCounts[event.risk] = riskCounts.getOrDefault(event.risk, 0f) + 1f

        val pieEntries = listOf(
            PieEntry(riskCounts["Safe"] ?: 0f, "Safe"),
            PieEntry(riskCounts["Medium"] ?: 0f, "Suspicious"),
            PieEntry(riskCounts["High"] ?: 0f, "Danger")
        )
        val pieDataSet = PieDataSet(pieEntries, "Risk Distribution").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.teal_200),
                ContextCompat.getColor(requireContext(), R.color.purple_500),
                ContextCompat.getColor(requireContext(), R.color.red)
            )
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }
        pieChart.data = PieData(pieDataSet)
        pieChart.invalidate()

        riskHistory.add(Entry(xCounter, event.riskScore.toFloat()))
        xCounter++
        val lineDataSet = LineDataSet(riskHistory, "Risk Over Time").apply {
            color = Color.BLUE
            setCircleColor(Color.BLACK)
            lineWidth = 2f
            setDrawValues(false)
        }
        lineChart.data = LineData(lineDataSet)
        lineChart.invalidate()
    }
}

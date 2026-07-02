package com.example.zyra.ui.timeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zyra.R
import com.example.zyra.model.Event
import com.example.zyra.service.BehavioralDataService
import com.example.zyra.service.EventRepository
import com.example.zyra.ui.details.DetailsActivity
import android.content.Intent as AndroidIntent

class TimelineFragment : Fragment(R.layout.activity_timeline) {

    private lateinit var recyclerTimeline: RecyclerView
    private lateinit var adapter: TimelineAdapter
    private val events = mutableListOf<Event>()

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BehavioralDataService.ACTION_EVENT_UPDATE) {
                val event: Event? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BehavioralDataService.EXTRA_EVENT, Event::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Event>(BehavioralDataService.EXTRA_EVENT)
                }
                event?.let { addEvent(it) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTimeline = view.findViewById(R.id.recyclerTimeline)
        recyclerTimeline.layoutManager = LinearLayoutManager(requireContext())

        adapter = TimelineAdapter(events) { event ->
            val bundle = Bundle()
            bundle.putParcelable("event", event)
            findNavController().navigate(R.id.action_timelineFragment_to_eventDetailsFragment, bundle)
        }
        recyclerTimeline.adapter = adapter

        // Load saved events
        loadSavedEvents()
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(
            eventReceiver,
            IntentFilter(BehavioralDataService.ACTION_EVENT_UPDATE), Context.RECEIVER_NOT_EXPORTED
        )
        // reload in case new events were added while paused
        loadSavedEvents()
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(eventReceiver)
    }

    private fun loadSavedEvents() {
        val savedEvents = EventRepository.getEvents(requireContext())
        Log.d("TimelineFragment", "Loaded ${savedEvents.size} events")

        // always keep at least 3 "Safe" if available
        val safeEvents = savedEvents.filter { it.risk.equals("Safe", ignoreCase = true) }.take(3)

        val mergedEvents = mutableListOf<Event>()
        mergedEvents.addAll(safeEvents)
        savedEvents.forEach { evt ->
            if (!mergedEvents.any { it.id == evt.id }) {
                mergedEvents.add(evt)
            }
        }

        events.clear()
        events.addAll(mergedEvents)
        adapter.notifyDataSetChanged()
    }

    private fun addEvent(event: Event) {
        events.add(0, event)
        adapter.notifyItemInserted(0)
        recyclerTimeline.scrollToPosition(0)

        EventRepository.addEvent(requireContext(), event)
        Log.d("TimelineFragment", "New event added: $event")
    }
}

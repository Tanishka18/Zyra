package com.example.zyra.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.zyra.model.Event
import org.json.JSONArray
import org.json.JSONObject

object EventRepository {
    private const val PREFS_NAME = "events_prefs"
    private const val KEY_EVENTS = "all_events"

    // ✅ Add this init function
    fun init(context: Context) {
        prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    }
    private lateinit var prefs: SharedPreferences

    // Save event
    fun addEvent(context: Context, event: Event) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingStr = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        val arr = JSONArray(existingStr)

        // Insert new event at the start
        arr.put(0, event.toMap())

        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
        Log.d("EventRepository", "Saved events: $arr")
    }

    // Load all events
    fun getEvents(context: Context): List<Event> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        Log.d("EventRepository", "Loading events: $jsonStr")

        val arr = JSONArray(jsonStr)
        val list = mutableListOf<Event>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            try {
                list.add(Event.fromJson(obj))
            } catch (e: Exception) {
                Log.e("EventRepository", "Failed to parse event JSON: $obj", e)
            }
        }

        return list
    }

    // Optional: clear events (for testing/debugging)
    fun clearEvents(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_EVENTS).apply()
    }
}

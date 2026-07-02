package com.example.zyra.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class Event(
    val id: String,            // unique id for the event
    val type: String,          // Safe | Suspicious | Danger
    val details: String,       // plain explanation
    val timestamp: String,     // display time
    val riskScore: Int,        // 0..100
    val confidence: Int,       // 0..100 (percent)
    val modality: String,      // Touch | Typing | Motion | Location | Apps
    val description: String,   // extra notes or explanation
    val risk: String           // High | Medium | Safe
) : Parcelable
{
    // Convert Event to JSONObject
    fun toMap(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("type", type)
        obj.put("details", details)
        obj.put("timestamp", timestamp)
        obj.put("riskScore", riskScore)
        obj.put("confidence", confidence)
        obj.put("modality", modality)
        obj.put("description", description)
        obj.put("risk", risk)
        return obj
    }

    // Create Event from JSONObject
    companion object {
        fun fromJson(json: JSONObject): Event {
            return Event(
                id = json.getString("id"),
                type = json.getString("type"),
                details = json.getString("details"),
                timestamp = json.getString("timestamp"),
                riskScore = json.getInt("riskScore"),
                confidence = json.getInt("confidence"),
                modality = json.getString("modality"),
                description = json.getString("description"),
                risk = json.getString("risk")
            )
        }
    }
}
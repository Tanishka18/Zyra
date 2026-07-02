package com.example.zyra.ml

import android.hardware.SensorEvent
import android.view.MotionEvent
import android.util.Log
import com.example.zyra.service.BehavioralDataService
import kotlin.math.abs
import kotlin.math.sqrt

class BehavioralFeatureExtractor {

    // ------------------ Data Buffers (All data within the 5-second window) ------------------
    // Your buffers are correct and efficiently handled.
    private val accelerometerData = mutableListOf<FloatArray>() // [x, y, z] values
    private val gyroscopeData = mutableListOf<FloatArray>()     // [x, y, z] values
    private val touchEvents = mutableListOf<MotionEvent>()
    private val keystrokeTimestamps = mutableListOf<Long>()     // Time when a key was pressed
    private var errorCount = 0

    // --- External Data ---
    private var windowDistanceChange: Float = 0f
    private var windowAverageSpeed: Float = 0f


    // ------------------ Data Collection Functions ------------------
    fun addAccelerometerData(event: SensorEvent) {
        // Correctly clones data to handle asynchronous updates
        accelerometerData.add(event.values.clone())
    }

    fun addGyroscopeData(event: SensorEvent) {
        // Correctly clones data to handle asynchronous updates
        gyroscopeData.add(event.values.clone())
    }

    fun addTouchEvent(event: MotionEvent) {
        // CRITICAL AND CORRECT: Using MotionEvent.obtain() to prevent memory leaks
        touchEvents.add(MotionEvent.obtain(event))
    }

    fun addKeystrokeTimestamp(timestamp: Long, isError: Boolean = false) {
        keystrokeTimestamps.add(timestamp)
        if (isError) {
            errorCount++
        }
    }

    fun setExternalMovementData(distanceChange: Float, averageSpeed: Float) {
        this.windowDistanceChange = distanceChange
        this.windowAverageSpeed = averageSpeed
    }

    // ------------------ Feature Calculation Logic ------------------

    fun getFeatures(): FloatArray {
        return try {
            // Feature calculation order MUST match P2's schema:
            val tapPressure = calculateTapPressure()
            val swipeSpeed = calculateSwipeSpeed()
            val gestureLength = calculateGestureLength()
            val keystrokeInterval = calculateKeystrokeInterval()
            val errorRate = calculateErrorRate()
            val typingSpeed = calculateTypingSpeed()
            // 💡 REFINEMENT 1: Calculate magnitude variance for better feature representation
            val accelVariance = calculateMotionVarianceMagnitude(accelerometerData)
            val gyroVariance = calculateMotionVarianceMagnitude(gyroscopeData)

            val distanceChange = windowDistanceChange
            val speed = windowAverageSpeed

            floatArrayOf(
                tapPressure,
                swipeSpeed,
                gestureLength,
                keystrokeInterval,
                errorRate,
                typingSpeed,
                accelVariance,
                gyroVariance,
                distanceChange,
                speed
            )
        } catch (e: Exception) {
            // Use the established TAG for consistent logging
            Log.e(BehavioralDataService.TAG, "Feature calculation failed: ${e.message}")
            // Return array of zeros if any calculation fails (safe fallback)
            floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    // ------------------ Helper Functions (Motion & Variance) ------------------

    /**
     * REFINEMENT 1: Calculates the Variance of the MAGNITUDE.
     * This is generally a better behavioral feature as it captures how much the
     * total movement intensity changes, regardless of axis rotation.
     * (We are replacing your original `calculateMotionVariance` for robustness)
     */
    private fun calculateMotionVarianceMagnitude(data: List<FloatArray>): Float {
        if (data.isEmpty()) return 0f

        // 1. Calculate Magnitude (vector length: sqrt(x^2 + y^2 + z^2)) for every reading
        val magnitudes = data.map { values ->
            sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        }

        // 2. Calculate Mean of Magnitudes
        val mean = magnitudes.average()

        // 3. Calculate Variance of Magnitudes
        val variance = magnitudes.sumOf { (it - mean) * (it - mean) } / magnitudes.size

        return variance.toFloat()
    }

    /* 💡 NOTE: The original calculateMotionVariance that averaged variance across axes
       is removed as the magnitude variance is generally more effective. */


    // ------------------ Helper Functions (Touch, Typing, Location) ------------------
    // Your original functions for Tap Pressure, Swipe Speed, Gesture Length, Keystroke Interval,
    // Error Rate, and Typing Speed are mathematically sound and retained as is.

    private fun calculateTapPressure(): Float {
        if (touchEvents.isEmpty()) return 0f
        val pressures = touchEvents.filter { it.action == MotionEvent.ACTION_DOWN || it.action == MotionEvent.ACTION_MOVE }
            .map { it.pressure }
        return pressures.average().toFloat()
    }

    private fun calculateSwipeSpeed(): Float {
        if (touchEvents.size < 2) return 0f
        var totalSpeed = 0f
        var movementPoints = 0
        for (i in 1 until touchEvents.size) {
            val prevEvent = touchEvents[i - 1]
            val currentEvent = touchEvents[i]
            if (currentEvent.action == MotionEvent.ACTION_MOVE) {
                val dx = currentEvent.x - prevEvent.x
                val dy = currentEvent.y - prevEvent.y
                val dt = (currentEvent.eventTime - prevEvent.eventTime).toFloat() / 1000
                if (dt > 0) {
                    val distance = sqrt(dx * dx + dy * dy)
                    totalSpeed += distance / dt
                    movementPoints++
                }
            }
        }
        return if (movementPoints > 0) totalSpeed / movementPoints else 0f
    }

    private fun calculateGestureLength(): Float {
        if (touchEvents.size < 2) return 0f
        var totalLength = 0f
        for (i in 1 until touchEvents.size) {
            val prevEvent = touchEvents[i - 1]
            val currentEvent = touchEvents[i]
            if (currentEvent.action == MotionEvent.ACTION_MOVE) {
                val dx = currentEvent.x - prevEvent.x
                val dy = currentEvent.y - prevEvent.y
                totalLength += sqrt(dx * dx + dy * dy)
            }
        }
        return totalLength
    }

    private fun calculateKeystrokeInterval(): Float {
        if (keystrokeTimestamps.size < 2) return 0f
        val intervals = keystrokeTimestamps.zipWithNext { a, b -> (b - a).toFloat() / 1000 }
        return intervals.average().toFloat()
    }

    private fun calculateErrorRate(): Float {
        if (keystrokeTimestamps.isEmpty()) return 0f
        return errorCount.toFloat() / keystrokeTimestamps.size.toFloat()
    }

    private fun calculateTypingSpeed(): Float {
        if (keystrokeTimestamps.size < 2) return 0f
        val totalTimeMinutes = (keystrokeTimestamps.last() - keystrokeTimestamps.first()).toFloat() / 60000
        return if (totalTimeMinutes > 0) keystrokeTimestamps.size.toFloat() / totalTimeMinutes else 0f
    }

    // ------------------ Data Reset Function ------------------

    // CRITICAL: Must be called after every window calculation
    fun clearData() {
        accelerometerData.clear()
        gyroscopeData.clear()

        // Recycle MotionEvents to prevent memory leaks
        touchEvents.forEach { it.recycle() }
        touchEvents.clear()

        keystrokeTimestamps.clear()
        errorCount = 0

        // Reset external data as well
        windowDistanceChange = 0f
        windowAverageSpeed = 0f
    }
}
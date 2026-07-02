package com.example.zyra.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.zyra.R
import com.example.zyra.ml.BehavioralFeatureExtractor
import com.example.zyra.ml.ModelScoringAgent
import com.example.zyra.model.Event
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp

class BehavioralDataService : Service(), SensorEventListener, LocationListener {

    companion object {
        const val TAG = "BEHAVIOR_DETECTOR"
        const val ACTION_TOUCH_EVENT = "com.example.behavioralanamolydetector.ACTION_TOUCH_EVENT"
        const val ACTION_KEYSTROKE_EVENT = "com.example.behavioralanamolydetector.ACTION_KEYSTROKE_EVENT"

        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "BehavioralDetectorChannel"
        const val WINDOW_SIZE_MS = 5000L // 5 seconds
        const val SENSOR_DELAY_US = 100_000 // 100 ms
        const val SAFE_DISPLAY_INTERVAL_MS = 30_000L // 30 sec for test

        const val ACTION_EVENT_UPDATE = "com.example.zyra.ACTION_EVENT_UPDATE"
        const val EXTRA_EVENT = "extra_event"
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var featureExtractor: BehavioralFeatureExtractor
    private lateinit var modelAgent: ModelScoringAgent

    private val touchEventReceiver = TouchEventReceiver()
    private val keystrokeReceiver = KeystrokeReceiver()

    private var lastFeatureCollectionTime: Long = 0
    private var lastLocation: Location? = null
    private var totalDistanceThisWindow: Float = 0f
    private var totalSpeedReadings: Float = 0f
    private var speedReadingCount: Int = 0
    private var lastSafeDisplayTime: Long = 0

    // Training buffers
    private val featureBuffer = mutableListOf<FloatArray>()
    private val labelBuffer = mutableListOf<Int>()
    private var weights: FloatArray? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created.")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        featureExtractor = BehavioralFeatureExtractor()
        modelAgent = ModelScoringAgent()

        // ✅ Load saved weights
        val prefs = getSharedPreferences("model_prefs", MODE_PRIVATE)
        prefs.getString("weights", null)?.let { saved ->
            try {
                weights = saved.split(",").map { it.toFloat() }.toFloatArray()
                Log.d(TAG, "Loaded saved model weights: ${weights!!.joinToString()}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse saved weights", e)
            }
        }

        lastFeatureCollectionTime = System.currentTimeMillis()

        // Register receivers
        val touchFilter = IntentFilter(ACTION_TOUCH_EVENT)
        ContextCompat.registerReceiver(
            this,
            touchEventReceiver,
            touchFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val keyFilter = IntentFilter(ACTION_KEYSTROKE_EVENT)
        ContextCompat.registerReceiver(
            this,
            keystrokeReceiver,
            keyFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Restore saved events and rebroadcast to repopulate UI
        loadSavedEvents().forEach { e ->
            sendBroadcast(Intent(ACTION_EVENT_UPDATE).apply { putExtra(EXTRA_EVENT, e) })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SENSOR_DELAY_US)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SENSOR_DELAY_US)
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                WINDOW_SIZE_MS,
                10f,
                this
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied: ${e.message}")
        }

        Log.d(TAG, "Service started and listeners registered.")
        return START_STICKY
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { ev ->
            when (ev.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> featureExtractor.addAccelerometerData(ev)
                Sensor.TYPE_GYROSCOPE -> featureExtractor.addGyroscopeData(ev)
            }
        }
        // Compute on window
        checkAndCalculateFeatures {}
    }

    private fun checkAndCalculateFeatures(function: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastFeatureCollectionTime >= WINDOW_SIZE_MS) {
            val avgSpeed = if (speedReadingCount > 0) totalSpeedReadings / speedReadingCount else 0f
            featureExtractor.setExternalMovementData(totalDistanceThisWindow, avgSpeed)

            val features = featureExtractor.getFeatures()

            // 🟢 inference only — no dummy labels
            val w = weights
            if (w != null) {
                val prediction = predict(features, w)
                val riskScore = (prediction * 100).toInt()
                val risk = when {
                    riskScore > 70 -> "High"
                    riskScore > 40 -> "Medium"
                    else -> "Safe"
                }

                val shouldEmit = risk != "Safe" || (now - lastSafeDisplayTime >= SAFE_DISPLAY_INTERVAL_MS)
                if (shouldEmit) {
                    if (risk == "Safe") lastSafeDisplayTime = now

                    val outEvt = Event(
                        id = System.currentTimeMillis().toString(),
                        type = risk,
                        details = "Prediction: %.2f".format(prediction),
                        timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        riskScore = riskScore,
                        confidence = (prediction * 100).toInt(),
                        modality = "Sensor",
                        description = "Behavioral analysis from actual data",
                        risk = risk
                    )

                    // 🟢 Save & broadcast only real prediction events
                    saveEvent(outEvt)
                    val intent = Intent(ACTION_EVENT_UPDATE).apply {
                        putExtra(EXTRA_EVENT, outEvt)
                    }
                    sendBroadcast(intent)
                }
            }

            // Reset window
            featureExtractor.clearData()
            totalDistanceThisWindow = 0f
            totalSpeedReadings = 0f
            speedReadingCount = 0
            lastFeatureCollectionTime = now
        }
    }

    /** Logistic Regression Training */
    private fun trainModel() {
        if (featureBuffer.isEmpty()) return
        val numFeatures = featureBuffer[0].size
        weights = FloatArray(numFeatures) { 0f }
        val lr = 0.01f

        repeat(100) { // epochs
            for (i in featureBuffer.indices) {
                val x = featureBuffer[i]
                val y = labelBuffer[i]
                val dot = x.zip(weights!!) { xi, wi -> xi * wi }.sum()
                val pred = 1f / (1f + exp(-dot))
                val error = y - pred
                for (j in weights!!.indices) {
                    weights!![j] += lr * error * x[j]
                }
            }
        }

        Log.d(TAG, "Model trained ✅ weights=${weights!!.joinToString()}")
        Toast.makeText(
            this@BehavioralDataService,
            "Model trained with ${featureBuffer.size} samples",
            Toast.LENGTH_SHORT
        ).show()

        // Persist weights so model survives rebuilds/restarts
        val prefs = getSharedPreferences("model_prefs", MODE_PRIVATE)
        prefs.edit().putString("weights", weights!!.joinToString(",")).apply()
    }

    /** Inference */
    private fun predict(features: FloatArray, w: FloatArray): Float {
        val dot = features.zip(w) { xi, wi -> xi * wi }.sum()
        return 1f / (1f + exp(-dot))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        lastLocation?.let { totalDistanceThisWindow += it.distanceTo(location) }
        if (location.hasSpeed()) {
            totalSpeedReadings += location.speed
            speedReadingCount++
        }
        lastLocation = location
    }

    private inner class TouchEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val motionEv: MotionEvent? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.getParcelableExtra("touch_event", MotionEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        it.getParcelableExtra("touch_event")
                    }
                motionEv?.let { me -> featureExtractor.addTouchEvent(me) }
            }
        }
    }

    private inner class KeystrokeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ts = intent?.getLongExtra("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
            val isError = intent?.getBooleanExtra("is_error", false) ?: false
            featureExtractor.addKeystrokeTimestamp(ts, isError)
            Log.d(TAG, "Received Keystroke: Error=$isError")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        unregisterReceiver(touchEventReceiver)
        unregisterReceiver(keystrokeReceiver)
        Log.d(TAG, "Service destroyed.")
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Behavior Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitoring user behavior" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Behavior Detector Active")
            .setContentText("Collecting & Training model...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartService = Intent(applicationContext, BehavioralDataService::class.java).also {
            it.setPackage(packageName)
        }
        val restartPendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartService,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartPendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    // ============================
    // Event persistence (ADD ONLY)
    // ============================
    private fun saveEvent(evt: Event) {
        val prefs = getSharedPreferences("events_prefs", MODE_PRIVATE)
        val existingStr = prefs.getString("all_events", "[]") ?: "[]"
        Log.d(TAG, "Saving event: $evt")

        val existing = try {
            JSONArray(existingStr)
        } catch (e: Exception) {
            Log.e(TAG, "Corrupt events JSON, resetting", e)
            JSONArray()
        }

        // newest on top, keep max 100
        val newArray = JSONArray()
        newArray.put(evt.toMap())
        for (i in 0 until existing.length()) {
            if (i >= 99) break
            newArray.put(existing.getJSONObject(i))
        }

        prefs.edit().putString("all_events", newArray.toString()).apply()

        // in-memory list
        EventRepository.addEvent(this, evt)
    }

    private fun loadSavedEvents(): List<Event> {
        val prefs = getSharedPreferences("events_prefs", MODE_PRIVATE)
        val savedStr = prefs.getString("all_events", "[]") ?: "[]"

        val arr = try {
            JSONArray(savedStr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse saved events JSON", e)
            JSONArray()
        }

        val restored = mutableListOf<Event>()
        for (i in 0 until arr.length()) {
            try {
                val e = Event.fromJson(arr.getJSONObject(i))
                restored.add(e)
                EventRepository.addEvent(this, e)
            } catch (ex: Exception) {
                Log.e(TAG, "Bad event at index $i", ex)
            }
        }
        return restored
    }
}

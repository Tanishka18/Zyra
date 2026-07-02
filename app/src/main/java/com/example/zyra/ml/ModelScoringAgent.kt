package com.example.zyra.ml

import android.util.Log
import com.example.zyra.service.BehavioralDataService

class ModelScoringAgent {

    // --- System State ---
    private val MAX_TRAINING_WINDOWS = 5000
    private var isTrainingMode = true
    private var windowCount = 0

    // This is the placeholder interface for Person 2's TFLite/Core ML model API
    // In a real system, P2 would implement this interface.
    interface AnomalyModelAPI {
        // Function P2 uses for incremental learning (label 0)
        fun trainOnWindow(features: FloatArray, label: Int = 0)

        // Function P2 uses for real-time inference
        fun scoreWindow(features: FloatArray): Float // Returns risk score (0 to 1)
    }

    // Assume we get a reference to P2's implementation
    private lateinit var p2Model: AnomalyModelAPI

    // Public function to set the actual Model implementation (called by P2 or main app)
    fun setModelAPI(api: AnomalyModelAPI) {
        this.p2Model = api
        Log.d(BehavioralDataService.TAG, "P2's Model API attached.")
    }


    /**
     * Processes a feature vector directly, either by training or scoring.
     * This eliminates the need for CSV files.
     */
    fun processFeatureVector(features: FloatArray) {
        if (!::p2Model.isInitialized) {
            Log.e(BehavioralDataService.TAG, "Model API not initialized. Data dropped.")
            return
        }

        windowCount++

        if (isTrainingMode) {
            // 1. TRAINING MODE: Feed features directly to P2's training function (label 0)
            p2Model.trainOnWindow(features, 0)
            Log.d(BehavioralDataService.TAG, "Training window $windowCount fed to P2's model.")

            // Check threshold
            if (windowCount >= MAX_TRAINING_WINDOWS) {
                isTrainingMode = false
                Log.i(BehavioralDataService.TAG, "TRAINING WINDOWS COMPLETE. Switching to Inference Mode.")
                // OPTIONAL: Call a function on P2's model to finalize baseline
                // p2Model.finalizeBaseline()
            }
        } else {
            // 2. INFERENCE MODE: Feed features to P2's scoring function
            val riskScore = p2Model.scoreWindow(features)

            // Log score (This output should go to P3's Policy Agent)
            Log.i(BehavioralDataService.TAG, "Anomaly Risk Score: $riskScore. Sending to Policy Agent (P3).")
            // TODO: Call P3's Policy Agent API here!
            // P3sPolicyAgent.evaluateRisk(riskScore)
        }
    }
}
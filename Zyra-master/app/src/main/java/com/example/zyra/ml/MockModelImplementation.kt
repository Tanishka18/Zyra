package com.example.zyra.ml

import android.util.Log
import com.example.zyra.service.BehavioralDataService


class MockModelImplementation : ModelScoringAgent.AnomalyModelAPI {

    override fun trainOnWindow(features: FloatArray, label: Int) {
        Log.d(BehavioralDataService.TAG, "MockModel: TRAINING data received (${features.size} features).")
    }

    override fun scoreWindow(features: FloatArray): Float {

        Log.d(BehavioralDataService.TAG, "MockModel: SCORING data received (${features.size} features).")

        return 0.5f
    }
}
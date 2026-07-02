package com.example.zyra

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.zyra.service.BehavioralDataService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Setup bottom nav with nav host
        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)

        // ✅ Start Monitoring Button
        val startButton = findViewById<FloatingActionButton>(R.id.startMonitoringButton)
        startButton.setOnClickListener {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, BehavioralDataService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // keeps running forever
        } else {
            startService(serviceIntent)
        }
    }
}

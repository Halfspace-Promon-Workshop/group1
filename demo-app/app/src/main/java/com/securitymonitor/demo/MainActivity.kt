package com.securitymonitor.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.securitymonitor.sdk.SecurityMonitor
import com.securitymonitor.sdk.SecurityEvent

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var riskScoreText: TextView
    private lateinit var threatsText: TextView
    private lateinit var startButton: Button
    private lateinit var simulateButton: Button
    
    private val PERMISSION_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize SDK
        SecurityMonitor.initialize(
            context = this,
            // For emulator: use "http://10.0.2.2:5000/api/events"
            // For physical device with adb reverse: use "http://localhost:5000/api/events" 
            //   (run: adb reverse tcp:5000 tcp:5000)
            // For physical device via network: use "http://YOUR_COMPUTER_IP:5000/api/events"
            apiEndpoint = "http://localhost:5000/api/events",
            enableLocalOnly = false
        )
        
        // Setup UI
        statusText = findViewById(R.id.statusText)
        riskScoreText = findViewById(R.id.riskScoreText)
        threatsText = findViewById(R.id.threatsText)
        startButton = findViewById(R.id.startButton)
        simulateButton = findViewById(R.id.simulateButton)
        
        startButton.setOnClickListener {
            requestPermissions()
            SecurityMonitor.startMonitoring()
            statusText.text = "Monitoring: ACTIVE"
            startButton.isEnabled = false
        }
        
        simulateButton.setOnClickListener {
            simulateSuspiciousActivity()
        }
        
        // Setup threat detection callback
        SecurityMonitor.onThreatDetected { event ->
            runOnUiThread {
                val analysis = event.analysis
                if (analysis != null) {
                    riskScoreText.text = "Risk Score: ${analysis.risk_score}"
                    threatsText.text = buildString {
                        append("Threats Detected:\n")
                        if (analysis.anomaly_detected) {
                            append("⚠️ Anomaly Detected\n")
                        }
                        analysis.compliance_violations.forEach {
                            append("⚠️ $it\n")
                        }
                        append("\nRecommendations:\n")
                        analysis.recommendations.forEach {
                            append("• $it\n")
                        }
                    }
                }
            }
        }
        
        statusText.text = "Monitoring: INACTIVE"
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun simulateSuspiciousActivity() {
        // Simulate suspicious network activity
        SecurityMonitor.trackNetworkActivity(2000000, 1000000, "http://suspicious-domain.com/api/data")
        SecurityMonitor.trackAPICall("http://unknown-endpoint.com/steal", "POST")
        SecurityMonitor.trackAPICall("http://another-bad-site.com/collect", "GET")
        SecurityMonitor.trackFileAccess("/sdcard/private_data.txt")
        
        // Simulate permission access without consent
        SecurityMonitor.trackPermission(Manifest.permission.ACCESS_FINE_LOCATION, true)
        SecurityMonitor.trackPermission(Manifest.permission.READ_CONTACTS, true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SecurityMonitor.stopMonitoring()
    }
}

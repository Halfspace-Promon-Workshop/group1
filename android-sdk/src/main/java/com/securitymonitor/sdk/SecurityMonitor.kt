package com.securitymonitor.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.*
import okhttp3.*
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Main Security Monitor SDK
 * Provides runtime security monitoring and threat detection
 */
object SecurityMonitor {
    private var context: Context? = null
    private var apiEndpoint: String? = null
    private var enableLocalOnly: Boolean = false
    private var isMonitoring: Boolean = false
    private var onThreatDetected: ((SecurityEvent) -> Unit)? = null
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var networkBytesSent: Long = 0
    private var networkBytesReceived: Long = 0
    private var apiCallsCount: Int = 0
    private var fileAccessCount: Int = 0
    private var locationAccess: Boolean = false
    private var contactsAccess: Boolean = false
    private var cameraAccess: Boolean = false
    private val unknownEndpoints = mutableSetOf<String>()
    
    /**
     * Initialize the Security Monitor
     */
    fun initialize(
        context: Context,
        apiEndpoint: String? = null,
        enableLocalOnly: Boolean = false
    ) {
        this.context = context.applicationContext
        this.apiEndpoint = apiEndpoint
        this.enableLocalOnly = enableLocalOnly
    }
    
    /**
     * Start monitoring security events
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        scope.launch {
            while (isMonitoring) {
                collectSecurityData()
                delay(5000) // Collect data every 5 seconds
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    /**
     * Configure threat detection callback
     */
    fun onThreatDetected(callback: (SecurityEvent) -> Unit) {
        this.onThreatDetected = callback
    }
    
    /**
     * Track network activity
     */
    fun trackNetworkActivity(bytesSent: Long, bytesReceived: Long, endpoint: String) {
        networkBytesSent += bytesSent
        networkBytesReceived += bytesReceived
        
        // Check for unknown endpoints
        if (!isKnownEndpoint(endpoint)) {
            unknownEndpoints.add(endpoint)
        }
    }
    
    /**
     * Track API call
     */
    fun trackAPICall(endpoint: String, method: String) {
        apiCallsCount++
        trackNetworkActivity(100, 200, endpoint) // Estimate
    }
    
    /**
     * Track file access
     */
    fun trackFileAccess(path: String) {
        fileAccessCount++
    }
    
    /**
     * Track permission usage
     */
    fun trackPermission(permission: String, granted: Boolean) {
        when (permission) {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> {
                locationAccess = granted
            }
            android.Manifest.permission.READ_CONTACTS -> {
                contactsAccess = granted
            }
            android.Manifest.permission.CAMERA -> {
                cameraAccess = granted
            }
        }
    }
    
    /**
     * Collect and send security data
     */
    private suspend fun collectSecurityData() {
        val ctx = context ?: return
        
        val eventData = SecurityEventData(
            network_bytes_sent = networkBytesSent,
            network_bytes_received = networkBytesReceived,
            api_calls_count = apiCallsCount,
            file_access_count = fileAccessCount,
            location_access = if (locationAccess) 1 else 0,
            location_consent = checkLocationConsent(ctx),
            contacts_access = if (contactsAccess) 1 else 0,
            contacts_consent = checkContactsConsent(ctx),
            camera_access = if (cameraAccess) 1 else 0,
            unknown_endpoints = unknownEndpoints.toList(),
            suspicious_patterns = detectSuspiciousPatterns()
        )
        
        val event = SecurityEvent(
            device_id = getDeviceId(ctx),
            app_id = ctx.packageName,
            event_type = "runtime_monitor",
            data = eventData
        )
        
        // Send to backend if not local-only mode
        if (!enableLocalOnly && apiEndpoint != null) {
            sendEventToBackend(event)
        }
        
        // Reset counters for next interval
        resetCounters()
    }
    
    /**
     * Send event to backend API
     */
    private suspend fun sendEventToBackend(event: SecurityEvent) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(event)
            val requestBody = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"),
                json
            )
            
            val request = Request.Builder()
                .url(apiEndpoint!!)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val analysis = gson.fromJson(responseBody, SecurityAnalysis::class.java)
                
                // Trigger callback if threat detected
                if (analysis.anomaly_detected || analysis.risk_score > 70) {
                    withContext(Dispatchers.Main) {
                        onThreatDetected?.invoke(event.copy(analysis = analysis))
                    }
                }
            }
        } catch (e: IOException) {
            // Handle network error silently or log
            e.printStackTrace()
        }
    }
    
    /**
     * Check if endpoint is known/trusted
     */
    private fun isKnownEndpoint(endpoint: String): Boolean {
        val knownDomains = listOf(
            "googleapis.com",
            "firebase.com",
            "amazonaws.com",
            "microsoft.com"
        )
        return knownDomains.any { endpoint.contains(it) }
    }
    
    /**
     * Detect suspicious patterns
     */
    private fun detectSuspiciousPatterns(): Int {
        var suspicious = 0
        
        // High network usage
        if (networkBytesSent > 1000000) suspicious++
        
        // Many unknown endpoints
        if (unknownEndpoints.size > 5) suspicious++
        
        // High API call rate
        if (apiCallsCount > 100) suspicious++
        
        return suspicious
    }
    
    /**
     * Check location consent (simplified)
     */
    private fun checkLocationConsent(context: Context): Boolean {
        // In real implementation, check shared preferences or consent manager
        return true // Placeholder
    }
    
    /**
     * Check contacts consent (simplified)
     */
    private fun checkContactsConsent(context: Context): Boolean {
        // In real implementation, check shared preferences or consent manager
        return true // Placeholder
    }
    
    /**
     * Get device ID
     */
    private fun getDeviceId(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.imei ?: android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.deviceId ?: android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            }
        } catch (e: Exception) {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        }
    }
    
    /**
     * Reset counters for next interval
     */
    private fun resetCounters() {
        networkBytesSent = 0
        networkBytesReceived = 0
        apiCallsCount = 0
        fileAccessCount = 0
        locationAccess = false
        contactsAccess = false
        cameraAccess = false
        unknownEndpoints.clear()
    }
}

/**
 * Data classes for events
 */
data class SecurityEvent(
    val device_id: String,
    val app_id: String,
    val event_type: String,
    val data: SecurityEventData,
    var analysis: SecurityAnalysis? = null
)

data class SecurityEventData(
    val network_bytes_sent: Long,
    val network_bytes_received: Long,
    val api_calls_count: Int,
    val file_access_count: Int,
    val location_access: Int,
    val location_consent: Boolean,
    val contacts_access: Int,
    val contacts_consent: Boolean,
    val camera_access: Int,
    val unknown_endpoints: List<String>,
    val suspicious_patterns: Int
)

data class SecurityAnalysis(
    val status: String,
    val risk_score: Double,
    val anomaly_detected: Boolean,
    val compliance_violations: List<String>,
    val recommendations: List<String>
)

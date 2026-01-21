package com.securitymonitor.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Helper data class for synchronized data extraction
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
    
    // Thread-safe counters using atomic operations
    private val networkBytesSent = AtomicLong(0)
    private val networkBytesReceived = AtomicLong(0)
    private val apiCallsCount = AtomicInteger(0)
    private val fileAccessCount = AtomicInteger(0)
    
    // Synchronized access for boolean flags and set
    private val dataMutex = Mutex()
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
        networkBytesSent.addAndGet(bytesSent)
        networkBytesReceived.addAndGet(bytesReceived)
        
        // Check for unknown endpoints (thread-safe)
        if (!isKnownEndpoint(endpoint)) {
            runBlocking {
                dataMutex.withLock {
                    unknownEndpoints.add(endpoint)
                }
            }
        }
    }
    
    /**
     * Track API call
     */
    fun trackAPICall(endpoint: String, method: String) {
        apiCallsCount.incrementAndGet()
        trackNetworkActivity(100, 200, endpoint) // Estimate
    }
    
    /**
     * Track file access
     */
    fun trackFileAccess(path: String) {
        fileAccessCount.incrementAndGet()
    }
    
    /**
     * Track permission usage
     */
    fun trackPermission(permission: String, granted: Boolean) {
        runBlocking {
            dataMutex.withLock {
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
        }
    }
    
    /**
     * Collect and send security data
     */
    private suspend fun collectSecurityData() {
        val ctx = context ?: return
        
        // Read all values atomically
        val bytesSent = networkBytesSent.get()
        val bytesReceived = networkBytesReceived.get()
        val apiCalls = apiCallsCount.get()
        val fileAccess = fileAccessCount.get()
        
        // Read synchronized data
        val syncData = dataMutex.withLock {
            Quadruple(
                locationAccess,
                contactsAccess,
                cameraAccess,
                unknownEndpoints.toList()
            )
        }
        
        val eventData = SecurityEventData(
            network_bytes_sent = bytesSent,
            network_bytes_received = bytesReceived,
            api_calls_count = apiCalls,
            file_access_count = fileAccess,
            location_access = if (syncData.first) 1 else 0,
            location_consent = checkLocationConsent(ctx, syncData.first),
            contacts_access = if (syncData.second) 1 else 0,
            contacts_consent = checkContactsConsent(ctx, syncData.second),
            camera_access = if (syncData.third) 1 else 0,
            unknown_endpoints = syncData.fourth,
            suspicious_patterns = detectSuspiciousPatterns(bytesSent, apiCalls, syncData.fourth.size)
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
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(apiEndpoint!!)
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null && responseBody.isNotBlank()) {
                    try {
                        val analysis = gson.fromJson(responseBody, SecurityAnalysis::class.java)
                        
                        // Trigger callback if threat detected
                        if (analysis.anomaly_detected || analysis.risk_score > 70) {
                            withContext(Dispatchers.Main) {
                                onThreatDetected?.invoke(event.copy(analysis = analysis))
                            }
                        }
                    } catch (e: Exception) {
                        // Handle JSON parsing error
                        e.printStackTrace()
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
    private fun detectSuspiciousPatterns(bytesSent: Long, apiCalls: Int, unknownEndpointsCount: Int): Int {
        var suspicious = 0
        
        // High network usage
        if (bytesSent > 1000000) suspicious++
        
        // Many unknown endpoints
        if (unknownEndpointsCount > 5) suspicious++
        
        // High API call rate
        if (apiCalls > 100) suspicious++
        
        return suspicious
    }
    
    /**
     * Check location consent
     * Returns true only if location permission is granted AND user has given consent
     */
    private fun checkLocationConsent(context: Context, hasPermission: Boolean): Boolean {
        if (!hasPermission) return false
        
        // Check SharedPreferences for user consent
        val prefs = context.getSharedPreferences("security_monitor_consent", Context.MODE_PRIVATE)
        return prefs.getBoolean("location_consent_given", false)
    }
    
    /**
     * Check contacts consent
     * Returns true only if contacts permission is granted AND user has given consent
     */
    private fun checkContactsConsent(context: Context, hasPermission: Boolean): Boolean {
        if (!hasPermission) return false
        
        // Check SharedPreferences for user consent
        val prefs = context.getSharedPreferences("security_monitor_consent", Context.MODE_PRIVATE)
        return prefs.getBoolean("contacts_consent_given", false)
    }
    
    /**
     * Get device ID
     * Uses ANDROID_ID which is available to all apps without special permissions
     */
    private fun getDeviceId(context: Context): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
    
    /**
     * Reset counters for next interval
     */
    private suspend fun resetCounters() {
        networkBytesSent.set(0)
        networkBytesReceived.set(0)
        apiCallsCount.set(0)
        fileAccessCount.set(0)
        
        dataMutex.withLock {
            locationAccess = false
            contactsAccess = false
            cameraAccess = false
            unknownEndpoints.clear()
        }
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

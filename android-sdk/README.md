# Mobile Security SDK - Android

Android SDK for runtime security monitoring and threat detection.

## Integration

### 1. Add to your `build.gradle` (Module: app)

```gradle
dependencies {
    implementation project(':security-sdk')
}
```

### 2. Initialize in your Application class

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SecurityMonitor.initialize(
            context = this,
            apiEndpoint = "http://your-backend-url:5000/api/events",
            enableLocalOnly = false // Set to true for privacy-first mode
        )
    }
}
```

### 3. Start monitoring

```kotlin
SecurityMonitor.startMonitoring()
```

## Features

- Network traffic monitoring
- API call tracking
- File access detection
- Privacy permission monitoring
- Real-time anomaly detection
- Compliance violation detection

## Configuration

```kotlin
SecurityMonitor.configure {
    enableNetworkMonitoring = true
    enableAPITracking = true
    enablePrivacyMonitoring = true
    riskThreshold = 70.0
    onThreatDetected { event ->
        // Handle threat
    }
}
```

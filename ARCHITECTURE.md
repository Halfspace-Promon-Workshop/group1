# Architecture Overview

## System Components

### 1. Android SDK (`android-sdk/`)
- **Purpose**: Runtime security monitoring library
- **Key Features**:
  - Network traffic monitoring
  - API call tracking
  - Permission usage detection
  - Privacy leak detection
  - Real-time data collection

- **Technology**: Kotlin, OkHttp, Coroutines
- **Deployment**: Library module for integration into mobile apps

### 2. Backend API (`backend/`)
- **Purpose**: Data processing, ML analysis, and storage
- **Key Features**:
  - RESTful API for event ingestion
  - ML-based anomaly detection (Isolation Forest)
  - Compliance violation checking (GDPR/CCPA)
  - Risk score calculation
  - SQLite database for event storage

- **Technology**: Python, Flask, scikit-learn, TensorFlow
- **Endpoints**:
  - `POST /api/events` - Submit security events
  - `GET /api/events` - Retrieve events with filtering
  - `GET /api/stats` - Aggregated statistics
  - `GET /api/health` - Health check

### 3. Web Dashboard (`dashboard/`)
- **Purpose**: Real-time visualization and monitoring
- **Key Features**:
  - Real-time event display
  - Risk score trends
  - Statistics dashboard
  - Compliance violation alerts

- **Technology**: React, Recharts, Axios
- **Updates**: Auto-refreshes every 5 seconds

### 4. Demo App (`demo-app/`)
- **Purpose**: Demonstration of SDK integration
- **Key Features**:
  - SDK integration example
  - Threat simulation
  - Real-time threat alerts
  - UI for monitoring status

- **Technology**: Android, Kotlin

## Data Flow

```
Mobile App (SDK)
    ↓
Collects runtime data (network, API calls, permissions)
    ↓
Sends to Backend API (/api/events)
    ↓
Backend processes:
  - Feature extraction
  - ML anomaly detection
  - Compliance checking
  - Risk scoring
    ↓
Stores in SQLite database
    ↓
Returns analysis results to mobile app
    ↓
Dashboard polls API (/api/events, /api/stats)
    ↓
Displays real-time visualization
```

## ML Model

- **Algorithm**: Isolation Forest
- **Features** (8-dimensional):
  1. Network bytes sent
  2. Network bytes received
  3. API calls count
  4. File access count
  5. Location access
  6. Contacts access
  7. Camera access
  8. Suspicious patterns count

- **Output**: Anomaly score and binary classification
- **Risk Score**: Calculated from anomaly score (0-100 scale)

## Security & Privacy

- **On-device option**: SDK supports local-only mode
- **Data minimization**: Only necessary features extracted
- **Encryption**: HTTPS recommended for production
- **Compliance**: Built-in GDPR/CCPA violation detection

## Scalability Considerations

- **Database**: SQLite for prototype; PostgreSQL/MySQL for production
- **ML Model**: Can be upgraded to more sophisticated models (LSTM, Autoencoders)
- **API**: Flask for prototype; FastAPI/Django for production
- **Real-time**: WebSocket support can be added for push notifications
- **Caching**: Redis for high-frequency queries

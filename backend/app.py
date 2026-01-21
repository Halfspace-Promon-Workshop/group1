"""
Backend API for Mobile App Security Runtime Monitor
Handles data collection, ML-based anomaly detection, and compliance checking
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import sqlite3
from datetime import datetime
import numpy as np
from sklearn.ensemble import IsolationForest
import joblib
import os
from pathlib import Path

app = Flask(__name__)
CORS(app)

# Initialize database
DB_PATH = 'security_monitor.db'

def init_db():
    """Initialize SQLite database for storing security events"""
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            app_id TEXT,
            event_type TEXT,
            timestamp TEXT,
            data TEXT,
            risk_score REAL,
            anomaly_detected INTEGER,
            compliance_violation TEXT
        )
    ''')
    conn.commit()
    conn.close()

# Initialize ML model
MODEL_PATH = 'models/anomaly_detector.joblib'

def init_ml_model():
    """Initialize or load ML model for anomaly detection"""
    Path('models').mkdir(exist_ok=True)
    
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)
    else:
        # Train a simple Isolation Forest model
        # In production, this would be trained on historical data
        model = IsolationForest(contamination=0.1, random_state=42)
        # Dummy training data (8 features: network_bytes, api_calls, file_access, etc.)
        dummy_data = np.random.rand(100, 8)
        model.fit(dummy_data)
        joblib.dump(model, MODEL_PATH)
        return model

# Initialize components
init_db()
ml_model = init_ml_model()

def extract_features(event_data):
    """Extract features from event data for ML model"""
    features = np.zeros(8)
    
    # Feature extraction
    features[0] = event_data.get('network_bytes_sent', 0)
    features[1] = event_data.get('network_bytes_received', 0)
    features[2] = event_data.get('api_calls_count', 0)
    features[3] = event_data.get('file_access_count', 0)
    features[4] = event_data.get('location_access', 0)
    features[5] = event_data.get('contacts_access', 0)
    features[6] = event_data.get('camera_access', 0)
    features[7] = event_data.get('suspicious_patterns', 0)
    
    return features.reshape(1, -1)

def check_compliance(event_data):
    """Check for GDPR/CCPA compliance violations"""
    violations = []
    
    # Check for unauthorized data collection
    if event_data.get('location_access', 0) and not event_data.get('location_consent', False):
        violations.append('GDPR: Location accessed without consent')
    
    if event_data.get('contacts_access', 0) and not event_data.get('contacts_consent', False):
        violations.append('GDPR: Contacts accessed without consent')
    
    # Check for data transmission to unknown endpoints
    if event_data.get('unknown_endpoints', []):
        violations.append(f'CCPA: Data sent to {len(event_data.get("unknown_endpoints", []))} unknown endpoints')
    
    # Check for excessive data collection
    if event_data.get('network_bytes_sent', 0) > 1000000:  # 1MB threshold
        violations.append('GDPR: Excessive data transmission detected')
    
    return violations

@app.route('/api/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'model_loaded': ml_model is not None})

@app.route('/api/events', methods=['POST'])
def receive_event():
    """Receive security events from mobile apps"""
    try:
        data = request.json
        device_id = data.get('device_id', 'unknown')
        app_id = data.get('app_id', 'unknown')
        event_type = data.get('event_type', 'unknown')
        event_data = data.get('data', {})
        
        # Extract features and run anomaly detection
        features = extract_features(event_data)
        anomaly_score = ml_model.decision_function(features)[0]
        is_anomaly = ml_model.predict(features)[0] == -1
        
        # Calculate risk score (0-100)
        risk_score = min(100, max(0, 50 + (anomaly_score * 10)))
        
        # Check compliance
        violations = check_compliance(event_data)
        
        # Store in database
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('''
            INSERT INTO events (device_id, app_id, event_type, timestamp, data, 
                              risk_score, anomaly_detected, compliance_violation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            device_id,
            app_id,
            event_type,
            datetime.now().isoformat(),
            json.dumps(event_data),
            risk_score,
            1 if is_anomaly else 0,
            '; '.join(violations) if violations else None
        ))
        conn.commit()
        conn.close()
        
        # Return analysis results
        return jsonify({
            'status': 'success',
            'risk_score': float(risk_score),
            'anomaly_detected': bool(is_anomaly),
            'compliance_violations': violations,
            'recommendations': generate_recommendations(is_anomaly, violations, event_data)
        })
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 400

def generate_recommendations(is_anomaly, violations, event_data):
    """Generate security recommendations based on analysis"""
    recommendations = []
    
    if is_anomaly:
        recommendations.append('Anomalous behavior detected. Review app permissions and network activity.')
    
    if violations:
        recommendations.append('Compliance violations found. Ensure proper user consent mechanisms.')
    
    if event_data.get('network_bytes_sent', 0) > 500000:
        recommendations.append('High data transmission detected. Verify data minimization practices.')
    
    if event_data.get('unknown_endpoints', []):
        recommendations.append('Unknown API endpoints detected. Review third-party SDK integrations.')
    
    return recommendations

@app.route('/api/events', methods=['GET'])
def get_events():
    """Retrieve security events with optional filtering"""
    try:
        device_id = request.args.get('device_id')
        app_id = request.args.get('app_id')
        limit = int(request.args.get('limit', 100))
        
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        
        query = 'SELECT * FROM events WHERE 1=1'
        params = []
        
        if device_id:
            query += ' AND device_id = ?'
            params.append(device_id)
        
        if app_id:
            query += ' AND app_id = ?'
            params.append(app_id)
        
        query += ' ORDER BY timestamp DESC LIMIT ?'
        params.append(limit)
        
        c.execute(query, params)
        rows = c.fetchall()
        conn.close()
        
        events = []
        for row in rows:
            events.append({
                'id': row[0],
                'device_id': row[1],
                'app_id': row[2],
                'event_type': row[3],
                'timestamp': row[4],
                'data': json.loads(row[5]),
                'risk_score': row[6],
                'anomaly_detected': bool(row[7]),
                'compliance_violation': row[8]
            })
        
        return jsonify({'events': events, 'count': len(events)})
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 400

@app.route('/api/stats', methods=['GET'])
def get_stats():
    """Get aggregated statistics"""
    try:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        
        # Total events
        c.execute('SELECT COUNT(*) FROM events')
        total_events = c.fetchone()[0]
        
        # Anomalies
        c.execute('SELECT COUNT(*) FROM events WHERE anomaly_detected = 1')
        total_anomalies = c.fetchone()[0]
        
        # Compliance violations
        c.execute('SELECT COUNT(*) FROM events WHERE compliance_violation IS NOT NULL')
        total_violations = c.fetchone()[0]
        
        # Average risk score
        c.execute('SELECT AVG(risk_score) FROM events')
        avg_risk = c.fetchone()[0] or 0
        
        # Events by type
        c.execute('SELECT event_type, COUNT(*) FROM events GROUP BY event_type')
        events_by_type = dict(c.fetchall())
        
        conn.close()
        
        return jsonify({
            'total_events': total_events,
            'total_anomalies': total_anomalies,
            'total_violations': total_violations,
            'average_risk_score': round(avg_risk, 2),
            'events_by_type': events_by_type
        })
    
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 400

if __name__ == '__main__':
    print('🚀 Starting Mobile App Security Monitor Backend...')
    print('📊 API available at http://localhost:5000')
    print('🔍 Health check: http://localhost:5000/api/health')
    app.run(debug=True, host='0.0.0.0', port=5000)

import React from 'react';
import { format } from 'date-fns';

function EventsList({ events }) {
  const getRiskClass = (score) => {
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  };

  const getEventClass = (event) => {
    if (event.anomaly_detected) return 'anomaly';
    if (event.compliance_violation) return 'violation';
    return '';
  };

  return (
    <div className="events-list">
      <h2>Recent Security Events</h2>
      {events.length === 0 ? (
        <p>No events yet. Start monitoring from the mobile app.</p>
      ) : (
        events.map((event) => (
          <div key={event.id} className={`event-item ${getEventClass(event)}`}>
            <div className="event-header">
              <div>
                <span className="event-type">{event.event_type}</span>
                <span className="event-details"> • {event.app_id} • {event.device_id.substring(0, 8)}...</span>
              </div>
              <span className={`risk-badge ${getRiskClass(event.risk_score)}`}>
                Risk: {event.risk_score.toFixed(1)}
              </span>
            </div>
            
            <div className="event-details">
              {format(new Date(event.timestamp), 'PPpp')}
            </div>

            {event.anomaly_detected && (
              <div className="event-details" style={{ color: '#e74c3c', fontWeight: 'bold' }}>
                ⚠️ Anomaly Detected
              </div>
            )}

            {event.compliance_violation && (
              <div className="violations">
                <h4>Compliance Violations:</h4>
                <ul>
                  {event.compliance_violation.split('; ').map((violation, idx) => (
                    <li key={idx}>{violation}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="event-details">
              <strong>Data:</strong> Network: {event.data.network_bytes_sent} bytes sent, 
              {event.data.api_calls_count} API calls, 
              {event.data.unknown_endpoints?.length || 0} unknown endpoints
            </div>
          </div>
        ))
      )}
    </div>
  );
}

export default EventsList;

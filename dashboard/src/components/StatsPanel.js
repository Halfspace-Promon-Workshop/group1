import React from 'react';

function StatsPanel({ stats }) {
  if (!stats) return null;

  const getRiskClass = (score) => {
    if (score >= 70) return 'high-risk';
    if (score >= 40) return 'medium-risk';
    return 'low-risk';
  };

  return (
    <div className="stats-panel">
      <div className={`stat-card ${getRiskClass(stats.average_risk_score)}`}>
        <h3>Total Events</h3>
        <div className="value">{stats.total_events}</div>
        <div className="label">Security events monitored</div>
      </div>

      <div className="stat-card high-risk">
        <h3>Anomalies Detected</h3>
        <div className="value">{stats.total_anomalies}</div>
        <div className="label">Suspicious activities</div>
      </div>

      <div className="stat-card medium-risk">
        <h3>Compliance Violations</h3>
        <div className="value">{stats.total_violations}</div>
        <div className="label">GDPR/CCPA issues</div>
      </div>

      <div className={`stat-card ${getRiskClass(stats.average_risk_score)}`}>
        <h3>Average Risk Score</h3>
        <div className="value">{stats.average_risk_score}</div>
        <div className="label">Out of 100</div>
      </div>
    </div>
  );
}

export default StatsPanel;

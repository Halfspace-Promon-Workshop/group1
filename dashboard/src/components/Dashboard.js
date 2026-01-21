import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

function Dashboard({ events }) {
  // Process events for chart
  const chartData = events.slice(0, 20).reverse().map((event, index) => ({
    time: new Date(event.timestamp).toLocaleTimeString(),
    risk: event.risk_score,
    anomaly: event.anomaly_detected ? 1 : 0
  }));

  return (
    <div className="dashboard">
      <h2>Risk Score Trend</h2>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="time" />
          <YAxis domain={[0, 100]} />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="risk" stroke="#667eea" strokeWidth={2} name="Risk Score" />
          <Line type="monotone" dataKey="anomaly" stroke="#e74c3c" strokeWidth={2} name="Anomaly (0/1)" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export default Dashboard;

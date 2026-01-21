import React, { useState, useEffect } from 'react';
import './App.css';
import Dashboard from './components/Dashboard';
import EventsList from './components/EventsList';
import StatsPanel from './components/StatsPanel';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:5000';

function App() {
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchData = async () => {
    try {
      const [statsRes, eventsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/stats`),
        fetch(`${API_BASE_URL}/api/events?limit=50`)
      ]);

      const statsData = await statsRes.json();
      const eventsData = await eventsRes.json();

      setStats(statsData);
      setEvents(eventsData.events || []);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching data:', error);
      setLoading(false);
    }
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>🛡️ Mobile App Security Monitor</h1>
        <p>Real-time threat detection and compliance monitoring</p>
      </header>
      
      <main className="App-main">
        {loading ? (
          <div className="loading">Loading...</div>
        ) : (
          <>
            <StatsPanel stats={stats} />
            <Dashboard events={events} />
            <EventsList events={events} />
          </>
        )}
      </main>
    </div>
  );
}

export default App;

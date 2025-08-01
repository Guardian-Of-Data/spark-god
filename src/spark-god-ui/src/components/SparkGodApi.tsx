import React, { useEffect, useState } from 'react';

interface ApplicationInfo {
  id: string;
  name: string;
  startTime: number;
  endTime: number;
  duration: number;
}

interface EnvironmentInfo {
  sparkVersion: string;
  javaVersion: string;
  scalaVersion: string;
  osInfo: string;
  pythonVersion: string;
}

interface SparkGodData {
  hello: string;
  application: ApplicationInfo;
  environment: EnvironmentInfo;
  timestamp: number;
}

const SparkGodApi: React.FC = () => {
  const [data, setData] = useState<SparkGodData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // API 경로: /spark-god/api/json/
        const response = await fetch('/spark-god/api/json/');
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const jsonData: SparkGodData = await response.json();
        setData(jsonData);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error occurred');
        console.error('Failed to fetch SparkGod API data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    
    // 5초마다 데이터 새로고침
    const interval = setInterval(fetchData, 5000);
    
    return () => clearInterval(interval);
  }, []);

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatDuration = (durationMs: number) => {
    const seconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <div>Loading SparkGod API data...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '20px', color: 'red' }}>
        <h3>Error loading data:</h3>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  if (!data) {
    return (
      <div style={{ padding: '20px' }}>
        <p>No data available</p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2>SparkGod API Data</h2>
      
      <div style={{ marginBottom: '20px', padding: '10px', backgroundColor: '#f0f0f0', borderRadius: '5px' }}>
        <strong>Message:</strong> {data.hello}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        <div style={{ border: '1px solid #ddd', borderRadius: '5px', padding: '15px' }}>
          <h3>Application Info</h3>
          <p><strong>ID:</strong> {data.application.id}</p>
          <p><strong>Name:</strong> {data.application.name}</p>
          <p><strong>Start Time:</strong> {formatTimestamp(data.application.startTime)}</p>
          <p><strong>End Time:</strong> {data.application.endTime > 0 ? formatTimestamp(data.application.endTime) : 'Running'}</p>
          <p><strong>Duration:</strong> {formatDuration(data.application.duration)}</p>
        </div>

        <div style={{ border: '1px solid #ddd', borderRadius: '5px', padding: '15px' }}>
          <h3>Environment Info</h3>
          <p><strong>Spark Version:</strong> {data.environment.sparkVersion}</p>
          <p><strong>Java Version:</strong> {data.environment.javaVersion}</p>
          <p><strong>Scala Version:</strong> {data.environment.scalaVersion}</p>
          <p><strong>OS Info:</strong> {data.environment.osInfo}</p>
          <p><strong>Python Version:</strong> {data.environment.pythonVersion}</p>
        </div>
      </div>

      <div style={{ marginTop: '20px', padding: '10px', backgroundColor: '#e8f4fd', borderRadius: '5px' }}>
        <p><strong>Last Updated:</strong> {formatTimestamp(data.timestamp)}</p>
      </div>
    </div>
  );
};

export default SparkGodApi; 
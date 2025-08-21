import { useState } from 'react'
import './App.css'
import reactLogo from './assets/react.svg'
import SparkGodApi from './components/SparkGodApi'
import viteLogo from '/vite.svg'

function App() {
  const [count, setCount] = useState(0)
  const [showApi, setShowApi] = useState(false)

  if (showApi) {
    return (
      <div className="App">
        <header className="App-header">
          <h1>SparkGod Dashboard</h1>
          <p>Real-time Spark application monitoring</p>
          <button 
            onClick={() => setShowApi(false)}
            style={{
              background: 'rgba(255, 255, 255, 0.2)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              padding: '0.5rem 1rem',
              borderRadius: '5px',
              cursor: 'pointer',
              marginTop: '1rem'
            }}
          >
            ← Back to Main
          </button>
        </header>
        <main>
          <SparkGodApi />
        </main>
      </div>
    )
  }

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>SparkGod</h1>
      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>
          count is {count}
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
        <button 
          onClick={() => setShowApi(true)}
          style={{
            background: '#667eea',
            color: 'white',
            border: 'none',
            padding: '0.75rem 1.5rem',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '1rem',
            fontWeight: '500',
            marginTop: '1rem',
            transition: 'background 0.2s ease'
          }}
          onMouseOver={(e) => e.currentTarget.style.background = '#5a6fd8'}
          onMouseOut={(e) => e.currentTarget.style.background = '#667eea'}
        >
          View SparkGod API Data
        </button>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </>
  )
}

export default App

import React from 'react';
import EventCatalog from './components/EventCatalog';

function App() {
  return (
    <div className="App">
      <header style={{ backgroundColor: '#282c34', padding: '10px 20px', color: 'white' }}>
        <h1>University Ticketing System</h1>
      </header>
      <main>
        <EventCatalog />
      </main>
    </div>
  );
}

export default App;

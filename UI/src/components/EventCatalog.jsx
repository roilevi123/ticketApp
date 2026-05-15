import React, { useState, useEffect } from 'react';

function EventCatalog() {
  // 1. useState: Managing component state
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 2. useEffect: Running side effects (fetching data)
  useEffect(() => {
    const fetchEvents = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/discovery/events/search', {
          method: 'GET',
          headers: {
            'Authorization': 'Bearer guest-temporary-token',
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        setEvents(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchEvents();
  }, []); // Run only once when the component mounts

  // 3. Conditional Rendering based on state
  if (loading) return <div>Loading events...</div>;
  if (error) return <div style={{ color: 'red' }}>Error: {error}</div>;

  // 4. Rendering the UI
  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2>Guest Event Catalog</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '20px' }}>
        {events.length === 0 ? (
          <p>No events found.</p>
        ) : (
          events.map((event, index) => (
            <div key={index} style={{ border: '1px solid #ccc', padding: '15px', borderRadius: '8px', backgroundColor: '#f9f9f9' }}>
              <h3 style={{ margin: '0 0 10px 0' }}>{event.eventName}</h3>
              <p style={{ margin: '5px 0' }}><strong>Artist:</strong> {event.artistName}</p>
              <p style={{ margin: '5px 0' }}><strong>Location:</strong> {event.location}</p>
              <p style={{ margin: '5px 0', color: 'green' }}><strong>Price:</strong> ${event.price}</p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default EventCatalog;

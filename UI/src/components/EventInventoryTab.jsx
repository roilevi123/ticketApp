import { useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function EventInventoryTab({ companyName }) {
  const [eventData, setEventData] = useState({
    name: '',
    artist: '',      
    type: '',       
    date: '',
    location: '',
    basePrice: '',
    totalTickets: ''
  });

  const handleChange = (e) => {
    setEventData({ ...eventData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!companyName) {
      alert("Error: No active company selected.");
      return;
    }

    // יצירת מפת המושבים (מערך דו-ממדי) בשביל ה-Java
    const numberOfTickets = parseInt(eventData.totalTickets) || 10;
    const dummyMap = [Array(numberOfTickets).fill("SEAT")];

    // תרגום המבנה ל-EventRequestDTO של השרת
    const payload = {
      eventName: eventData.name,
      eventType: eventData.type,
      date: new Date(eventData.date).toISOString(), 
      location: eventData.location,
      price: parseFloat(eventData.basePrice),
      artistName: eventData.artist || "TBD", 
      companyName: companyName, 
      map: dummyMap, 
      rating: 0 
    };

    try {
      await axiosClient.post('/company/events', payload);
      alert('Event configuration saved successfully (Created or Updated in Database)!');
      // איפוס הטופס
      setEventData({ name: '', artist: '', type: '', date: '', location: '', basePrice: '', totalTickets: '' });
    } catch (error) {
      const msg = error.response?.data || error.message || "Network error.";
      alert(`Failed to save event: ${msg}`);
    }
  };

  return (
    <div className="w-full">
      {/* כותרת */}
      <div className="mb-8">
        <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Event & Inventory Management</h2>
        <p className="text-on-surface-variant">Create new events or edit existing ones, set pricing, and manage inventory.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* טופס יצירה ועריכה */}
        <div className="lg:col-span-7">
          <div className="bg-[#191c1e] border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">edit_calendar</span>
              Configure Event (Create / Edit)
            </h3>
            
            <p className="text-xs text-on-surface-variant mb-4 bg-[#101415] p-3 rounded-lg border border-outline-variant/30">
            💡 <strong>Producer Tip:</strong> Entering an existing event name will update its details in the system. Entering a new name will create a new event in the inventory.
            </p>
            
            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              {/* שורה 1: שם וסוג */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Name</label>
                  <input 
                    name="name" value={eventData.name} onChange={handleChange}
                    className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                    placeholder="e.g., STUDENTFEST" 
                    required
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Type</label>
                  <select 
                    name="type" value={eventData.type} onChange={handleChange}
                    className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    required
                  >
                    <option value="" disabled>Select event type</option>
                    <option value="LIVE_PERFORMANCE">Live Performance</option>
                    <option value="PLAY">Play (Theater)</option>
                    <option value="CONFERENCE">Conference</option>
                    <option value="FESTIVAL">Festival</option>
                  </select>
                </div>
              </div>

              {/* שורה חדשה: אמן (Artist) */}
              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Performing Artist / Lineup</label>
                <input 
                  name="artist" value={eventData.artist} onChange={handleChange}
                  className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                  placeholder="e.g., Travis Scott, Don Toliver, or Various Artists"
                />
              </div>

              {/* שורה 2: תאריך ומיקום */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Date & Time</label>
                  <input 
                    type="datetime-local" name="date" value={eventData.date} onChange={handleChange}
                    className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                    required
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Location / Venue</label>
                  <input 
                    name="location" value={eventData.location} onChange={handleChange}
                    className="w-full bg-[#101415] border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                    placeholder="e.g., Main Campus Amphitheater" required
                  />
                </div>
              </div>

              {/* קו מפריד */}
              <hr className="border-outline-variant my-2" />

              {/* שורה 3: מלאי ומחיר */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-secondary uppercase tracking-wider font-bold">Base Price ($)</label>
                  <input 
                    type="number" min="0" step="0.01" name="basePrice" value={eventData.basePrice} onChange={handleChange}
                    className="w-full bg-[#101415] border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                    placeholder="0.00" required
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-secondary uppercase tracking-wider font-bold">Total Ticket Inventory</label>
                  <input 
                    type="number" min="1" name="totalTickets" value={eventData.totalTickets} onChange={handleChange}
                    className="w-full bg-[#101415] border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none" 
                    placeholder="e.g., 500" required
                  />
                </div>
              </div>

              <button type="submit" className="mt-4 w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex justify-center items-center gap-2">
                <span className="material-symbols-outlined">cloud_sync</span>
                Save Event Configuration
              </button>
            </form>
          </div>
        </div>

        {/* פאנל סיכום צדדי */}
        <div className="lg:col-span-5">
          <div className="bg-[#1d2022] border border-outline-variant rounded-xl p-6 h-full flex flex-col">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary-fixed">analytics</span>
              Inventory Overview
            </h3>
            
            <div className="flex-grow flex flex-col justify-center items-center text-center opacity-50">
              <span className="material-symbols-outlined text-6xl mb-4">inventory_2</span>
              <p className="text-lg">Fill out or modify the form to calculate pricing changes.</p>
            </div>
            
            {eventData.basePrice && eventData.totalTickets && (
              <div className="mt-6 p-4 bg-[#101415] rounded-lg border border-outline-variant">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-on-surface-variant">Estimated Max Revenue:</span>
                  <span className="text-secondary font-bold text-xl">
                    ${(Number(eventData.basePrice) * Number(eventData.totalTickets)).toLocaleString()}
                  </span>
                </div>
                <div className="w-full bg-surface-container-highest rounded-full h-2">
                  <div className="bg-secondary h-2 rounded-full w-full"></div>
                </div>
              </div>
            )}
          </div>
        </div>
        
      </div>
    </div>
  );
}
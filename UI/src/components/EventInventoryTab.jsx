import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';

const EMPTY = {
  name: '', artist: '', type: '', date: '', location: '', basePrice: '',
  seatingType: 'general', totalTickets: '', rows: '', cols: '',
  isHighDemand: false, lotteryStartDate: '', lotteryEndDate: '', lotteryMaxWinners: '',
};

function toDatetimeLocal(isoOrDate) {
  if (!isoOrDate) return '';
  const d = new Date(isoOrDate);
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function EventInventoryTab({ companyName }) {
  const [eventData, setEventData] = useState(EMPTY);
  const [saving, setSaving] = useState(false);

  const [events, setEvents] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState('');
  const [deleting, setDeleting] = useState(false);

  const fetchEvents = useCallback(async () => {
    if (!companyName) return;
    setLoadingEvents(true);
    try {
      const res = await axiosClient.get(`/discovery/companies/${encodeURIComponent(companyName)}/events`);
      setEvents(res.data || []);
    } catch {
      setEvents([]);
    } finally {
      setLoadingEvents(false);
    }
  }, [companyName]);

  useEffect(() => { fetchEvents(); }, [fetchEvents]);

  const nowStr = (() => {
    const d = new Date();
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  })();

  const handleChange = (e) => {
    setEventData(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleEventDateChange = (e) => {
    const newDate = e.target.value;
    setEventData(prev => ({
      ...prev,
      date: newDate,
      lotteryStartDate: prev.lotteryStartDate > newDate ? '' : prev.lotteryStartDate,
      lotteryEndDate:   prev.lotteryEndDate   > newDate ? '' : prev.lotteryEndDate,
    }));
  };

  const handleLotteryStartChange = (e) => {
    const newStart = e.target.value;
    setEventData(prev => ({
      ...prev,
      lotteryStartDate: newStart,
      lotteryEndDate: prev.lotteryEndDate < newStart ? '' : prev.lotteryEndDate,
    }));
  };

  const ticketCount = () => {
    if (eventData.seatingType === 'arranged') {
      return (parseInt(eventData.rows) || 0) * (parseInt(eventData.cols) || 0);
    }
    return parseInt(eventData.totalTickets) || 0;
  };

  const buildMap = () => {
    if (eventData.seatingType === 'arranged') {
      const r = parseInt(eventData.rows) || 1;
      const c = parseInt(eventData.cols) || 1;
      return Array.from({ length: r }, () => Array(c).fill('SEAT'));
    }
    return [Array(parseInt(eventData.totalTickets) || 10).fill('SEAT')];
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const now = new Date();
    const eventDate = new Date(eventData.date);
    if (eventDate <= now) {
      alert('Event date must be in the future.');
      return;
    }
    if (eventData.isHighDemand) {
      const lotteryEnd = new Date(eventData.lotteryEndDate);
      if (lotteryEnd <= now) {
        alert('Lottery end date must be in the future.');
        return;
      }
      if (lotteryEnd > eventDate) {
        alert('Lottery end date cannot be after the event date.');
        return;
      }
      if (eventData.lotteryStartDate) {
        const lotteryStart = new Date(eventData.lotteryStartDate);
        if (lotteryStart <= now) {
          alert('Lottery start date must be in the future.');
          return;
        }
        if (lotteryStart >= lotteryEnd) {
          alert('Lottery start date must be before the end date.');
          return;
        }
      }
    }

    setSaving(true);
    try {
      await axiosClient.post('/company/events', {
        eventName: eventData.name,
        eventType: eventData.type,
        date: new Date(eventData.date).toISOString(),
        location: eventData.location,
        price: parseFloat(eventData.basePrice),
        artistName: eventData.artist || 'TBD',
        companyName,
        map: buildMap(),
        rating: 0,
      });

      if (eventData.isHighDemand) {
        await axiosClient.post(
          `/lottery/${encodeURIComponent(companyName)}/${encodeURIComponent(eventData.name)}/configure`,
          {
            startDate: eventData.lotteryStartDate ? new Date(eventData.lotteryStartDate).toISOString() : null,
            endDate: new Date(eventData.lotteryEndDate).toISOString(),
            maxWinners: parseInt(eventData.lotteryMaxWinners),
          }
        );
      }

      alert('Event configuration saved successfully!');
      setEventData(EMPTY);
      fetchEvents();
    } catch (error) {
      const msg = error.response?.data || error.message || 'Network error.';
      alert(`Failed to save event: ${msg}`);
    } finally {
      setSaving(false);
    }
  };

  const handleLoadIntoForm = () => {
    const event = events.find(e => e.eventId === selectedEventId);
    if (!event) return;
    const isArranged = event.map && event.map.length > 1;
    setEventData({
      name: event.name || '',
      artist: event.artistName || '',
      type: event.type || '',
      date: toDatetimeLocal(event.date),
      location: event.location || '',
      basePrice: String(event.price ?? ''),
      seatingType: isArranged ? 'arranged' : 'general',
      totalTickets: isArranged ? '' : String(event.map?.[0]?.length ?? event.totalTickets ?? ''),
      rows: isArranged ? String(event.map.length) : '',
      cols: isArranged ? String(event.map[0]?.length ?? '') : '',
      isHighDemand: event.highDemand || false,
      lotteryStartDate: '',
      lotteryEndDate: toDatetimeLocal(event.lotteryEndDate),
      lotteryMaxWinners: event.lotteryMaxWinners ? String(event.lotteryMaxWinners) : '',
    });
  };

  const handleDelete = async () => {
    const event = events.find(e => e.eventId === selectedEventId);
    if (!event) return;
    if (!window.confirm(`Delete "${event.name}"? This will notify all ticket holders and cannot be undone.`)) return;
    setDeleting(true);
    try {
      await axiosClient.delete('/company/events', {
        params: { eventId: selectedEventId, companyName },
      });
      setSelectedEventId('');
      fetchEvents();
    } catch (error) {
      const msg = error.response?.data?.error || error.message || 'Network error.';
      alert(`Failed to delete event: ${msg}`);
    } finally {
      setDeleting(false);
    }
  };

  const count = ticketCount();
  const revenue = (parseFloat(eventData.basePrice) || 0) * count;

  return (
    <div className="w-full">
      <div className="mb-8">
        <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">Event & Inventory Management</h2>
        <p className="text-on-surface-variant">Create new events or edit existing ones, set pricing, and manage inventory.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* ── Form ── */}
        <div className="lg:col-span-7">
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">edit_calendar</span>
              Configure Event (Create / Edit)
            </h3>

            <p className="text-xs text-on-surface-variant mb-4 bg-background p-3 rounded-lg border border-outline-variant/30">
              <strong>Producer Tip:</strong> Entering an existing event name will update its details. Entering a new name will create a new event. Use "Load into Form" below to pre-fill from an existing event.
            </p>

            <form onSubmit={handleSubmit} className="flex flex-col gap-5">

              {/* Name + Type */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Name</label>
                  <input
                    name="name" value={eventData.name} onChange={handleChange}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., STUDENTFEST" required
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Type</label>
                  <select
                    name="type" value={eventData.type} onChange={handleChange}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
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

              {/* Artist */}
              <div className="flex flex-col gap-2">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Performing Artist / Lineup</label>
                <input
                  name="artist" value={eventData.artist} onChange={handleChange}
                  className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                  placeholder="e.g., Travis Scott, Don Toliver, or Various Artists"
                />
              </div>

              {/* Date + Location */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Date & Time</label>
                  <input
                    type="datetime-local" name="date" value={eventData.date}
                    onChange={handleEventDateChange}
                    min={nowStr}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    required
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Location / Venue</label>
                  <input
                    name="location" value={eventData.location} onChange={handleChange}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="e.g., Main Campus Amphitheater" required
                  />
                </div>
              </div>

              <hr className="border-outline-variant" />

              {/* Price + Ticket count (changes with seating type) */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-secondary uppercase tracking-wider font-bold">Base Price ($)</label>
                  <input
                    type="number" min="0" step="0.01" name="basePrice" value={eventData.basePrice} onChange={handleChange}
                    className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                    placeholder="0.00" required
                  />
                </div>

                {eventData.seatingType === 'general' ? (
                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-secondary uppercase tracking-wider font-bold">Total Ticket Inventory</label>
                    <input
                      type="number" min="1" name="totalTickets" value={eventData.totalTickets} onChange={handleChange}
                      className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                      placeholder="e.g., 500" required
                    />
                  </div>
                ) : (
                  <div className="flex gap-3">
                    <div className="flex flex-col gap-2 flex-1">
                      <label className="text-sm text-secondary uppercase tracking-wider font-bold">Rows</label>
                      <input
                        type="number" min="1" name="rows" value={eventData.rows} onChange={handleChange}
                        className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., 10" required
                      />
                    </div>
                    <div className="flex flex-col gap-2 flex-1">
                      <label className="text-sm text-secondary uppercase tracking-wider font-bold">Seats / Row</label>
                      <input
                        type="number" min="1" name="cols" value={eventData.cols} onChange={handleChange}
                        className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., 20" required
                      />
                    </div>
                  </div>
                )}
              </div>

              <hr className="border-outline-variant" />

              {/* Seating arrangement toggle */}
              <div className="flex flex-col gap-3">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Seating Arrangement</label>
                <div className="flex gap-3">
                  {[
                    { value: 'general',  icon: 'confirmation_number', label: 'General Admission' },
                    { value: 'arranged', icon: 'table_rows',           label: 'Arranged Seats'   },
                  ].map(opt => (
                    <button
                      key={opt.value} type="button"
                      onClick={() => setEventData(prev => ({ ...prev, seatingType: opt.value }))}
                      className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg border text-label-md font-medium transition-all ${
                        eventData.seatingType === opt.value
                          ? 'bg-secondary text-on-secondary border-secondary'
                          : 'border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary'
                      }`}
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{opt.icon}</span>
                      {opt.label}
                    </button>
                  ))}
                </div>
                <p className="text-xs text-on-surface-variant">
                  {eventData.seatingType === 'general'
                    ? 'Tickets have no assigned seat. Buyers choose how many they want.'
                    : 'Each ticket maps to a specific row and seat. Buyers pick from the seat map.'}
                </p>
              </div>

              <hr className="border-outline-variant" />

              {/* High demand toggle */}
              <div className="flex flex-col gap-3">
                <label className="text-sm text-on-surface-variant uppercase tracking-wider">Demand Level</label>
                <div className="flex gap-3">
                  {[
                    { value: false, icon: 'shopping_cart',        label: 'Standard Sales'       },
                    { value: true,  icon: 'local_fire_department', label: 'High Demand (Lottery)' },
                  ].map(opt => (
                    <button
                      key={String(opt.value)} type="button"
                      onClick={() => setEventData(prev => ({ ...prev, isHighDemand: opt.value }))}
                      className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg border text-label-md font-medium transition-all ${
                        eventData.isHighDemand === opt.value
                          ? 'bg-secondary text-on-secondary border-secondary'
                          : 'border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary'
                      }`}
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{opt.icon}</span>
                      {opt.label}
                    </button>
                  ))}
                </div>

                {eventData.isHighDemand && (
                  <div className="flex flex-col gap-4 mt-1 bg-background border border-secondary/30 rounded-lg p-4">
                    <p className="text-xs text-secondary font-medium">
                      Buyers register during the lottery window. Winners are drawn automatically when it closes.
                    </p>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="flex flex-col gap-2">
                        <label className="text-sm text-on-surface-variant uppercase tracking-wider">Lottery Opens</label>
                        <input
                          type="datetime-local" name="lotteryStartDate"
                          value={eventData.lotteryStartDate} onChange={handleLotteryStartChange}
                          min={nowStr}
                          max={eventData.date || undefined}
                          className="w-full bg-surface-container border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        />
                        <span className="text-xs text-on-surface-variant">Leave blank to open immediately</span>
                      </div>
                      <div className="flex flex-col gap-2">
                        <label className="text-sm text-on-surface-variant uppercase tracking-wider">Lottery Closes</label>
                        <input
                          type="datetime-local" name="lotteryEndDate"
                          value={eventData.lotteryEndDate} onChange={handleChange}
                          min={eventData.lotteryStartDate || nowStr}
                          max={eventData.date || undefined}
                          className="w-full bg-surface-container border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                          required={eventData.isHighDemand}
                        />
                      </div>
                    </div>
                    <div className="flex flex-col gap-2">
                      <label className="text-sm text-on-surface-variant uppercase tracking-wider">Number of Winners</label>
                      <input
                        type="number" min="1" name="lotteryMaxWinners"
                        value={eventData.lotteryMaxWinners} onChange={handleChange}
                        className="w-full bg-surface-container border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., 100"
                        required={eventData.isHighDemand}
                      />
                    </div>
                  </div>
                )}
              </div>

              <button
                type="submit" disabled={saving}
                className="mt-2 w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex justify-center items-center gap-2 disabled:opacity-60"
              >
                {saving
                  ? <span className="material-symbols-outlined animate-spin" style={{ fontSize: '18px' }}>progress_activity</span>
                  : <span className="material-symbols-outlined">cloud_sync</span>}
                {saving ? 'Saving...' : 'Save Event Configuration'}
              </button>
            </form>
          </div>
        </div>

        {/* ── Right column ── */}
        <div className="lg:col-span-5 flex flex-col gap-6">

          {/* Inventory overview */}
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6 flex flex-col">
            <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">analytics</span>
              Inventory Overview
            </h3>

            {count > 0 && eventData.basePrice ? (
              <div className="flex flex-col gap-4">
                <div className="p-4 bg-background rounded-lg border border-outline-variant">
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-on-surface-variant">Estimated Max Revenue:</span>
                    <span className="text-secondary font-bold text-xl">${revenue.toLocaleString()}</span>
                  </div>
                  <div className="w-full bg-surface-container-highest rounded-full h-2">
                    <div className="bg-secondary h-2 rounded-full w-full" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                    <p className="text-xs text-on-surface-variant mb-1">Total Tickets</p>
                    <p className="text-2xl font-bold text-secondary">{count.toLocaleString()}</p>
                  </div>
                  <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                    <p className="text-xs text-on-surface-variant mb-1">Price Each</p>
                    <p className="text-2xl font-bold text-secondary">${parseFloat(eventData.basePrice).toFixed(2)}</p>
                  </div>

                  {eventData.seatingType === 'arranged' && eventData.rows && eventData.cols && (
                    <div className="col-span-2 p-3 bg-background rounded-lg border border-outline-variant text-center">
                      <p className="text-xs text-on-surface-variant mb-1">Seat Map</p>
                      <p className="text-sm font-bold text-secondary">
                        {eventData.rows} rows × {eventData.cols} seats/row
                      </p>
                    </div>
                  )}

                  {eventData.isHighDemand && (
                    <div className="col-span-2 p-3 bg-background rounded-lg border border-secondary/30 text-center">
                      <p className="text-xs text-secondary mb-1">Lottery Winners</p>
                      <p className="text-2xl font-bold text-secondary">{eventData.lotteryMaxWinners || '—'}</p>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="flex-grow flex flex-col justify-center items-center text-center opacity-50">
                <span className="material-symbols-outlined text-6xl mb-4">inventory_2</span>
                <p className="text-lg">Fill out the form to preview inventory details.</p>
              </div>
            )}
          </div>

          {/* Manage existing events */}
          <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
            <h3 className="text-xl font-semibold text-on-surface mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary">manage_search</span>
              Manage Existing Events
            </h3>

            {loadingEvents ? (
              <div className="flex items-center gap-2 text-on-surface-variant text-sm">
                <span className="material-symbols-outlined animate-spin" style={{ fontSize: '18px' }}>progress_activity</span>
                Loading events…
              </div>
            ) : events.length === 0 ? (
              <p className="text-sm text-on-surface-variant opacity-60">No events found for this company.</p>
            ) : (
              <div className="flex flex-col gap-4">
                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Select Event</label>
                  <select
                    value={selectedEventId}
                    onChange={e => setSelectedEventId(e.target.value)}
                    className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                  >
                    <option value="">— Choose an event —</option>
                    {events.map(ev => (
                      <option key={ev.eventId} value={ev.eventId}>
                        {ev.name} ({new Date(ev.date).toLocaleDateString()})
                      </option>
                    ))}
                  </select>
                </div>

                {selectedEventId && (
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={handleLoadIntoForm}
                      className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg border border-secondary text-secondary text-label-md font-medium hover:bg-secondary/10 transition-all"
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>edit</span>
                      Load into Form
                    </button>
                    <button
                      type="button"
                      onClick={handleDelete}
                      disabled={deleting}
                      className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg border border-error text-error text-label-md font-medium hover:bg-error/10 transition-all disabled:opacity-60"
                    >
                      {deleting
                        ? <span className="material-symbols-outlined animate-spin" style={{ fontSize: '18px' }}>progress_activity</span>
                        : <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>delete</span>}
                      {deleting ? 'Deleting…' : 'Delete Event'}
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

        </div>
      </div>
    </div>
  );
}

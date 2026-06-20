import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';

const createDefaultMap = (rows, cols) =>
    Array.from({ length: rows }, () => Array(cols).fill('SEAT'));

const EMPTY = {
  name: '',
  artist: '',
  type: '',
  date: '',
  location: '',
  basePrice: '',
  mapRows: '',
  mapCols: '',
  isHighDemand: false,
  lotteryStartDate: '',
  lotteryEndDate: '',
  lotteryMaxWinners: '',
};

function toDatetimeLocal(isoOrDate) {
  if (!isoOrDate) return '';
  const d = new Date(isoOrDate);
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function EventInventoryTab({ companyName }) {
  const [eventData, setEventData] = useState(EMPTY);
  const [venueMap, setVenueMap] = useState([]);
  const [selectedTool, setSelectedTool] = useState('STAND');

  const [saving, setSaving] = useState(false);
  const [events, setEvents] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState('');
  const [deleting, setDeleting] = useState(false);

  const fetchEvents = useCallback(async () => {
    if (!companyName) return;

    setLoadingEvents(true);
    try {
      const res = await axiosClient.get(
          `/discovery/companies/${encodeURIComponent(companyName)}/events`
      );
      setEvents(res.data || []);
    } catch {
      setEvents([]);
    } finally {
      setLoadingEvents(false);
    }
  }, [companyName]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const nowStr = (() => {
    const d = new Date();
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  })();

  const handleChange = e => {
    setEventData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleMapDimensionChange = e => {
    const { name, value } = e.target;

    setEventData(prev => {
      const updated = {
        ...prev,
        [name]: value,
      };

      const rows = parseInt(updated.mapRows);
      const cols = parseInt(updated.mapCols);

      if (!rows || !cols || rows < 1 || cols < 1) {
        setVenueMap([]);
      } else {
        setVenueMap(createDefaultMap(rows, cols));
      }

      return updated;
    });
  };

  const handleCellClick = (rowIndex, colIndex) => {
    setVenueMap(prev =>
        prev.map((row, r) =>
            row.map((cell, c) => {
              if (r === rowIndex && c === colIndex) {
                return selectedTool;
              }
              return cell;
            })
        )
    );
  };

  const handleEventDateChange = e => {
    const newDate = e.target.value;

    setEventData(prev => ({
      ...prev,
      date: newDate,
      lotteryStartDate: prev.lotteryStartDate > newDate ? '' : prev.lotteryStartDate,
      lotteryEndDate: prev.lotteryEndDate > newDate ? '' : prev.lotteryEndDate,
    }));
  };

  const handleLotteryStartChange = e => {
    const newStart = e.target.value;

    setEventData(prev => ({
      ...prev,
      lotteryStartDate: newStart,
      lotteryEndDate: prev.lotteryEndDate < newStart ? '' : prev.lotteryEndDate,
    }));
  };

  const ticketCount = () => venueMap.flat().length;

  const countType = type =>
      venueMap.flat().filter(cell => cell === type).length;

  const buildMap = () => venueMap;

  const validateMap = () => {
    const rows = parseInt(eventData.mapRows);
    const cols = parseInt(eventData.mapCols);

    if (!rows || rows < 1 || !cols || cols < 1) {
      alert('Map rows and columns must be at least 1.');
      return false;
    }

    if (venueMap.length !== rows || venueMap.some(row => row.length !== cols)) {
      alert('Map dimensions do not match rows and columns.');
      return false;
    }

    const standingCount = countType('STAND');
    const seatCount = countType('SEAT');

    // if (standingCount === 0 || seatCount === 0) {
    //   alert('Mixed map must include both standing and seated places.');
    //   return false;
    // }

    return true;
  };

  const getErrorMessage = error => {
    const data = error.response?.data;

    if (typeof data === 'string') return data;
    if (data?.error) return data.error;
    if (data?.message) return data.message;
    if (data) return JSON.stringify(data);

    return error.message || 'Network error.';
  };

  const handleSubmit = async e => {
    e.preventDefault();

    if (!validateMap()) return;

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
              startDate: eventData.lotteryStartDate
                  ? new Date(eventData.lotteryStartDate).toISOString()
                  : null,
              endDate: new Date(eventData.lotteryEndDate).toISOString(),
              maxWinners: parseInt(eventData.lotteryMaxWinners),
            }
        );
      }

      alert('Event configuration saved successfully!');
      setEventData(EMPTY);
      setVenueMap([]);
      fetchEvents();
    } catch (error) {
      console.error('Save event failed:', error);
      console.error('Response data:', error.response?.data);
      alert(`Failed to save event: ${getErrorMessage(error)}`);
    } finally {
      setSaving(false);
    }
  };

  const handleLoadIntoForm = () => {
    const event = events.find(e => e.eventId === selectedEventId);
    if (!event) return;

    const map = event.map || [];
    const rows = map.length || '';
    const cols = map[0]?.length || '';

    setVenueMap(map);

    setEventData({
      name: event.name || '',
      artist: event.artistName || '',
      type: event.type || '',
      date: toDatetimeLocal(event.date),
      location: event.location || '',
      basePrice: String(event.price ?? ''),
      mapRows: String(rows),
      mapCols: String(cols),
      isHighDemand: event.highDemand || false,
      lotteryStartDate: '',
      lotteryEndDate: toDatetimeLocal(event.lotteryEndDate),
      lotteryMaxWinners: event.lotteryMaxWinners ? String(event.lotteryMaxWinners) : '',
    });
  };

  const handleDelete = async () => {
    const event = events.find(e => e.eventId === selectedEventId);
    if (!event) return;

    if (!window.confirm(`Delete "${event.name}"? This will notify all ticket holders and cannot be undone.`)) {
      return;
    }

    setDeleting(true);

    try {
      await axiosClient.delete('/company/events', {
        params: {
          eventId: selectedEventId,
          companyName,
        },
      });

      setSelectedEventId('');
      fetchEvents();
    } catch (error) {
      alert(`Failed to delete event: ${getErrorMessage(error)}`);
    } finally {
      setDeleting(false);
    }
  };

  const count = ticketCount();
  const standingTickets = countType('STAND');
  const seatedTickets = countType('SEAT');
  const revenue = (parseFloat(eventData.basePrice) || 0) * count;

  return (
      <div className="w-full">
        <div className="mb-8">
          <h2 className="font-display-lg text-3xl font-bold text-on-surface mb-2">
            Event & Inventory Management
          </h2>
          <p className="text-on-surface-variant">
            Create new events or edit existing ones, set pricing, and manage inventory.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-7">
            <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
              <h3 className="text-xl font-semibold text-on-surface mb-6 flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">edit_calendar</span>
                Configure Event
              </h3>

              <form onSubmit={handleSubmit} className="flex flex-col gap-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Name</label>
                    <input
                        name="name"
                        value={eventData.name}
                        onChange={handleChange}
                        className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., STUDENTFEST"
                        required
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-on-surface-variant uppercase tracking-wider">Event Type</label>
                    <select
                        name="type"
                        value={eventData.type}
                        onChange={handleChange}
                        className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        required
                    >
                      <option value="" disabled>Select event type</option>
                      <option value="LIVE_PERFORMANCE">Live Performance</option>
                      <option value="PLAY">Play</option>
                      <option value="CONFERENCE">Conference</option>
                      <option value="FESTIVAL">Festival</option>
                    </select>
                  </div>
                </div>

                <div className="flex flex-col gap-2">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">
                    Performing Artist / Lineup
                  </label>
                  <input
                      name="artist"
                      value={eventData.artist}
                      onChange={handleChange}
                      className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                      placeholder="e.g., Travis Scott"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-on-surface-variant uppercase tracking-wider">Date & Time</label>
                    <input
                        type="datetime-local"
                        name="date"
                        value={eventData.date}
                        onChange={handleEventDateChange}
                        min={nowStr}
                        className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        required
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-on-surface-variant uppercase tracking-wider">Location / Venue</label>
                    <input
                        name="location"
                        value={eventData.location}
                        onChange={handleChange}
                        className="w-full bg-background border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., Main Campus Amphitheater"
                        required
                    />
                  </div>
                </div>

                <hr className="border-outline-variant" />

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-secondary uppercase tracking-wider font-bold">Base Price</label>
                    <input
                        type="number"
                        min="0"
                        step="0.01"
                        name="basePrice"
                        value={eventData.basePrice}
                        onChange={handleChange}
                        className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="0.00"
                        required
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-secondary uppercase tracking-wider font-bold">Map Rows</label>
                    <input
                        type="number"
                        min="1"
                        name="mapRows"
                        value={eventData.mapRows}
                        onChange={handleMapDimensionChange}
                        className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., 10"
                        required
                    />
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-sm text-secondary uppercase tracking-wider font-bold">Map Columns</label>
                    <input
                        type="number"
                        min="1"
                        name="mapCols"
                        value={eventData.mapCols}
                        onChange={handleMapDimensionChange}
                        className="w-full bg-background border border-secondary/50 text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                        placeholder="e.g., 20"
                        required
                    />
                  </div>
                </div>

                {venueMap.length > 0 && (
                    <div className="flex flex-col gap-4">
                      <div className="flex flex-col gap-3 bg-background border border-outline-variant rounded-lg p-4">
                        <label className="text-sm text-on-surface-variant uppercase tracking-wider">
                          Map Editor
                        </label>

                        <div className="flex gap-3">
                          <button
                              type="button"
                              onClick={() => setSelectedTool('SEAT')}
                              className={`flex-1 py-2 rounded-lg border font-medium ${
                                  selectedTool === 'SEAT'
                                      ? 'bg-secondary text-on-secondary border-secondary'
                                      : 'border-outline-variant text-on-surface-variant hover:border-secondary'
                              }`}
                          >
                            Seat
                          </button>

                          <button
                              type="button"
                              onClick={() => setSelectedTool('STAND')}
                              className={`flex-1 py-2 rounded-lg border font-medium ${
                                  selectedTool === 'STAND'
                                      ? 'bg-secondary text-on-secondary border-secondary'
                                      : 'border-outline-variant text-on-surface-variant hover:border-secondary'
                              }`}
                          >
                            Standing
                          </button>

                          <button
                              type="button"
                              onClick={() =>
                                  setVenueMap(
                                      createDefaultMap(
                                          parseInt(eventData.mapRows),
                                          parseInt(eventData.mapCols)
                                      )
                                  )
                              }
                              className="flex-1 py-2 rounded-lg border border-outline-variant text-on-surface-variant hover:border-secondary"
                          >
                            Reset to Seats
                          </button>
                        </div>

                        <p className="text-xs text-on-surface-variant">
                          Default is all seats. Choose Seat or Standing, then click cells on the map.
                        </p>

                        <div
                            className="grid gap-1 overflow-auto p-2 bg-surface-container rounded-lg border border-outline-variant"
                            style={{
                              gridTemplateColumns: `repeat(${venueMap[0]?.length || 1}, minmax(32px, 1fr))`,
                            }}
                        >
                          {venueMap.map((row, rowIndex) =>
                              row.map((cell, colIndex) => (
                                  <button
                                      key={`${rowIndex}-${colIndex}`}
                                      type="button"
                                      onClick={() => handleCellClick(rowIndex, colIndex)}
                                      className={`h-9 rounded-md text-[10px] font-bold border transition-all ${
                                          cell === 'SEAT'
                                              ? 'bg-background text-on-surface border-outline-variant'
                                              : 'bg-secondary text-on-secondary border-secondary'
                                      }`}
                                      title={`Row ${rowIndex + 1}, Col ${colIndex + 1}: ${cell}`}
                                  >
                                    {cell === 'SEAT' ? 'S' : 'ST'}
                                  </button>
                              ))
                          )}
                        </div>
                      </div>
                    </div>
                )}

                <hr className="border-outline-variant" />

                <div className="flex flex-col gap-3">
                  <label className="text-sm text-on-surface-variant uppercase tracking-wider">Demand Level</label>

                  <div className="flex gap-3">
                    {[
                      { value: false, icon: 'shopping_cart', label: 'Standard Sales' },
                      { value: true, icon: 'local_fire_department', label: 'High Demand' },
                    ].map(opt => (
                        <button
                            key={String(opt.value)}
                            type="button"
                            onClick={() =>
                                setEventData(prev => ({
                                  ...prev,
                                  isHighDemand: opt.value,
                                }))
                            }
                            className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg border text-label-md font-medium transition-all ${
                                eventData.isHighDemand === opt.value
                                    ? 'bg-secondary text-on-secondary border-secondary'
                                    : 'border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary'
                            }`}
                        >
                      <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                        {opt.icon}
                      </span>
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
                                type="datetime-local"
                                name="lotteryStartDate"
                                value={eventData.lotteryStartDate}
                                onChange={handleLotteryStartChange}
                                min={nowStr}
                                max={eventData.date || undefined}
                                className="w-full bg-surface-container border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                            />
                          </div>

                          <div className="flex flex-col gap-2">
                            <label className="text-sm text-on-surface-variant uppercase tracking-wider">Lottery Closes</label>
                            <input
                                type="datetime-local"
                                name="lotteryEndDate"
                                value={eventData.lotteryEndDate}
                                onChange={handleChange}
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
                              type="number"
                              min="1"
                              name="lotteryMaxWinners"
                              value={eventData.lotteryMaxWinners}
                              onChange={handleChange}
                              className="w-full bg-surface-container border border-outline-variant text-on-surface rounded-lg py-3 px-4 focus:border-secondary focus:ring-1 focus:outline-none"
                              placeholder="e.g., 100"
                              required={eventData.isHighDemand}
                          />
                        </div>
                      </div>
                  )}
                </div>

                <button
                    type="submit"
                    disabled={saving}
                    className="mt-2 w-full py-3 bg-secondary text-on-secondary font-bold rounded-lg hover:brightness-110 transition-all flex justify-center items-center gap-2 disabled:opacity-60"
                >
                  {saving ? 'Saving...' : 'Save Event Configuration'}
                </button>
              </form>
            </div>
          </div>

          <div className="lg:col-span-5 flex flex-col gap-6">
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
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                        <p className="text-xs text-on-surface-variant mb-1">Total Tickets</p>
                        <p className="text-2xl font-bold text-secondary">{count.toLocaleString()}</p>
                      </div>

                      <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                        <p className="text-xs text-on-surface-variant mb-1">Price Each</p>
                        <p className="text-2xl font-bold text-secondary">
                          ${parseFloat(eventData.basePrice).toFixed(2)}
                        </p>
                      </div>

                      <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                        <p className="text-xs text-on-surface-variant mb-1">Standing</p>
                        <p className="text-2xl font-bold text-secondary">{standingTickets.toLocaleString()}</p>
                      </div>

                      <div className="p-3 bg-background rounded-lg border border-outline-variant text-center">
                        <p className="text-xs text-on-surface-variant mb-1">Seated</p>
                        <p className="text-2xl font-bold text-secondary">{seatedTickets.toLocaleString()}</p>
                      </div>

                      {eventData.mapRows && eventData.mapCols && (
                          <div className="col-span-2 p-3 bg-background rounded-lg border border-outline-variant text-center">
                            <p className="text-xs text-on-surface-variant mb-1">Map Size</p>
                            <p className="text-sm font-bold text-secondary">
                              {eventData.mapRows} × {eventData.mapCols}
                            </p>
                          </div>
                      )}

                      {eventData.isHighDemand && (
                          <div className="col-span-2 p-3 bg-background rounded-lg border border-secondary/30 text-center">
                            <p className="text-xs text-secondary mb-1">Lottery Winners</p>
                            <p className="text-2xl font-bold text-secondary">
                              {eventData.lotteryMaxWinners || '—'}
                            </p>
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

            <div className="bg-surface-container border border-outline-variant rounded-xl p-6">
              <h3 className="text-xl font-semibold text-on-surface mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">manage_search</span>
                Manage Existing Events
              </h3>

              {loadingEvents ? (
                  <div className="flex items-center gap-2 text-on-surface-variant text-sm">
                    Loading events…
                  </div>
              ) : events.length === 0 ? (
                  <p className="text-sm text-on-surface-variant opacity-60">
                    No events found for this company.
                  </p>
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
                            Load into Form
                          </button>

                          <button
                              type="button"
                              onClick={handleDelete}
                              disabled={deleting}
                              className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg border border-error text-error text-label-md font-medium hover:bg-error/10 transition-all disabled:opacity-60"
                          >
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
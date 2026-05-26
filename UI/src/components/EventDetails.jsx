import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

const TYPE_CONFIG = {
  LIVE_PERFORMANCE: { gradient: 'from-blue-950 via-indigo-900 to-blue-800', icon: 'music_note' },
  PLAY:             { gradient: 'from-purple-950 via-violet-900 to-purple-800', icon: 'theater_comedy' },
  FESTIVAL:         { gradient: 'from-rose-950 via-pink-900 to-rose-800', icon: 'celebration' },
  CONFERENCE:       { gradient: 'from-slate-900 via-zinc-800 to-slate-700', icon: 'groups' },
};
const DEFAULT_TYPE = { gradient: 'from-slate-900 via-slate-800 to-slate-700', icon: 'event' };

const ROW_LABELS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

// Converts the backend 2D map array into flat seat objects
function parseMapData(map) {
  return map.flatMap((row, rowIdx) =>
    row.map((cell, colIdx) => ({
      id: `${ROW_LABELS[rowIdx] ?? rowIdx}${colIdx + 1}`,
      row: ROW_LABELS[rowIdx] ?? String(rowIdx),
      col: colIdx + 1,
      available: cell === 'SEAT',
      vip: rowIdx === 0,
    }))
  );
}

// Fallback mock seats used when backend provides no map
const TAKEN = new Set(['A2', 'A5', 'B1', 'B4', 'B7', 'C3', 'C6', 'D2', 'D5', 'E1', 'E4', 'E7']);
const MOCK_SEATS = ['A', 'B', 'C', 'D', 'E'].flatMap((row) =>
  Array.from({ length: 8 }, (_, i) => {
    const id = `${row}${i + 1}`;
    return { id, row, col: i + 1, available: !TAKEN.has(id), vip: row === 'A' };
  })
);

function formatDate(dateStr) {
  if (!dateStr) return 'TBD';
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
    });
  } catch {
    return String(dateStr);
  }
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  try {
    return new Date(dateStr).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  } catch {
    return '';
  }
}

function formatPrice(price) {
  if (price == null || price === 0) return 'Free';
  return `$${Number(price).toFixed(2)}`;
}

function EventDetailsSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="w-full h-[397px] bg-surface-container-highest" />
      <div className="px-margin-mobile mt-6 space-y-4">
        <div className="h-4 w-24 bg-surface-container-highest rounded-full" />
        <div className="h-9 w-3/4 bg-surface-container-highest rounded" />
        <div className="h-5 w-1/2 bg-surface-container-highest rounded" />
        <div className="flex flex-col gap-3 mt-6">
          {[1, 2].map((i) => (
            <div key={i} className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-surface-container-highest shrink-0" />
              <div className="space-y-2 flex-1">
                <div className="h-4 w-2/3 bg-surface-container-highest rounded" />
                <div className="h-3 w-1/3 bg-surface-container-highest rounded" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function SeatingMap({ seats, selectedSeat, onSeatSelect }) {
  const rows = [...new Set(seats.map((s) => s.row))];

  return (
    <div>
      <div className="flex justify-center mb-6">
        <div className="px-8 py-2 rounded bg-surface-container border border-outline-variant text-label-sm text-on-surface-variant tracking-widest uppercase">
          Stage
        </div>
      </div>

      <div className="flex flex-col gap-2">
        {rows.map((row) => (
          <div key={row} className="flex items-center gap-2">
            <span className="text-label-sm text-on-surface-variant w-4 text-center shrink-0">{row}</span>
            <div className="flex gap-1.5 flex-wrap">
              {seats
                .filter((s) => s.row === row)
                .map((seat) => {
                  const isSelected = selectedSeat?.id === seat.id;
                  let cls;
                  if (!seat.available) {
                    cls = 'bg-surface-container-highest text-outline cursor-not-allowed opacity-40';
                  } else if (isSelected) {
                    cls = 'bg-secondary text-on-secondary scale-110 shadow-lg';
                  } else if (seat.vip) {
                    cls = 'bg-primary-container border border-primary/40 text-primary hover:border-primary active:scale-95';
                  } else {
                    cls = 'bg-surface-container border border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary active:scale-95';
                  }
                  return (
                    <button
                      key={seat.id}
                      disabled={!seat.available}
                      onClick={() => onSeatSelect(isSelected ? null : seat)}
                      title={seat.available ? `Seat ${seat.id}${seat.vip ? ' (VIP)' : ''}` : `Seat ${seat.id} — Unavailable`}
                      className={`w-8 h-8 rounded text-label-sm font-bold transition-all ${cls}`}
                    >
                      {seat.col}
                    </button>
                  );
                })}
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap gap-4 mt-6">
        {[
          { cls: 'bg-surface-container border border-outline-variant', label: 'Available' },
          { cls: 'bg-primary-container border border-primary/40', label: 'VIP' },
          { cls: 'bg-secondary', label: 'Selected' },
          { cls: 'bg-surface-container-highest opacity-40', label: 'Taken' },
        ].map(({ cls, label }) => (
          <div key={label} className="flex items-center gap-2">
            <div className={`w-4 h-4 rounded ${cls}`} />
            <span className="text-label-sm text-on-surface-variant">{label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function EventDetails() {
  const { companyName, eventName } = useParams();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedSeat, setSelectedSeat] = useState(null);

  // Mock auth state — wire to real auth context when available
  const isRegisteredMember = false;

  useEffect(() => {
    const controller = new AbortController();

    axiosClient.get(
      `/discovery/companies/${encodeURIComponent(companyName)}/events/${encodeURIComponent(eventName)}`,
      { signal: controller.signal }
    )
      .then((res) => {
        // DEV: force isHighDemand true to test lottery UI — remove once backend sets this flag
        setEvent({ ...res.data, isHighDemand: true });
        setLoading(false);
      })
      .catch((err) => {
        if (err.code !== 'ERR_CANCELED') {
          setError(err.message);
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [companyName, eventName]);

  const { gradient, icon } = TYPE_CONFIG[event?.type?.toUpperCase()] ?? DEFAULT_TYPE;
  const seats = event?.map?.length ? parseMapData(event.map) : MOCK_SEATS;
  const minPrice = event?.price ?? 0;

  return (
    <div className="bg-background text-on-surface min-h-screen pb-32">

      {/* ── Top Nav ── */}
      <nav className="flex items-center justify-between px-margin-mobile w-full h-16 bg-surface z-40 fixed top-0">
        <Link to="/" className="p-2 -ml-2 text-primary flex items-center gap-1 hover:text-secondary transition-colors">
          <span className="material-symbols-outlined">arrow_back</span>
          <span className="text-label-md font-medium">Catalog</span>
        </Link>
        <span className="text-headline-sm font-bold tracking-tight text-on-surface">UNIVERSITY EVENTS</span>
        <button className="p-2 -mr-2 text-on-surface-variant hover:text-primary transition-colors">
          <span className="material-symbols-outlined">share</span>
        </button>
      </nav>

      <main className="pt-16">
        {loading ? (
          <EventDetailsSkeleton />
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-20 text-center px-margin-mobile">
            <span className="material-symbols-outlined text-error mb-4" style={{ fontSize: '48px' }}>error_outline</span>
            <p className="text-headline-sm text-on-surface mb-2">Unable to load event</p>
            <p className="text-body-md text-on-surface-variant">{error}</p>
          </div>
        ) : (
          <>
            {/* ── Hero ── */}
            <section className="relative w-full h-[397px] overflow-hidden">
              <div className={`absolute inset-0 bg-gradient-to-br ${gradient} flex items-center justify-center`}>
                <span className="material-symbols-outlined text-white/10" style={{ fontSize: '120px' }}>{icon}</span>
              </div>
              <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent" />
            </section>

            {/* ── Event Identity ── */}
            <section className="px-margin-mobile -mt-12 relative z-10">
              {event.type && (
                <div className="inline-flex items-center px-3 py-1 rounded-full bg-on-tertiary-container/10 border border-on-tertiary-container/20 mb-4">
                  <span className="text-label-sm text-on-tertiary-container uppercase tracking-wider">
                    {event.type.replace(/_/g, ' ')}
                  </span>
                </div>
              )}

              <h1 className="text-display-lg-mobile text-on-surface font-bold mb-2">{event.name}</h1>

              {event.artistName && (
                <p className="text-body-lg text-on-surface-variant mb-1">{event.artistName}</p>
              )}

              <div className="flex flex-col gap-3 mt-6">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center border border-outline-variant shrink-0">
                    <span className="material-symbols-outlined text-secondary">calendar_month</span>
                  </div>
                  <div>
                    <p className="text-label-md text-on-surface">{formatDate(event.date ?? event.startDate)}</p>
                    <p className="text-label-sm text-on-surface-variant">{formatTime(event.date ?? event.startDate)}</p>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center border border-outline-variant shrink-0">
                    <span className="material-symbols-outlined text-secondary">location_on</span>
                  </div>
                  <div>
                    <p className="text-label-md text-on-surface">{event.location || 'Venue TBD'}</p>
                    {event.companyName ? (
                      <Link
                        to={`/company/${encodeURIComponent(event.companyName)}`}
                        className="text-label-sm text-secondary hover:underline"
                      >
                        {event.companyName}
                      </Link>
                    ) : (
                      <span className="text-label-sm text-on-surface-variant">View Map</span>
                    )}
                  </div>
                </div>
              </div>
            </section>

            {/* ── About ── */}
            <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
            <section className="px-margin-mobile">
              <h2 className="text-headline-sm text-on-surface mb-3">About the Event</h2>
              <p className="text-body-md text-on-surface-variant leading-relaxed">
                {event.description || 'No description available.'}
              </p>
            </section>

            {/* ── Ticket Options ── */}
            {event.price != null && (
              <>
                <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
                <section className="px-margin-mobile">
                  <h2 className="text-headline-sm text-on-surface mb-4">Ticket Options</h2>
                  <div className="grid gap-3">
                    <div className="p-4 rounded-xl bg-surface-container border border-outline-variant flex justify-between items-center">
                      <div>
                        <p className="text-label-md text-on-surface">General Admission</p>
                        <p className="text-label-sm text-on-surface-variant">Standard seating access</p>
                      </div>
                      <div className="text-right">
                        <p className="text-headline-sm text-secondary">{formatPrice(event.price)}</p>
                        <div className="inline-flex bg-secondary/10 px-2 py-0.5 rounded mt-1">
                          <span className="text-[10px] font-bold text-secondary uppercase tracking-wider">Available</span>
                        </div>
                      </div>
                    </div>
                    {event.studentPrice != null && (
                      <div className="p-4 rounded-xl bg-surface-container border border-outline-variant flex justify-between items-center">
                        <div>
                          <p className="text-label-md text-on-surface">Student</p>
                          <p className="text-label-sm text-on-surface-variant">Valid University ID required</p>
                        </div>
                        <div className="text-right">
                          <p className="text-headline-sm text-secondary">{formatPrice(event.studentPrice)}</p>
                        </div>
                      </div>
                    )}
                  </div>
                </section>
              </>
            )}

            {/* ── Seating Map ── */}
            <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
            <section className="px-margin-mobile">
              <div className="flex items-center justify-between mb-1">
                <h2 className="text-headline-sm text-on-surface">Seating Map</h2>
                {selectedSeat && (
                  <span className="text-label-sm text-secondary">
                    Selected: <span className="font-bold">{selectedSeat.id}</span>
                    {selectedSeat.vip ? ' (VIP)' : ''}
                  </span>
                )}
              </div>
              <p className="text-label-sm text-on-surface-variant mb-5">
                Tap an available seat to select it.
                {!event.map?.length && ' (Preview — map data pending)'}
              </p>
              <div className="overflow-x-auto pb-2">
                <SeatingMap seats={seats} selectedSeat={selectedSeat} onSeatSelect={setSelectedSeat} />
              </div>
            </section>

            {/* ── High Demand / Lottery ── */}
            {event.isHighDemand && (
              <>
                <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
                <section className="px-margin-mobile">
                  <div className="p-4 rounded-xl bg-tertiary-container/20 border border-on-tertiary-container/30">
                    <div className="flex items-start gap-3 mb-4">
                      <span className="material-symbols-outlined text-on-tertiary-container shrink-0">local_fire_department</span>
                      <div>
                        <h3 className="text-headline-sm text-on-tertiary-container mb-1">High Demand Event</h3>
                        <p className="text-label-sm text-on-surface-variant">
                          Tickets are distributed by lottery. Registered members can enter for a chance to purchase.
                        </p>
                      </div>
                    </div>
                    <div className="relative group">
                      <button
                        disabled={!isRegisteredMember}
                        className={`w-full py-3 rounded-xl font-bold text-label-md uppercase tracking-wider flex items-center justify-center gap-2 transition-all ${
                          isRegisteredMember
                            ? 'bg-on-tertiary-container text-on-tertiary active:scale-95 hover:brightness-110'
                            : 'bg-surface-container text-outline cursor-not-allowed border border-outline-variant'
                        }`}
                      >
                        <span className="material-symbols-outlined text-[18px]">casino</span>
                        Enter Lottery
                      </button>
                      {!isRegisteredMember && (
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-1.5 bg-surface-container-highest rounded-lg text-label-sm text-on-surface whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none border border-outline-variant z-10">
                          Members only — register to enter the lottery
                        </div>
                      )}
                    </div>
                  </div>
                </section>
              </>
            )}
          </>
        )}
      </main>

      {/* ── Fixed Bottom Action Bar ── */}
      {!loading && !error && event && (
        <footer className="fixed bottom-0 left-0 w-full z-50 bg-surface-container-high shadow-lg border-t border-outline-variant px-margin-mobile py-4 flex items-center justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-label-sm text-on-surface-variant">Starting from</span>
            <span className="text-headline-md text-on-surface font-bold">{formatPrice(minPrice)}</span>
          </div>

          {event.isHighDemand ? (
            <div className="relative group">
              <button
                disabled={!isRegisteredMember}
                className={`font-bold px-8 py-3 rounded-full transition-transform active:scale-95 flex items-center gap-2 ${
                  isRegisteredMember
                    ? 'bg-on-tertiary-container text-on-tertiary hover:brightness-110'
                    : 'bg-surface-container border border-outline-variant text-outline cursor-not-allowed'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">casino</span>
                Lottery Registration
              </button>
              {!isRegisteredMember && (
                <div className="absolute bottom-full right-0 mb-2 px-3 py-1.5 bg-surface-container-highest rounded-lg text-label-sm text-on-surface whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none border border-outline-variant z-10">
                  Members only — register to enter the lottery
                </div>
              )}
            </div>
          ) : (
            <button className={`font-bold px-8 py-3 rounded-full transition-transform active:scale-95 flex items-center gap-2 ${
              minPrice === 0 ? 'bg-primary text-on-primary' : 'bg-secondary text-on-secondary'
            }`}>
              <span className="material-symbols-outlined text-[20px]">confirmation_number</span>
              {minPrice === 0 ? 'Get Passes' : 'Buy Tickets'}
            </button>
          )}
        </footer>
      )}

    </div>
  );
}

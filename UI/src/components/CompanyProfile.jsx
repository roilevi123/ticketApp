import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

const TYPE_CONFIG = {
  LIVE_PERFORMANCE: {
    gradient: "from-blue-950 via-indigo-900 to-blue-800",
    icon: "music_note",
  },
  PLAY: {
    gradient: "from-purple-950 via-violet-900 to-purple-800",
    icon: "theater_comedy",
  },
  FESTIVAL: {
    gradient: "from-rose-950 via-pink-900 to-rose-800",
    icon: "celebration",
  },
  CONFERENCE: {
    gradient: "from-slate-900 via-zinc-800 to-slate-700",
    icon: "groups",
  },
};
const DEFAULT_TYPE = {
  gradient: "from-slate-900 via-slate-800 to-slate-700",
  icon: "event",
};

function formatDate(dateStr) {
  if (!dateStr) return "TBD";
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    });
  } catch {
    return String(dateStr);
  }
}

function formatPrice(price) {
  if (price == null || price === 0) return "Free";
  return `$${price}`;
}

function EventCardSkeleton() {
  return (
    <div className="bg-surface-container-high rounded-xl overflow-hidden border border-outline-variant animate-pulse">
      <div className="h-48 bg-surface-container-highest" />
      <div className="p-4 space-y-3">
        <div className="h-5 w-3/4 bg-surface-container-highest rounded" />
        <div className="h-4 w-1/2 bg-surface-container-highest rounded" />
        <div className="flex justify-between pt-2">
          <div className="h-6 w-16 bg-surface-container-highest rounded" />
          <div className="h-8 w-28 bg-surface-container-highest rounded" />
        </div>
      </div>
    </div>
  );
}

function EventCard({ event }) {
  const { gradient, icon } =
    TYPE_CONFIG[event.type?.toUpperCase()] || DEFAULT_TYPE;
  const isFree = !event.price || event.price === 0;

  return (
    <div className="bg-surface-container-high rounded-xl overflow-hidden border border-outline-variant transition-all hover:border-secondary">
      <div
        className={`relative h-48 bg-gradient-to-br ${gradient} flex items-center justify-center`}
      >
        <span
          className="material-symbols-outlined text-white/10"
          style={{ fontSize: "72px" }}
        >
          {icon}
        </span>
        <div className="absolute top-3 left-3 bg-surface-dim/80 backdrop-blur-sm text-on-surface-variant px-3 py-1 rounded-full text-label-sm">
          {formatDate(event.date)}
        </div>
      </div>
      <div className="p-4">
        <p className="text-label-sm text-on-surface-variant/70 mb-1 tracking-wider">
          {event.type?.toUpperCase() || "EVENT"}
        </p>
        <h3 className="text-headline-sm text-on-surface mb-2 line-clamp-2">
          {event.name}
        </h3>
        <div className="flex items-center gap-2 text-on-surface-variant mb-4">
          <span
            className="material-symbols-outlined"
            style={{ fontSize: "18px" }}
          >
            location_on
          </span>
          <span className="text-body-md truncate">{event.location}</span>
        </div>
        {event.artistName && (
          <p className="text-label-md text-on-surface-variant mb-3">
            {event.artistName}
          </p>
        )}
        <div className="flex items-center justify-between mt-4">
          <div className="flex flex-col">
            <span className="text-label-sm text-on-surface-variant">
              {isFree ? "Admission" : "From"}
            </span>
            <span className="text-headline-sm text-secondary font-bold uppercase">
              {formatPrice(event.price)}
            </span>
          </div>
          <button className="bg-secondary text-on-secondary px-6 py-2 rounded-lg text-label-md font-bold uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all">
            {isFree ? "Get Passes" : "Buy Tickets"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function CompanyProfile() {
  const params = useParams();
  const companyName = params.companyName ?? params.id;
  const [company, setCompany] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    Promise.allSettled([
      axiosClient.get('/discovery/companies', { signal: controller.signal })
        .then((res) => res.data.find((c) => c.companyName === companyName) ?? null),

      axiosClient.get(`/discovery/companies/${encodeURIComponent(companyName)}/events`, { signal: controller.signal })
        .then((res) => res.data),
    ])
      .then(([companyResult, eventsResult]) => {
        if (companyResult.status === "fulfilled")
          setCompany(companyResult.value);
        if (eventsResult.status === "fulfilled") {
          setEvents(eventsResult.value);
        } else if (eventsResult.reason?.code !== 'ERR_CANCELED') {
          setError(eventsResult.reason?.message ?? 'Failed to load events');
        }
        setLoading(false);
      })
      .catch((err) => {
        if (err.code !== 'ERR_CANCELED') { setError(err.message); setLoading(false); }
      });

    return () => controller.abort();
  }, [companyName]);

  const headerGradient = "from-blue-950 via-indigo-900 to-slate-900";

  return (
    <div className="bg-background text-on-surface min-h-screen pb-24">
      {/* ── Top Nav ── */}
      <nav className="flex items-center justify-between px-margin-mobile w-full h-16 bg-surface z-40 fixed top-0">
        <Link
          to="/"
          className="p-2 -ml-2 text-primary flex items-center gap-1 hover:text-secondary transition-colors"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </Link>
        <span className="text-headline-sm font-bold tracking-tight text-on-surface">
          UNIVERSITY EVENTS
        </span>
        <button className="p-2 -mr-2 text-primary">
          <span className="material-symbols-outlined">share</span>
        </button>
      </nav>

      {/* ── Hero Header ── */}
      <header className="relative w-full h-[397px] pt-16">
        <div
          className={`absolute inset-0 bg-gradient-to-br ${headerGradient} z-0`}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent z-10" />
        <div className="absolute bottom-0 left-0 right-0 p-margin-mobile z-20">
          {loading ? (
            <div className="space-y-3 animate-pulse">
              <div className="h-9 w-64 bg-surface-container-high rounded" />
              <div className="h-5 w-80 bg-surface-container-high rounded" />
            </div>
          ) : (
            <>
              <h1 className="text-display-lg-mobile md:text-display-lg text-on-surface font-bold leading-tight mb-2">
                {companyName}
              </h1>
              {company ? (
                <p className="text-body-md text-on-surface-variant max-w-sm">
                  Rating:{" "}
                  {company.rating > 0
                    ? `${company.rating.toFixed(1)} / 5.0`
                    : "No ratings yet"}
                </p>
              ) : (
                <p className="text-body-md text-on-surface-variant max-w-sm">
                  Explore upcoming events from this organizer.
                </p>
              )}
            </>
          )}
        </div>
      </header>

      {/* ── Content ── */}
      <main className="px-margin-mobile mt-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-label-sm uppercase tracking-[0.2em] text-secondary">
            EVENTS BY THIS COMPANY
          </h2>
          <span className="material-symbols-outlined text-outline">
            filter_list
          </span>
        </div>

        {error ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <span
              className="material-symbols-outlined text-error mb-4"
              style={{ fontSize: "48px" }}
            >
              error_outline
            </span>
            <p className="text-headline-sm text-on-surface mb-2">
              Unable to load events
            </p>
            <p className="text-body-md text-on-surface-variant">{error}</p>
          </div>
        ) : (
          <div className="flex flex-col gap-6">
            {loading ? (
              Array.from({ length: 3 }, (_, i) => <EventCardSkeleton key={i} />)
            ) : events.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-center">
                <span
                  className="material-symbols-outlined text-on-surface-variant mb-4"
                  style={{ fontSize: "48px" }}
                >
                  event_busy
                </span>
                <p className="text-headline-sm text-on-surface mb-2">
                  No events yet
                </p>
                <p className="text-body-md text-on-surface-variant">
                  Check back soon for upcoming events.
                </p>
              </div>
            ) : (
              events.map((event, i) => (
                <EventCard key={event.eventId ?? i} event={event} />
              ))
            )}
          </div>
        )}
      </main>

      {/* ── Bottom Nav ── */}
      <footer className="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-4 py-3 bg-surface-container-high shadow-lg rounded-t-xl border-t border-outline-variant">
        {[
          { icon: "home", label: "Home", active: false },
          { icon: "calendar_month", label: "Schedule", active: false },
          { icon: "confirmation_number", label: "Tickets", active: false },
          { icon: "person", label: "Profile", active: true },
        ].map(({ icon, label, active }) => (
          <div
            key={label}
            className={`flex flex-col items-center justify-center transition-all ${
              active
                ? "bg-secondary-container text-on-secondary-container rounded-full px-4 py-1"
                : "text-on-surface-variant hover:text-secondary"
            }`}
          >
            <span className="material-symbols-outlined">{icon}</span>
            <span className="text-label-sm">{label}</span>
          </div>
        ))}
      </footer>
    </div>
  );
}

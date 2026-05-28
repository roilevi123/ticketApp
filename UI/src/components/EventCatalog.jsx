import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";
import { useActiveOrder } from "../contexts/ActiveOrderContext";
import { getOrderErrorMessage } from "../utils/orderErrors";
import { useNotifications } from "../contexts/NotificationContext";

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

function toEventPath(event) {
  return `/event/${encodeURIComponent(event.companyName)}/${encodeURIComponent(event.name)}`;
}

function formatDate(dateStr) {
  if (!dateStr) return "TBD";
  try {
    return new Date(dateStr)
      .toLocaleDateString("en-US", { month: "short", day: "numeric" })
      .toUpperCase();
  } catch {
    return String(dateStr).toUpperCase();
  }
}

// קומפוננטה חדשה שמטפלת בתצוגת מחירי מבצע לעומת מחיר רגיל
function PriceDisplay({ price, discountedPrice }) {
  if (price == null || price === 0) return <span className="text-headline-sm text-secondary">Free</span>;
  
  if (discountedPrice != null && discountedPrice < price) {
    return (
      <div className="flex flex-col items-end">
        <span className="text-label-sm text-on-surface-variant line-through opacity-70">
          ${Number(price).toFixed(2)}
        </span>
        <span className="text-headline-sm text-secondary">
          From ${Number(discountedPrice).toFixed(2)}
        </span>
      </div>
    );
  }
  
  return <span className="text-headline-sm text-secondary">From ${Number(price).toFixed(2)}</span>;
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function EventCardSkeleton() {
  return (
    <div className="bg-surface-container-low border border-outline-variant rounded-xl overflow-hidden animate-pulse">
      <div className="aspect-video bg-surface-container-high" />
      <div className="p-5 space-y-3">
        <div className="h-3 w-16 bg-surface-container-high rounded" />
        <div className="h-5 w-3/4 bg-surface-container-high rounded" />
        <div className="h-4 w-1/2 bg-surface-container-high rounded" />
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 bg-surface-container-high rounded-full" />
          <div className="h-4 w-1/3 bg-surface-container-high rounded" />
        </div>
        <div className="flex justify-between pt-2">
          <div className="h-6 w-20 bg-surface-container-high rounded" />
          <div className="h-8 w-24 bg-surface-container-high rounded" />
        </div>
      </div>
    </div>
  );
}

function EventCard({ event }) {
  const { gradient, icon } = TYPE_CONFIG[event.type?.toUpperCase()] || DEFAULT_TYPE;
  const navigate = useNavigate();
  const path = toEventPath(event);

  return (
    <div
      onClick={() => navigate(path)}
      className="bg-surface-container-low border border-outline-variant rounded-xl overflow-hidden group hover:border-secondary transition-all duration-300 flex flex-col cursor-pointer"
    >
      <div className={`aspect-video relative overflow-hidden bg-gradient-to-br ${gradient} flex items-center justify-center`}>
        <span className="material-symbols-outlined text-white/10" style={{ fontSize: "72px" }}>
          {icon}
        </span>
        <div className="absolute top-2 left-2 px-2 py-1 bg-surface-dim/80 backdrop-blur-sm rounded text-label-sm text-tertiary">
          {formatDate(event.date)}
        </div>
        {event.soldOut ? (
          <div className="absolute top-2 right-2 px-2 py-1 bg-surface-container-highest text-outline font-bold rounded shadow-lg text-label-sm">
            SOLD OUT
          </div>
        ) : event.discountedPrice != null && event.discountedPrice < event.price && (
          <div className="absolute top-2 right-2 px-2 py-1 bg-error text-on-error font-bold rounded shadow-lg text-label-sm animate-pulse">
            SALE
          </div>
        )}
      </div>
      <div className="p-5 flex flex-col flex-1">
        <p className="text-label-sm text-on-tertiary-container/80 mb-1 tracking-wider">
          {event.type?.toUpperCase() || "EVENT"}
        </p>
        <h3 className="text-headline-sm text-on-surface mb-1 line-clamp-2">
          {event.name}
        </h3>
        <p className="text-body-md text-on-surface-variant mb-4">
          {event.artistName}
        </p>
        <div className="flex items-center gap-2 text-on-surface-variant mb-6">
          <span className="material-symbols-outlined" style={{ fontSize: "18px" }}>
            location_on
          </span>
          <span className="text-label-md truncate">{event.location}</span>
        </div>
        <div className="flex items-center justify-between mt-auto">
          <PriceDisplay price={event.price} discountedPrice={event.discountedPrice} />
          
          <div className="flex items-center gap-2">
            {event.companyName && (
              <Link
                to={`/company/${encodeURIComponent(event.companyName)}`}
                onClick={(e) => e.stopPropagation()}
                className="text-label-sm text-on-surface-variant hover:text-secondary transition-colors px-2 py-1 rounded border border-outline-variant hover:border-secondary"
              >
                {event.companyName}
              </Link>
            )}
            {event.soldOut ? (
              <div className="flex items-center gap-1 px-4 py-2 text-label-md font-bold rounded bg-surface-container-highest text-outline border border-outline-variant cursor-not-allowed">
                <span className="material-symbols-outlined" style={{ fontSize: "16px" }}>block</span>
                Sold Out
              </div>
            ) : (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(path);
                }}
                className="bg-secondary text-on-secondary px-4 py-2 text-label-md font-bold rounded hover:brightness-110 active:scale-95 transition-all"
              >
                Buy Tickets
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function HeroCard({ event }) {
  const { gradient } = TYPE_CONFIG[event?.type?.toUpperCase()] || DEFAULT_TYPE;
  const navigate = useNavigate();
  const path = event ? toEventPath(event) : null;

  return (
    <div
      onClick={() => path && navigate(path)}
      className={`md:col-span-8 relative rounded-xl overflow-hidden min-h-[280px] bg-gradient-to-br ${gradient} border border-outline-variant ${path ? "cursor-pointer" : ""}`}
    >
      <div className="absolute inset-0 bg-gradient-to-t from-surface-dim via-surface-dim/30 to-transparent" />
      <div className="absolute bottom-0 left-0 p-8 w-full">
        <span className="inline-block px-3 py-1 bg-secondary/20 text-secondary text-label-sm rounded-full mb-4 tracking-wider">
          {event?.type?.toUpperCase() || "FEATURED"}
        </span>
        <h3 className="text-display-lg-mobile md:text-display-lg text-on-surface mb-2">
          {event?.name || "Upcoming Events"}
        </h3>
        <p className="text-body-lg text-on-surface-variant mb-6">
          {event
            ? `Featuring ${event.artistName}`
            : "Explore what's happening on campus"}
        </p>
        {path && (
          <div className="flex items-center gap-4">
            <button
              onClick={(e) => {
                e.stopPropagation();
                navigate(path);
              }}
              className="bg-secondary text-on-secondary px-8 py-3 text-label-md font-bold rounded-lg hover:brightness-110 transition-all active:scale-95"
            >
              Buy Tickets
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                navigate(path);
              }}
              className="border border-on-surface text-on-surface px-8 py-3 text-label-md font-bold rounded-lg hover:bg-on-surface/10 transition-all active:scale-95"
            >
              View Details
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function SideCard({ event }) {
  const { gradient } = TYPE_CONFIG[event?.type?.toUpperCase()] || DEFAULT_TYPE;
  const sharedClass = `flex-1 relative rounded-xl overflow-hidden min-h-[150px] bg-gradient-to-br ${gradient} border border-outline-variant`;
  const content = (
    <>
      <div className="absolute inset-0 bg-gradient-to-t from-surface-dim/80 to-transparent" />
      <div className="absolute bottom-0 left-0 p-6">
        <h4 className="text-headline-sm text-on-surface">
          {event?.name || <span className="opacity-30">Coming Soon</span>}
        </h4>
        <div className="flex items-center gap-2 mt-1">
          {event && <PriceDisplay price={event.price} discountedPrice={event.discountedPrice} />}
          <span className="text-label-md text-secondary">
            {event ? ` · ${formatDate(event.date)}` : " "}
          </span>
        </div>
      </div>
    </>
  );
  if (!event) return <div className={sharedClass}>{content}</div>;
  return (
    <Link
      to={toEventPath(event)}
      className={`${sharedClass} hover:border-secondary transition-all duration-300`}
    >
      {content}
    </Link>
  );
}

function MoreComingCard() {
  return (
    <div className="bg-surface-container-low border border-outline-variant border-dashed rounded-xl flex flex-col items-center justify-center p-8 opacity-60">
      <span
        className="material-symbols-outlined text-on-surface-variant mb-4"
        style={{ fontSize: "48px" }}
      >
        add_circle
      </span>
      <p className="text-label-md text-on-surface-variant text-center">
        Stay tuned for more upcoming campus events
      </p>
    </div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function EventCatalog() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const { role, logout, token, isAdmin } = useAuth();
  const { orderCount } = useActiveOrder();
  const { hasUnread } = useNotifications();
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    setError(null);

    const controller = new AbortController();
    const timer = setTimeout(
      () => {
        const endpoint = searchQuery
          ? `/discovery/events/search?query=${searchQuery}`
          : "/discovery/events/search";

        axiosClient
          .get(endpoint, {
            signal: controller.signal,
          })
          .then((response) => {
            setEvents(response.data);
            setLoading(false);
          })
          .catch((err) => {
            if (err.name !== "CanceledError") {
              setError(getOrderErrorMessage(err, "Unable to load events."));
              setLoading(false);
            }
          });
      },
      searchQuery ? 350 : 0,
    );

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [searchQuery]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const [hero, side1, side2] = events;
  const isSearching = searchQuery.length > 0;

  return (
    <div className="bg-background text-on-background min-h-screen flex flex-col">
      {/* ── Top Nav ── */}
      <header className="w-full sticky top-0 z-50 bg-surface-dim border-b border-outline-variant">
        <div className="flex items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto gap-4">
          <span className="text-headline-md font-bold text-secondary flex-shrink-0">
            UNI-TICKETS
          </span>
          <nav className="hidden md:flex items-center gap-4 flex-shrink-0">
            <a className="text-secondary border-b-2 border-secondary pb-1 font-bold text-body-md" href="#">Events</a>
            <Link className="text-on-surface-variant hover:text-on-surface transition-colors text-body-md" to="/my-tickets">My Tickets</Link>
          </nav>
          <div className="flex items-center gap-3 ml-auto">
            <div className="relative w-48 hidden sm:block">
              <span
                className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant"
                style={{ fontSize: "18px" }}
              >
                search
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search..."
                className="w-full bg-surface-container-lowest border border-outline text-on-surface py-1.5 pl-9 pr-3 rounded-lg focus:border-secondary outline-none text-label-md placeholder:text-on-surface-variant"
              />
            </div>
            {isAdmin && (
              <Link
                to="/admin"
                className="flex items-center gap-1 text-label-md font-medium px-3 py-1.5 rounded-lg bg-secondary/10 text-secondary hover:bg-secondary/20 transition-colors flex-shrink-0"
                title="Admin Dashboard"
              >
                <span className="material-symbols-outlined" style={{ fontSize: "18px" }}>admin_panel_settings</span>
                <span className="hidden sm:inline">Admin</span>
              </Link>
            )}
            {token && role !== "GUEST" && (
              <button
                onClick={() => navigate("/select-company")}
                className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors text-label-md font-medium flex-shrink-0"
                title="Producer Dashboard"
              >
                <span className="material-symbols-outlined" style={{ fontSize: "20px" }}>dashboard</span>
                <span className="hidden sm:inline">Dashboard</span>
              </button>
            )}
            {token && role !== "GUEST" && (
              <button
                onClick={() => navigate("/inbox")}
                className="relative flex items-center p-2 rounded-full text-on-surface-variant hover:text-secondary transition-colors flex-shrink-0"
                title="Notifications"
              >
                <span className="material-symbols-outlined" style={{ fontSize: "24px" }}>notifications</span>
                {hasUnread && (
                  <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full" />
                )}
              </button>
            )}
            <Link
              to="/profile"
              className="hover:bg-surface-container-highest transition-all p-2 rounded-full active:scale-95 duration-150 flex items-center"
              title="My Profile"
            >
              <span className="material-symbols-outlined text-secondary">
                account_circle
              </span>
            </Link>
            {token && role !== "GUEST" ? (
              <>
                <Link
                  to="/checkout"
                  className="text-label-md text-secondary font-bold border border-secondary/40 px-3 py-1.5 rounded-full hover:bg-secondary/10 transition-colors"
                >
                  Cart ({orderCount})
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-label-md text-on-surface-variant hover:text-secondary transition-colors font-medium"
                >
                  Logout
                </button>
              </>
            ) : (
              <Link
                to="/login"
                className="text-label-md text-on-surface-variant hover:text-secondary transition-colors font-medium"
              >
                Sign in
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="flex-grow">
        {!isSearching && (
          <section className="max-w-container-max-width mx-auto px-margin-mobile md:px-margin-desktop py-12">
            <h2 className="text-headline-md mb-8 text-on-surface">
              Featured Events
            </h2>
            {loading ? (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter h-auto md:h-[500px]">
                <div className="md:col-span-8 rounded-xl bg-surface-container-high animate-pulse min-h-[280px]" />
                <div className="md:col-span-4 flex flex-col gap-gutter">
                  <div className="flex-1 rounded-xl bg-surface-container-high animate-pulse min-h-[150px]" />
                  <div className="flex-1 rounded-xl bg-surface-container-high animate-pulse min-h-[150px]" />
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter h-auto md:h-[500px]">
                <HeroCard event={hero} />
                <div className="md:col-span-4 flex flex-col gap-gutter">
                  <SideCard event={side1} />
                  <SideCard event={side2} />
                </div>
              </div>
            )}
          </section>
        )}

        <section className="max-w-container-max-width mx-auto px-margin-mobile md:px-margin-desktop py-12">
          <div className="mb-8 border-b border-outline-variant pb-4">
            <h2 className="text-headline-md text-on-surface">
              {isSearching ? `Results for "${searchQuery}"` : "All Events"}
            </h2>
          </div>

          {error ? (
            <div className="flex flex-col items-center justify-center py-20 text-center">
              <span className="material-symbols-outlined text-error mb-4" style={{ fontSize: "48px" }}>
                error_outline
              </span>
              <p className="text-headline-sm text-on-surface mb-2">
                Unable to load events
              </p>
              <p className="text-body-md text-on-surface-variant">{error}</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-gutter">
              {loading ? (
                Array.from({ length: 8 }, (_, i) => <EventCardSkeleton key={i} />)
              ) : events.length === 0 ? (
                <div className="col-span-full flex flex-col items-center justify-center py-20 text-center">
                  <span className="material-symbols-outlined text-on-surface-variant mb-4" style={{ fontSize: "48px" }}>
                    {isSearching ? "search_off" : "event_busy"}
                  </span>
                  <p className="text-headline-sm text-on-surface mb-2">
                    {isSearching ? "No results found" : "No events yet"}
                  </p>
                  <p className="text-body-md text-on-surface-variant">
                    {isSearching
                      ? "Try adjusting your search terms"
                      : "Check back soon for upcoming campus events"}
                  </p>
                </div>
              ) : (
                <>
                  {events.map((event, i) => (
                    <EventCard key={event.id ?? i} event={event} />
                  ))}
                  {!isSearching && <MoreComingCard />}
                </>
              )}
            </div>
          )}
        </section>
      </main>

      <footer className="w-full mt-auto bg-surface-dim border-t border-outline-variant">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter px-margin-mobile md:px-margin-desktop py-8 max-w-container-max-width mx-auto">
          <div className="flex flex-col gap-4">
            <span className="text-headline-sm font-bold text-secondary">
              UNI-TICKETS
            </span>
            <p className="text-label-md text-on-surface-variant">
              © 2024 University Ticketing Services. All rights reserved.
            </p>
          </div>
          <div className="flex flex-wrap gap-x-8 gap-y-4 md:justify-end items-center">
            {["Contact Us", "Privacy Policy", "Terms of Service", "Campus Map"].map((link) => (
              <a
                key={link}
                className="text-label-md text-on-surface-variant hover:text-primary transition-colors opacity-80 hover:opacity-100"
                href="#"
              >
                {link}
              </a>
            ))}
          </div>
        </div>
      </footer>
    </div>
  );
}
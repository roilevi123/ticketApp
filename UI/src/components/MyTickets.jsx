import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../contexts/AuthContext';
import { useNotifications } from '../contexts/NotificationContext';

// ─── Toast ────────────────────────────────────────────────────────────────────

function Toast({ toast }) {
  if (!toast) return null;
  const isSuccess = toast.type === 'success';
  return (
    <div
      className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-lg shadow-xl border text-label-md font-bold animate-fadeIn ${
        isSuccess
          ? 'bg-surface-container border-secondary text-secondary'
          : 'bg-surface-container border-error text-error'
      }`}
    >
      <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
        {isSuccess ? 'check_circle' : 'error'}
      </span>
      {toast.message}
    </div>
  );
}

// ─── QR Code Modal ────────────────────────────────────────────────────────────

function QRModal({ ticket, eventName, onClose }) {
  if (!ticket) return null;
  const isGA = ticket.isGA === true;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="bg-surface-container border border-outline-variant rounded-lg p-8 max-w-sm w-full mx-4 flex flex-col items-center gap-5"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex justify-between items-start w-full">
          <div>
            <h3 className="text-headline-sm text-on-surface font-bold">{eventName}</h3>
            <p className="text-label-md text-on-surface-variant mt-0.5">
              {seatLabel(ticket)}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-on-surface-variant hover:text-secondary transition-colors ml-4 flex-shrink-0"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* QR placeholder — swap with a real QR library if desired */}
        <div className="bg-white rounded-lg p-5 flex items-center justify-center">
          <span
            className="material-symbols-outlined text-black"
            style={{ fontSize: '160px', fontVariationSettings: "'FILL' 1" }}
          >
            qr_code_2
          </span>
        </div>

        <div className="text-center space-y-1 w-full">
          <p className="text-label-sm text-secondary uppercase tracking-wider">Ticket ID</p>
          <p className="text-on-surface font-mono text-body-md break-all bg-surface-container-high px-3 py-2 rounded">
            {ticket.id}
          </p>
        </div>

        <button
          onClick={onClose}
          className="w-full bg-secondary text-on-secondary py-3 text-label-md font-bold hover:opacity-90 transition-opacity uppercase"
        >
          Close
        </button>
      </div>
    </div>
  );
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function SkeletonGroup() {
  return (
    <div className="bg-surface-container border border-outline-variant overflow-hidden animate-pulse">
      <div className="flex flex-col lg:flex-row">
        <div className="lg:w-1/3 h-48 lg:h-auto min-h-[12rem] bg-surface-container-high" />
        <div className="lg:w-2/3 p-6 md:p-8 space-y-4">
          <div className="h-6 bg-surface-container-high rounded w-1/2" />
          <div className="h-4 bg-surface-container-high rounded w-1/3" />
          <div className="h-4 bg-surface-container-high rounded w-1/4" />
          <div className="border-t border-outline-variant pt-6 space-y-3">
            <div className="h-16 bg-surface-container-high rounded" />
            <div className="h-16 bg-surface-container-high rounded" />
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const ROW_LABELS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

function seatLabel(ticket) {
  if (ticket.isGA) return "General Admission";
  const row = ROW_LABELS[ticket.row] ?? String(ticket.row + 1);
  const seat = ticket.col + 1;
  return `${row}, Seat ${seat}`;
}

function rowLabel(ticket) {
  if (ticket.isGA) return "General Admission";
  return `Row ${ROW_LABELS[ticket.row] ?? ticket.row + 1}`;
}

function formatDate(dateVal) {
  if (!dateVal) return null;
  const d = new Date(dateVal);
  if (isNaN(d.getTime())) return null;
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
}

function getGroupLatestDate(tickets) {
  if (!tickets?.length) return 0;
  return Math.max(...tickets.map(t => (t.date ? new Date(t.date).getTime() : 0)));
}

// ─── TicketRow ────────────────────────────────────────────────────────────────

function TicketRow({ ticket, onViewQR }) {
  const isGA = ticket.isGA === true;
  return (
    <div
      style={{ background: 'linear-gradient(135deg, #1d2022 0%, #101415 100%)' }}
      className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 border border-outline-variant"
    >
      <div className="flex items-center gap-4">
        <div className="p-2 bg-primary-container rounded flex-shrink-0">
          <span className="material-symbols-outlined text-primary" style={{ fontSize: '24px' }}>
            confirmation_number
          </span>
        </div>
        <div>
          <p className="text-label-sm text-secondary uppercase font-bold">
            {rowLabel(ticket)}
          </p>
          {!isGA && (
            <p className="text-body-md text-on-surface">
              {seatLabel(ticket)}
            </p>
          )}
        </div>
      </div>
      <div>
        <button
          onClick={() => onViewQR(ticket)}
          className="bg-secondary text-on-secondary px-6 py-2 text-label-md font-bold active:scale-95 transition-all hover:brightness-110 flex items-center gap-2 whitespace-nowrap"
        >
          <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>qr_code_2</span>
          View QR Code
        </button>
      </div>
    </div>
  );
}

// ─── EventGroup card ──────────────────────────────────────────────────────────

function EventGroup({ group, onViewQR }) {
  const latestDate = getGroupLatestDate(group.tickets);
  const formattedDate = latestDate ? formatDate(latestDate) : null;
  const count = group.tickets.length;

  return (
    <section className="bg-surface-container border border-outline-variant overflow-hidden group">
      <div className="flex flex-col lg:flex-row">
        {/* Left: event visual */}
        <div className="lg:w-1/3 relative h-48 lg:h-auto min-h-[12rem] bg-primary-container overflow-hidden flex items-center justify-center">
          <span
            className="material-symbols-outlined text-primary opacity-20"
            style={{ fontSize: '140px', fontVariationSettings: "'FILL' 1" }}
          >
            confirmation_number
          </span>
          <div className="absolute inset-0 bg-gradient-to-br from-primary-container/80 to-background/60" />
          <div className="absolute top-4 left-4 bg-secondary text-on-secondary px-3 py-1 text-label-sm font-bold uppercase">
            {count} {count === 1 ? 'Ticket' : 'Tickets'}
          </div>
          <div className="absolute bottom-4 left-4 right-4">
            <p className="text-on-surface font-bold text-headline-sm truncate">{group.event}</p>
          </div>
        </div>

        {/* Right: info + tickets */}
        <div className="lg:w-2/3 p-6 md:p-8 flex flex-col justify-between">
          <div>
            <h2 className="text-headline-md text-on-surface mb-3">{group.event}</h2>
            {formattedDate && (
              <div className="flex items-center text-on-surface-variant text-label-md mb-1">
                <span className="material-symbols-outlined mr-2" style={{ fontSize: '18px' }}>calendar_today</span>
                {formattedDate}
              </div>
            )}
            <div className="flex items-center text-on-surface-variant text-label-md">
              <span className="material-symbols-outlined mr-2" style={{ fontSize: '18px' }}>location_on</span>
              {group.company}
            </div>
          </div>

          <div className="mt-6 border-t border-outline-variant pt-6 space-y-3">
            {group.tickets.map(ticket => (
              <TicketRow key={ticket.id} ticket={ticket} onViewQR={onViewQR} />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function MyTickets() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [qrTicket, setQrTicket] = useState(null);
  const [qrEventName, setQrEventName] = useState('');

  const navigate = useNavigate();
  const { token } = useAuth();
  const { hasUnread } = useNotifications();

  const fetchHistory = useCallback(() => {
    setLoading(true);
    setError(null);
    axiosClient
      .get('/users/history')
      .then(res => setOrders(res.data ?? []))
      .catch(err => setError(err.response?.data?.error ?? err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory, token]);

  // Group by (company + event), merge tickets, sort newest first
  const eventGroups = useMemo(() => {
    const map = new Map();
    for (const order of orders) {
      const key = `${order.company}||${order.event}`;
      if (!map.has(key)) {
        map.set(key, { event: order.event, company: order.company, tickets: [] });
      }
      map.get(key).tickets.push(...(order.tickets ?? []));
    }
    return [...map.values()].sort(
      (a, b) => getGroupLatestDate(b.tickets) - getGroupLatestDate(a.tickets)
    );
  }, [orders]);

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      <QRModal
        ticket={qrTicket}
        eventName={qrEventName}
        onClose={() => { setQrTicket(null); setQrEventName(''); }}
      />

      {/* Decorative background blobs */}
      <div className="fixed inset-0 -z-10 pointer-events-none opacity-20">
        <div className="absolute top-[10%] right-[5%] w-96 h-96 bg-secondary/10 blur-[120px] rounded-full" />
        <div className="absolute bottom-[20%] left-[10%] w-64 h-64 bg-primary/10 blur-[100px] rounded-full" />
      </div>

      {/* ── Header ── */}
      <header className="w-full sticky top-0 z-50 bg-surface-dim border-b border-outline-variant">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/")}
              className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors"
            >
              <span className="material-symbols-outlined">arrow_back</span>
              <span className="text-label-md font-medium">Home</span>
            </button>
          </div>

          <nav className="hidden md:flex items-center gap-8">
            <Link
              to="/"
              className="text-label-md text-on-surface-variant hover:text-secondary transition-colors duration-200"
            >
              Events
            </Link>
            <span className="text-label-md text-secondary font-bold border-b-2 border-secondary pb-0.5">
              My Tickets
            </span>
          </nav>

          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/inbox")}
              className="relative flex items-center p-2 rounded-full text-on-surface-variant hover:text-secondary transition-colors"
              title="Notifications"
            >
              <span className="material-symbols-outlined" style={{ fontSize: "24px" }}>notifications</span>
              {hasUnread && (
                <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full" />
              )}
            </button>
            <Link to="/profile">
              <span
                className="material-symbols-outlined text-secondary hover:opacity-75 transition-opacity cursor-pointer"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                account_circle
              </span>
            </Link>
          </div>
        </div>
      </header>

      {/* ── Main ── */}
      <main className="flex-grow pt-10 pb-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto w-full">
        <header className="mb-10">
          <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-on-surface tracking-tight mb-2">
            My Tickets
          </h1>
          <p className="text-body-md text-on-surface-variant">View your ticket purchase history.</p>
        </header>

        {/* Loading */}
        {loading && (
          <div className="space-y-6">
            <SkeletonGroup />
            <SkeletonGroup />
            <SkeletonGroup />
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className="flex flex-col items-center justify-center py-24 gap-5 text-center">
            <span className="material-symbols-outlined text-error" style={{ fontSize: '56px' }}>
              error_outline
            </span>
            <div>
              <p className="text-headline-sm text-on-surface mb-1">Could not load tickets</p>
              <p className="text-body-md text-on-surface-variant">{error}</p>
            </div>
            <button
              onClick={fetchHistory}
              className="border border-secondary text-secondary px-8 py-3 text-label-md font-bold hover:bg-secondary/10 transition-colors uppercase"
            >
              Try Again
            </button>
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && eventGroups.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 gap-5 text-center">
            <span
              className="material-symbols-outlined text-on-surface-variant"
              style={{ fontSize: '72px', fontVariationSettings: "'FILL' 0" }}
            >
              confirmation_number
            </span>
            <div>
              <h2 className="text-headline-md text-on-surface mb-2">No tickets yet</h2>
              <p className="text-body-md text-on-surface-variant max-w-sm">
                You haven't purchased any tickets. Browse upcoming events to get started.
              </p>
            </div>
            <Link
              to="/"
              className="mt-2 bg-secondary text-on-secondary px-8 py-3 text-label-md font-bold hover:opacity-90 transition-opacity uppercase"
            >
              Browse Events
            </Link>
          </div>
        )}

        {/* Ticket groups */}
        {!loading && !error && eventGroups.length > 0 && (
          <div className="space-y-6">
            {eventGroups.map(group => (
              <EventGroup
                key={`${group.company}||${group.event}`}
                group={group}
                onViewQR={(ticket) => {
                  setQrTicket(ticket);
                  setQrEventName(group.event);
                }}
              />
            ))}
          </div>
        )}
      </main>

      {/* ── Footer ── */}
      <footer className="w-full py-8 mt-auto bg-surface-container-lowest border-t border-outline-variant">
        <div className="flex flex-col md:flex-row justify-between items-center px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto gap-gutter">
          <div className="flex flex-col items-center md:items-start gap-2">
            <span className="text-headline-sm text-secondary font-bold">UNI-TIX</span>
            <p className="text-label-sm text-on-surface-variant">
              © 2024 University Ticketing Systems. All rights reserved.
            </p>
          </div>
          <div className="flex flex-wrap justify-center gap-x-8 gap-y-4">
            {['Campus Map', 'Privacy Policy', 'Terms of Service', 'Accessibility', 'Contact Faculty'].map(link => (
              <a
                key={link}
                href="#"
                className="text-label-sm text-on-surface-variant hover:text-primary transition-colors"
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

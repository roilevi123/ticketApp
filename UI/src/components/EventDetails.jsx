import React, { useState, useEffect, useCallback } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";
import { useActiveOrder } from "../contexts/ActiveOrderContext";
import { getOrderErrorMessage } from "../utils/orderErrors";

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

const ROW_LABELS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];

// Converts the backend 2D map array into flat seat objects.
// rawRow / rawCol are the 0-based indices used by the backend reserve endpoint.
function parseMapData(map) {
  return map.flatMap((row, rowIdx) =>
    row.map((cell, colIdx) => ({
      id: `${ROW_LABELS[rowIdx] ?? rowIdx}${colIdx + 1}`,
      row: ROW_LABELS[rowIdx] ?? String(rowIdx),
      rawRow: rowIdx,
      col: colIdx + 1,
      rawCol: colIdx,
      available: cell === "SEAT",
      vip: rowIdx === 0,
    })),
  );
}

function toSeatRequests(seats) {
  return seats.map((seat) => [seat.rawCol, seat.rawRow]);
}

// Fallback mock seats used when backend provides no map
const TAKEN = new Set([
  "A2", "A5", "B1", "B4", "B7", "C3", "C6", "D2", "D5", "E1", "E4", "E7",
]);
const MOCK_SEATS = ["A", "B", "C", "D", "E"].flatMap((row, rowIdx) =>
  Array.from({ length: 8 }, (_, i) => {
    const id = `${row}${i + 1}`;
    return { id, row, rawRow: rowIdx, col: i + 1, rawCol: i, available: !TAKEN.has(id), vip: row === "A" };
  }),
);

function formatDate(dateStr) {
  if (!dateStr) return "TBD";
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  } catch {
    return String(dateStr);
  }
}

function formatTime(dateStr) {
  if (!dateStr) return "";
  try {
    return new Date(dateStr).toLocaleTimeString("en-US", {
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return "";
  }
}

function formatPrice(price) {
  if (price == null || price === 0) return "Free";
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

// ── SeatingMap ────────────────────────────────────────────────────────────────
// selectedSeats : array of seat objects currently chosen by the user
// atLimit       : true when the policy cap has been reached (greys out non-selected available seats)
function SeatingMap({ seats, selectedSeats, onSeatSelect, atLimit }) {
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
            <span className="text-label-sm text-on-surface-variant w-4 text-center shrink-0">
              {row}
            </span>
            <div className="flex gap-1.5 flex-wrap">
              {seats
                .filter((s) => s.row === row)
                .map((seat) => {
                  const isSelected = selectedSeats.some((s) => s.id === seat.id);
                  const blockedByLimit = !isSelected && atLimit && seat.available;
                  const isDisabled = !seat.available || blockedByLimit;

                  let cls;
                  if (!seat.available) {
                    cls = "bg-surface-container-highest text-outline cursor-not-allowed opacity-40";
                  } else if (isSelected) {
                    cls = "bg-secondary text-on-secondary scale-110 shadow-lg ring-2 ring-secondary/50";
                  } else if (blockedByLimit) {
                    cls = "bg-surface-container border border-outline-variant text-on-surface-variant opacity-30 cursor-not-allowed";
                  } else if (seat.vip) {
                    cls = "bg-primary-container border border-primary/40 text-primary hover:border-primary active:scale-95";
                  } else {
                    cls = "bg-surface-container border border-outline-variant text-on-surface-variant hover:border-secondary hover:text-secondary active:scale-95";
                  }

                  const title = !seat.available
                    ? `Seat ${seat.id} — Unavailable`
                    : isSelected
                    ? `Seat ${seat.id} — Click to deselect`
                    : blockedByLimit
                    ? `Seat limit reached — deselect a seat to choose this one`
                    : `Seat ${seat.id}${seat.vip ? " (VIP)" : ""}`;

                  return (
                    <button
                      key={seat.id}
                      disabled={isDisabled}
                      onClick={() => onSeatSelect(seat)}
                      title={title}
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
          { cls: "bg-surface-container border border-outline-variant", label: "Available" },
          { cls: "bg-primary-container border border-primary/40", label: "VIP" },
          { cls: "bg-secondary ring-2 ring-secondary/50", label: "Selected" },
          { cls: "bg-surface-container-highest opacity-40", label: "Taken" },
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

// ── Main component ────────────────────────────────────────────────────────────
export default function EventDetails() {
  const { companyName, eventName } = useParams();
  const { role, token } = useAuth();
  const navigate = useNavigate();
  const { refreshActiveOrder } = useActiveOrder();

  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [actionError, setActionError] = useState(null);
  const [isReserving, setIsReserving] = useState(false);

  // Multi-seat selection state
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [maxSeats, setMaxSeats] = useState(null); // null = unlimited

  // Reservation state (regular events)
  const [reservation, setReservation] = useState(null); // { orderId, seatCount }

  // ── Lottery state ──────────────────────────────────────────────────────────
  const [lotteryStatus, setLotteryStatus] = useState(null); // { hasLottery, registered, hasWon, winningCode, … }
  const [lotteryLoading, setLotteryLoading] = useState(false);
  const [lotteryActionMsg, setLotteryActionMsg] = useState(null); // { type: 'success'|'error', text }

  // ── Purchase panel state (for lottery winners) ─────────────────────────────
  const [showPurchasePanel, setShowPurchasePanel] = useState(false);
  const [purchaseEmail, setPurchaseEmail] = useState("");
  const [purchaseError, setPurchaseError] = useState(null);
  const [purchaseSuccess, setPurchaseSuccess] = useState(false);
  const [purchaseLoading, setPurchaseLoading] = useState(false);

  const normalizedRole = (role ?? "").toUpperCase();
  const isRegisteredMember = Boolean(token) && normalizedRole !== "GUEST";
  const isHighDemand = event?.highDemand === true;

  // ── Load event ──────────────────────────────────────────────────────────────
  useEffect(() => {
    const controller = new AbortController();

    async function loadEvent() {
      try {
        const res = await axiosClient.get(
          `/discovery/companies/${encodeURIComponent(companyName)}/events/${encodeURIComponent(eventName)}`,
          { signal: controller.signal },
        );
        setEvent(res.data);
      } catch (err) {
        if (err.code !== "ERR_CANCELED") {
          setError(err.message);
        }
      } finally {
        setLoading(false);
      }
    }

    loadEvent();
    return () => controller.abort();
  }, [companyName, eventName]);

  // ── Load seat limit from policy ─────────────────────────────────────────────
  useEffect(() => {
    axiosClient
      .get(
        `/discovery/companies/${encodeURIComponent(companyName)}/events/${encodeURIComponent(eventName)}/seat-limit`,
      )
      .then((res) => setMaxSeats(res.data.maxSeats ?? null))
      .catch(() => setMaxSeats(null)); // graceful fallback: no limit enforced in UI
  }, [companyName, eventName]);

  // ── Load lottery status ────────────────────────────────────────────────────
  const fetchLotteryStatus = useCallback(async () => {
    if (!event) return;
    try {
      const res = await axiosClient.get(
        `/lottery/${encodeURIComponent(companyName)}/${encodeURIComponent(eventName)}/status`,
      );
      setLotteryStatus(res.data);
    } catch {
      // lottery status is best-effort; don't surface errors
    }
  }, [companyName, eventName, event]);

  useEffect(() => {
    fetchLotteryStatus();
  }, [fetchLotteryStatus]);

  // ── Seat toggle with limit enforcement ─────────────────────────────────────
  function handleSeatSelect(seat) {
    setSelectedSeats((prev) => {
      const alreadySelected = prev.some((s) => s.id === seat.id);
      if (alreadySelected) {
        // Always allow deselection
        return prev.filter((s) => s.id !== seat.id);
      }
      // Enforce policy cap
      if (maxSeats !== null && prev.length >= maxSeats) {
        return prev;
      }
      return [...prev, seat];
    });
  }

  // ── Reserve tickets (regular events) ────────────────────────────────────────
  async function handleReserve() {
    if (!isRegisteredMember || selectedSeats.length === 0 || isReserving) return;

    setIsReserving(true);
    setActionError(null);

    try {
      // Backend expects: List<int[]> where each entry is [colIndex, rowIndex] (0-based)
      const requests = toSeatRequests(selectedSeats);
      const res = await axiosClient.post("/orders/reserve", {
        company: companyName,
        event: eventName,
        requests,
      });
      await refreshActiveOrder();
      setReservation({ orderId: res.data, seatCount: selectedSeats.length });
      setSelectedSeats([]);
    } catch (err) {
      setActionError(
        getOrderErrorMessage(err, "Reservation failed. Please try again."),
      );
    } finally {
      setIsReserving(false);
    }
  }

  // ── Lottery registration ───────────────────────────────────────────────────
  const handleEnterLottery = async () => {
    if (!isRegisteredMember) return;
    setLotteryLoading(true);
    setLotteryActionMsg(null);
    try {
      await axiosClient.post(
        `/lottery/${encodeURIComponent(companyName)}/${encodeURIComponent(eventName)}/register`,
      );
      setLotteryActionMsg({ type: "success", text: "You're in! We'll notify you if you win." });
      await fetchLotteryStatus();
    } catch (err) {
      const msg =
        err.response?.data ?? err.response?.data?.message ?? err.message ?? "Registration failed";
      setLotteryActionMsg({ type: "error", text: String(msg) });
    } finally {
      setLotteryLoading(false);
    }
  };

  // ── Ticket purchase (lottery winner flow) ──────────────────────────────────
  const handleLotteryPurchase = async () => {
    if (selectedSeats.length === 0) {
      setPurchaseError("Please select a seat first.");
      return;
    }
    if (!purchaseEmail.trim()) {
      setPurchaseError("Please enter your email address.");
      return;
    }

    setPurchaseLoading(true);
    setPurchaseError(null);

    try {
      // 1. Reserve the selected seats with the lottery code
      const reserveRes = await axiosClient.post("/orders/reserve", {
        company: companyName,
        event: eventName,
        requests: toSeatRequests(selectedSeats),
        lotteryCode: lotteryStatus.winningCode,
      });
      const orderId = reserveRes.data;

      // 2. Purchase the reserved ticket
      await axiosClient.post("/orders/purchase", {
        email: purchaseEmail,
        orderId,
      });

      setPurchaseSuccess(true);
      setShowPurchasePanel(false);
      await refreshActiveOrder();
      await fetchLotteryStatus(); // code is now consumed
    } catch (err) {
      const msg =
        err.response?.data?.error ??
        err.response?.data ??
        err.message ??
        "Purchase failed";
      setPurchaseError(String(msg));
    } finally {
      setPurchaseLoading(false);
    }
  };

  const { gradient, icon } = TYPE_CONFIG[event?.type?.toUpperCase()] ?? DEFAULT_TYPE;
  const seats = event?.map?.length ? parseMapData(event.map) : MOCK_SEATS;
  const atLimit = maxSeats !== null && selectedSeats.length >= maxSeats;

  // ── Selection counter label ─────────────────────────────────────────────────
  const selectionLabel =
    selectedSeats.length === 0
      ? null
      : maxSeats !== null
      ? `${selectedSeats.length} / ${maxSeats} seat${maxSeats !== 1 ? "s" : ""} selected`
      : `${selectedSeats.length} seat${selectedSeats.length !== 1 ? "s" : ""} selected`;

  // Derived lottery helpers
  const lotteryIsOpen  = lotteryStatus?.hasLottery && !lotteryStatus?.drawn;
  const lotteryIsDrawn = lotteryStatus?.hasLottery && lotteryStatus?.drawn;
  const userRegistered = lotteryStatus?.registered === true;
  const userWon        = lotteryStatus?.hasWon === true;
  const winningCode    = lotteryStatus?.winningCode;

  return (
    <div className="bg-background text-on-surface min-h-screen">
      {/* ── Top Nav ── */}
      <nav className="flex items-center justify-between px-margin-mobile w-full h-16 bg-surface">
        <Link
          to="/"
          className="p-2 -ml-2 text-primary flex items-center gap-1 hover:text-secondary transition-colors"
        >
          <span className="material-symbols-outlined">arrow_back</span>
          <span className="text-label-md font-medium">Home</span>
        </Link>
        <span className="text-headline-sm font-bold tracking-tight text-on-surface">
          UNIVERSITY EVENTS
        </span>
        <div className="w-10" />
      </nav>

      <main className="pt-16">
        {loading ? (
          <EventDetailsSkeleton />
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-20 text-center px-margin-mobile">
            <span className="material-symbols-outlined text-error mb-4" style={{ fontSize: "48px" }}>
              error_outline
            </span>
            <p className="text-headline-sm text-on-surface mb-2">Unable to load event</p>
            <p className="text-body-md text-on-surface-variant">{error}</p>
          </div>
        ) : event ? (
          <>
            {/* ── Hero ── */}
            <section className="relative w-full h-[397px] overflow-hidden">
              <div className={`absolute inset-0 bg-gradient-to-br ${gradient} flex items-center justify-center`}>
                <span className="material-symbols-outlined text-white/10" style={{ fontSize: "120px" }}>
                  {icon}
                </span>
              </div>
              <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent" />
            </section>

            {/* ── Event Identity ── */}
            <section className="px-margin-mobile -mt-12 relative z-10">
              {event.type && (
                <div className="inline-flex items-center px-3 py-1 rounded-full bg-on-tertiary-container/10 border border-on-tertiary-container/20 mb-4">
                  <span className="text-label-sm text-on-tertiary-container uppercase tracking-wider">
                    {event.type.replace(/_/g, " ")}
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
                    <p className="text-label-md text-on-surface">{event.location || "Venue TBD"}</p>
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
                {event.description || "No description available."}
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
                {selectionLabel && (
                  <span className={`text-label-sm font-bold ${atLimit ? "text-error" : "text-secondary"}`}>
                    {selectionLabel}
                  </span>
                )}
              </div>

              <p className="text-label-sm text-on-surface-variant mb-1">
                {maxSeats !== null
                  ? `Select up to ${maxSeats} seat${maxSeats !== 1 ? "s" : ""} — policy limit applies.`
                  : "Select one or more seats."}
                {!event.map?.length && " (Preview — map data pending)"}
              </p>

              {atLimit && (
                <p className="text-label-sm text-error mb-4">
                  Limit reached. Deselect a seat to choose a different one.
                </p>
              )}

              <div className="overflow-x-auto pb-2">
                <SeatingMap
                  seats={seats}
                  selectedSeats={selectedSeats}
                  onSeatSelect={handleSeatSelect}
                  atLimit={atLimit}
                />
              </div>
            </section>

            {/* ── Reserve / Buy Tickets ── */}
            {!isHighDemand && (
              <>
                <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
                <section className="px-margin-mobile">
                  <div className="p-4 rounded-xl bg-surface-container border border-outline-variant">
                    {reservation ? (
                      <div className="space-y-4">
                        <div className="flex items-start gap-3">
                          <span className="material-symbols-outlined text-secondary shrink-0">
                            check_circle
                          </span>
                          <div>
                            <h3 className="text-headline-sm text-on-surface mb-1">
                              Seats reserved
                            </h3>
                            <p className="text-label-md text-on-surface-variant">
                              Reservation #{reservation.orderId} for {reservation.seatCount} {reservation.seatCount === 1 ? "seat" : "seats"} is ready for checkout.
                            </p>
                          </div>
                        </div>
                        <button
                          onClick={() => navigate("/checkout")}
                          className="w-full py-3 rounded-xl font-bold text-label-md uppercase tracking-wider bg-secondary text-on-secondary hover:brightness-110 active:scale-95 transition-all"
                        >
                          Go to Checkout
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="flex items-start gap-3">
                          <span className="material-symbols-outlined text-secondary shrink-0">
                            shopping_cart
                          </span>
                          <div>
                            <h3 className="text-headline-sm text-on-surface mb-1">
                              Reserve your tickets
                            </h3>
                            <p className="text-label-md text-on-surface-variant">
                              Select one or more seats above, then reserve them to continue to checkout.
                            </p>
                          </div>
                        </div>

                        {actionError && (
                          <div className="px-4 py-3 rounded-lg bg-error/10 border border-error/30 flex items-center gap-2">
                            <span className="material-symbols-outlined text-error text-[18px]">
                              error
                            </span>
                            <p className="text-label-sm text-error">{actionError}</p>
                          </div>
                        )}

                        <button
                          onClick={handleReserve}
                          disabled={!isRegisteredMember || selectedSeats.length === 0 || isReserving}
                          className={`w-full py-3 rounded-xl font-bold text-label-md uppercase tracking-wider transition-all ${
                            isRegisteredMember && selectedSeats.length > 0
                              ? "bg-secondary text-on-secondary hover:brightness-110 active:scale-95"
                              : "bg-surface-container-highest text-outline cursor-not-allowed"
                          }`}
                        >
                          {isReserving
                            ? "Reserving…"
                            : `Buy Tickets${selectedSeats.length > 0 ? ` (${selectedSeats.length})` : ""}`}
                        </button>

                        {!isRegisteredMember && (
                          <p className="text-label-sm text-on-surface-variant text-center">
                            Sign in as a member to reserve tickets.
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                </section>
              </>
            )}

            {/* ── High Demand / Lottery ── */}
            {isHighDemand && (
              <>
                <div className="mx-margin-mobile h-px bg-outline-variant my-8" />
                <section className="px-margin-mobile">
                  {/* ── Winner purchase panel ── */}
                  {userWon && !purchaseSuccess && (
                    <div className="mb-4 p-4 rounded-xl bg-secondary/10 border border-secondary/30">
                      <div className="flex items-center gap-2 mb-3">
                        <span className="material-symbols-outlined text-secondary">
                          celebration
                        </span>
                        <p className="text-label-md font-bold text-secondary">
                          🎉 You won! Use your code to purchase.
                        </p>
                      </div>
                      <div className="flex items-center gap-2 mb-3 p-2 bg-surface-container rounded-lg border border-outline-variant">
                        <span className="material-symbols-outlined text-on-surface-variant text-[18px]">
                          confirmation_number
                        </span>
                        <code className="text-label-sm text-on-surface font-mono break-all">
                          {winningCode}
                        </code>
                      </div>
                      {!showPurchasePanel ? (
                        <button
                          onClick={() => setShowPurchasePanel(true)}
                          className="w-full py-3 rounded-xl font-bold text-label-md uppercase tracking-wider bg-secondary text-on-secondary hover:brightness-110 active:scale-95 transition-all flex items-center justify-center gap-2"
                        >
                          <span className="material-symbols-outlined text-[18px]">
                            shopping_cart
                          </span>
                          Use Code to Buy Ticket
                        </button>
                      ) : (
                        <div className="space-y-3">
                          {selectedSeats.length === 0 && (
                            <p className="text-label-sm text-on-surface-variant">
                              ← Select a seat from the map above, then complete purchase.
                            </p>
                          )}
                          {selectedSeats.length > 0 && (
                            <p className="text-label-sm text-secondary font-medium">
                              Seat{selectedSeats.length !== 1 ? "s" : ""} selected:{" "}
                              <strong>{selectedSeats.map((s) => s.id).join(", ")}</strong>
                            </p>
                          )}
                          <input
                            type="email"
                            placeholder="Your email address for ticket delivery"
                            value={purchaseEmail}
                            onChange={(e) => setPurchaseEmail(e.target.value)}
                            className="w-full px-4 py-2.5 rounded-xl bg-surface-container border border-outline-variant text-on-surface text-body-md focus:outline-none focus:border-secondary"
                          />
                          {purchaseError && (
                            <p className="text-label-sm text-error">{purchaseError}</p>
                          )}
                          <div className="flex gap-3">
                            <button
                              onClick={() => { setShowPurchasePanel(false); setPurchaseError(null); }}
                              className="flex-1 py-2.5 rounded-xl border border-outline-variant text-on-surface-variant text-label-md font-medium hover:bg-surface-container transition-colors"
                            >
                              Cancel
                            </button>
                            <button
                              onClick={handleLotteryPurchase}
                              disabled={purchaseLoading}
                              className="flex-1 py-2.5 rounded-xl bg-secondary text-on-secondary font-bold text-label-md hover:brightness-110 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {purchaseLoading ? "Processing…" : "Confirm Purchase"}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* ── Purchase success ── */}
                  {purchaseSuccess && (
                    <div className="mb-4 p-4 rounded-xl bg-secondary/10 border border-secondary/30 flex items-center gap-3">
                      <span className="material-symbols-outlined text-secondary">
                        check_circle
                      </span>
                      <p className="text-label-md text-secondary font-bold">
                        Purchase complete! Your ticket is on its way.
                      </p>
                    </div>
                  )}

                  {/* ── Main lottery card ── */}
                  <div className="p-4 rounded-xl bg-tertiary-container/20 border border-on-tertiary-container/30">
                    <div className="flex items-start gap-3 mb-4">
                      <span className="material-symbols-outlined text-on-tertiary-container shrink-0">
                        local_fire_department
                      </span>
                      <div>
                        <h3 className="text-headline-sm text-on-tertiary-container mb-1">
                          High Demand Event
                        </h3>
                        <p className="text-label-sm text-on-surface-variant">
                          Tickets are distributed by lottery. Registered members
                          can enter for a chance to purchase.
                        </p>
                        {lotteryStatus?.endDate && (
                          <p className="text-label-sm text-on-surface-variant mt-1">
                            Registration closes:{" "}
                            <span className="text-on-surface font-medium">
                              {formatDate(lotteryStatus.endDate)}
                            </span>
                            {" · "}
                            {formatTime(lotteryStatus.endDate)}
                          </p>
                        )}
                      </div>
                    </div>

                    {/* State-based button */}
                    {userRegistered && !userWon && !lotteryIsDrawn && (
                      <div className="flex items-center gap-2 w-full py-3 px-4 rounded-xl bg-secondary/10 border border-secondary/30 text-label-md text-secondary font-bold">
                        <span className="material-symbols-outlined text-[18px]">
                          how_to_reg
                        </span>
                        Registered — awaiting draw
                      </div>
                    )}

                    {userRegistered && lotteryIsDrawn && !userWon && (
                      <div className="flex items-center gap-2 w-full py-3 px-4 rounded-xl bg-surface-container border border-outline-variant text-label-md text-on-surface-variant">
                        <span className="material-symbols-outlined text-[18px]">
                          sentiment_dissatisfied
                        </span>
                        Draw complete — better luck next time
                      </div>
                    )}

                    {!userRegistered && lotteryIsDrawn && (
                      <div className="flex items-center gap-2 w-full py-3 px-4 rounded-xl bg-surface-container border border-outline-variant text-label-md text-on-surface-variant">
                        <span className="material-symbols-outlined text-[18px]">
                          lock
                        </span>
                        Lottery closed
                      </div>
                    )}

                    {!userRegistered && lotteryIsOpen && (
                      <div className="relative group">
                        <button
                          disabled={!isRegisteredMember || lotteryLoading}
                          onClick={handleEnterLottery}
                          className={`w-full py-3 rounded-xl font-bold text-label-md uppercase tracking-wider flex items-center justify-center gap-2 transition-all ${
                            isRegisteredMember
                              ? "bg-on-tertiary-container text-on-tertiary active:scale-95 hover:brightness-110 disabled:opacity-50"
                              : "bg-surface-container text-outline cursor-not-allowed border border-outline-variant"
                          }`}
                        >
                          <span className="material-symbols-outlined text-[18px]">
                            casino
                          </span>
                          {lotteryLoading ? "Registering…" : "Enter Lottery"}
                        </button>
                        {!isRegisteredMember && (
                          <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-1.5 bg-surface-container-highest rounded-lg text-label-sm text-on-surface whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none border border-outline-variant z-10">
                            Members only — register to enter the lottery
                          </div>
                        )}
                      </div>
                    )}

                    {/* Inline feedback message */}
                    {lotteryActionMsg && (
                      <p
                        className={`mt-3 text-label-sm text-center ${
                          lotteryActionMsg.type === "success"
                            ? "text-secondary"
                            : "text-error"
                        }`}
                      >
                        {lotteryActionMsg.text}
                      </p>
                    )}
                  </div>
                </section>
              </>
            )}
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-center px-margin-mobile">
            <p className="text-headline-sm text-on-surface mb-2">Event not found</p>
            <p className="text-body-md text-on-surface-variant">The event details could not be loaded.</p>
          </div>
        )}
      </main>

    </div>
  );
}

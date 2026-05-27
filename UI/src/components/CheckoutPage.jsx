import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useActiveOrder } from "../contexts/ActiveOrderContext";
import { getOrderErrorMessage } from "../utils/orderErrors";

function formatMoney(value) {
  return `$${Number(value ?? 0).toFixed(2)}`;
}

function formatRemaining(ms) {
  if (ms <= 0) {
    return "Expired";
  }

  const totalSeconds = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function ticketLabel(ticket) {
  if (ticket.row === 0 && ticket.col === 0) {
    return "General Admission";
  }

  return `Row ${ticket.row}, Seat ${ticket.col}`;
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { userID } = useAuth();
  const {
    tickets,
    loading,
    error,
    remainingMs,
    expirationTime,
    orderCount,
    hasActiveOrder,
    purchaseActiveOrder,
    refreshActiveOrder,
  } = useActiveOrder();
  const [email, setEmail] = useState("");
  const [coupon, setCoupon] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    if (typeof userID === "string" && userID.includes("@")) {
      setEmail(userID);
    }
  }, [userID]);

  const total = useMemo(
    () => tickets.reduce((sum, ticket) => sum + Number(ticket.price ?? 0), 0),
    [tickets],
  );

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!hasActiveOrder) {
      setSubmitError("Your cart is empty. Reserve tickets before checkout.");
      return;
    }

    if (!email.trim()) {
      setSubmitError(
        "Enter the email address for the receipt before checkout.",
      );
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError(null);
      setSuccessMessage(null);

      await purchaseActiveOrder({ email: email.trim(), coupon: coupon.trim() });
      await refreshActiveOrder();

      setSuccessMessage("Purchase completed successfully.");
      setCoupon("");
    } catch (err) {
      setSubmitError(
        getOrderErrorMessage(
          err,
          "Checkout failed. Please review your details and try again.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background text-on-surface">
      <div className="max-w-container-max-width mx-auto px-margin-mobile md:px-margin-desktop py-10">
        <div className="flex flex-col lg:flex-row gap-6">
          <section className="lg:w-[58%] space-y-6">
            <div>
              <p className="text-label-sm uppercase tracking-[0.3em] text-secondary mb-2">
                Active Order
              </p>
              <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-on-surface">
                Cart & Checkout
              </h1>
              <p className="text-body-md text-on-surface-variant mt-2">
                Review your reserved tickets, watch the countdown, and finalize
                the purchase.
              </p>
            </div>

            {successMessage && (
              <div className="p-4 rounded-2xl border border-secondary bg-secondary/10 text-secondary">
                {successMessage}
                <div className="mt-3 flex gap-3">
                  <Link
                    to="/my-tickets"
                    className="inline-flex items-center rounded-full bg-secondary px-5 py-2 text-label-md font-bold text-on-secondary"
                  >
                    View My Tickets
                  </Link>
                  <button
                    type="button"
                    onClick={() => navigate("/")}
                    className="inline-flex items-center rounded-full border border-secondary px-5 py-2 text-label-md font-bold text-secondary"
                  >
                    Back to Events
                  </button>
                </div>
              </div>
            )}

            {error && !loading && (
              <div className="p-4 rounded-2xl border border-error bg-error/10 text-error">
                {error}
              </div>
            )}

            <div className="rounded-3xl border border-outline-variant bg-surface-container-low p-5 md:p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-headline-sm text-on-surface font-bold">
                    Cart
                  </h2>
                  <p className="text-label-md text-on-surface-variant">
                    {orderCount} {orderCount === 1 ? "ticket" : "tickets"}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-label-sm text-on-surface-variant uppercase tracking-wide">
                    Reservation Timer
                  </p>
                  <p className="text-headline-sm text-secondary font-bold">
                    {formatRemaining(remainingMs)}
                  </p>
                </div>
              </div>

              {loading ? (
                <div className="space-y-3 animate-pulse">
                  <div className="h-20 rounded-2xl bg-surface-container-high" />
                  <div className="h-20 rounded-2xl bg-surface-container-high" />
                </div>
              ) : hasActiveOrder ? (
                <div className="space-y-3">
                  {tickets.map((ticket) => (
                    <div
                      key={ticket.id}
                      className="rounded-2xl border border-outline-variant bg-surface-container p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3"
                    >
                      <div>
                        <p className="text-label-sm uppercase tracking-wider text-secondary font-bold">
                          {ticket.event}
                        </p>
                        <h3 className="text-body-lg text-on-surface font-semibold">
                          {ticketLabel(ticket)}
                        </h3>
                        <p className="text-label-md text-on-surface-variant">
                          {ticket.company}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-body-sm text-on-surface-variant">
                          Reserved until
                        </p>
                        <p className="text-label-md text-on-surface font-medium">
                          {expirationTime?.toLocaleTimeString([], {
                            hour: "numeric",
                            minute: "2-digit",
                          }) ?? "Pending"}
                        </p>
                        <p className="text-headline-sm text-secondary font-bold mt-1">
                          {formatMoney(ticket.price)}
                        </p>
                      </div>
                    </div>
                  ))}

                  <div className="flex items-center justify-between rounded-2xl bg-surface-container-high px-4 py-3">
                    <span className="text-label-md text-on-surface-variant">
                      Estimated total
                    </span>
                    <span className="text-headline-sm text-on-surface font-bold">
                      {formatMoney(total)}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-outline-variant p-8 text-center text-on-surface-variant">
                  <p className="text-headline-sm text-on-surface mb-2">
                    Your cart is empty
                  </p>
                  <p className="text-body-md mb-5">
                    Reserve a ticket from an event page, then return here to
                    finish checkout.
                  </p>
                  <Link
                    to="/"
                    className="inline-flex items-center rounded-full bg-secondary px-6 py-3 text-label-md font-bold text-on-secondary"
                  >
                    Browse Events
                  </Link>
                </div>
              )}
            </div>
          </section>

          <aside className="lg:w-[42%] space-y-6">
            <div className="rounded-3xl border border-outline-variant bg-surface-container-low p-5 md:p-6 shadow-sm sticky top-6">
              <div className="flex items-center justify-between mb-5">
                <div>
                  <p className="text-label-sm uppercase tracking-[0.25em] text-secondary mb-2">
                    Checkout
                  </p>
                  <h2 className="text-headline-sm text-on-surface font-bold">
                    Purchase Details
                  </h2>
                </div>
                <span className="material-symbols-outlined text-secondary">
                  credit_card
                </span>
              </div>

              <Link
                to="/"
                className="mb-5 inline-flex w-full items-center justify-center rounded-2xl border border-secondary px-4 py-3 text-label-md font-bold text-secondary transition-colors hover:bg-secondary/10"
              >
                <span className="material-symbols-outlined mr-2 text-[18px]">
                  arrow_back
                </span>
                Back to Events
              </Link>

              <form className="space-y-4" onSubmit={handleSubmit}>
                <label className="block space-y-2">
                  <span className="text-label-md text-on-surface-variant">
                    Receipt email
                  </span>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@university.edu"
                    className="w-full rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                  />
                </label>

                <label className="block space-y-2">
                  <span className="text-label-md text-on-surface-variant">
                    Coupon code
                  </span>
                  <input
                    type="text"
                    value={coupon}
                    onChange={(e) => setCoupon(e.target.value)}
                    placeholder="Optional"
                    className="w-full rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                  />
                </label>

                {submitError && (
                  <div className="rounded-2xl border border-error bg-error/10 px-4 py-3 text-error text-body-md">
                    {submitError}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={submitting || !hasActiveOrder}
                  className={`w-full rounded-full px-6 py-3 text-label-md font-bold transition-all ${submitting || !hasActiveOrder ? "bg-surface-container-high text-outline cursor-not-allowed" : "bg-secondary text-on-secondary hover:brightness-110 active:scale-[0.99]"}`}
                >
                  {submitting ? "Processing..." : "Complete Purchase"}
                </button>
              </form>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}

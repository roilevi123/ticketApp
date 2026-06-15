import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useActiveOrder } from "../contexts/ActiveOrderContext";
import { getOrderErrorMessage } from "../utils/orderErrors";
import axiosClient from "../api/axiosClient";

function formatMoney(value) {
  return `$${Number(value ?? 0).toFixed(2)}`;
}

function formatRemaining(ms) {
  if (ms <= 0) return "Expired";
  const totalSeconds = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function ticketLabel(ticket) {
  if (ticket.row === 0 && ticket.col === 0) return "General Admission";
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

  // === משתני State חדשים עבור שדות האשראי ===
  const [cardNumber, setCardNumber] = useState("");
  const [month, setMonth] = useState("");
  const [year, setYear] = useState("");
  const [holder, setHolder] = useState("");
  const [cvv, setCvv] = useState("");
  const [holderId, setHolderId] = useState("");

  // States for Coupon Logic
  const [discountedTotal, setDiscountedTotal] = useState(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [couponMsg, setCouponMsg] = useState(null);

  useEffect(() => {
    if (typeof userID === "string" && userID.includes("@")) {
      setEmail(userID);
    }
  }, [userID]);

  const originalTotal = useMemo(
      () => tickets.reduce((sum, ticket) => sum + Number(ticket.price ?? 0), 0),
      [tickets],
  );

  // פונקציה מרכזית לחישוב המחיר מול השרת (תומכת גם בהנחות אוטומטיות וגם בקופון ידני)
  const calculateFinalPrice = async (couponCode = "") => {
    const firstTicket = tickets[0];
    if (!firstTicket) return;

    setIsCalculating(true);
    try {
      const firstTicket = tickets[0];
      if (!firstTicket) return;

      const res = await axiosClient.post('/company/policies/discount/calculate', {
        eventId: firstTicket.event,
        companyName: firstTicket.company,
        originalPrice: originalTotal,
        quantity: tickets.length,
        coupon: couponCode.trim() || null // שליחת null אם אין קופון כדי לאפשר הנחה אוטומטית (כמו Simple או Quantity)
      });

      const newTotal = res.data.finalPrice ?? res.data;
      setDiscountedTotal(newTotal);

      // עדכון הודעת פידבק רק אם המשתמש ניסה להזין קופון באופן אקטיבי
      if (couponCode.trim()) {
        if (newTotal < originalTotal) {
          setCouponMsg({ type: 'success', text: 'Coupon applied successfully!' });
        } else {
          setCouponMsg({ type: 'error', text: 'Coupon is invalid or not applicable.' });
        }
      }
    } catch (err) {
      if (couponCode.trim()) {
        setCouponMsg({ type: 'error', text: 'Failed to verify coupon.' });
      }
      console.error("Discount calculation failed:", err);
    } finally {
      setIsCalculating(false);
    }
  };

  // אפקט שרץ אוטומטית כשהעגלה או הכרטיסים נטענים - בודק הנחות אוטומטיות מהשרת
  useEffect(() => {
    if (hasActiveOrder && tickets.length > 0) {
      calculateFinalPrice(coupon);
    } else {
      setDiscountedTotal(null);
    }
  }, [tickets, originalTotal, hasActiveOrder]);

  // הפעלה בלחיצה ידנית על כפתור Apply
  const handleApplyCoupon = () => {
    if (!coupon.trim()) return;
    setCouponMsg(null);
    calculateFinalPrice(coupon);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!hasActiveOrder) {
      setSubmitError("Your cart is empty. Reserve tickets before checkout.");
      return;
    }

    if (!email.trim()) {
      setSubmitError("Enter the email address for the receipt before checkout.");
      return;
    }

    // וולידציה בסיסית שכל שדות האשראי מולאו
    if (!cardNumber || !month || !year || !holder || !cvv || !holderId) {
      setSubmitError("Please fill out all credit card details.");
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError(null);
      setSuccessMessage(null);

      // מעבירים את המבנה החדש הכולל את creditCardDetails ל-Context
      await purchaseActiveOrder({
        email: email.trim(),
        coupon: coupon.trim(),
        creditCardDetails: {
          cardNumber: cardNumber.trim(),
          month: month.trim(),
          year: year.trim(),
          holder: holder.trim(),
          cvv: cvv.trim(),
          id: holderId.trim()
        }
      });

      await refreshActiveOrder();

      setSuccessMessage("Purchase completed successfully.");
      setCoupon("");
      setDiscountedTotal(null);

      // איפוס שדות האשראי
      setCardNumber("");
      setMonth("");
      setYear("");
      setHolder("");
      setCvv("");
      setHolderId("");
    } catch (err) {
      setSubmitError(
          getOrderErrorMessage(err, "Checkout failed. Please review your details and try again."),
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
                  Review your reserved tickets, watch the countdown, and finalize the purchase.
                </p>
              </div>

              {successMessage && (
                  <div className="p-4 rounded-2xl border border-secondary bg-secondary/10 text-secondary">
                    {successMessage}
                    <div className="mt-3 flex gap-3">
                      {userID && (
                        <Link
                            to="/my-tickets"
                            className="inline-flex items-center rounded-full bg-secondary px-5 py-2 text-label-md font-bold text-on-secondary"
                        >
                          View My Tickets
                        </Link>
                        )}
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

                      <div className="flex items-center justify-between rounded-2xl bg-surface-container-high px-4 py-3 mt-4">
                    <span className="text-label-md text-on-surface-variant">
                      Total to pay
                    </span>
                        <div className="flex flex-col items-end">
                          {/* תוקן: מציג את המחיר המקורי הלא מוזל עם קו מוחק עליו */}
                          {discountedTotal !== null && discountedTotal < originalTotal && (
                              <span className="text-label-sm text-on-surface-variant line-through opacity-70">
                          {formatMoney(originalTotal)}
                        </span>
                          )}
                          {/* מציג את המחיר החדש לאחר ההנחה האוטומטית/קופון */}
                          <span className="text-headline-sm text-on-surface font-bold">
                        {formatMoney(discountedTotal !== null ? discountedTotal : originalTotal)}
                      </span>
                        </div>
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

                  <div className="block space-y-2">
                  <span className="text-label-md text-on-surface-variant">
                    Coupon code
                  </span>
                    <div className="flex gap-2">
                      <input
                          type="text"
                          value={coupon}
                          onChange={(e) => {
                            setCoupon(e.target.value);
                            setDiscountedTotal(null);
                            setCouponMsg(null);
                          }}
                          placeholder="Optional"
                          className="flex-1 rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                      />
                      <button
                          type="button"
                          onClick={handleApplyCoupon}
                          disabled={isCalculating || !coupon.trim() || !hasActiveOrder}
                          className="px-4 py-3 rounded-2xl bg-surface-container-highest text-on-surface font-bold text-label-md hover:bg-secondary/20 transition-colors disabled:opacity-50"
                      >
                        {isCalculating ? "..." : "Apply"}
                      </button>
                    </div>
                    {couponMsg && (
                        <p className={`text-label-sm ${couponMsg.type === 'success' ? 'text-secondary' : 'text-error'}`}>
                          {couponMsg.text}
                        </p>
                    )}
                  </div>

                  {/* === החלק החדש שמציג את שדות האשראי בטופס === */}
                  <div className="border-t border-outline-variant pt-4 mt-4 space-y-4">
                    <h3 className="text-body-lg font-bold text-on-surface">Payment Information</h3>

                    <label className="block space-y-2">
                      <span className="text-label-md text-on-surface-variant">Card Holder Name</span>
                      <input
                          type="text"
                          value={holder}
                          onChange={(e) => setHolder(e.target.value)}
                          placeholder="Israel Israeli"
                          className="w-full rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                      />
                    </label>

                    <label className="block space-y-2">
                      <span className="text-label-md text-on-surface-variant">Holder ID</span>
                      <input
                          type="text"
                          value={holderId}
                          onChange={(e) => setHolderId(e.target.value)}
                          placeholder="123456789"
                          className="w-full rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                      />
                    </label>

                    <label className="block space-y-2">
                      <span className="text-label-md text-on-surface-variant">Card Number</span>
                      <input
                          type="text"
                          value={cardNumber}
                          onChange={(e) => setCardNumber(e.target.value)}
                          placeholder="1111222233334444"
                          className="w-full rounded-2xl border border-outline-variant bg-surface-container px-4 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                      />
                    </label>

                    <div className="grid grid-cols-3 gap-3">
                      <label className="block space-y-2">
                        <span className="text-label-md text-on-surface-variant">Exp Month</span>
                        <input
                            type="text"
                            value={month}
                            onChange={(e) => setMonth(e.target.value)}
                            placeholder="MM"
                            maxLength="2"
                            className="w-full text-center rounded-2xl border border-outline-variant bg-surface-container px-2 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                        />
                      </label>

                      <label className="block space-y-2">
                        <span className="text-label-md text-on-surface-variant">Exp Year</span>
                        <input
                            type="text"
                            value={year}
                            onChange={(e) => setYear(e.target.value)}
                            placeholder="YYYY"
                            maxLength="4"
                            className="w-full text-center rounded-2xl border border-outline-variant bg-surface-container px-2 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                        />
                      </label>

                      <label className="block space-y-2">
                        <span className="text-label-md text-on-surface-variant">CVV</span>
                        <input
                            type="password"
                            value={cvv}
                            onChange={(e) => setCvv(e.target.value)}
                            placeholder="123"
                            maxLength="3"
                            className="w-full text-center rounded-2xl border border-outline-variant bg-surface-container px-2 py-3 text-body-md text-on-surface outline-none focus:border-secondary"
                        />
                      </label>
                    </div>
                  </div>

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
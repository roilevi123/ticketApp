import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import axiosClient from "../api/axiosClient";
import { useAuth } from "./AuthContext";
import { getOrderErrorMessage } from "../utils/orderErrors";

const ActiveOrderContext = createContext(null);
const ROW_LABELS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];

function toReservationRequest(seat) {
  if (!seat) {
    return [0, 0];
  }

  const normalizedRow = String(seat.row ?? "")
    .trim()
    .toUpperCase();
  const rowIndex = Number.isInteger(seat.row)
    ? seat.row
    : ROW_LABELS.indexOf(normalizedRow) >= 0
      ? ROW_LABELS.indexOf(normalizedRow)
      : Number.parseInt(normalizedRow, 10) || 0;

  return [Number(seat.col ?? 0), rowIndex];
}

function getExpirationTime(tickets) {
  const firstExpiration = tickets?.[0]?.date;
  const expiration = firstExpiration ? new Date(firstExpiration) : null;
  return expiration && !Number.isNaN(expiration.getTime()) ? expiration : null;
}

export function ActiveOrderProvider({ children }) {
  const { token } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [now, setNow] = useState(Date.now());

  const refreshActiveOrder = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axiosClient.get("/orders/active");
      setTickets(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setTickets([]);
      setError(getOrderErrorMessage(err, "Could not load your active order."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    refreshActiveOrder();
  }, [refreshActiveOrder, token]);

  useEffect(() => {
    if (!tickets.length) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => window.clearInterval(timer);
  }, [tickets.length]);

  const expirationTime = useMemo(() => getExpirationTime(tickets), [tickets]);
  const remainingMs = useMemo(() => {
    if (!expirationTime) {
      return 0;
    }

    return Math.max(expirationTime.getTime() - now, 0);
  }, [expirationTime, now]);
  const isExpired = expirationTime ? remainingMs <= 0 : false;

  useEffect(() => {
    if (isExpired && tickets.length) {
      refreshActiveOrder();
    }
  }, [isExpired, refreshActiveOrder, tickets.length]);

  const reserveTickets = useCallback(
    async ({ company, event, seat }) => {
      const response = await axiosClient.post("/orders/reserve", {
        company,
        event,
        requests: [toReservationRequest(seat)],
      });

      await refreshActiveOrder();
      return response.data;
    },
    [refreshActiveOrder],
  );

  const purchaseActiveOrder = useCallback(
      async ({ email, coupon, creditCardDetails }) => { // <-- הוספנו את המשתנה כאן בתוך הסוגריים המסולסלים
        const response = await axiosClient.post("/orders/purchase", {
          email,
          coupon,
          creditCardDetails
        });

        await refreshActiveOrder();
        return response.data;
      },
      [refreshActiveOrder],
  );

  const value = useMemo(
    () => ({
      tickets,
      loading,
      error,
      expirationTime,
      remainingMs,
      orderCount: tickets.length,
      hasActiveOrder: tickets.length > 0,
      refreshActiveOrder,
      reserveTickets,
      purchaseActiveOrder,
    }),
    [
      tickets,
      loading,
      error,
      expirationTime,
      remainingMs,
      refreshActiveOrder,
      reserveTickets,
      purchaseActiveOrder,
    ],
  );

  return (
    <ActiveOrderContext.Provider value={value}>
      {children}
    </ActiveOrderContext.Provider>
  );
}

export function useActiveOrder() {
  const context = useContext(ActiveOrderContext);

  if (!context) {
    throw new Error(
      "useActiveOrder must be used within an ActiveOrderProvider",
    );
  }

  return context;
}

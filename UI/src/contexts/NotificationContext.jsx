import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from "react";
import { useAuth } from "./AuthContext";
import axiosClient from "../api/axiosClient";

const NotificationContext = createContext();

function extractTitle(raw) {
  try {
    const parsed = JSON.parse(raw);
    const match = (parsed.message || "").match(/^\[([^\]]+)\]/);
    if (match) return match[1];
    return parsed.title || "New message";
  } catch {
    return "New message";
  }
}

export const NotificationProvider = ({ children }) => {
  const { token, role } = useAuth();
  const [hasUnread, setHasUnread] = useState(false);
  const [popup, setPopup] = useState(null);
  const [inboxRefreshTick, setInboxRefreshTick] = useState(0);
  const abortRef = useRef(null);
  const popupTimerRef = useRef(null);

  const clearUnread = () => setHasUnread(false);
  const dismissPopup = useCallback(() => setPopup(null), []);
  const triggerInboxRefresh = useCallback(() => setInboxRefreshTick((t) => t + 1), []);

  const showPopup = useCallback((title) => {
    if (popupTimerRef.current) clearTimeout(popupTimerRef.current);
    setPopup({ title });
    popupTimerRef.current = setTimeout(() => setPopup(null), 4500);
  }, []);

  const refreshUnread = useCallback(async () => {
    if (!token || role === "GUEST") return;
    try {
      const res = await axiosClient.get("/notifications");
      setHasUnread(res.data.some((n) => !n.read));
    } catch (e) {
      console.error("Failed to refresh unread status", e);
    }
  }, [token, role]);

  useEffect(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }

    if (!token || role === "GUEST") return;

    const controller = new AbortController();
    abortRef.current = controller;

    const connectSSE = async () => {
      try {
        const response = await fetch("http://localhost:8080/api/notifications/stream", {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "text/event-stream",
          },
          signal: controller.signal,
        });

        if (!response.ok) return;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        // Messages arriving within the first 2s after connect are replayed
        // unread notifications — count them but don't show individual popups.
        let replayWindowOpen = true;
        let replayCount = 0;
        const replayTimer = setTimeout(() => {
          replayWindowOpen = false;
          if (replayCount > 0) {
            showPopup(
              replayCount === 1
                ? "You have 1 unread message"
                : `You have ${replayCount} unread messages`
            );
          }
        }, 2000);

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          const text = decoder.decode(value, { stream: true });
          for (const line of text.split("\n")) {
            if (line.startsWith("data:")) {
              const data = line.slice(5).trim();
              setHasUnread(true);
              if (replayWindowOpen) {
                replayCount++;
              } else {
                showPopup(extractTitle(data));
                triggerInboxRefresh();
              }
            }
          }
        }

        clearTimeout(replayTimer);
      } catch (e) {
        if (e.name !== "AbortError") {
          console.error("SSE connection error", e);
        }
      }
    };

    connectSSE();

    return () => {
      controller.abort();
    };
  }, [token, role, showPopup, triggerInboxRefresh]);

  return (
    <NotificationContext.Provider value={{
      hasUnread,
      clearUnread,
      refreshUnread,
      popup,
      dismissPopup,
      inboxRefreshTick,
      triggerInboxRefresh,
    }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => useContext(NotificationContext);

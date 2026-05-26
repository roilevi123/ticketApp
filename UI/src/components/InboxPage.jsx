import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useNotifications } from "../contexts/NotificationContext";

function timeAgo(isoString) {
  if (!isoString) return "";
  const diff = Date.now() - new Date(isoString).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes} min${minutes > 1 ? "s" : ""} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours > 1 ? "s" : ""} ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 7) return `${days} days ago`;
  return new Date(isoString).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function parseTitle(message) {
  if (!message) return "";
  try {
    const parsed = JSON.parse(message);
    const match = (parsed.message || "").match(/^\[([^\]]+)\]/);
    if (match) return match[1];
    return parsed.title || message;
  } catch {
    // Fallback for malformed JSON (e.g. unescaped newlines from old server code)
    const subjectMatch = message.match(/"message"\s*:\s*"(\[[^\]]+\])/);
    if (subjectMatch) return subjectMatch[1].replace(/^\[|\]$/g, "");
    const titleMatch = message.match(/"title"\s*:\s*"([^"]+)"/);
    if (titleMatch) return titleMatch[1];
    return message;
  }
}

export default function InboxPage() {
  const { clearUnread, refreshUnread, inboxRefreshTick } = useNotifications();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const fetchNotifications = useCallback(async () => {
    try {
      const res = await axiosClient.get("/notifications");
      const sorted = [...res.data].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
      );
      setNotifications(sorted);
    } catch (e) {
      console.error("Failed to fetch notifications", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
    clearUnread();
  }, [inboxRefreshTick]);

  const toggleRead = async (notif) => {
    const endpoint = notif.read
      ? `/notifications/${notif.id}/unread`
      : `/notifications/${notif.id}/read`;
    try {
      await axiosClient.put(endpoint);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notif.id ? { ...n, read: !n.read } : n))
      );
      refreshUnread();
    } catch (e) {
      console.error("Failed to toggle read status", e);
    }
  };

  const markAllRead = async () => {
    try {
      await axiosClient.put("/notifications/read-all");
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      refreshUnread();
    } catch (e) {
      console.error("Failed to mark all as read", e);
    }
  };

  const filtered = notifications.filter((n) =>
    parseTitle(n.message).toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-background text-on-surface flex flex-col">
      <header className="w-full bg-surface-dim border-b border-outline-variant">
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
        </div>
      </header>

      <main className="flex-grow w-full max-w-[1280px] mx-auto px-4 md:px-16 py-12">
        <header className="mb-10">
          <h1 className="text-[32px] leading-10 md:text-[48px] md:leading-[56px] font-bold tracking-tight text-on-surface mb-2">
            Inbox
          </h1>
          <p className="text-on-surface-variant text-body-md">
            Stay updated with your latest event notifications and campus updates.
          </p>
        </header>

        <div className="bg-surface-container-low rounded-xl border border-outline-variant overflow-hidden">
          {/* Header row */}
          <div className="px-6 py-4 border-b border-outline-variant flex justify-between items-center bg-surface-container">
            <span className="text-label-md text-on-surface-variant uppercase tracking-wider">
              Recent Communications
            </span>
            <div className="flex items-center gap-4">
              <div className="relative hidden md:block">
                <span
                  className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none"
                  style={{ fontSize: "18px" }}
                >
                  search
                </span>
                <input
                  type="text"
                  placeholder="Search messages..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="bg-surface-container-low border border-outline-variant rounded px-10 py-1.5 text-label-md text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors w-56"
                />
              </div>
              <button
                onClick={markAllRead}
                className="text-secondary text-label-sm font-semibold hover:underline"
              >
                Mark all as read
              </button>
            </div>
          </div>

          {/* List */}
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <span className="material-symbols-outlined animate-spin text-on-surface-variant" style={{ fontSize: "32px" }}>
                progress_activity
              </span>
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 px-6 text-center">
              <span
                className="material-symbols-outlined text-outline-variant mb-4"
                style={{ fontSize: "64px" }}
              >
                mail_outline
              </span>
              <p className="text-headline-sm font-semibold text-on-surface mb-1">
                {search ? "No matching messages" : "Your inbox is clear"}
              </p>
              <p className="text-on-surface-variant text-body-md">
                {search
                  ? "Try a different search term."
                  : "We'll notify you when something important arrives."}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-outline-variant">
              {filtered.map((notif) => {
                const title = parseTitle(notif.message);
                return (
                  <div
                    key={notif.id}
                    onClick={() => navigate(`/inbox/${notif.id}`, { state: { notif } })}
                    className="flex items-center justify-between px-6 py-5 transition-colors duration-200 hover:bg-surface-container cursor-pointer"
                  >
                    <div className="flex items-center gap-4 min-w-0">
                      {/* Unread dot */}
                      <div
                        className="flex-shrink-0 w-2 h-2 rounded-full bg-secondary transition-opacity duration-200"
                        style={{ opacity: notif.read ? 0 : 1 }}
                      />
                      <span
                        className={`text-headline-sm font-semibold truncate transition-colors duration-200 ${
                          notif.read ? "text-on-surface-variant" : "text-on-surface"
                        }`}
                      >
                        {title}
                      </span>
                    </div>

                    <div className="flex items-center gap-6 flex-shrink-0 ml-4">
                      <span className="text-label-sm text-on-surface-variant hidden sm:block">
                        {timeAgo(notif.createdAt)}
                      </span>
                      <button
                        onClick={(e) => { e.stopPropagation(); toggleRead(notif); }}
                        title={notif.read ? "Mark as unread" : "Mark as read"}
                        className={`p-2 rounded-full transition-colors duration-200 ${
                          notif.read
                            ? "text-secondary"
                            : "text-on-surface-variant hover:text-secondary"
                        }`}
                      >
                        <span
                          className="material-symbols-outlined"
                          style={{
                            fontSize: "22px",
                            fontVariationSettings: notif.read
                              ? "'FILL' 1"
                              : "'FILL' 0",
                          }}
                        >
                          check_circle
                        </span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

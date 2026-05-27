import React, { useEffect, useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useNotifications } from "../contexts/NotificationContext";

function parseAppointment(displayTitle, body) {
  if (!displayTitle.toLowerCase().includes("appointment")) return null;
  const isManager = displayTitle.toLowerCase().includes("manager");
  const isOwner = displayTitle.toLowerCase().includes("owner");
  if (!isManager && !isOwner) return null;
  const match = body.match(/of '([^']+)'/);
  const companyName = match ? match[1] : null;
  return { type: isManager ? "MANAGER" : "OWNER", companyName };
}

function parseNotification(raw) {
  if (!raw) return { displayTitle: "", body: "", from: null };
  try {
    const parsed = JSON.parse(raw);
    const match = (parsed.message || "").match(/^\[([^\]]+)\]\s*([\s\S]*)/);
    if (match) {
      return {
        displayTitle: match[1],
        body: match[2].trim(),
        from: parsed.title || null,
      };
    }
    return {
      displayTitle: parsed.title || raw,
      body: parsed.message || "",
      from: null,
    };
  } catch {
    // Fallback regex extraction for malformed JSON
    const subjectMatch = raw.match(/"message"\s*:\s*"(\[[^\]]+\])\s*([^"]*)"/);
    if (subjectMatch) {
      return {
        displayTitle: subjectMatch[1].replace(/^\[|\]$/g, ""),
        body: subjectMatch[2].trim(),
        from: (raw.match(/"title"\s*:\s*"([^"]+)"/) || [])[1] || null,
      };
    }
    const titleMatch = raw.match(/"title"\s*:\s*"([^"]+)"/);
    const msgMatch = raw.match(/"message"\s*:\s*"([^"]+)"/);
    return {
      displayTitle: titleMatch ? titleMatch[1] : raw,
      body: msgMatch ? msgMatch[1] : "",
      from: null,
    };
  }
}

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

export default function MessagePage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { refreshUnread } = useNotifications();

  const [notif, setNotif] = useState(location.state?.notif ?? null);
  const [loading, setLoading] = useState(!location.state?.notif);
  const [appointmentStatus, setAppointmentStatus] = useState(null); // null | "approved" | "rejected"
  const [appointmentLoading, setAppointmentLoading] = useState(false);

  useEffect(() => {
    if (notif) return;
    axiosClient
      .get("/notifications")
      .then((res) => {
        const found = res.data.find((n) => String(n.id) === id);
        setNotif(found ?? null);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  // Mark as read when opened (unread → read only)
  useEffect(() => {
    if (!notif || notif.read) return;
    axiosClient
      .put(`/notifications/${notif.id}/read`)
      .then(() => refreshUnread())
      .catch(() => {});
  }, [notif?.id, notif?.read]);

  const handleApprove = async (appointment) => {
    setAppointmentLoading(true);
    try {
      await axiosClient.post(
        `/company/approve-appointment?companyName=${encodeURIComponent(appointment.companyName)}&type=${appointment.type}`
      );
      setAppointmentStatus("approved");
    } catch (err) {
      alert(err.response?.data?.error || err.response?.data || "Failed to approve appointment.");
    } finally {
      setAppointmentLoading(false);
    }
  };

  const handleReject = async (appointment) => {
    setAppointmentLoading(true);
    try {
      const endpoint = appointment.type === "MANAGER"
        ? `/company/manager/relinquish?companyName=${encodeURIComponent(appointment.companyName)}`
        : `/company/owner/relinquish?companyName=${encodeURIComponent(appointment.companyName)}`;
      await axiosClient.delete(endpoint);
      setAppointmentStatus("rejected");
    } catch (err) {
      alert(err.response?.data?.error || err.response?.data || "Failed to reject appointment.");
    } finally {
      setAppointmentLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <span
          className="material-symbols-outlined animate-spin text-on-surface-variant"
          style={{ fontSize: "32px" }}
        >
          progress_activity
        </span>
      </div>
    );
  }

  if (!notif) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center gap-4">
        <span
          className="material-symbols-outlined text-on-surface-variant"
          style={{ fontSize: "48px" }}
        >
          mail_off
        </span>
        <p className="text-on-surface-variant text-body-md">Message not found.</p>
        <button
          onClick={() => navigate("/inbox")}
          className="text-secondary text-label-md hover:underline"
        >
          Back to Inbox
        </button>
      </div>
    );
  }

  const { displayTitle, body, from } = parseNotification(notif.message);

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      {/* Sticky header */}
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex items-center h-16 gap-4 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <button
            onClick={() => navigate("/inbox")}
            className="flex items-center text-on-surface-variant hover:text-secondary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <span className="text-headline-md font-bold text-secondary">Inbox</span>
        </div>
      </header>

      <main className="flex-grow pt-12 pb-16 px-margin-mobile md:px-margin-desktop max-w-3xl mx-auto w-full">
        {/* Back breadcrumb */}
        <nav className="mb-8">
          <button
            onClick={() => navigate("/inbox")}
            className="inline-flex items-center gap-2 text-secondary text-label-md hover:-translate-x-1 transition-transform duration-200"
          >
            <span className="material-symbols-outlined" style={{ fontSize: "18px" }}>
              arrow_back
            </span>
            Back to Inbox
          </button>
        </nav>

        {/* Title section */}
        <div className="mb-10">
          {from && (
            <p className="text-label-sm text-on-surface-variant uppercase tracking-wider mb-3">
              {from}
            </p>
          )}
          <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-on-surface leading-tight mb-3">
            {displayTitle}
          </h1>
          {notif.createdAt && (
            <p className="text-label-md text-on-surface-variant">{timeAgo(notif.createdAt)}</p>
          )}
        </div>

        {/* Content card */}
        <div
          className="border border-outline-variant rounded-xl p-8 md:p-12 shadow-xl"
          style={{
            background:
              "linear-gradient(180deg, rgba(30, 41, 59, 0.3) 0%, rgba(15, 23, 42, 0) 100%)",
          }}
        >
          <p className="text-body-lg text-on-surface whitespace-pre-wrap leading-relaxed">
            {body || displayTitle}
          </p>
        </div>

        {/* Appointment approve / reject */}
        {(() => {
          const appointment = parseAppointment(displayTitle, body || displayTitle);
          if (!appointment || !appointment.companyName) return null;
          if (appointmentStatus === "approved") {
            return (
              <div className="mt-6 flex items-center gap-2 text-green-400 text-body-md">
                <span className="material-symbols-outlined" style={{ fontSize: "20px" }}>check_circle</span>
                You are now a {appointment.type.toLowerCase()} of <strong>{appointment.companyName}</strong>.
              </div>
            );
          }
          if (appointmentStatus === "rejected") {
            return (
              <div className="mt-6 flex items-center gap-2 text-on-surface-variant text-body-md">
                <span className="material-symbols-outlined" style={{ fontSize: "20px" }}>cancel</span>
                You have declined the {appointment.type.toLowerCase()} appointment for <strong>{appointment.companyName}</strong>.
              </div>
            );
          }
          return (
            <div className="mt-6 flex gap-4 items-center">
              <p className="text-label-sm text-on-surface-variant mr-2">Respond to appointment:</p>
              <button
                onClick={() => handleApprove(appointment)}
                disabled={appointmentLoading}
                className="px-5 py-2 bg-secondary text-on-secondary text-label-md font-bold rounded hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
              >
                {appointmentLoading
                  ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>
                  : <span className="material-symbols-outlined" style={{ fontSize: "16px" }}>check</span>}
                Approve
              </button>
              <button
                onClick={() => handleReject(appointment)}
                disabled={appointmentLoading}
                className="px-5 py-2 border border-error text-error text-label-md font-bold rounded hover:bg-error hover:text-on-error transition-colors disabled:opacity-50 flex items-center gap-2"
              >
                {appointmentLoading
                  ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>
                  : <span className="material-symbols-outlined" style={{ fontSize: "16px" }}>close</span>}
                Reject
              </button>
            </div>
          );
        })()}
      </main>
    </div>
  );
}

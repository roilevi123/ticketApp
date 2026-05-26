import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../contexts/AuthContext';
import { useNotifications } from '../contexts/NotificationContext';

// ─── Toast notification ───────────────────────────────────────────────────────

function Toast({ toast }) {
  if (!toast) return null;
  const isSuccess = toast.type === "success";
  return (
    <div
      className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-lg shadow-xl border text-label-md font-bold animate-fadeIn ${
        isSuccess
          ? "bg-surface-container border-secondary text-secondary"
          : "bg-surface-container border-error text-error"
      }`}
    >
      <span className="material-symbols-outlined" style={{ fontSize: "20px" }}>
        {isSuccess ? "check_circle" : "error"}
      </span>
      {toast.message}
    </div>
  );
}

// ─── Skeleton input ───────────────────────────────────────────────────────────

function SkeletonInput() {
  return (
    <div className="w-full h-12 bg-surface-container-high rounded animate-pulse" />
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function MemberProfile() {
  // Profile state
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [studentId, setStudentId] = useState("");
  const [age, setAge] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  // Loading flags
  const [profileLoading, setProfileLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);

  // Support form state
  const [supportType, setSupportType] = useState("ADMIN");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");

  // Toast: { type: 'success' | 'error', message: string } | null
  const [toast, setToast] = useState(null);

  const navigate = useNavigate();
  const { token, role, logout } = useAuth();
  const { hasUnread } = useNotifications();

  function showToast(type, msg) {
    setToast({ type, message: msg });
    setTimeout(() => setToast(null), 3500);
  }

  // ── Fetch profile on mount ──────────────────────────────────────────────────
  useEffect(() => {
    axiosClient.get('/users/profile')
      .then((res) => {
        const data = res.data;
        // Backend stores one combined name field; split it for the two inputs
        const parts = (data.name ?? '').trim().split(/\s+/);
        setFirstName(parts[0] ?? '');
        setLastName(parts.slice(1).join(' '));
        setEmail(data.email ?? '');
        setStudentId(data.ID ?? '');
        setAge(data.age ?? null);
      })
      .finally(() => setProfileLoading(false));
  }, [token]);

  // ── Save profile ────────────────────────────────────────────────────────────
  async function handleSave() {
    setSaveLoading(true);
    try {
      // Backend uses combined name + ID (studentId) + email + age
      await axiosClient.put('/users/profile', {
        name: `${firstName} ${lastName}`.trim(),
        ID: studentId,
        email,
        age,
      });
      setIsEditing(false);
      showToast("success", "Profile saved successfully.");
    } catch (err) {
      // Extract the real server-side error message when available
      const serverMsg =
        err.response?.data?.error ||
        err.response?.data?.message ||
        err.message;
      const status = err.response?.status ?? 'Network error';
      showToast("error", `Could not save profile (${status}): ${serverMsg}`);
    } finally {
      setSaveLoading(false);
    }
  }

  // ── Submit support ticket ───────────────────────────────────────────────────
  async function handleSupportSubmit(e) {
    e.preventDefault();
    setSubmitLoading(true);
    try {
      await axiosClient.post('/users/support/message', {
        recipientRole: supportType,
        content: subject ? `[${subject}] ${message}` : message,
      });
      setSubject('');
      setMessage('');
      setSupportType('ADMIN');
      showToast('success', "Message sent successfully.");
    } catch (err) {
      showToast("error", `Could not send message: ${err.message}`);
    } finally {
      setSubmitLoading(false);
    }
  }

  const inputBase =
    "w-full bg-background border border-outline-variant text-body-md px-4 py-3 rounded focus:outline-none focus:border-secondary transition-all";

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      <Toast toast={toast} />

      {/* Decorative background blobs */}
      <div className="fixed inset-0 -z-10 pointer-events-none opacity-20">
        <div className="absolute top-[10%] right-[5%] w-96 h-96 bg-secondary/10 blur-[120px] rounded-full" />
        <div className="absolute bottom-[20%] left-[10%] w-64 h-64 bg-primary/10 blur-[100px] rounded-full" />
      </div>

      {/* ── Header ── */}
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="flex items-center text-on-surface-variant hover:text-secondary transition-colors"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </button>
            <span className="text-headline-md font-bold text-secondary">
              My Profile &amp; Support
            </span>
          </div>

          <nav className="hidden md:flex items-center gap-8">
            <Link
              to="/"
              className="text-label-md text-on-surface-variant hover:text-secondary transition-colors duration-200"
            >
              Events
            </Link>
            <Link
              to="/my-tickets"
              className="text-label-md text-on-surface-variant hover:text-secondary transition-colors duration-200"
            >
              My Tickets
            </Link>
          </nav>

          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate("/inbox")}
              className="relative flex items-center p-2 rounded-full text-on-surface-variant hover:text-secondary transition-colors"
              title="Notifications"
            >
              <span className="material-symbols-outlined">notifications</span>
              {hasUnread && (
                <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full" />
              )}
            </button>
            <span
              className="material-symbols-outlined text-secondary cursor-pointer"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              account_circle
            </span>
          </div>
        </div>
      </header>

      {/* ── Main ── */}
      <main className="flex-grow pt-8 pb-12 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto w-full">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter">
          {/* ── Left Column: Personal Details ── */}
          <section className="md:col-span-5 flex flex-col gap-6">
            <div className="bg-surface-container p-8 rounded-lg border border-outline-variant shadow-lg">
              <div className="flex items-center justify-between mb-8">
                <h2 className="text-headline-sm text-on-surface">
                  Personal Details
                </h2>
                {profileLoading ? (
                  <div className="h-6 w-6 rounded-full bg-surface-container-high animate-pulse" />
                ) : (
                  <span className="material-symbols-outlined text-secondary">
                    badge
                  </span>
                )}
              </div>

              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    First Name
                  </label>
                  {profileLoading ? (
                    <SkeletonInput />
                  ) : (
                    <input
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      readOnly={!isEditing}
                      className={`${inputBase} ${isEditing ? "text-on-surface cursor-text" : "text-on-surface-variant cursor-not-allowed"}`}
                    />
                  )}
                </div>

                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    Last Name
                  </label>
                  {profileLoading ? (
                    <SkeletonInput />
                  ) : (
                    <input
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      readOnly={!isEditing}
                      className={`${inputBase} ${isEditing ? "text-on-surface cursor-text" : "text-on-surface-variant cursor-not-allowed"}`}
                    />
                  )}
                </div>

                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    Email Address
                  </label>
                  {profileLoading ? (
                    <SkeletonInput />
                  ) : (
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      readOnly={!isEditing}
                      className={`${inputBase} ${isEditing ? "text-on-surface cursor-text" : "text-on-surface-variant cursor-not-allowed"}`}
                    />
                  )}
                </div>

                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    University Student ID
                  </label>
                  {profileLoading ? (
                    <SkeletonInput />
                  ) : (
                    <input
                      type="text"
                      value={studentId}
                      readOnly
                      className={`${inputBase} text-on-surface-variant cursor-not-allowed`}
                    />
                  )}
                </div>

                <div className="pt-4 flex flex-col sm:flex-row gap-4">
                  {isEditing ? (
                    <button
                      onClick={() => setIsEditing(false)}
                      disabled={saveLoading}
                      className="flex-1 border border-outline-variant text-on-surface-variant text-label-md py-3 px-6 rounded hover:bg-surface-container-high transition-colors uppercase font-bold disabled:opacity-50"
                    >
                      Cancel
                    </button>
                  ) : (
                    <button
                      onClick={() => setIsEditing(true)}
                      disabled={profileLoading}
                      className="flex-1 border border-secondary text-secondary text-label-md py-3 px-6 rounded hover:bg-secondary/10 transition-colors uppercase font-bold disabled:opacity-50"
                    >
                      Edit Details
                    </button>
                  )}
                  <button
                    onClick={handleSave}
                    disabled={saveLoading || profileLoading}
                    className="flex-1 bg-secondary text-on-secondary text-label-md py-3 px-6 rounded hover:opacity-90 transition-opacity uppercase font-bold disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {saveLoading && (
                      <span
                        className="material-symbols-outlined animate-spin"
                        style={{ fontSize: "16px" }}
                      >
                        progress_activity
                      </span>
                    )}
                    Save Changes
                  </button>
                </div>
              </div>
            </div>

            {/* Account Status card */}
            <div className="bg-primary-container p-6 rounded-lg border border-outline-variant relative overflow-hidden">
              <div className="relative z-10">
                <h3 className="text-on-primary-container text-label-md font-bold mb-2 uppercase">
                  Account Status
                </h3>
                <div className="flex items-center gap-2">
                  <span className="inline-block h-2 w-2 rounded-full bg-secondary" />
                  <p className="text-on-surface text-body-md">
                    Verified Alumni Member
                  </p>
                </div>
              </div>
              <div className="absolute top-[-20%] right-[-10%] opacity-10 pointer-events-none">
                <span
                  className="material-symbols-outlined text-on-surface"
                  style={{ fontSize: "120px" }}
                >
                  school
                </span>
              </div>
            </div>
          </section>

          {/* ── Right Column: Contact Support ── */}
          <section className="md:col-span-7">
            <div className="bg-surface-container p-8 rounded-lg border border-outline-variant shadow-lg h-full flex flex-col">
              <div className="flex items-center justify-between mb-8">
                <h2 className="text-headline-sm text-on-surface">
                  Contact Support
                </h2>
                <span className="material-symbols-outlined text-secondary">
                  contact_support
                </span>
              </div>

              <form
                onSubmit={handleSupportSubmit}
                className="space-y-6 flex-grow flex flex-col"
              >
                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    Send message to:
                  </label>
                  <div className="relative">
                    <select
                      value={supportType}
                      onChange={(e) => setSupportType(e.target.value)}
                      className="w-full bg-background border border-outline-variant text-on-surface text-body-md px-4 py-3 rounded appearance-none focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-all"
                    >
                      <option value="ADMIN">Admin</option>
                      <option value="PRODUCER">Event Producer</option>
                    </select>
                    <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
                      <span className="material-symbols-outlined text-on-surface-variant">
                        expand_more
                      </span>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    Subject
                  </label>
                  <input
                    type="text"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    required
                    placeholder="e.g. Issue with Alumni Dinner Ticket"
                    className="w-full bg-background border border-outline-variant text-on-surface text-body-md px-4 py-3 rounded focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-all placeholder:text-on-surface-variant/40"
                  />
                </div>

                <div className="space-y-2 flex-grow flex flex-col">
                  <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                    Message
                  </label>
                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    required
                    placeholder="How can we help you today?"
                    className="w-full flex-grow bg-background border border-outline-variant text-on-surface text-body-md px-4 py-4 rounded focus:outline-none focus:border-secondary focus:ring-1 focus:ring-secondary transition-all placeholder:text-on-surface-variant/40 min-h-[250px] resize-none"
                  />
                </div>

                <div className="pt-6">
                  <button
                    type="submit"
                    disabled={submitLoading}
                    className="w-full bg-secondary text-on-secondary text-label-md py-4 px-8 rounded hover:opacity-90 transition-opacity uppercase font-bold flex items-center justify-center gap-3 disabled:opacity-50"
                  >
                    {submitLoading ? (
                      <span
                        className="material-symbols-outlined animate-spin"
                        style={{ fontSize: "20px" }}
                      >
                        progress_activity
                      </span>
                    ) : (
                      <span
                        className="material-symbols-outlined"
                        style={{ fontSize: "20px" }}
                      >
                        verified_user
                      </span>
                    )}
                    {submitLoading ? "Submitting…" : "Submit Securely"}
                  </button>
                  <p className="text-center text-label-sm text-on-surface-variant mt-4">
                    Average response time: 2–4 business hours
                  </p>
                </div>
              </form>
            </div>
          </section>
        </div>
      </main>

      {/* ── Footer ── */}
      <footer className="w-full py-8 mt-auto bg-surface-container-lowest border-t border-outline-variant">
        <div className="flex flex-col md:flex-row justify-between items-center px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto gap-gutter">
          <div className="flex flex-col items-center md:items-start gap-2">
            <span className="text-headline-sm text-secondary font-bold">
              UNI-TIX
            </span>
            <p className="text-label-sm text-on-surface-variant">
              © 2024 University Ticketing Systems. All rights reserved.
            </p>
          </div>
          <div className="flex flex-wrap justify-center gap-x-8 gap-y-4">
            {[
              "Campus Map",
              "Privacy Policy",
              "Terms of Service",
              "Accessibility",
              "Contact Faculty",
            ].map((link) => (
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

import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";

export default function CreateCompany() {
  const [companyName, setCompanyName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const parseJwt = (token) => {
    try {
      return JSON.parse(atob(token.split(".")[1]));
    } catch {
      return null;
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await axiosClient.post("/company/open", { companyName });
      const founderToken = res.data.token;
      const decoded = parseJwt(founderToken);
      const role = decoded?.role?.toUpperCase() || "FOUNDER";
      const userId = decoded?.sub || "unknown";
      login(founderToken, role, userId);
      localStorage.setItem("activeCompany", companyName);
      navigate("/producer-dashboard");
    } catch (err) {
      setError(err.response?.data?.error || "Failed to create company. The name may already be taken.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      {/* Header */}
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <button
            onClick={() => navigate("/select-company")}
            className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
            <span className="text-label-md font-medium">Back</span>
          </button>
          <span className="text-headline-md font-bold text-secondary">New Company</span>
          <Link to="/profile">
            <span
              className="material-symbols-outlined text-secondary hover:opacity-75 transition-opacity cursor-pointer"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              account_circle
            </span>
          </Link>
        </div>
      </header>

      <main className="flex-grow pt-10 pb-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto w-full">
        <header className="mb-10">
          <h1 className="text-display-lg-mobile md:text-display-lg font-bold text-on-surface tracking-tight mb-2">
            Establish Your Company
          </h1>
          <p className="text-body-md text-on-surface-variant">
            You will become the Founder and Owner of this organization.
          </p>
        </header>

        <div className="max-w-lg">
          <div className="bg-surface-container border border-outline-variant rounded-lg p-8 shadow-lg">
            {error && (
              <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="flex flex-col gap-6">
              <div className="space-y-2">
                <label className="block text-label-sm text-on-surface-variant uppercase tracking-wider">
                  Company Name
                </label>
                <input
                  type="text"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  placeholder="e.g. BGU Productions"
                  required
                  className="w-full bg-background border border-outline-variant text-on-surface text-body-md px-4 py-3 rounded focus:outline-none focus:border-secondary transition-all placeholder:text-on-surface-variant/40"
                />
              </div>

              <div className="pt-2 space-y-3">
                <button
                  type="submit"
                  disabled={loading || !companyName.trim()}
                  className="w-full bg-secondary text-on-secondary py-3 text-label-md font-bold hover:opacity-90 transition-opacity uppercase disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {loading && (
                    <span className="material-symbols-outlined animate-spin" style={{ fontSize: "16px" }}>
                      progress_activity
                    </span>
                  )}
                  {loading ? "Creating..." : "Launch Company"}
                </button>
                <button
                  type="button"
                  onClick={() => navigate("/select-company")}
                  className="w-full border border-outline-variant text-on-surface-variant py-3 text-label-md font-bold hover:bg-surface-container-high transition-colors uppercase"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}

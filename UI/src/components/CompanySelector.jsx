import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";

export default function CompanySelector() {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
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

  useEffect(() => {
    axiosClient
      .get("/auth/my-companies")
      .then((res) => setCompanies(res.data))
      .catch(() => setError("Failed to load your companies."))
      .finally(() => setLoading(false));
  }, []);

  const handleSelectCompany = async (companyName) => {
    try {
      const res = await axiosClient.post("/auth/switch-company", { companyName });
      const newToken = res.data.token;
      const decoded = parseJwt(newToken);
      const newRole = decoded?.role?.toUpperCase() || "MEMBER";
      const userId = decoded?.sub || "unknown";
      localStorage.setItem("priorMemberToken", localStorage.getItem("token"));
      login(newToken, newRole, userId);
      localStorage.setItem("activeCompany", companyName);
      navigate("/producer-dashboard");
    } catch {
      setError("Failed to switch to the selected company.");
    }
  };

  return (
    <div className="bg-background text-on-surface min-h-screen flex flex-col">
      {/* Header */}
      <header className="w-full bg-surface-dim border-b border-outline-variant">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <button
            onClick={() => navigate("/")}
            className="flex items-center gap-1 text-on-surface-variant hover:text-secondary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
            <span className="text-label-md font-medium">Home</span>
          </button>
          <span className="text-headline-md font-bold text-secondary">Producer Portal</span>
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
            Your Companies
          </h1>
          <p className="text-body-md text-on-surface-variant">Select a company to enter its producer dashboard.</p>
        </header>

        {error && (
          <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        {loading && (
          <div className="flex items-center gap-3 text-on-surface-variant">
            <span className="material-symbols-outlined animate-spin">progress_activity</span>
            <span className="text-body-md">Loading your workspace...</span>
          </div>
        )}

        {!loading && companies.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 gap-6 text-center">
            <span
              className="material-symbols-outlined text-on-surface-variant"
              style={{ fontSize: "72px", fontVariationSettings: "'FILL' 0" }}
            >
              business
            </span>
            <div>
              <h2 className="text-headline-md text-on-surface mb-2">You have no companies yet</h2>
              <p className="text-body-md text-on-surface-variant max-w-sm">
                Create your first company to start managing events and tickets.
              </p>
            </div>
            <button
              onClick={() => navigate("/create-company")}
              className="bg-secondary text-on-secondary py-3 px-8 text-label-md font-bold hover:opacity-90 transition-opacity uppercase"
            >
              Create New Company
            </button>
          </div>
        )}

        {!loading && companies.length > 0 && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
              {companies.map((company) => (
                <button
                  key={company}
                  onClick={() => handleSelectCompany(company)}
                  className="bg-surface-container border border-outline-variant p-8 rounded-lg text-left hover:border-secondary hover:bg-secondary/5 transition-all group"
                >
                  <div className="flex items-start justify-between mb-4">
                    <span
                      className="material-symbols-outlined text-secondary"
                      style={{ fontSize: "40px", fontVariationSettings: "'FILL' 1" }}
                    >
                      business
                    </span>
                    <span className="material-symbols-outlined text-on-surface-variant group-hover:text-secondary transition-colors">
                      arrow_forward
                    </span>
                  </div>
                  <h3 className="text-headline-sm font-bold text-on-surface group-hover:text-secondary transition-colors mb-1">
                    {company}
                  </h3>
                  <p className="text-body-md text-on-surface-variant">Enter Producer Dashboard</p>
                </button>
              ))}
            </div>

            <div className="border-t border-outline-variant pt-8 flex justify-center">
              <button
                onClick={() => navigate("/create-company")}
                className="border border-secondary text-secondary px-8 py-3 text-label-md font-bold hover:bg-secondary/10 transition-colors uppercase"
              >
                + Create Another Company
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

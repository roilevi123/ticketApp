import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";

function CompanySelector() {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const parseJwt = (token) => {
    try {
      return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
      return null;
    }
  };

  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const response = await axiosClient.get("/auth/my-companies");
        setCompanies(response.data);
      } catch (err) {
        setError("Failed to load your companies.");
      } finally {
        setLoading(false);
      }
    };
    fetchCompanies();
  }, []);

  const handleSelectCompany = async (companyName) => {
    try {
      const response = await axiosClient.post("/auth/switch-company", { companyName });
      const newToken = response.data.token;

      const decoded = parseJwt(newToken);
      const newRole = decoded && decoded.role ? String(decoded.role).toUpperCase() : "MEMBER";
      const userId = decoded ? decoded.sub : "unknown";

      login(newToken, newRole, userId);
      localStorage.setItem("activeCompany", companyName);
      navigate("/producer-dashboard");
    } catch (err) {
      setError("Failed to switch to the selected company.");
    }
  };

  if (loading) {
    return <div className="text-center mt-20 text-headline-sm text-on-surface">Loading your workspace...</div>;
  }

  // מצב 1: למשתמש אין חברות
  if (companies.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-8 min-h-[70vh] bg-surface-dim rounded-2xl border border-outline-variant my-8">
        <h2 className="text-headline-lg font-bold mb-8 text-center text-on-surface">
          You are not a part of any company
        </h2>
        <button
          onClick={() => navigate("/create-company")}
          className="bg-secondary text-on-secondary py-4 px-8 text-label-lg font-bold rounded-xl hover:brightness-110 active:scale-95 transition-all shadow-lg"
        >
          Create New Company
        </button>
      </div>
    );
  }

  // מצב 2: יש למשתמש חברות
  return (
    <div className="flex flex-col items-center p-8 min-h-[70vh] bg-surface-dim rounded-2xl border border-outline-variant my-8">
      <h2 className="text-headline-md font-bold mb-8 text-center text-on-surface">
        Select a Company to Manage
      </h2>

      {error && (
        <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-4xl mb-12">
        {companies.map((company) => (
          <button
            key={company}
            onClick={() => handleSelectCompany(company)}
            className="bg-surface-container-highest border border-outline-variant p-8 rounded-xl shadow-lg hover:border-secondary hover:bg-secondary/5 transition-all text-left group"
          >
            <h3 className="text-headline-sm font-bold text-on-surface group-hover:text-secondary transition-colors">
              {company}
            </h3>
            <p className="text-body-md text-on-surface-variant mt-2">Click to enter Producer Dashboard</p>
          </button>
        ))}
      </div>

      <div className="w-full max-w-2xl pt-8 border-t border-outline-variant flex justify-center">
        <button
          onClick={() => navigate("/create-company")}
          className="border-2 border-secondary text-secondary py-3 px-8 text-label-md font-bold rounded-lg hover:bg-secondary/10 transition-all"
        >
          + Create Another Company
        </button>
      </div>
    </div>
  );
}

export default CompanySelector;
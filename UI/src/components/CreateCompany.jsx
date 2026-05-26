import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../contexts/AuthContext";

function CreateCompany() {
  const [companyName, setCompanyName] = useState("");
  const [loading, setLoading] = useState(false);
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await axiosClient.post("/company/open", { companyName });
      const founderToken = response.data.token;
      
      const decoded = parseJwt(founderToken);
      const role = decoded && decoded.role ? String(decoded.role).toUpperCase() : "FOUNDER";
      const userId = decoded ? decoded.sub : "unknown";

      login(founderToken, role, userId);
      localStorage.setItem("activeCompany", companyName);
      navigate("/producer-dashboard");
    } catch (err) {
      const errMsg = err.response?.data?.error || "Failed to create company. Maybe name is taken?";
      setError(errMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 min-h-[70vh] bg-surface-dim rounded-2xl border border-outline-variant my-8">
      <div className="bg-surface-container-highest border border-outline-variant p-8 rounded-xl w-full max-w-md shadow-2xl">
        <h2 className="text-headline-md text-on-surface mb-2 text-center">
          Establish Your Company
        </h2>
        <p className="text-body-md text-on-surface-variant mb-6 text-center">
          You will be the Founder and Owner of this organization.
        </p>

        {error && (
          <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="e.g. BGU Productions"
            required
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />

          <button
            type="submit"
            disabled={loading || !companyName.trim()}
            className="bg-secondary text-on-secondary py-3 px-4 text-label-md font-bold rounded-lg hover:brightness-110 active:scale-95 transition-all mt-4 disabled:opacity-50"
          >
            {loading ? "Creating..." : "Launch Company"}
          </button>
          
          <button
            type="button"
            onClick={() => navigate("/select-company")}
            className="mt-2 text-center text-body-md text-on-surface-variant hover:text-secondary hover:underline transition-colors font-medium"
          >
            Cancel
          </button>
        </form>
      </div>
    </div>
  );
}

export default CreateCompany;
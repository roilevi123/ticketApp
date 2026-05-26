import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import axiosClient from "../api/axiosClient";

function Login() {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  // Helper to decode JWT to get userID and role
  const parseJwt = (token) => {
    try {
      return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
      return null;
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMsg("");
    setSuccessMsg("");

    try {
      const response = await axiosClient.post("/auth/login", formData);
      const data = response.data;

      setSuccessMsg("Login successful! Redirecting...");

      // Extract token payload
      const jwtToken = data.token;
      const decoded = parseJwt(jwtToken);
      const userId = decoded ? decoded.sub : "unknown";

      // Keep role canonical in UPPERCASE
      let role =
        decoded && decoded.role ? String(decoded.role).toUpperCase() : "MEMBER";

      login(jwtToken, role, userId);

      setTimeout(() => {
        navigate("/");
      }, 1500);
    } catch (err) {
      const respData = err.response?.data;
      setErrorMsg(`Login failed: ${respData?.error || "Invalid credentials"}`);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 min-h-[70vh] bg-surface-dim rounded-2xl border border-outline-variant my-8">
      <div className="bg-surface-container-highest border border-outline-variant p-12 rounded-xl w-full max-w-[36rem] shadow-2xl">
        <h2 className="text-headline-md text-on-surface mb-6 text-center">
          Login
        </h2>

        {errorMsg && (
          <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="bg-secondary/10 border border-secondary text-secondary text-label-md px-4 py-3 rounded mb-6">
            {successMsg}
          </div>
        )}

        <form onSubmit={handleLogin} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Username"
            required
            onChange={(e) =>
              setFormData({ ...formData, username: e.target.value })
            }
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <input
            type="password"
            placeholder="Password"
            required
            onChange={(e) =>
              setFormData({ ...formData, password: e.target.value })
            }
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <button
            type="submit"
            className="bg-secondary text-on-secondary py-3 px-4 text-label-md font-bold rounded-lg hover:brightness-110 active:scale-95 transition-all mt-2"
          >
            Log In
          </button>
        </form>

        <div className="mt-6 text-center text-body-md text-on-surface-variant">
          Don't have an account?{" "}
          <Link
            to="/register"
            className="text-secondary hover:underline font-medium"
          >
            Register here
          </Link>
        </div>

        <div className="mt-4 pt-4 border-t border-outline-variant text-center">
          <button
            onClick={() => navigate("/")}
            className="text-on-surface-variant text-label-md hover:text-secondary transition-colors"
          >
            View as guest
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login;

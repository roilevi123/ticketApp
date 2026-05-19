import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

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
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer guest-temporary-token",
        },
        body: JSON.stringify(formData),
      });
      const data = await response.json();

      if (response.ok && data.success) {
        setSuccessMsg("Login successful! Redirecting...");

        // Extract token payload
        const jwtToken = data.data;
        const decoded = parseJwt(jwtToken);
        const userId = decoded ? decoded.sub : "unknown";

        // Normalize role to Title Case (e.g. "MEMBER" -> "Member")
        let role = decoded && decoded.role ? decoded.role : "Member";
        if (role === role.toUpperCase()) {
          role = role.charAt(0).toUpperCase() + role.slice(1).toLowerCase();
        }

        login(jwtToken, role, userId);

        setTimeout(() => {
          navigate("/profile");
        }, 1500);
      } else {
        setErrorMsg(`Login failed: ${data.message || "Invalid credentials"}`);
      }
    } catch (err) {
      setErrorMsg("Login failed: Unable to connect to server");
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 min-h-[50vh]">
      <div className="bg-white p-6 rounded-lg shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-bold mb-4 text-center">Login</h2>

        {errorMsg && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative mb-4">
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
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <input
            type="password"
            placeholder="Password"
            required
            onChange={(e) =>
              setFormData({ ...formData, password: e.target.value })
            }
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <button
            type="submit"
            className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
          >
            Log In
          </button>
        </form>

        <div className="mt-4 text-center text-sm">
          Don't have an account?{" "}
          <Link to="/register" className="text-blue-600 hover:underline">
            Register here
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Login;

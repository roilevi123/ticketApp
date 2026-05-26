import React, { createContext, useContext, useState, useEffect } from "react";
import axiosClient from "../api/axiosClient";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  // Start without restoring any previous member token so the app
  // always loads as a guest session on first open.
  const [token, setToken] = useState(null);
  const [role, setRole] = useState("GUEST");
  const [userID, setUserID] = useState(null);

  const fetchGuestToken = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/auth/guest");
      const data = await response.json();
      if (response.ok) {
        const guestToken = data.token;
        setToken(guestToken);
        setRole("GUEST");
        localStorage.setItem("token", guestToken);
        localStorage.setItem("role", "GUEST");
      }
    } catch (e) {
      console.error("Failed to fetch guest token", e);
    }
  };

  useEffect(() => {
    // Ensure any previously stored member token is cleared so the
    // app loads as guest (prevents staying logged-in on page reload).
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("userID");
    fetchGuestToken();
  }, []);

  const login = (newToken, authRole, authUserID) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("role", (authRole || "MEMBER").toUpperCase());
    localStorage.setItem("userID", authUserID);

    setToken(newToken);
    setRole((authRole || "MEMBER").toUpperCase());
    setUserID(authUserID);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("userID");

    setToken(null);
    setRole("GUEST");
    setUserID(null);

    fetchGuestToken();
  };

  return (
    <AuthContext.Provider value={{ token, role, userID, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

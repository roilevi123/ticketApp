import React, { createContext, useContext, useState } from "react";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem("token") || null);
  const [role, setRole] = useState(localStorage.getItem("role") || "Guest");
  const [userID, setUserID] = useState(localStorage.getItem("userID") || null);

  const login = (newToken, authRole, authUserID) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("role", authRole || "Member");
    localStorage.setItem("userID", authUserID);

    setToken(newToken);
    setRole(authRole || "Member");
    setUserID(authUserID);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("userID");

    setToken(null);
    setRole("Guest");
    setUserID(null);
  };

  return (
    <AuthContext.Provider value={{ token, role, userID, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

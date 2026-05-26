import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

function ProtectedRoute({ children, allowedRoles }) {
  const { token, role } = useAuth();

  if (!token || role === "Guest") {
    // If user is not authenticated or just a Guest, redirect to login page
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    // If authenticated but role doesn't match, redirect to home page
    return <Navigate to="/" replace />;
  }

  return children;
}

export default ProtectedRoute;

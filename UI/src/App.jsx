import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";

import EventCatalog from "./components/EventCatalog";
import CompanyProfile from "./components/CompanyProfile";
import EventDetails from "./components/EventDetails";
import MemberProfile from "./components/MemberProfile";
import Login from "./components/Login";
import Register from "./components/Register";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<EventCatalog />} />
          <Route path="/company/:companyName" element={<CompanyProfile />} />
          <Route
            path="/event/:companyName/:eventName"
            element={<EventDetails />}
          />

          {/* Auth Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Routes */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute allowedRoles={["Member", "Admin", "Producer"]}>
                <MemberProfile />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

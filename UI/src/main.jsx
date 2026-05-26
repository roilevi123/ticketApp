import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "./index.css";
import { AuthProvider } from "./contexts/AuthContext.jsx";

// Ensure any persisted member session is cleared synchronously
// before the app renders so the initial load is treated as a guest.
//localStorage.removeItem("token");
//localStorage.removeItem("role");
//localStorage.removeItem("userID");

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>,
);

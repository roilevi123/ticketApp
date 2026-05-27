import { Outlet, useLocation } from "react-router-dom";
import { useNotifications } from "../contexts/NotificationContext";

export default function Layout() {
  const { popup, dismissPopup } = useNotifications();
  const location = useLocation();
  const isProducerDashboard = location.pathname.includes("/producer-dashboard");

  return (
    <div className={`min-h-screen ${isProducerDashboard ? "bg-[#101415]" : "bg-background"}`}>
      {popup && (
        <div className="fixed top-16 right-4 z-50 animate-slide-in">
          <div className="flex items-start gap-3 bg-[#1d2022] border border-[#e9c349] text-white px-4 py-3 rounded-lg shadow-2xl min-w-[260px] max-w-xs">
            <span
              className="material-symbols-outlined text-[#e9c349] flex-shrink-0 mt-0.5"
              style={{ fontSize: "20px", fontVariationSettings: "'FILL' 1" }}
            >
              notifications
            </span>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold uppercase tracking-wider text-[#e9c349] mb-0.5">
                New message
              </p>
              <p className="text-sm text-white truncate">{popup.title}</p>
            </div>
            <button
              onClick={dismissPopup}
              className="text-gray-400 hover:text-white flex-shrink-0 ml-1"
            >
              <span className="material-symbols-outlined" style={{ fontSize: "18px" }}>
                close
              </span>
            </button>
          </div>
        </div>
      )}
      <Outlet />
    </div>
  );
}

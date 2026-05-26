import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import axiosClient from "../api/axiosClient";

export default function Layout() {
  const { role, logout, login, token } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // מזהה האם אנחנו באזור שדורש תפריט של מפיק
  const isProducerArea = 
    location.pathname.includes("/producer-dashboard") || 
    location.pathname.includes("/select-company") || 
    location.pathname.includes("/create-company");

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const parseJwt = (token) => {
    try {
      return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
      return null;
    }
  };

  // הפונקציה שמוחקת את החברה ומחזירה אותך למשתמש רגיל!
  const handleExitCompany = async () => {
    try {
      const response = await axiosClient.post("/auth/exit-company");
      const memberToken = response.data.token;
      
      const decoded = parseJwt(memberToken);
      const newRole = decoded && decoded.role ? String(decoded.role).toUpperCase() : "MEMBER";
      const userId = decoded ? decoded.sub : "unknown";

      localStorage.removeItem("activeCompany");
      login(memberToken, newRole, userId);
      
      navigate("/");
    } catch (err) {
      console.error("Failed to exit company context", err);
    }
  };

  return (
    <div className={`min-h-screen flex flex-col ${isProducerArea ? "bg-[#101415]" : "bg-zinc-100"}`}>
      {/* כאן התפריט משנה צבע אם אתה מפיק או משתמש רגיל */}
      <nav className={`${isProducerArea ? "bg-[#191c1e] border-b border-outline-variant" : "bg-blue-500"} text-white px-6 py-3 transition-colors duration-300`}>
        <div className="flex justify-between items-center">
          <Link to="/" className="font-bold text-lg flex items-center gap-2">
            Ticket System
          </Link>

          <div className="flex gap-4 items-center">
            
            {/* הכפתור הקריטי: מופיע רק כשאתה מפיק! */}
            {isProducerArea && role !== "MEMBER" && role !== "GUEST" ? (
              <button onClick={handleExitCompany} className="font-medium text-error hover:brightness-110 transition-colors flex items-center gap-1 bg-error-container/10 px-3 py-1 rounded">
                <span className="material-symbols-outlined text-[18px]">logout</span>
                Exit Dashboard
              </button>
            ) : (
              <Link to="/" className="font-medium hover:underline">Home</Link>
            )}

            {role === "ADMIN" && <Link to="/admin">Dashboard</Link>}

            {token && role !== "GUEST" && !isProducerArea && (
              <Link to="/select-company" className="font-medium hover:underline">Producer Area</Link>
            )}

            {token && role !== "GUEST" ? (
              <>
                <Link to="/profile" className="font-medium hover:underline">Profile</Link>
                <button onClick={handleLogout} className="font-medium hover:underline text-gray-300">Logout</button>
              </>
            ) : (
              <Link to="/login" className="font-medium hover:underline">Sign in</Link>
            )}
          </div>
        </div>
      </nav>

      <div className={`${isProducerArea ? "" : "p-5"} flex-grow flex flex-col`}>
        <Outlet />
      </div>
    </div>
  );
}
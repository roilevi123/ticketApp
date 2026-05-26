import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Layout() {
  const { role, logout, token } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isProducerArea = 
    location.pathname.includes("/producer-dashboard") || 
    location.pathname.includes("/select-company") || 
    location.pathname.includes("/create-company");

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div
      className={`min-h-screen ${isProducerArea ? "bg-[#101415]" : "bg-zinc-100"}`}
    >
      <nav className="bg-blue-500 text-white px-6 py-3">
        <div className="flex justify-between">
          <Link to="/" className="font-bold text-lg">
            Ticket System
          </Link>

          <div className="flex gap-3 items-center">
            <Link to="/">Home</Link>

            {role === "ADMIN" && <Link to="/admin">Dashboard</Link>}
            {/* השינוי: מציגים לכל משתמש מחובר, ומפנים למסך בחירת החברה */}
            {token && role !== "GUEST" && (
              <Link to="/select-company">Producer Area</Link>
            )}

            {token && role !== "GUEST" ? (
              <>
                <Link to="/profile">Profile</Link>
                <button
                  onClick={handleLogout}
                  className="font-medium hover:underline"
                >
                  Logout
                </button>
              </>
            ) : (
              <Link to="/login" className="font-medium hover:underline">
                Sign in
              </Link>
            )}
          </div>
        </div>
      </nav>

      <div className="p-5">
        <Outlet />
      </div>
    </div>
  );
}

import { Link, Outlet, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useNotifications } from "../contexts/NotificationContext";

export default function Layout() {
  const { role } = useAuth();
  const { popup, dismissPopup } = useNotifications();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const searchQuery = searchParams.get('q') ?? '';
  const isProducerDashboard = location.pathname.includes("/producer-dashboard");

  const handleSearch = (value) => {
    if (location.pathname === '/') {
      setSearchParams(value ? { q: value } : {}, { replace: true });
    } else {
      navigate(value ? `/?q=${encodeURIComponent(value)}` : '/');
    }
  };

  const navLink = (to, label) => (
    <Link
      to={to}
      className={`text-body-md transition-colors ${
        location.pathname === to
          ? 'text-secondary border-b-2 border-secondary pb-1 font-bold'
          : 'text-on-surface-variant hover:text-on-surface'
      }`}
    >
      {label}
    </Link>
  );

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
      <header className="w-full sticky top-0 bg-surface-dim border-b border-outline-variant z-50">
        <div className="flex justify-between items-center h-16 px-margin-mobile md:px-margin-desktop max-w-container-max-width mx-auto">
          <div className="flex items-center gap-8">
            <Link to="/" className="text-headline-md font-bold text-secondary">UNI-TICKETS</Link>
            <nav className="hidden md:flex items-center gap-6">
              {navLink('/', 'Events')}
              {navLink('/my-tickets', 'My Tickets')}
              <a href="#" className="text-on-surface-variant hover:text-on-surface transition-colors text-body-md">Help</a>
              {role === 'ADMIN' && navLink('/admin', 'Dashboard')}
            </nav>
          </div>
          <div className="flex items-center gap-4 flex-1 justify-end">
            <div className="relative max-w-md w-full hidden sm:block">
              <span
                className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant"
                style={{ fontSize: '20px' }}
              >
                search
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => handleSearch(e.target.value)}
                placeholder="Search events, artists, venues..."
                className="w-full bg-surface-container-lowest border border-outline text-on-surface py-2 pl-10 pr-4 rounded-xl focus:border-secondary outline-none text-body-md placeholder:text-on-surface-variant"
              />
            </div>
            <Link
              to="/profile"
              className="hover:bg-surface-container-highest transition-all p-2 rounded-full active:scale-95 duration-150 flex items-center"
              title="My Profile"
            >
              <span className="material-symbols-outlined text-secondary">account_circle</span>
            </Link>
          </div>
        </div>
      </header>
      <Outlet />
    </div>
  );
}

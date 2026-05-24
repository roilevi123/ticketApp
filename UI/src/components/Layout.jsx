import { Link, Outlet, useLocation } from 'react-router-dom';


export default function Layout() {
    const role = localStorage.getItem('role');
    const location = useLocation();

    const isProducerDashboard = location.pathname.includes('/producer-dashboard');

    const logout = () => {
        localStorage.removeItem('role');
        localStorage.removeItem('token');
        window.location.href = '/login';
    };

    return (
            <div className={`min-h-screen ${isProducerDashboard ? 'bg-[#101415]' : 'bg-zinc-100'}`}>            <nav className="bg-blue-500 text-white px-6 py-3">
                <div className="flex justify-between">
                    <Link to="/" className="font-bold text-lg">
                        Ticket System
                    </Link>

                    <div className="flex gap-3">
                        <Link to="/">Home</Link>

                        {role === 'ADMIN' && (
                            <Link to="/admin">Dashboard</Link>
                        )}

                        {(role === 'OWNER' || role === 'MANAGER' || role === 'FOUNDER') && (
                            <Link to="/producer-dashboard">Producer Area</Link>
                        )}

                        {role ? (
                            <button onClick={logout}>
                                Logout
                            </button>
                        ) : (
                            <Link to="/login">Sign in</Link>
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
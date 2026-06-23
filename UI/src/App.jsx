import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Layout from "./components/Layout";
import EventCatalog from "./components/EventCatalog";
import CompanyProfile from "./components/CompanyProfile";
import EventDetails from "./components/EventDetails";
import ProducerDashboard from "./components/ProducerDashboard";
import CompanySelector from "./components/CompanySelector";
import CreateCompany from "./components/CreateCompany";
import Login from "./components/Login";
import Register from "./components/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import MemberProfile from "./components/MemberProfile";
import MyTickets from "./components/MyTickets";
import AdminDashboard from "./components/AdminDashboard";
import InboxPage from "./components/InboxPage";
import MessagePage from "./components/MessagePage";
import CheckoutPage from "./components/CheckoutPage";
import RemovedAccountPage from "./components/RemovedAccountPage";

import { NotificationProvider } from "./contexts/NotificationContext";
import { ActiveOrderProvider } from "./contexts/ActiveOrderContext";
import { useAuth } from "./contexts/AuthContext";

function AdminRoute({ children }) {
    const { token, role, isAdmin } = useAuth();

    if (!token || role === "GUEST") return <Navigate to="/login" replace />;
    if (!isAdmin) return <Navigate to="/" replace />;

    return children;
}

function WaitingForExternalSystem() {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 px-4 text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-gray-900 mb-6"></div>

            <h1 className="text-3xl font-bold mb-3">
                External System Unavailable
            </h1>

            <p className="text-gray-600 max-w-md">
                Waiting for a connection to the external payment and ticketing services.
            </p>

            <p className="text-gray-500 mt-2">
                Retrying automatically...
            </p>
        </div>
    );
}

function App() {
    const [externalReady, setExternalReady] = useState(false);

    useEffect(() => {
        async function checkExternalSystem() {
            try {
                const response = await fetch("http://localhost:8080/api/system/status", {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem("token")}`
                    }
                });

                console.log("status response:", response.status);

                const text = await response.text();

                console.log("status body:", text);

                setExternalReady(text.trim() === "true");
            } catch (e) {
                console.error("Status check failed:", e);
                setExternalReady(false);
            }
        }

        checkExternalSystem();

        const interval = setInterval(checkExternalSystem, 3000);

        return () => clearInterval(interval);
    }, []);

    if (!externalReady) {
        return <WaitingForExternalSystem />;
    }

    return (
        <BrowserRouter>
            <NotificationProvider>
                <ActiveOrderProvider>
                    <Routes>
                        <Route path="/" element={<Layout />}>
                            <Route index element={<EventCatalog />} />

                            <Route
                                path="company/:companyName"
                                element={<CompanyProfile />}
                            />

                            <Route
                                path="event/:companyName/:eventName"
                                element={<EventDetails />}
                            />

                            <Route path="checkout" element={<CheckoutPage />} />

                            <Route
                                path="producer-dashboard"
                                element={
                                    <ProtectedRoute allowedRoles={["OWNER", "MANAGER", "FOUNDER"]}>
                                        <ProducerDashboard />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="select-company"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <CompanySelector />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="create-company"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <CreateCompany />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="admin"
                                element={
                                    <AdminRoute>
                                        <AdminDashboard />
                                    </AdminRoute>
                                }
                            />

                            <Route path="login" element={<Login />} />
                            <Route path="register" element={<Register />} />

                            <Route
                                path="/profile"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <MemberProfile />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="/my-tickets"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <MyTickets />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="/inbox"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <InboxPage />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="/inbox/:id"
                                element={
                                    <ProtectedRoute
                                        allowedRoles={[
                                            "MEMBER",
                                            "ADMIN",
                                            "OWNER",
                                            "MANAGER",
                                            "FOUNDER",
                                        ]}
                                    >
                                        <MessagePage />
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="account-removed"
                                element={<RemovedAccountPage />}
                            />

                            <Route
                                path="*"
                                element={
                                    <div className="text-center text-2xl mt-10">
                                        404 - Page Not Found
                                    </div>
                                }
                            />
                        </Route>
                    </Routes>
                </ActiveOrderProvider>
            </NotificationProvider>
        </BrowserRouter>
    );
}

export default App;
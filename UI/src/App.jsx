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
import { NotificationProvider } from "./contexts/NotificationContext";
import CheckoutPage from "./components/CheckoutPage";
import { ActiveOrderProvider } from "./contexts/ActiveOrderContext";
import { useAuth } from "./contexts/AuthContext";
import RemovedAccountPage from "./components/RemovedAccountPage";
import GlobalErrorBanner from "./components/GlobalErrorBanner";
function AdminRoute({ children }) {
  const { token, role, isAdmin } = useAuth();
  if (!token || role === "GUEST") return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/" replace />;
  return children;
}

function App() {
  return (
    <BrowserRouter>
        <GlobalErrorBanner />
      <NotificationProvider>
        <ActiveOrderProvider>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<EventCatalog />} />
          <Route path="company/:companyName" element={<CompanyProfile />} />
          <Route path="event/:companyName/:eventName" element={<EventDetails />} />
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
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
                <CompanySelector />
              </ProtectedRoute>
            }
          />
          <Route
            path="create-company"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
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
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
                <MemberProfile />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-tickets"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
                <MyTickets />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inbox"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
                <InboxPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inbox/:id"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "ADMIN", "OWNER", "MANAGER", "FOUNDER"]}>
                <MessagePage />
              </ProtectedRoute>
            }
          />
          <Route path="account-removed" element={<RemovedAccountPage />} />
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

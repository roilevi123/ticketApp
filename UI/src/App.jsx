import { BrowserRouter, Routes, Route } from "react-router-dom";
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
import InboxPage from "./components/InboxPage";
import MessagePage from "./components/MessagePage";
import { NotificationProvider } from "./contexts/NotificationContext";

function App() {
  return (
    <BrowserRouter>
      <NotificationProvider>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<EventCatalog />} />
          <Route path="company/:companyName" element={<CompanyProfile />} />
          <Route path="event/:companyName/:eventName" element={<EventDetails />} />
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
      </NotificationProvider>
    </BrowserRouter>
  );
}

export default App;

import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import EventCatalog from "./components/EventCatalog";
import CompanyProfile from "./components/CompanyProfile";
import EventDetails from "./components/EventDetails";
import ProducerDashboard from "./components/ProducerDashboard";
import Login from "./components/Login";
import Register from "./components/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import MemberProfile from "./components/MemberProfile";
import CompanySelector from "./components/CompanySelector";
import CreateCompany from "./components/CreateCompany";

function App() {
  return (
    <BrowserRouter>
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
            path="/select-company"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "OWNER", "MANAGER", "FOUNDER"]}>
                <CompanySelector />
              </ProtectedRoute>
            }
          />
          <Route
            path="/create-company"
            element={
              <ProtectedRoute allowedRoles={["MEMBER", "OWNER", "MANAGER", "FOUNDER"]}>
                <CreateCompany />
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
    </BrowserRouter>
  );
}

export default App;

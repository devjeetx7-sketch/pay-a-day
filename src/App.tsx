import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { LanguageProvider } from "@/contexts/LanguageContext";
import { AppDataProvider } from "@/contexts/AppDataContext";
import ProtectedRoute from "@/components/ProtectedRoute";
import AdminRoute from "@/components/AdminRoute";
import Login from "@/pages/Login";
import RoleSelection from "@/pages/RoleSelection";
import Dashboard from "@/pages/Dashboard";
import { ContractorDetail } from "@/pages/ContractorDetail";
import { WorkerDetail } from "@/pages/WorkerDetail";
import CalendarPage from "@/pages/CalendarPage";
import HistoryRouter from "@/pages/HistoryRouter";
import StatsPage from "@/pages/StatsPage";
import SettingsPage from "@/pages/SettingsPage";
import WorkersPage from "@/pages/WorkersPage";
import AdminDashboard from "@/pages/admin/AdminDashboard";
import AdminUserDetail from "@/pages/admin/AdminUserDetail";
import NotFound from "@/pages/NotFound";

const App = () => (
  <LanguageProvider>
    <AuthProvider>
      <AppDataProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/select-role" element={<RoleSelection />} />
          <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/contractor/:id" element={<ProtectedRoute><ContractorDetail /></ProtectedRoute>} />
          <Route path="/worker/:id" element={<ProtectedRoute><WorkerDetail /></ProtectedRoute>} />
          <Route path="/calendar" element={<ProtectedRoute><CalendarPage /></ProtectedRoute>} />
          <Route path="/history" element={<ProtectedRoute><HistoryRouter /></ProtectedRoute>} />
          <Route path="/stats" element={<ProtectedRoute><StatsPage /></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
          <Route path="/workers" element={<ProtectedRoute><WorkersPage /></ProtectedRoute>} />
          <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
          <Route path="/admin/user/:userId" element={<AdminRoute><AdminUserDetail /></AdminRoute>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
      </AppDataProvider>
    </AuthProvider>
  </LanguageProvider>
);

export default App;

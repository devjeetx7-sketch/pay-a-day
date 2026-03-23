import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { LanguageProvider } from "@/contexts/LanguageContext";
import ProtectedRoute from "@/components/ProtectedRoute";
import AdminRoute from "@/components/AdminRoute";
import Login from "@/pages/Login";
import WebProtectedRoute from "@/components/WebProtectedRoute";
import MobileAppRoute from "@/mobile/MobileAppRoute";
import RoleSelection from "@/pages/RoleSelection";
import Dashboard from "@/pages/Dashboard";
import { WorkerDetail } from "@/pages/WorkerDetail";
import { PersonalPassbook } from "@/pages/PersonalPassbook";
import CalendarPage from "@/pages/CalendarPage";
import History from "@/pages/History";
import StatsPage from "@/pages/StatsPage";
import SettingsPage from "@/pages/SettingsPage";
import PremiumPage from "@/pages/PremiumPage";
import WorkersPage from "@/pages/WorkersPage";
import AdminDashboard from "@/pages/admin/AdminDashboard";
import AdminUserDetail from "@/pages/admin/AdminUserDetail";
import NotFound from "@/pages/NotFound";
import AppLayout from "@/components/layout/AppLayout";

const App = () => (
  <LanguageProvider>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/select-role" element={<RoleSelection />} />

          {/* Web Routes */}
          <Route path="/" element={<WebProtectedRoute><AppLayout><Dashboard /></AppLayout></WebProtectedRoute>} />
          <Route path="/worker/:id" element={<WebProtectedRoute><AppLayout><WorkerDetail /></AppLayout></WebProtectedRoute>} />
          <Route path="/passbook" element={<WebProtectedRoute><AppLayout><PersonalPassbook /></AppLayout></WebProtectedRoute>} />
          <Route path="/calendar" element={<WebProtectedRoute><AppLayout><CalendarPage /></AppLayout></WebProtectedRoute>} />
          <Route path="/history" element={<WebProtectedRoute><AppLayout><History /></AppLayout></WebProtectedRoute>} />
          <Route path="/stats" element={<WebProtectedRoute><AppLayout><StatsPage /></AppLayout></WebProtectedRoute>} />
          <Route path="/settings" element={<WebProtectedRoute><AppLayout><SettingsPage /></AppLayout></WebProtectedRoute>} />
          <Route path="/workers" element={<WebProtectedRoute><AppLayout><WorkersPage /></AppLayout></WebProtectedRoute>} />
          <Route path="/premium" element={<WebProtectedRoute><AppLayout><PremiumPage /></AppLayout></WebProtectedRoute>} />
          <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
          <Route path="/admin/user/:userId" element={<AdminRoute><AdminUserDetail /></AdminRoute>} />

          {/* Mobile Routes */}
          <Route path="/app/*" element={<MobileAppRoute />} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </LanguageProvider>
);

export default App;

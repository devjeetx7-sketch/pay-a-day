import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { LanguageProvider } from "@/contexts/LanguageContext";
import ProtectedRoute from "@/components/ProtectedRoute";
import AdminRoute from "@/components/AdminRoute";
import Login from "@/pages/Login";
import RoleSelection from "@/pages/RoleSelection";
import Dashboard from "@/pages/Dashboard";
import { WorkerDetail } from "@/pages/WorkerDetail";
import { PersonalPassbook } from "@/pages/PersonalPassbook";
import CalendarPage from "@/pages/CalendarPage";
import History from "@/pages/History";
import StatsPage from "@/pages/StatsPage";
import SettingsPage from "@/pages/SettingsPage";
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
          <Route path="/" element={<ProtectedRoute><AppLayout><Dashboard /></AppLayout></ProtectedRoute>} />
          <Route path="/worker/:id" element={<ProtectedRoute><AppLayout><WorkerDetail /></AppLayout></ProtectedRoute>} />
          <Route path="/passbook" element={<ProtectedRoute><AppLayout><PersonalPassbook /></AppLayout></ProtectedRoute>} />
          <Route path="/calendar" element={<ProtectedRoute><AppLayout><CalendarPage /></AppLayout></ProtectedRoute>} />
          <Route path="/history" element={<ProtectedRoute><AppLayout><History /></AppLayout></ProtectedRoute>} />
          <Route path="/stats" element={<ProtectedRoute><AppLayout><StatsPage /></AppLayout></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><AppLayout><SettingsPage /></AppLayout></ProtectedRoute>} />
          <Route path="/workers" element={<ProtectedRoute><AppLayout><WorkersPage /></AppLayout></ProtectedRoute>} />
          <Route path="/premium" element={<ProtectedRoute><AppLayout><PremiumPage /></AppLayout></ProtectedRoute>} />
          <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
          <Route path="/admin/user/:userId" element={<AdminRoute><AdminUserDetail /></AdminRoute>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </LanguageProvider>
);

export default App;

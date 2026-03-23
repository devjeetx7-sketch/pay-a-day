import { Routes, Route, Navigate } from "react-router-dom";
import MobileProtectedRoute from "./components/MobileProtectedRoute";
import MobileRoleSelection from "./pages/MobileRoleSelection";
import MobileDashboard from "./pages/MobileDashboard";
import { WorkerDetail } from "@/pages/WorkerDetail";
import { PersonalPassbook } from "@/pages/PersonalPassbook";
import CalendarPage from "@/pages/CalendarPage";
import History from "@/pages/History";
import StatsPage from "@/pages/StatsPage";
import SettingsPage from "@/pages/SettingsPage";
import WorkersPage from "@/pages/WorkersPage";
import PremiumPage from "@/pages/PremiumPage";

const MobileAppRoute = () => {
  return (
    <Routes>
      <Route path="select-role" element={<MobileRoleSelection />} />
      <Route element={<MobileProtectedRoute />}>
        <Route path="worker/*" element={
          <Routes>
            <Route path="dashboard" element={<MobileDashboard />} />
            <Route path="passbook" element={<PersonalPassbook />} />
            <Route path="calendar" element={<CalendarPage />} />
            <Route path="history" element={<History />} />
            <Route path="stats" element={<StatsPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="premium" element={<PremiumPage />} />
            <Route path="*" element={<Navigate to="dashboard" replace />} />
          </Routes>
        } />
        <Route path="contractor/*" element={
          <Routes>
            <Route path="dashboard" element={<MobileDashboard />} />
            <Route path="worker/:id" element={<WorkerDetail />} />
            <Route path="calendar" element={<CalendarPage />} />
            <Route path="history" element={<History />} />
            <Route path="stats" element={<StatsPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="workers" element={<WorkersPage />} />
            <Route path="premium" element={<PremiumPage />} />
            <Route path="*" element={<Navigate to="dashboard" replace />} />
          </Routes>
        } />
        <Route path="*" element={<MobileEntry />} />
      </Route>
    </Routes>
  );
};

import { useEffect, useState } from "react";
import { Preferences } from "@capacitor/preferences";

const MobileEntry = () => {
  const [role, setRole] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      const { value } = await Preferences.get({ key: "mobile_role" });
      setRole(value);
      setLoading(false);
    };
    init();
  }, []);

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-background"><div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" /></div>;

  if (!role) {
    return <Navigate to="/app/select-role" replace />;
  }

  if (role === "contractor") {
    return <Navigate to="/app/contractor/dashboard" replace />;
  } else {
    // Labour / Helper / Mistry -> Worker
    return <Navigate to="/app/worker/dashboard" replace />;
  }
};

export default MobileAppRoute;

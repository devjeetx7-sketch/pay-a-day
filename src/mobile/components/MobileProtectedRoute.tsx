import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Capacitor } from "@capacitor/core";
import { useEffect, useState } from "react";
import { Preferences } from "@capacitor/preferences";
import AppLayout from "@/components/layout/AppLayout";

const MobileProtectedRoute = () => {
  const { user, loading } = useAuth();
  const [role, setRole] = useState<string | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const checkRole = async () => {
      if (Capacitor.isNativePlatform()) {
        const { value } = await Preferences.get({ key: "mobile_role" });
        setRole(value);
      }
      setIsReady(true);
    };
    checkRole();
  }, []);

  if (loading || !isReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  if (Capacitor.isNativePlatform() && !role) {
    return <Navigate to="/app/select-role" replace />;
  }

  return (
    <AppLayout>
      <Outlet context={{ role }} />
    </AppLayout>
  );
};

export default MobileProtectedRoute;

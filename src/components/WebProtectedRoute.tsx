import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Capacitor } from "@capacitor/core";

const WebProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading, userData } = useAuth();

  // If we are on mobile native app, redirect to mobile entry point
  if (Capacitor.isNativePlatform()) {
    return <Navigate to="/app" replace />;
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  const validRoles = ["labour", "helper", "mistry", "contractor", "personal", "admin"];
  if (userData && (!userData.role || !validRoles.includes(userData.role))) {
    return <Navigate to="/select-role" replace />;
  }

  return <>{children}</>;
};

export default WebProtectedRoute;

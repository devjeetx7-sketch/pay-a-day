import { Navigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading, userData } = useAuth();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  // If user has no role set, redirect to role selection
  const validRoles = ["labour", "helper", "mistry", "contractor", "personal", "admin"];
  if (userData && (!userData.role || !validRoles.includes(userData.role))) {
    return <Navigate to="/select-role" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;

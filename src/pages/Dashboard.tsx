import { useAuth } from "@/contexts/AuthContext";
import { ContractorHome } from "@/pages/ContractorHome";
import { PersonalDashboard } from "@/pages/PersonalDashboard";
import BottomNav from "@/components/BottomNav";
import { useDataMigration } from "@/hooks/useDataMigration";

const Dashboard = () => {
  const { userData } = useAuth();
  const currentRole = userData?.role || localStorage.getItem("workday_role");
  const { isMigrating } = useDataMigration();

  if (isMigrating) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background flex-col gap-4">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="text-sm font-bold text-muted-foreground">Migrating data...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pt-6 px-4 max-w-lg mx-auto">
      {currentRole === "contractor" ? <ContractorHome /> : <PersonalDashboard />}
      <BottomNav />
    </div>
  );
};

export default Dashboard;

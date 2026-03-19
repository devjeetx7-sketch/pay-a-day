import { useAuth } from "@/contexts/AuthContext";
import { ContractorDashboard } from "@/pages/ContractorDashboard";
import { PersonalDashboard } from "@/pages/PersonalDashboard";
import BottomNav from "@/components/BottomNav";

const Dashboard = () => {
  const { userData } = useAuth();
  const currentRole = userData?.role || localStorage.getItem("dailywork_role");

  return (
    <div className="min-h-screen bg-background pt-6 px-4 max-w-lg mx-auto pb-20">
      {currentRole === "contractor" ? <ContractorDashboard /> : <PersonalDashboard />}
      <BottomNav />
    </div>
  );
};

export default Dashboard;
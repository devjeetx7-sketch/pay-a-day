import { useAuth } from "@/contexts/AuthContext";
import { ContractorDashboard } from "@/pages/ContractorDashboard";
import { PersonalDashboard } from "@/pages/PersonalDashboard";


const Dashboard = () => {
  const { userData } = useAuth();
  const currentRole = userData?.role || localStorage.getItem("dailywork_role");

  return (
    <div className="min-h-screen bg-background pt-6 px-4 max-w-7xl mx-auto pb-20 md:pb-6">
      {currentRole === "contractor" ? <ContractorDashboard /> : <PersonalDashboard />}
    </div>
  );
};

export default Dashboard;
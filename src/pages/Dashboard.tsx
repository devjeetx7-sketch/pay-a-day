import { useAuth } from "@/contexts/AuthContext";
import { ContractorDashboard } from "@/pages/ContractorDashboard";
import { PersonalDashboard } from "@/pages/PersonalDashboard";


const Dashboard = () => {
  const { userData } = useAuth();
  const currentRole = userData?.role || localStorage.getItem("dailywork_role");

  return (
    <div className="min-h-screen bg-background pt-6 px-4 pb-20 md:pb-6 w-full">
      <div className="max-w-lg mx-auto md:max-w-3xl lg:max-w-6xl">
        {currentRole === "contractor" ? <ContractorDashboard /> : <PersonalDashboard />}
      </div>
    </div>
  );
};

export default Dashboard;
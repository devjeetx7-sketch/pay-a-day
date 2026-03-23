import { useOutletContext } from "react-router-dom";
import { ContractorDashboard } from "@/pages/ContractorDashboard";
import { PersonalDashboard } from "@/pages/PersonalDashboard";

const MobileDashboard = () => {
  const { role } = useOutletContext<{ role: string }>();

  return (
    <div className="min-h-screen bg-background pt-6 px-4 pb-20 md:pb-6 w-full">
      <div className="max-w-lg mx-auto md:max-w-3xl lg:max-w-6xl">
        {role === "contractor" ? <ContractorDashboard /> : <PersonalDashboard />}
      </div>
    </div>
  );
};

export default MobileDashboard;

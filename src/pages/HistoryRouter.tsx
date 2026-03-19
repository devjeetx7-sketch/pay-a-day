import { useAuth } from "@/contexts/AuthContext";
import History from "@/pages/History";
import PersonalHistory from "@/pages/PersonalHistory";

const HistoryRouter = () => {
  const { userData } = useAuth();
  const currentRole = userData?.role || localStorage.getItem("workday_role");

  if (currentRole === "contractor") {
    return <History />;
  }

  return <PersonalHistory />;
};

export default HistoryRouter;

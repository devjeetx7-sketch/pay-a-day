import { Home, CalendarDays, Clock, BarChart3, Settings, Users, Crown } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";

const BottomNav = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const { userData } = useAuth();

  const isContractor = userData?.role === "contractor";

  const tabs = [
    { path: "/", icon: Home, label: t("dashboard") },
    { path: "/calendar", icon: CalendarDays, label: t("calendar") },
    ...(isContractor ? [{ path: "/workers", icon: Users, label: t("workers") }] : []),
    { path: "/stats", icon: BarChart3, label: t("stats") },
    { path: "/premium", icon: Crown, label: t("premium") },
    { path: "/settings", icon: Settings, label: t("settings") },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-background border-t border-border md:hidden">
      <div className="flex justify-around items-center h-16 max-w-lg mx-auto">
        {tabs.map(({ path, icon: Icon, label }) => {
          const active = location.pathname === path;
          return (
            <button
              key={path}
              onClick={() => navigate(path)}
              className={`flex flex-col items-center justify-center gap-0.5 flex-1 transition-colors ${
                active ? "text-primary" : "text-muted-foreground"
              }`}
            >
              <Icon size={20} strokeWidth={active ? 2.5 : 2} />
              <span className="text-[10px] font-medium">{label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

export default BottomNav;

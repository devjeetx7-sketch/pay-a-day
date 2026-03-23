import { Home, CalendarDays, Clock, BarChart3, Settings, Users, Crown } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";
import { Capacitor } from "@capacitor/core";

const BottomNav = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const { userData } = useAuth();

  const isMobileApp = Capacitor.isNativePlatform();
  const isContractor = isMobileApp
    ? location.pathname.includes("/app/contractor")
    : userData?.role === "contractor";

  const basePath = isMobileApp
    ? (isContractor ? "/app/contractor" : "/app/worker")
    : "";

  const tabs = [
    { path: isMobileApp ? `${basePath}/dashboard`.replace('//', '/') : "/", icon: Home, label: t("dashboard") },
    { path: isMobileApp ? `${basePath}/calendar`.replace('//', '/') : "/calendar", icon: CalendarDays, label: t("calendar") },
    ...(isContractor ? [{ path: isMobileApp ? `${basePath}/workers`.replace('//', '/') : "/workers", icon: Users, label: t("workers") }] : []),
    { path: isMobileApp ? `${basePath}/stats`.replace('//', '/') : "/stats", icon: BarChart3, label: t("stats") },
    { path: isMobileApp ? `${basePath}/settings`.replace('//', '/') : "/settings", icon: Settings, label: t("settings") },
  ];

  // Adjust path matching for the dashboard base path case (when basePath is empty or for native apps matching exactly)
  const isMatch = (tabPath: string) => {
    if (tabPath === "/" && location.pathname !== "/") return false;
    if (tabPath.endsWith("/dashboard") && (location.pathname === basePath || location.pathname === `${basePath}/dashboard`)) return true;
    return location.pathname === tabPath;
  };

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-background border-t border-border md:hidden">
      <div className="flex justify-around items-center h-16 max-w-lg mx-auto">
        {tabs.map(({ path, icon: Icon, label }) => {
          const active = isMatch(path);
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

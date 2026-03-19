import { Home, CalendarDays, BarChart3, Settings, Users, Briefcase } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";

const Sidebar = () => {
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
    { path: "/settings", icon: Settings, label: t("settings") },
  ];

  return (
    <aside className="hidden md:flex flex-col w-64 h-screen border-r border-border bg-card fixed left-0 top-0">
      <div className="flex items-center gap-3 p-6 border-b border-border">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
          <Briefcase size={20} className="text-primary-foreground" />
        </div>
        <span className="text-xl font-bold text-foreground">{t("appName")}</span>
      </div>

      <nav className="flex-1 overflow-y-auto py-6 px-4 space-y-2">
        {tabs.map(({ path, icon: Icon, label }) => {
          const active = location.pathname === path;
          return (
            <button
              key={path}
              onClick={() => navigate(path)}
              className={`flex items-center gap-3 w-full px-4 py-3 rounded-xl transition-all ${
                active
                  ? "bg-primary text-primary-foreground font-bold shadow-sm"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground font-medium"
              }`}
            >
              <Icon size={20} strokeWidth={active ? 2.5 : 2} />
              <span className="text-sm">{label}</span>
            </button>
          );
        })}
      </nav>

      {userData && (
        <div className="p-4 border-t border-border">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
              {userData.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-bold text-foreground truncate">{userData.name}</p>
              <p className="text-xs text-muted-foreground truncate">{userData.email}</p>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
};

export default Sidebar;

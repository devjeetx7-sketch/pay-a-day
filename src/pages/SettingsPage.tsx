import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { LogOut, Globe, Wallet } from "lucide-react";
import BottomNav from "@/components/BottomNav";

const SettingsPage = () => {
  const { user, userData, logout } = useAuth();
  const { t, lang, setLang, languages } = useLanguage();
  const [wage, setWage] = useState(String(userData?.daily_wage || 500));
  const [saved, setSaved] = useState(false);

  const saveWage = async () => {
    if (!user) return;
    const val = parseInt(wage, 10);
    if (isNaN(val) || val <= 0) return;
    await updateDoc(doc(db, "users", user.uid), { daily_wage: val });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const handleLangChange = async (newLang: string) => {
    setLang(newLang);
    if (user) {
      await updateDoc(doc(db, "users", user.uid), { language: newLang });
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-6">{t("settings")}</h1>

        {/* Language */}
        <div className="rounded-2xl bg-card border border-border p-4 mb-4">
          <div className="flex items-center gap-3 mb-3">
            <Globe size={20} className="text-muted-foreground" />
            <span className="text-base font-bold text-foreground">{t("language")}</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {Object.entries(languages).map(([code, name]) => (
              <button
                key={code}
                onClick={() => handleLangChange(code)}
                className={`rounded-xl px-3 py-3 text-sm font-semibold transition-all active:scale-95 touch-target ${
                  lang === code
                    ? "bg-primary text-primary-foreground"
                    : "bg-background border border-border text-foreground"
                }`}
              >
                {name}
              </button>
            ))}
          </div>
        </div>

        {/* Daily Wage */}
        <div className="rounded-2xl bg-card border border-border p-4 mb-4">
          <div className="flex items-center gap-3 mb-3">
            <Wallet size={20} className="text-muted-foreground" />
            <span className="text-base font-bold text-foreground">{t("dailyWage")}</span>
          </div>
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
              <input
                type="number"
                value={wage}
                onChange={(e) => setWage(e.target.value)}
                className="w-full rounded-xl border border-border bg-background px-8 py-3 text-lg font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <button
              onClick={saveWage}
              className="rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground active:scale-95 touch-target"
            >
              {saved ? t("saved") : t("save")}
            </button>
          </div>
        </div>

        {/* Logout */}
        <button
          onClick={logout}
          className="w-full rounded-2xl border-2 border-destructive py-4 flex items-center justify-center gap-2 text-destructive font-bold text-base active:scale-95 touch-target"
        >
          <LogOut size={20} />
          {t("logout")}
        </button>
      </div>
      <BottomNav />
    </div>
  );
};

export default SettingsPage;

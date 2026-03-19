import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { useNavigate } from "react-router-dom";
import { HardHat, Wrench, Crown, Building2 } from "lucide-react";

const roles = [
  { id: "labour", icon: HardHat, color: "text-blue-500", bg: "bg-blue-500/10" },
  { id: "helper", icon: Wrench, color: "text-green-500", bg: "bg-green-500/10" },
  { id: "mistry", icon: Crown, color: "text-purple-500", bg: "bg-purple-500/10" },
  { id: "contractor", icon: Building2, color: "text-orange-500", bg: "bg-orange-500/10" },
];

const roleLabels: Record<string, string> = {
  labour: "Labour",
  helper: "Helper",
  mistry: "Mistry",
  contractor: "Contractor (Thekedar / Maalik)",
};

const RoleSelection = () => {
  const { user, refreshUserData } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const [selected, setSelected] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    if (!user || !selected) return;
    setSaving(true);
    try {
      await updateDoc(doc(db, "users", user.uid), { role: selected });
      localStorage.setItem("workday_role", selected);
      await refreshUserData();
      navigate("/", { replace: true });
    } catch (err) {
      console.error("Error saving role:", err);
    }
    setSaving(false);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="text-xl font-bold text-foreground text-center mb-1">{t("selectRole")}</h1>
        <p className="text-sm text-muted-foreground text-center mb-6">{t("selectRoleDesc")}</p>

        <div className="space-y-3 mb-6">
          {roles.map(({ id, icon: Icon, color, bg }) => (
            <button
              key={id}
              onClick={() => setSelected(id)}
              className={`w-full rounded-2xl border-2 p-4 flex items-center gap-4 transition-all active:scale-[0.98] ${
                selected === id
                  ? "border-primary bg-primary/5"
                  : "border-border bg-card"
              }`}
            >
              <div className={`h-12 w-12 rounded-full ${bg} flex items-center justify-center`}>
                <Icon size={24} className={color} />
              </div>
              <div className="text-left">
                <p className="text-base font-bold text-foreground">{roleLabels[id]}</p>
                <p className="text-[11px] text-muted-foreground">{t(`role_${id}_desc`)}</p>
              </div>
            </button>
          ))}
        </div>

        <button
          onClick={handleSave}
          disabled={!selected || saving}
          className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-base active:scale-95 disabled:opacity-50 transition-all"
        >
          {saving ? t("saving") : t("continue")}
        </button>
      </div>
    </div>
  );
};

export default RoleSelection;

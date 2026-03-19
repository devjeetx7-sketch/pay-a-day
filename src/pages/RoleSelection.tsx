import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { useNavigate } from "react-router-dom";
import { Building2, User as UserIcon } from "lucide-react";

const roles = [
  {
    id: "contractor",
    icon: Building2,
    title: "Contractor Mode",
    desc: "Manage workers, attendance & payments",
    color: "text-orange-500",
    bg: "bg-orange-500/10"
  },
  {
    id: "personal",
    icon: UserIcon,
    title: "Personal Mode",
    desc: "Track your own daily work & earnings",
    color: "text-blue-500",
    bg: "bg-blue-500/10"
  },
];

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
        <h1 className="text-2xl font-bold text-foreground text-center mb-2">Select Your Role</h1>
        <p className="text-sm text-muted-foreground text-center mb-8">Choose how you want to use the app</p>

        <div className="space-y-4 mb-8">
          {roles.map(({ id, icon: Icon, title, desc, color, bg }) => (
            <button
              key={id}
              onClick={() => setSelected(id)}
              className={`w-full rounded-2xl border-2 p-5 flex items-start gap-4 transition-all active:scale-[0.98] ${
                selected === id
                  ? "border-primary bg-primary/5"
                  : "border-border bg-card"
              }`}
            >
              <div className={`h-14 w-14 rounded-full ${bg} flex items-center justify-center shrink-0 mt-1`}>
                <Icon size={28} className={color} />
              </div>
              <div className="text-left">
                <p className="text-lg font-bold text-foreground mb-1">{title}</p>
                <p className="text-sm text-muted-foreground leading-snug">{desc}</p>
              </div>
            </button>
          ))}
        </div>

        <button
          onClick={handleSave}
          disabled={!selected || saving}
          className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-base active:scale-95 disabled:opacity-50 transition-all shadow-lg"
        >
          {saving ? "Saving..." : "Continue"}
        </button>
      </div>
    </div>
  );
};

export default RoleSelection;

import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { useNavigate } from "react-router-dom";
import { Building2, User as UserIcon, Globe, ArrowRight, Check } from "lucide-react";

const languageEmojis: Record<string, string> = {
  en: "🇬🇧", hi: "🇮🇳", bn: "🇧🇩", pa: "🌾",
  mr: "🚩", ta: "🛕", te: "🌶️", gu: "🦁",
  kn: "🐘", ml: "🌴"
};

const roles = [
  {
    id: "labour",
    icon: UserIcon,
    emoji: "👷",
    title: "Labour",
    desc: "Daily wage worker",
    color: "text-blue-500",
    bg: "bg-blue-500/10",
    borderColor: "border-blue-500"
  },
  {
    id: "helper",
    icon: UserIcon,
    emoji: "🤝",
    title: "Helper",
    desc: "Assistant worker",
    color: "text-green-500",
    bg: "bg-green-500/10",
    borderColor: "border-green-500"
  },
  {
    id: "mistry",
    icon: UserIcon,
    emoji: "🧱",
    title: "Mistry",
    desc: "Skilled worker",
    color: "text-purple-500",
    bg: "bg-purple-500/10",
    borderColor: "border-purple-500"
  },
  {
    id: "contractor",
    icon: Building2,
    emoji: "🏗️",
    title: "Contractor Mode",
    desc: "Manage workers, attendance & payments",
    color: "text-orange-500",
    bg: "bg-orange-500/10",
    borderColor: "border-orange-500"
  }
];

const RoleSelection = () => {
  const { user, refreshUserData, userData } = useAuth();
  const { t, lang, setLang, languages } = useLanguage();
  const navigate = useNavigate();

  // Steps: 1 = Language, 2 = Role
  const [step, setStep] = useState(1);
  const [selectedLang, setSelectedLang] = useState<string | null>(null);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [animating, setAnimating] = useState(false);

  useEffect(() => {
    // If language is already explicitly set and role is what's missing, skip to step 2
    if (userData?.language && !userData?.role) {
      setStep(2);
    }
  }, [userData]);

  const handleNextStep = () => {
    if (step === 1 && selectedLang) {
      setLang(selectedLang);
      setAnimating(true);
      setTimeout(() => {
        setStep(2);
        setAnimating(false);
      }, 300);
    }
  };

  const handleSave = async () => {
    if (!user || !selectedRole) return;
    setSaving(true);
    try {
      const updateData: any = { role: selectedRole };
      if (selectedLang) {
        updateData.language = selectedLang;
      }

      await updateDoc(doc(db, "users", user.uid), updateData);
      localStorage.setItem("dailywork_role", selectedRole);

      await refreshUserData();
      navigate("/", { replace: true });
    } catch (err) {
      console.error("Error saving preferences:", err);
    }
    setSaving(false);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 relative overflow-hidden">
      {/* Decorative background blur blobs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/20 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-secondary/20 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

      <div className={`w-full max-w-md transition-all duration-500 ${animating ? 'opacity-0 scale-95' : 'opacity-100 scale-100'} relative z-10`}>

        {step === 1 ? (
          <div className="bg-card/80 backdrop-blur-md border border-border shadow-xl rounded-3xl p-8 animate-in fade-in slide-in-from-bottom-8 duration-500">
            <div className="flex justify-center mb-6">
              <div className="h-20 w-20 bg-primary/10 rounded-full flex items-center justify-center animate-bounce">
                 <Globe size={40} className="text-primary" />
              </div>
            </div>
            <h1 className="text-3xl font-black text-foreground text-center mb-2">{t("selectLanguage")}</h1>
            <p className="text-sm font-medium text-muted-foreground text-center mb-8">{t("languageDesc")}</p>

            <div className="space-y-4 mb-8">
              {Object.entries(languages).map(([code, title]) => (
                <button
                  key={code}
                  onClick={() => setSelectedLang(code)}
                  className={`w-full rounded-2xl border-2 p-5 flex items-center justify-between transition-all active:scale-95 shadow-sm ${
                    selectedLang === code
                      ? "border-primary bg-primary/10 ring-4 ring-primary/5"
                      : "border-border bg-background hover:border-primary/30"
                  }`}
                >
                  <div className="flex items-center gap-4">
                    <span className="text-4xl">{languageEmojis[code] || '🌐'}</span>
                    <span className="text-lg font-bold text-foreground">{title}</span>
                  </div>
                  {selectedLang === code && (
                    <div className="h-8 w-8 rounded-full bg-primary flex items-center justify-center animate-in zoom-in">
                      <Check size={16} className="text-white" />
                    </div>
                  )}
                </button>
              ))}
            </div>

            <button
              onClick={handleNextStep}
              disabled={!selectedLang}
              className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-lg active:scale-95 disabled:opacity-50 transition-all shadow-lg shadow-primary/30 flex items-center justify-center gap-2"
            >
              Continue <ArrowRight size={20} />
            </button>
          </div>
        ) : (
          <div className="bg-card/80 backdrop-blur-md border border-border shadow-xl rounded-3xl p-6 sm:p-8 animate-in fade-in slide-in-from-bottom-8 duration-500">
            <h1 className="text-3xl font-black text-foreground text-center mb-2">{t("selectRole")}</h1>
            <p className="text-sm font-medium text-muted-foreground text-center mb-8">{t("selectRoleDesc")}</p>

            <div className="space-y-4 mb-8">
              {roles.map(({ id, emoji, title, desc, color, bg, borderColor }) => (
                <button
                  key={id}
                  onClick={() => setSelectedRole(id)}
                  className={`w-full rounded-2xl border-2 p-5 flex items-start gap-4 transition-all active:scale-[0.98] shadow-sm relative overflow-hidden group ${
                    selectedRole === id
                      ? `${borderColor} ${bg} ring-4 ring-primary/5`
                      : "border-border bg-background hover:border-border/80"
                  }`}
                >
                  {/* Subtle highlight effect on selection */}
                  {selectedRole === id && <div className="absolute inset-0 bg-white/5 opacity-50 pointer-events-none"></div>}

                  <div className="text-4xl shrink-0 mt-1 transform group-hover:scale-110 group-hover:rotate-3 transition-transform">
                    {emoji}
                  </div>
                  <div className="text-left flex-1 relative z-10">
                    <p className={`text-lg font-bold mb-1 ${selectedRole === id ? color : 'text-foreground'}`}>{title}</p>
                    <p className="text-xs font-medium text-muted-foreground leading-snug">{desc}</p>
                  </div>
                  {selectedRole === id && (
                    <div className={`absolute top-4 right-4 h-6 w-6 rounded-full ${bg.replace('/10', '')} flex items-center justify-center animate-in zoom-in`}>
                      <Check size={14} className="text-white" />
                    </div>
                  )}
                </button>
              ))}
            </div>

            <button
              onClick={handleSave}
              disabled={!selectedRole || saving}
              className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-lg active:scale-95 disabled:opacity-50 transition-all shadow-lg shadow-primary/30"
            >
              {saving ? "Setting up your workspace..." : "Get Started"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default RoleSelection;

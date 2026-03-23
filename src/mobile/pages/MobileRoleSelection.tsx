import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { useNavigate } from "react-router-dom";
import { Building2, Globe, ArrowRight, Check } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Hammer, Handshake, BrickWall } from "lucide-react";
import { Preferences } from "@capacitor/preferences";

const languageIcons: Record<string, React.FC<any>> = {
  en: () => <span className="text-3xl">Aa</span>,
  hi: () => <span className="text-3xl">अ</span>,
  bn: () => <span className="text-3xl">অ</span>,
  pa: () => <span className="text-3xl">ਅ</span>,
  mr: () => <span className="text-3xl">अ</span>,
  ta: () => <span className="text-3xl">அ</span>,
  te: () => <span className="text-3xl">అ</span>,
  gu: () => <span className="text-3xl">અ</span>,
  kn: () => <span className="text-3xl">ಅ</span>,
  ml: () => <span className="text-3xl">അ</span>
};

const AnimatedHammer = () => (
  <motion.div animate={{ rotate: [0, -20, 0] }} transition={{ repeat: Infinity, duration: 1 }}>
    <Hammer size={32} className="text-blue-500" />
  </motion.div>
);
const AnimatedHandshake = () => (
  <motion.div animate={{ scale: [1, 1.1, 1] }} transition={{ repeat: Infinity, duration: 1.5 }}>
    <Handshake size={32} className="text-green-500" />
  </motion.div>
);
const AnimatedBrick = () => (
  <motion.div animate={{ y: [0, -5, 0] }} transition={{ repeat: Infinity, duration: 2 }}>
    <BrickWall size={32} className="text-purple-500" />
  </motion.div>
);
const AnimatedBuilding = () => (
  <motion.div animate={{ opacity: [0.8, 1, 0.8] }} transition={{ repeat: Infinity, duration: 2 }}>
    <Building2 size={32} className="text-orange-500" />
  </motion.div>
);

const roles = [
  {
    id: "labour",
    animatedIcon: AnimatedHammer,
    titleKey: "role_labour",
    descKey: "role_labour_desc",
    color: "text-blue-500",
  },
  {
    id: "helper",
    animatedIcon: AnimatedHandshake,
    titleKey: "role_helper",
    descKey: "role_helper_desc",
    color: "text-green-500",
  },
  {
    id: "mistry",
    animatedIcon: AnimatedBrick,
    titleKey: "role_mistry",
    descKey: "role_mistry_desc",
    color: "text-purple-500",
  },
  {
    id: "contractor",
    animatedIcon: AnimatedBuilding,
    titleKey: "role_contractor",
    descKey: "role_contractor_desc",
    color: "text-orange-500",
  }
];

const MobileRoleSelection = () => {
  const { user } = useAuth();
  const { t, setLang, languages } = useLanguage();
  const navigate = useNavigate();

  const [step, setStep] = useState(1);
  const [selectedLang, setSelectedLang] = useState<string | null>(null);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const langSet = localStorage.getItem("langSet");
    if (langSet === "true") {
      setStep(2);
    }
  }, []);

  const handleNextStep = () => {
    if (step === 1 && selectedLang) {
      setLang(selectedLang);
      localStorage.setItem("langSet", "true");
      setStep(2);
    }
  };

  const handleSave = async () => {
    if (!user || !selectedRole) return;
    setSaving(true);
    try {
      // Map all worker types to "worker", but keep "contractor" as "contractor"
      const mappedRole = selectedRole === "contractor" ? "contractor" : "worker";

      await Preferences.set({ key: "mobile_role", value: mappedRole });

      if (mappedRole === "contractor") {
        navigate("/app/contractor/dashboard", { replace: true });
      } else {
        navigate("/app/worker/dashboard", { replace: true });
      }
    } catch (err) {
      console.error("Error saving preferences:", err);
    }
    setSaving(false);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 relative overflow-hidden">
      <AnimatePresence mode="wait">
        {step === 1 ? (
          <motion.div
            key="step1"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            transition={{ duration: 0.3 }}
            className="w-full max-w-md bg-card border-none sm:border sm:border-border shadow-none rounded-3xl p-4 sm:p-8 relative z-10"
          >
            <div className="flex justify-center mb-6">
              <motion.div
                animate={{ rotate: [0, 10, -10, 0] }}
                transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                className="h-20 w-20 bg-primary/10 rounded-full flex items-center justify-center"
              >
                 <Globe size={40} className="text-primary" />
              </motion.div>
            </div>
            <h1 className="text-3xl font-black text-foreground text-center mb-2">{t("selectLanguage")}</h1>
            <p className="text-sm font-medium text-muted-foreground text-center mb-8">{t("languageDesc")}</p>

            <div className="space-y-4 mb-8">
              <div className="grid grid-cols-2 gap-3">
                {Object.entries(languages).map(([code, title]) => {
                  const IconComponent = languageIcons[code] || languageIcons['en'];
                  return (
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      key={code}
                      onClick={() => setSelectedLang(code)}
                      className={`relative rounded-2xl border-2 p-4 flex flex-col items-center justify-center gap-2 transition-colors ${
                        selectedLang === code
                          ? "border-primary bg-primary/5"
                          : "border-border bg-transparent hover:border-primary/30"
                      }`}
                    >
                      <div className={`text-foreground/80 ${selectedLang === code ? 'text-primary' : ''}`}>
                        <IconComponent />
                      </div>
                      <span className={`text-sm font-bold ${selectedLang === code ? 'text-primary' : 'text-foreground'}`}>
                        {title as string}
                      </span>
                      {selectedLang === code && (
                        <motion.div
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          className="absolute top-2 right-2 h-5 w-5 rounded-full bg-primary flex items-center justify-center"
                        >
                          <Check size={12} className="text-white" />
                        </motion.div>
                      )}
                    </motion.button>
                  )
                })}
              </div>
            </div>

            <motion.button
              whileTap={selectedLang ? { scale: 0.95 } : {}}
              onClick={handleNextStep}
              disabled={!selectedLang}
              className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-lg disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {t("continue")} <ArrowRight size={20} />
            </motion.button>
          </motion.div>
        ) : (
          <motion.div
            key="step2"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.3 }}
            className="w-full max-w-md bg-card border-none sm:border sm:border-border shadow-none rounded-3xl p-4 sm:p-8 relative z-10"
          >
            <h1 className="text-3xl font-black text-foreground text-center mb-2">{t("selectRole")}</h1>
            <p className="text-sm font-medium text-muted-foreground text-center mb-8">{t("selectRoleDesc")}</p>

            <div className="space-y-4 mb-8">
              {roles.map(({ id, animatedIcon: AnimatedIcon, titleKey, descKey, color }) => (
                <motion.button
                  whileTap={{ scale: 0.98 }}
                  key={id}
                  onClick={() => setSelectedRole(id)}
                  className={`w-full rounded-2xl border-2 p-4 flex items-center gap-4 transition-colors ${
                    selectedRole === id
                      ? "border-primary bg-primary/5"
                      : "border-border bg-transparent hover:border-primary/30"
                  }`}
                >
                  <div className="shrink-0 flex items-center justify-center w-12 h-12">
                    <AnimatedIcon />
                  </div>
                  <div className="text-left flex-1">
                    <p className={`text-base font-bold mb-0.5 ${selectedRole === id ? color : 'text-foreground'}`}>{t(titleKey)}</p>
                    <p className="text-xs font-medium text-muted-foreground">{t(descKey)}</p>
                  </div>
                  {selectedRole === id && (
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      className="h-6 w-6 rounded-full bg-primary flex items-center justify-center shrink-0"
                    >
                      <Check size={14} className="text-white" />
                    </motion.div>
                  )}
                </motion.button>
              ))}
            </div>

            <motion.button
              whileTap={(!selectedRole || saving) ? {} : { scale: 0.95 }}
              onClick={handleSave}
              disabled={!selectedRole || saving}
              className="w-full rounded-2xl bg-primary py-4 text-primary-foreground font-bold text-lg disabled:opacity-50 transition-all flex items-center justify-center"
            >
              {saving ? t("settingUp") : t("getStarted")}
            </motion.button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default MobileRoleSelection;

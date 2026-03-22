import { useState } from "react";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";
import { Check, X, Sparkles, Crown, Zap, Shield, BarChart3, FileText, Users, CloudUpload, ArrowRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const PremiumPage = () => {
  const { t } = useLanguage();
  const { userData } = useAuth();

  const currentRole = userData?.role || "labour";
  const isContractor = currentRole === "contractor";

  const basePrice = isContractor ? 99 : 49;
  const comboPrice = 399;

  // Plan Durations
  const plans = [
    { id: "monthly", label: "Monthly", price: basePrice, multiplier: 1, tag: null },
    { id: "half-yearly", label: "6 Months", price: basePrice * 5, multiplier: 5, tag: "Save 16%" },
    { id: "yearly", label: "Yearly", price: basePrice * 10, multiplier: 10, tag: "Save 16%" },
    { id: "lifetime", label: "Lifetime", price: basePrice * 20, multiplier: 20, tag: "Best Value" },
  ];

  const [selectedPlanId, setSelectedPlanId] = useState("lifetime");
  const selectedPlan = plans.find(p => p.id === selectedPlanId) || plans[3];


  const freeFeatures = [
    { key: "pf_attendance", included: true },
    { key: "pf_calendar", included: true },
    { key: "pf_basicStats", included: true },
    { key: "pf_singleRole", included: true },
    { key: "pf_advanceTrack", included: true },
    { key: "pf_unlimitedWorkers", included: false },
    { key: "pf_pdfExport", included: false },
    { key: "pf_advancedAnalytics", included: false },
    { key: "pf_cloudBackup", included: false },
    { key: "pf_whatsappShare", included: false },
    { key: "pf_prioritySupport", included: false },
  ];

  const premiumFeatures = [
    { key: "pf_attendance", included: true },
    { key: "pf_calendar", included: true },
    { key: "pf_basicStats", included: true },
    { key: "pf_singleRole", included: true },
    { key: "pf_advanceTrack", included: true },
    { key: "pf_unlimitedWorkers", included: true },
    { key: "pf_pdfExport", included: true },
    { key: "pf_advancedAnalytics", included: true },
    { key: "pf_cloudBackup", included: true },
    { key: "pf_whatsappShare", included: true },
    { key: "pf_prioritySupport", included: true },
  ];

  const highlights = [
    { icon: Users, key: "ph_workers" },
    { icon: FileText, key: "ph_export" },
    { icon: BarChart3, key: "ph_analytics" },
    { icon: CloudUpload, key: "ph_backup" },
  ];

  return (
    <div className="min-h-screen bg-background pb-20 md:pb-6">
      <div className="mx-auto max-w-lg md:max-w-3xl lg:max-w-5xl px-4 pt-6">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-2">
            <Crown size={28} className="text-amber-500" />
            <h1 className="text-2xl font-bold text-foreground">{t("premiumTitle")}</h1>
          </div>
          <p className="text-sm text-muted-foreground font-medium">{t("premiumSubtitle")}</p>
        </div>

        {/* Plan Selector */}
        <div className="max-w-2xl mx-auto mb-10">
          <div className="text-center mb-6">
            <h2 className="text-lg font-bold text-foreground">
              {isContractor ? t("contractorPremium") : t("workerPremium")}
            </h2>
            <p className="text-sm text-muted-foreground">Select your billing cycle</p>
          </div>

          <div className="grid grid-cols-2 gap-3 mb-6">
            {plans.map((plan) => (
              <button
                key={plan.id}
                onClick={() => setSelectedPlanId(plan.id)}
                className={`relative rounded-2xl p-4 border-2 text-left transition-all ${
                  selectedPlanId === plan.id
                    ? "border-primary bg-primary/5 ring-2 ring-primary/20"
                    : "border-border bg-card hover:border-primary/30"
                }`}
              >
                {plan.tag && (
                  <div className={`absolute -top-2.5 right-3 text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    plan.tag === "Best Value"
                      ? "bg-amber-500 text-white"
                      : "bg-green-500 text-white"
                  }`}>
                    {plan.tag}
                  </div>
                )}
                <div className="flex items-center justify-between mb-1">
                  <span className={`text-sm font-bold ${selectedPlanId === plan.id ? 'text-primary' : 'text-foreground'}`}>
                    {plan.label}
                  </span>
                  {selectedPlanId === plan.id && (
                    <motion.div initial={{scale:0}} animate={{scale:1}}>
                      <Check size={16} className="text-primary" />
                    </motion.div>
                  )}
                </div>
                <div className="flex items-baseline gap-1">
                  <span className="text-xl font-black text-foreground">₹{plan.price}</span>
                  {plan.id !== "lifetime" && <span className="text-[10px] text-muted-foreground font-medium">/{plan.label.toLowerCase()}</span>}
                </div>
              </button>
            ))}
          </div>

          <div className="rounded-2xl border-2 border-primary bg-card p-6  relative overflow-hidden flex flex-col items-center text-center">
            <h3 className="text-lg font-bold text-foreground mb-2">
              Get {selectedPlan.label} Plan
            </h3>
            <p className="text-sm text-muted-foreground mb-6 max-w-sm">
              {isContractor
                ? t("contractorPremiumDesc")
                : t("workerPremiumDesc")}
            </p>
            <button className="w-full py-4 bg-primary text-primary-foreground text-sm font-bold rounded-xl transition-all  active:scale-95 flex items-center justify-center gap-2">
              <Zap size={18} />
              Upgrade for ₹{selectedPlan.price}
            </button>
          </div>
        </div>

        {/* Combo Divider */}
        <div className="flex items-center justify-center gap-4 mb-10 max-w-2xl mx-auto">
          <div className="h-px flex-1 bg-border"></div>
          <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">OR</span>
          <div className="h-px flex-1 bg-border"></div>
        </div>

        {/* Combo Plan */}
        <div className="max-w-2xl mx-auto mb-12">
          <div className="rounded-2xl border-2 border-amber-200 bg-amber-50/50 dark:bg-amber-950/10 p-1">
            <div className="rounded-xl bg-card p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-[10px] font-bold px-3 py-1 rounded-bl-xl uppercase tracking-wider flex items-center gap-1">
                <Crown size={12} /> Unlock Everything
              </div>
              <h3 className="text-lg font-bold text-amber-600 dark:text-amber-500 mb-2">
                {t("comboPremium")}
              </h3>
              <div className="flex items-baseline gap-1 mb-4">
                <span className="text-4xl font-black text-amber-600 dark:text-amber-500">₹{comboPrice}</span>
                <span className="text-sm text-amber-600/70 font-medium">/ {t("lifetime")}</span>
              </div>
              <p className="text-sm text-muted-foreground mb-6">
                {t("comboPremiumDesc")} Unlocks both Contractor & Personal Modes permanently.
              </p>
              <button className="w-full py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-sm font-bold rounded-xl transition-all  active:scale-95 flex items-center justify-center gap-2">
                <Sparkles size={18} />
                {t("getCombo")}
              </button>
            </div>
          </div>
        </div>

        {/* Highlight Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
          {highlights.map(({ icon: Icon, key }) => (
            <div key={key} className="rounded-xl bg-card border border-border p-4 text-center ">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-2">
                <Icon size={20} className="text-primary" />
              </div>
              <p className="text-xs font-bold text-foreground">{t(key)}</p>
            </div>
          ))}
        </div>

        {/* Comparison Table */}
        <div className="rounded-2xl bg-card border border-border  overflow-hidden mb-8">
          <div className="grid grid-cols-3 bg-muted/50 border-b border-border">
            <div className="p-4 text-xs font-bold text-muted-foreground uppercase tracking-wider">
              {t("features")}
            </div>
            <div className="p-4 text-center text-xs font-bold text-muted-foreground uppercase tracking-wider border-l border-border">
              {t("freePlan")}
            </div>
            <div className="p-4 text-center text-xs font-bold text-amber-600 uppercase tracking-wider border-l border-border flex items-center justify-center gap-1">
              <Sparkles size={12} /> {t("premiumPlan")}
            </div>
          </div>

          {freeFeatures.map((feat, i) => (
            <div key={feat.key} className={`grid grid-cols-3 ${i < freeFeatures.length - 1 ? "border-b border-border" : ""}`}>
              <div className="p-3.5 text-xs font-medium text-foreground flex items-center">
                {t(feat.key)}
              </div>
              <div className="p-3.5 flex items-center justify-center border-l border-border">
                {feat.included ? (
                  <Check size={16} className="text-green-500" />
                ) : (
                  <X size={16} className="text-muted-foreground/40" />
                )}
              </div>
              <div className="p-3.5 flex items-center justify-center border-l border-border">
                {premiumFeatures[i].included ? (
                  <Check size={16} className="text-amber-500" />
                ) : (
                  <X size={16} className="text-muted-foreground/40" />
                )}
              </div>
            </div>
          ))}
        </div>

        {/* CTA (Removed old duplicate) */}
        <div className="text-center mb-8">
          <p className="text-[11px] text-muted-foreground mt-3 font-medium">{t("cancelAnytime")}</p>
        </div>

        {/* Trust Badges */}
        <div className="flex items-center justify-center gap-6 pb-8">
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Shield size={14} />
            <span className="text-[11px] font-medium">{t("securePayment")}</span>
          </div>
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Zap size={14} />
            <span className="text-[11px] font-medium">{t("instantAccess")}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PremiumPage;

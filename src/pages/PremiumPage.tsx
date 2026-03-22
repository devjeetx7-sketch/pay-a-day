import { useState } from "react";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";
import { Check, X, Sparkles, Crown, Zap, Shield, BarChart3, FileText, Users, CloudUpload } from "lucide-react";

const PremiumPage = () => {
  const { t } = useLanguage();
  const { userData } = useAuth();

  const currentRole = userData?.role || "labour";
  const isContractor = currentRole === "contractor";

  const planPrice = isContractor ? 99 : 49;
  const comboPrice = 399;

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

        {/* Pricing Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8 max-w-2xl mx-auto">
          {/* Individual Plan */}
          <div className="rounded-2xl border-2 border-primary bg-card p-6 shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 bg-primary text-primary-foreground text-[10px] font-bold px-3 py-1 rounded-bl-xl uppercase tracking-wider">
              {t("yourPlan")}
            </div>
            <h3 className="text-lg font-bold text-foreground mb-2">
              {isContractor ? t("contractorPremium") : t("workerPremium")}
            </h3>
            <div className="flex items-baseline gap-1 mb-4">
              <span className="text-4xl font-black text-foreground">₹{planPrice}</span>
              <span className="text-sm text-muted-foreground font-medium">/ {t("lifetime")}</span>
            </div>
            <p className="text-sm text-muted-foreground mb-6 h-10">
              {isContractor
                ? t("contractorPremiumDesc")
                : t("workerPremiumDesc")}
            </p>
            <button className="w-full py-3 bg-primary text-primary-foreground text-sm font-bold rounded-xl transition-all shadow-sm hover:shadow-md active:scale-95 flex items-center justify-center gap-2">
              <Zap size={18} />
              {t("upgradeNow")}
            </button>
          </div>

          {/* Combo Plan */}
          <div className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-950/20 dark:to-orange-950/20 p-6 shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-[10px] font-bold px-3 py-1 rounded-bl-xl uppercase tracking-wider flex items-center gap-1">
              <Crown size={12} /> {t("bestValue")}
            </div>
            <h3 className="text-lg font-bold text-amber-700 dark:text-amber-500 mb-2">
              {t("comboPremium")}
            </h3>
            <div className="flex items-baseline gap-1 mb-4">
              <span className="text-4xl font-black text-amber-700 dark:text-amber-500">₹{comboPrice}</span>
              <span className="text-sm text-amber-600/70 dark:text-amber-500/70 font-medium">/ {t("lifetime")}</span>
            </div>
            <p className="text-sm text-amber-700/80 dark:text-amber-500/80 mb-6 h-10">
              {t("comboPremiumDesc")}
            </p>
            <button className="w-full py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-sm font-bold rounded-xl transition-all shadow-sm hover:shadow-md active:scale-95 flex items-center justify-center gap-2">
              <Sparkles size={18} />
              {t("getCombo")}
            </button>
          </div>
        </div>

        {/* Highlight Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
          {highlights.map(({ icon: Icon, key }) => (
            <div key={key} className="rounded-xl bg-card border border-border p-4 text-center shadow-sm">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-2">
                <Icon size={20} className="text-primary" />
              </div>
              <p className="text-xs font-bold text-foreground">{t(key)}</p>
            </div>
          ))}
        </div>

        {/* Comparison Table */}
        <div className="rounded-2xl bg-card border border-border shadow-sm overflow-hidden mb-8">
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

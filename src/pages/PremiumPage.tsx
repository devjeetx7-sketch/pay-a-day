import { useState } from "react";
import { useLanguage } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";
import { Check, X, Sparkles, Crown, Zap, Shield, BarChart3, FileText, Users, CloudUpload } from "lucide-react";

type BillingCycle = "monthly" | "halfYearly" | "yearly";

const pricing: Record<BillingCycle, { price: number; original: number; perMonth: number; save: number }> = {
  monthly: { price: 99, original: 99, perMonth: 99, save: 0 },
  halfYearly: { price: 499, original: 594, perMonth: 83, save: 16 },
  yearly: { price: 899, original: 1188, perMonth: 75, save: 24 },
};

const PremiumPage = () => {
  const { t } = useLanguage();
  const { userData } = useAuth();
  const [cycle, setCycle] = useState<BillingCycle>("yearly");

  const plan = pricing[cycle];

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

        {/* Billing Cycle Selector */}
        <div className="flex items-center justify-center mb-8">
          <div className="inline-flex bg-muted rounded-xl p-1 gap-1">
            {(["monthly", "halfYearly", "yearly"] as BillingCycle[]).map((c) => (
              <button
                key={c}
                onClick={() => setCycle(c)}
                className={`px-4 py-2.5 rounded-lg text-xs font-bold transition-all ${
                  cycle === c
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {t(`billing_${c}`)}
                {c === "yearly" && (
                  <span className="ml-1.5 bg-green-500 text-white text-[9px] px-1.5 py-0.5 rounded-full font-bold">
                    {t("bestValue")}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Price Display */}
        <div className="text-center mb-8">
          <div className="flex items-baseline justify-center gap-2">
            {plan.save > 0 && (
              <span className="text-lg text-muted-foreground line-through font-medium">₹{plan.original}</span>
            )}
            <span className="text-5xl font-black text-foreground">₹{plan.price}</span>
          </div>
          <p className="text-sm text-muted-foreground font-medium mt-1">
            {cycle !== "monthly" && `₹${plan.perMonth}/${t("perMonth")}`}
            {plan.save > 0 && (
              <span className="ml-2 text-green-600 font-bold">{t("save")} {plan.save}%</span>
            )}
          </p>
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

        {/* CTA */}
        <div className="text-center mb-8">
          <button className="w-full max-w-md mx-auto py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-base font-bold rounded-xl transition-all shadow-lg hover:shadow-xl active:scale-95 flex items-center justify-center gap-2">
            <Zap size={20} />
            {t("upgradePremium")} — ₹{plan.price}/{t(`billing_${cycle}`)}
          </button>
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

import { useState } from "react";
import { Dialog, DialogContent, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Sparkles, CheckCircle2, Zap } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { useLanguage } from "@/contexts/LanguageContext";

interface PremiumModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const PremiumModal = ({ open, onOpenChange }: PremiumModalProps) => {
  const { userData } = useAuth();
  const navigate = useNavigate();
  const { t } = useLanguage();

  // Role-based pricing
  // Labour / Helper / Mistry -> ₹49
  // Contractor -> ₹99
  // Combo -> ₹399

  const role = userData?.role || 'personal';
  const roleName = role === 'contractor' ? t("contractorPremium") : t("workerPremium");
  const basePrice = role === 'contractor' ? 99 : 49;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md mx-auto p-0 overflow-hidden bg-card border-border rounded-3xl">
        <div className="bg-gradient-to-br from-amber-500/20 via-orange-500/10 to-red-500/20 p-6 pb-8 text-center relative overflow-hidden">
          <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[150%] bg-gradient-to-br from-amber-500/10 to-transparent rounded-full blur-3xl opacity-50 pointer-events-none transform -rotate-45"></div>
          <div className="flex justify-center mb-4 relative z-10">
            <div className="h-16 w-16 bg-white dark:bg-card rounded-2xl shadow-xl flex items-center justify-center transform rotate-12">
               <Sparkles size={32} className="text-amber-500" fill="currentColor" />
            </div>
          </div>
          <DialogTitle className="text-2xl font-black text-foreground relative z-10">{t("upgradePremium")}</DialogTitle>
          <DialogDescription className="text-sm font-medium text-muted-foreground mt-2 relative z-10">
            {t("unlockAdvancedFeatures")}
          </DialogDescription>
        </div>

        <div className="p-6 space-y-4 -mt-4 relative z-10">
          {/* Individual Plan */}
          <div className="bg-background border-2 border-border/50 rounded-2xl p-5 shadow-sm hover:border-primary/30 transition-all relative overflow-hidden group">
            <div className="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div className="flex justify-between items-start mb-3">
              <div>
                <p className="text-xs font-bold text-primary mb-1 uppercase tracking-wider">{roleName}</p>
                <div className="flex items-baseline gap-1">
                  <span className="text-2xl font-black text-foreground">₹{basePrice}</span>
                  <span className="text-xs text-muted-foreground font-medium">{t("perMonthShort")}</span>
                </div>
              </div>
            </div>
            <ul className="text-xs text-muted-foreground space-y-2.5 font-medium mt-4">
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> {t("advancedTrackingTools")}</li>
              {role === 'contractor' ? (
                <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> {t("pf_unlimitedWorkers")}</li>
              ) : (
                <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> {t("detailedWageLogs")}</li>
              )}
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> {t("pdfExportAndBackup")}</li>
            </ul>
            <button className="w-full mt-5 py-3 bg-primary/10 hover:bg-primary/20 text-primary text-sm font-bold rounded-xl transition-all active:scale-95">
              {t("selectPlan")}
            </button>
          </div>

          {/* See Full Premium Page Link */}
          <button
             onClick={() => { onOpenChange(false); navigate("/premium"); }}
             className="w-full py-4 mt-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-sm font-bold rounded-2xl transition-all shadow-sm active:scale-95 flex items-center justify-center gap-2"
          >
            <Zap size={18} />
            View Premium Page
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

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
  const navigate = useNavigate();
  const { t } = useLanguage();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[85%] max-w-sm mx-auto p-6 bg-card border border-border rounded-2xl text-center flex flex-col items-center ">
        <div className="flex justify-center mb-4 mt-2">
          <div className="h-14 w-14 bg-amber-100 dark:bg-amber-900/30 rounded-full flex items-center justify-center">
             <Sparkles size={28} className="text-amber-500" fill="currentColor" />
          </div>
        </div>
        <DialogTitle className="text-xl font-bold text-foreground mb-2">
          {t("upgradePremium")}
        </DialogTitle>
        <DialogDescription className="text-sm text-muted-foreground mb-6">
          Unlock unlimited workers, cloud backup, and PDF exports.
        </DialogDescription>

        <button
           onClick={() => { onOpenChange(false); navigate("/premium"); }}
           className="w-full py-3.5 bg-primary text-primary-foreground text-sm font-bold rounded-xl transition-all active:scale-95 flex items-center justify-center gap-2"
        >
          <Zap size={18} />
          View Plans
        </button>

        <button
           onClick={() => onOpenChange(false)}
           className="mt-3 text-xs font-bold text-muted-foreground hover:text-foreground transition-colors"
        >
          Maybe Later
        </button>
      </DialogContent>
    </Dialog>
  );
};

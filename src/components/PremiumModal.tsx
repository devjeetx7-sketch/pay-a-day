import { useState } from "react";
import { Dialog, DialogContent, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Sparkles, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";

interface PremiumModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const PremiumModal = ({ open, onOpenChange }: PremiumModalProps) => {
  const { userData } = useAuth();

  // Role-based pricing
  // Labour / Helper / Mistry -> ₹49
  // Contractor -> ₹99
  // Combo -> ₹199

  const role = userData?.role || 'personal';
  const roleName = role === 'contractor' ? 'Contractor' : 'Worker (Labour/Helper/Mistry)';
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
          <DialogTitle className="text-2xl font-black text-foreground relative z-10">Upgrade to Premium</DialogTitle>
          <DialogDescription className="text-sm font-medium text-muted-foreground mt-2 relative z-10">
            Unlock advanced features and grow your business.
          </DialogDescription>
        </div>

        <div className="p-6 space-y-4 -mt-4 relative z-10">
          {/* Individual Plan */}
          <div className="bg-background border-2 border-border/50 rounded-2xl p-5 shadow-sm hover:border-primary/30 transition-all relative overflow-hidden group">
            <div className="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div className="flex justify-between items-start mb-3">
              <div>
                <p className="text-xs font-bold text-primary mb-1 uppercase tracking-wider">{roleName} Premium</p>
                <div className="flex items-baseline gap-1">
                  <span className="text-2xl font-black text-foreground">₹{basePrice}</span>
                  <span className="text-xs text-muted-foreground font-medium">/mo</span>
                </div>
              </div>
            </div>
            <ul className="text-xs text-muted-foreground space-y-2.5 font-medium mt-4">
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Advanced tracking tools</li>
              {role === 'contractor' ? (
                <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Unlimited Workers</li>
              ) : (
                <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Detailed wage logs</li>
              )}
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> PDF Export & Backup</li>
            </ul>
            <button className="w-full mt-5 py-3 bg-primary/10 hover:bg-primary/20 text-primary text-sm font-bold rounded-xl transition-all active:scale-95">
              Select {roleName} Plan
            </button>
          </div>

          {/* Combo Plan - Always visible */}
          <div className="bg-gradient-to-br from-indigo-500/10 to-purple-500/10 border-2 border-indigo-500/30 rounded-2xl p-5 shadow-md relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-tl from-indigo-500/20 to-transparent rounded-tl-full opacity-50"></div>
            <div className="absolute -top-3 -right-3">
              <span className="bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-[10px] font-bold px-3 py-1 rounded-bl-xl shadow-md uppercase tracking-wider">Best Value</span>
            </div>
            <div className="flex justify-between items-start mb-3">
              <div>
                <p className="text-xs font-bold text-indigo-600 dark:text-indigo-400 mb-1 uppercase tracking-wider">Combo Plan (All Roles)</p>
                <div className="flex items-baseline gap-1">
                  <span className="text-2xl font-black text-indigo-700 dark:text-indigo-300">₹199</span>
                  <span className="text-xs text-muted-foreground font-medium">/mo</span>
                </div>
              </div>
            </div>
            <p className="text-xs text-muted-foreground font-medium mb-4">Includes everything in Labour, Helper, Mistry & Contractor Premium.</p>
            <button className="w-full py-3 bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white text-sm font-bold rounded-xl transition-all shadow-md active:scale-95">
              Get Full Access
            </button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

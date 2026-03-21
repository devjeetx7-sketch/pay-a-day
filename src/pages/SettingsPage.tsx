import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { LogOut, Globe, Wallet, User, Shield, Info, Bell, Moon, Sun, Sparkles, CalendarDays, IndianRupee, CreditCard, BarChart2, RefreshCw, Briefcase, Settings2, Lock, FileText, CheckCircle2, Phone } from "lucide-react";

import { Input } from "@/components/ui/input";
import { PremiumModal } from "@/components/PremiumModal";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Switch } from "@/components/ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { useWorkTypes } from "@/hooks/useWorkTypes";

const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState('account');
  const { user, userData, logout } = useAuth();
  const { t, lang, setLang, languages } = useLanguage();
  const navigate = useNavigate();
  const [wage, setWage] = useState(String(userData?.daily_wage || 500));
  const [name, setName] = useState(userData?.name || "");
  const [saved, setSaved] = useState(false);
  const [nameSaved, setNameSaved] = useState(false);
  const [darkMode, setDarkMode] = useState(() => document.documentElement.classList.contains("dark"));
  const [reminders, setReminders] = useState(() => localStorage.getItem("reminders") === "true");
  const [showHowToUse, setShowHowToUse] = useState(false);
  const [showRoleChange, setShowRoleChange] = useState(false);
  const { workTypes, addWorkType } = useWorkTypes();
  const [workType, setWorkType] = useState((userData as any)?.workType || "");
  const [customType, setCustomType] = useState("");
  const [isAddingType, setIsAddingType] = useState(false);
  const [workTypeSaved, setWorkTypeSaved] = useState(false);
  const [showPremiumModal, setShowPremiumModal] = useState(false);

  useEffect(() => {
    if (userData?.name) setName(userData.name);
    if (userData?.daily_wage) setWage(String(userData.daily_wage));
    if ((userData as any)?.workType) setWorkType((userData as any).workType);
  }, [userData]);

  const toggleDarkMode = (enabled: boolean) => {
    setDarkMode(enabled);
    if (enabled) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  };

  const toggleReminders = (enabled: boolean) => {
    setReminders(enabled);
    localStorage.setItem("reminders", String(enabled));
  };

  const saveWage = async () => {
    if (!user) return;
    const val = parseInt(wage, 10);
    if (isNaN(val) || val <= 0) return;
    try {
      await updateDoc(doc(db, "users", user.uid), { daily_wage: val });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      console.error("Error saving wage:", err);
    }
  };

  const saveName = async () => {
    if (!user || !name.trim()) return;
    try {
      await updateDoc(doc(db, "users", user.uid), { name: name.trim() });
      setNameSaved(true);
      setTimeout(() => setNameSaved(false), 2000);
    } catch (err) {
      console.error("Error saving name:", err);
    }
  };

  const saveWorkType = async () => {
    if (!user || !workType) return;
    try {
      await updateDoc(doc(db, "users", user.uid), { workType });
      setWorkTypeSaved(true);
      setTimeout(() => setWorkTypeSaved(false), 2000);
    } catch (err) {
      console.error("Error saving work type:", err);
    }
  };

  const handleAddCustomType = async () => {
    if (!customType.trim()) return;
    const added = await addWorkType(customType);
    if (added) {
      setWorkType(added.name);
      setIsAddingType(false);
      setCustomType("");
    }
  };

  const handleChangeRole = async () => {
    if (!user) return;
    try {
      await updateDoc(doc(db, "users", user.uid), { role: "" });
      localStorage.removeItem("dailywork_role");
      window.location.href = "/select-role";
    } catch (err) {
      console.error("Error changing role:", err);
    }
  };

  const roleLabels: Record<string, string> = {
    contractor: "Contractor", personal: "Personal", labour: "Labour", helper: "Helper", mistry: "Mistry",
  };

  const handleLangChange = async (newLang: string) => {
    setLang(newLang);
    if (user) {
      try {
        await updateDoc(doc(db, "users", user.uid), { language: newLang });
      } catch (err) {
        console.error("Error saving language:", err);
      }
    }
  };

  const initials = (userData?.name || "U")
    .split(" ")
    .map((w: string) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  return (
    <div className="min-h-screen bg-background pb-20 md:pb-6">
      <div className="mx-auto max-w-lg md:max-w-4xl lg:max-w-5xl px-4 pt-6">
        <div className="flex items-center gap-3 mb-6">
          <Settings2 size={24} className="text-primary" />
          <h1 className="text-2xl font-bold text-foreground">{t("settings")}</h1>
        </div>


        {/* Settings Layout */}
        <div className="flex flex-col md:flex-row gap-8">
                    {/* Sidebar Menu (Desktop only) */}
          <div className="hidden md:flex flex-col gap-2 w-64 shrink-0 sticky top-6 self-start">
            <button onClick={() => setActiveTab('account')} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all ${activeTab === 'account' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}`}>
              <User size={18} className={activeTab === 'account' ? 'text-primary' : 'text-muted-foreground'} />
              Account
            </button>
            <button onClick={() => setActiveTab('preferences')} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all ${activeTab === 'preferences' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}`}>
              <Settings2 size={18} className={activeTab === 'preferences' ? 'text-primary' : 'text-muted-foreground'} />
              App Preferences
            </button>
            <button onClick={() => setActiveTab('role')} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all ${activeTab === 'role' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}`}>
              <RefreshCw size={18} className={activeTab === 'role' ? 'text-primary' : 'text-muted-foreground'} />
              Role Management
            </button>
                        <button onClick={() => setActiveTab('data')} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all ${activeTab === 'data' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}`}>
              <FileText size={18} className={activeTab === 'data' ? 'text-primary' : 'text-muted-foreground'} />
              Data & Export
            </button>
            <button onClick={() => setActiveTab('support')} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all ${activeTab === 'support' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}`}>
              <Shield size={18} className={activeTab === 'support' ? 'text-primary' : 'text-muted-foreground'} />
              Support & Privacy
            </button>
            {userData?.role === "admin" && (
              <button onClick={() => navigate("/admin")} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all hover:bg-muted text-muted-foreground font-semibold`}>
                <Shield size={18} className="text-muted-foreground" />
                Admin Panel
              </button>
            )}
            <button onClick={logout} className={`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all hover:bg-destructive/10 text-destructive font-semibold mt-4`}>
              <LogOut size={18} />
              {t("logout")}
            </button>
          </div>

          {/* Details Panel */}
          <div className="flex-1 space-y-6 md:space-y-0">
            {/* Account Settings */}
            <div className={`rounded-2xl bg-card border border-border p-5 shadow-sm block ${activeTab === 'account' ? 'md:block' : 'md:hidden'}`}>              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Account</h2>
              <div className="flex items-center gap-4 mb-5">
                <Avatar className="h-16 w-16 shadow-sm">
                  <AvatarImage src={user?.photoURL || ""} alt={userData?.name || ""} />
                  <AvatarFallback className="bg-primary/10 text-primary text-lg font-bold">
                    {initials}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <p className="text-lg font-bold text-foreground truncate">{userData?.name || "User"}</p>
                  <p className="text-xs text-muted-foreground truncate">{userData?.email}</p>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="text-xs text-muted-foreground font-medium mb-1.5 block">{t("name")}</label>
                  <div className="flex gap-2">
                    <Input value={name} onChange={(e) => setName(e.target.value)} className="flex-1 rounded-xl" />
                    <button onClick={saveName} className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-all active:scale-95 shadow-sm">
                      {nameSaved ? t("saved") : t("save")}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="text-xs text-muted-foreground font-medium mb-1.5 block">{t("email")}</label>
                  <Input value={userData?.email || ""} disabled className="opacity-70 rounded-xl bg-muted/50" />
                </div>
              </div>
            </div>

            {/* App Preferences */}
            <div className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 block ${activeTab === 'preferences' ? 'md:block' : 'md:hidden'}`}>              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">App Preferences</h2>

              <div className="flex items-center justify-between pb-4 border-b border-border">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    {darkMode ? <Moon size={20} className="text-primary" /> : <Sun size={20} className="text-primary" />}
                  </div>
                  <span className="text-base font-semibold text-foreground">Theme</span>
                </div>
                <Switch checked={darkMode} onCheckedChange={toggleDarkMode} />
              </div>

              <div className="flex items-center justify-between pb-4 border-b border-border">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <Bell size={20} className="text-primary" />
                  </div>
                  <div>
                    <span className="text-base font-semibold text-foreground">Reminders</span>
                    <p className="text-[11px] text-muted-foreground">Daily attendance reminder</p>
                  </div>
                </div>
                <Switch checked={reminders} onCheckedChange={toggleReminders} />
              </div>

              <div className="pb-4 border-b border-border">
                <div className="flex items-center gap-3 mb-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <Globe size={20} className="text-primary" />
                  </div>
                  <span className="text-base font-semibold text-foreground">{t("language")}</span>
                </div>
                <Select value={lang} onValueChange={handleLangChange}>
                  <SelectTrigger className="w-full h-12 rounded-xl text-base font-semibold bg-background">
                    <SelectValue placeholder="Select Language" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(languages).map(([code, langName]) => (
                      <SelectItem key={code} value={code} className="text-base font-medium py-3">
                        {langName as string}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Daily Wage */}
              <div>
                <div className="flex items-center gap-3 mb-3">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <Wallet size={20} className="text-primary" />
                  </div>
                  <span className="text-base font-semibold text-foreground">{t("dailyWage")}</span>
                </div>
                <div className="flex gap-2 mb-4">
                  <div className="flex-1 relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                    <input type="number" value={wage} onChange={(e) => setWage(e.target.value)} className="w-full rounded-xl border border-border bg-background px-8 py-3 text-lg font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary transition-all" />
                  </div>
                  <button onClick={saveWage} className="rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-all active:scale-95 shadow-sm">
                    {saved ? t("saved") : t("save")}
                  </button>
                </div>

                {/* Work Type (Only for Personal Mode) */}
                {userData?.role !== "contractor" && (
                  <div className="pt-4 border-t border-border">
                    <div className="flex items-center gap-3 mb-3">
                      <Briefcase size={20} className="text-muted-foreground" />
                      <span className="text-base font-bold text-foreground">Work Type</span>
                    </div>

                    {isAddingType ? (
                      <div className="flex gap-2">
                        <Input placeholder="E.g., Welder" value={customType} onChange={(e) => setCustomType(e.target.value)} className="flex-1 rounded-xl" />
                        <Button onClick={handleAddCustomType} className="rounded-xl">Add</Button>
                        <Button variant="outline" onClick={() => setIsAddingType(false)} className="rounded-xl">Cancel</Button>
                      </div>
                    ) : (
                      <div className="flex gap-2">
                        <Select value={workType} onValueChange={(v) => setWorkType(v)}>
                          <SelectTrigger className="w-full h-12 rounded-xl text-base font-semibold bg-background">
                            <SelectValue placeholder="Select Work Type" />
                          </SelectTrigger>
                          <SelectContent>
                            {workTypes.map(t => (
                              <SelectItem key={t.id} value={t.name} className="text-base font-medium py-3">{t.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <Button variant="outline" onClick={() => setIsAddingType(true)} className="h-12 rounded-xl border-dashed">New</Button>
                        <button onClick={saveWorkType} disabled={!workType} className="rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-all active:scale-95 disabled:opacity-50 shrink-0 shadow-sm">
                          {workTypeSaved ? t("saved") : t("save")}
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Role Management */}
            <div className={`rounded-2xl bg-card border border-border p-5 shadow-sm block ${activeTab === 'role' ? 'md:block' : 'md:hidden'}`}>              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Role Management</h2>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-muted/30 border border-border">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                    <RefreshCw size={24} className="text-primary" />
                  </div>
                  <div>
                    <span className="text-lg font-bold text-foreground block">App Mode</span>
                    <p className="text-xs text-muted-foreground mt-0.5">Current: <span className="font-bold text-primary">{roleLabels[userData?.role || localStorage.getItem("dailywork_role") || ""] || "Not Selected"}</span></p>
                  </div>
                </div>
                <button onClick={() => setShowRoleChange(true)} className="rounded-xl bg-background border-2 border-primary text-primary hover:bg-primary/5 px-6 py-2.5 text-sm font-bold transition-all active:scale-95 shadow-sm">
                  Switch Role
                </button>
              </div>
            </div>

            {/* Data & Export */}
            <div className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-4 block ${activeTab === 'data' ? 'md:block' : 'md:hidden'}`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Data & Export</h2>

              <button onClick={() => { if (userData?.role !== 'contractor') { setShowPremiumModal(true); } else { navigate('/history'); } }} className="w-full rounded-xl bg-background hover:bg-muted border border-border py-4 px-5 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98] shadow-sm relative overflow-hidden group">
                <div className="flex items-center gap-4">
                  <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center group-hover:bg-primary/20 transition-colors">
                    <FileText size={20} className="text-primary" />
                  </div>
                  <div className="text-left">
                    <span className="block text-base">Export as PDF</span>
                    <span className="text-[11px] text-muted-foreground font-medium">Only Contractor Premium</span>
                  </div>
                </div>
                {userData?.role !== 'contractor' && <Lock size={16} className="text-muted-foreground" />}
              </button>

              <button onClick={() => navigate('/passbook')} className="w-full rounded-xl bg-background hover:bg-muted border border-border py-4 px-5 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98] shadow-sm relative overflow-hidden group">
                <div className="flex items-center gap-4">
                  <div className="h-10 w-10 rounded-full bg-green-500/10 flex items-center justify-center group-hover:bg-green-500/20 transition-colors">
                    <CreditCard size={20} className="text-green-600" />
                  </div>
                  <div className="text-left">
                    <span className="block text-base">Share via WhatsApp</span>
                    <span className="text-[11px] text-muted-foreground font-medium">Send digital passbook</span>
                  </div>
                </div>
              </button>

              <div className="p-4 rounded-xl bg-blue-500/5 border border-blue-500/20 flex items-start gap-3">
                <div className="h-8 w-8 rounded-full bg-blue-500/20 flex items-center justify-center shrink-0 mt-0.5">
                  <Shield size={14} className="text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-blue-600 dark:text-blue-400 mb-1">Cloud Backup & Restore</h3>
                  <p className="text-xs text-muted-foreground font-medium mb-3">Keep your data safe across devices. Available in Premium.</p>
                  <button onClick={() => setShowPremiumModal(true)} className="text-xs font-bold text-blue-600 dark:text-blue-400 bg-blue-500/10 hover:bg-blue-500/20 px-3 py-1.5 rounded-lg transition-colors active:scale-95">
                    Enable Auto-Backup
                  </button>                </div>
              </div>
            </div>

            {/* Support & Privacy */}
            <div className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 block ${activeTab === 'support' ? 'md:block' : 'md:hidden'}`}>              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Support & Privacy</h2>

              <button onClick={() => setShowHowToUse(true)} className="w-full rounded-xl bg-muted/30 hover:bg-muted border border-border py-4 px-5 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98]">
                <div className="flex items-center gap-4">
                  <div className="h-10 w-10 rounded-full bg-background border border-border flex items-center justify-center shadow-sm">
                    <Info size={20} className="text-foreground" />
                  </div>
                  <div className="text-left">
                    <span className="block text-base">How to Use DailyWork</span>
                    <span className="text-[11px] text-muted-foreground font-medium">Quick guide & tutorials</span>
                  </div>
                </div>
              </button>

              <div className="flex items-center gap-4 p-4 rounded-xl border border-border bg-background">
                <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Shield size={20} className="text-primary" />
                </div>
                <div>
                  <span className="text-base font-bold text-foreground block">Data & Privacy Policy</span>
                  <p className="text-[11px] text-muted-foreground font-medium">Your data is stored securely in the cloud.</p>
                </div>
              </div>
                            <div className="flex items-center gap-4 p-4 rounded-xl border border-border bg-background">
                <div className="h-10 w-10 rounded-full bg-green-500/10 flex items-center justify-center">
                  <Phone size={20} className="text-green-600" />
                </div>
                <div>
                  <span className="text-base font-bold text-foreground block">Contact Support</span>
                  <p className="text-[11px] text-muted-foreground font-medium">WhatsApp us for help</p>
                </div>
              </div>

              {/* Mobile Logout */}
              <div className="pt-4 border-t border-border md:hidden space-y-4 mt-8">
                {userData?.role === "admin" && (
                  <button onClick={() => navigate("/admin")} className="w-full rounded-xl bg-muted py-4 flex items-center justify-center gap-2 text-foreground font-bold text-base active:scale-[0.98] transition-all">
                    <Shield size={20} />
                    Admin Panel
                  </button>
                )}
                <button onClick={logout} className="w-full rounded-xl bg-destructive/10 hover:bg-destructive border border-destructive/20 hover:border-destructive py-4 flex items-center justify-center gap-2 text-destructive hover:text-destructive-foreground font-bold text-base active:scale-[0.98] transition-all">
                  <LogOut size={20} />
                  {t("logout")}
                </button>
              </div>
            </div>

            <div className={`md:hidden text-center text-[10px] text-muted-foreground mt-6 ${activeTab === 'support' ? 'block' : 'hidden'}`}>DailyWork Version 1.0.0</div>
          </div>
        </div>
</div>
      {/* How to Use Dialog */}
      <Dialog open={showHowToUse} onOpenChange={setShowHowToUse}>
        <DialogContent className="max-w-sm mx-auto max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>How to Use DailyWork</DialogTitle>
            <DialogDescription>A quick guide to tracking your work effectively</DialogDescription>
          </DialogHeader>
          <div className="space-y-5 mt-2">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <CalendarDays size={16} className="text-primary" />
                <h3 className="text-sm font-bold text-foreground">Marking Attendance</h3>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed pl-6">
                On the Dashboard, click <b>Full Day</b> or <b>Half Day</b> to mark today's attendance. Add overtime using the + / - buttons before saving. If you didn't work, click <b>Mark Absent</b>.
              </p>
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <IndianRupee size={16} className="text-green-600" />
                <h3 className="text-sm font-bold text-foreground">Net Payable & Earnings</h3>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed pl-6">
                Your <b>Earnings</b> are automatically calculated by multiplying your working days with your Daily Wage. <b>Net Payable</b> on the dashboard and stats page shows your final take-home amount: <br/><br/>
                <span className="font-mono bg-muted px-1 py-0.5 rounded text-primary block mt-1">Net Payable = Total Earnings - Advance</span>
              </p>
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <CreditCard size={16} className="text-orange-500" />
                <h3 className="text-sm font-bold text-foreground">Advance Payments</h3>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed pl-6">
                If you receive money ahead of time, click <b>Add Advance Payment Today</b> on the Dashboard or add it directly on a specific date inside the <b>Calendar</b>. This is automatically deducted from your Net Payable.
              </p>
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <BarChart2 size={16} className="text-blue-500" />
                <h3 className="text-sm font-bold text-foreground">Calendar & History</h3>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed pl-6">
                Use the <b>Calendar</b> to edit past records (e.g. if you forgot to mark attendance yesterday). Use <b>History</b> to export your monthly logs as a PDF or CSV, and click the Share button to send reports via WhatsApp or Email.
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <PremiumModal open={showPremiumModal} onOpenChange={setShowPremiumModal} />
      {/* Change Role Confirmation Dialog */}
      <Dialog open={showRoleChange} onOpenChange={setShowRoleChange}>
        <DialogContent className="max-w-xs mx-auto">
          <DialogHeader>
            <DialogTitle>{t("changeRole")}</DialogTitle>
            <DialogDescription>{t("changeRoleConfirm")}</DialogDescription>
          </DialogHeader>
          <div className="flex gap-2 mt-4">
            <button
              onClick={() => setShowRoleChange(false)}
              className="flex-1 rounded-xl border border-border py-3 text-sm font-bold text-foreground active:scale-95"
            >
              {t("cancel")}
            </button>
            <button
              onClick={handleChangeRole}
              className="flex-1 rounded-xl bg-primary py-3 text-sm font-bold text-primary-foreground active:scale-95"
            >
              {t("confirm")}
            </button>
          </div>
        </DialogContent>
      </Dialog>


    </div>
  );
};

export default SettingsPage;
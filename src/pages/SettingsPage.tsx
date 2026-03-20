import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { doc, updateDoc } from "firebase/firestore";
import { LogOut, Globe, Wallet, User, Shield, Info, Bell, Moon, Sun, Sparkles, CalendarDays, IndianRupee, CreditCard, BarChart2, RefreshCw, Briefcase, Settings2, Lock, FileText } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Switch } from "@/components/ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { useWorkTypes } from "@/hooks/useWorkTypes";

const SettingsPage = () => {
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
  const [workType, setWorkType] = useState(userData?.workType || "");
  const [customType, setCustomType] = useState("");
  const [isAddingType, setIsAddingType] = useState(false);
  const [workTypeSaved, setWorkTypeSaved] = useState(false);

  useEffect(() => {
    if (userData?.name) setName(userData.name);
    if (userData?.daily_wage) setWage(String(userData.daily_wage));
    if (userData?.workType) setWorkType(userData.workType);
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
    contractor: "Contractor Mode", personal: "Personal Mode",
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

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

        {/* Left Column */}
        <div className="space-y-6">

        {/* Account Settings */}
        <div className="rounded-2xl bg-card border border-border p-5 shadow-sm">
          <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Account</h2>
          <div className="flex items-center gap-4 mb-5">
            <Avatar className="h-16 w-16">
              <AvatarImage src={user?.photoURL || ""} alt={userData?.name || ""} />
              <AvatarFallback className="bg-primary text-primary-foreground text-lg font-bold">
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
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="flex-1 rounded-xl"
                />
                <button
                  onClick={saveName}
                  className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-colors active:scale-95"
                >
                  {nameSaved ? t("saved") : t("save")}
                </button>
              </div>
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1.5 block">{t("email")}</label>
              <Input
                value={userData?.email || ""}
                disabled
                className="opacity-70 rounded-xl"
              />
            </div>
          </div>
        </div>

        {/* App Preferences */}
        <div className="rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5">
          <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">App Preferences</h2>

          <div className="flex items-center justify-between pb-4 border-b border-border">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
                {darkMode ? <Moon size={20} className="text-foreground" /> : <Sun size={20} className="text-foreground" />}
              </div>
              <span className="text-base font-semibold text-foreground">Dark Mode</span>
            </div>
            <Switch checked={darkMode} onCheckedChange={toggleDarkMode} />
          </div>

          <div className="flex items-center justify-between pb-4 border-b border-border">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
                <Bell size={20} className="text-foreground" />
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
              <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
                <Globe size={20} className="text-foreground" />
              </div>
              <span className="text-base font-semibold text-foreground">{t("language")}</span>
            </div>
            <Select value={lang} onValueChange={handleLangChange}>
              <SelectTrigger className="w-full h-12 rounded-xl text-base font-semibold">
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
              <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
                <Wallet size={20} className="text-foreground" />
              </div>
              <span className="text-base font-semibold text-foreground">{t("dailyWage")}</span>
            </div>
            <div className="flex gap-2 mb-4">
              <div className="flex-1 relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                <input
                  type="number"
                  value={wage}
                  onChange={(e) => setWage(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-8 py-3 text-lg font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
              <button
                onClick={saveWage}
                className="rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-colors active:scale-95"
              >
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
                    <Input
                      placeholder="E.g., Welder"
                      value={customType}
                      onChange={(e) => setCustomType(e.target.value)}
                      className="flex-1 rounded-xl"
                    />
                    <Button onClick={handleAddCustomType} className="rounded-xl">Add</Button>
                    <Button variant="outline" onClick={() => setIsAddingType(false)} className="rounded-xl">Cancel</Button>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <Select value={workType} onValueChange={(v) => setWorkType(v)}>
                      <SelectTrigger className="w-full h-12 rounded-xl text-base font-semibold">
                        <SelectValue placeholder="Select Work Type" />
                      </SelectTrigger>
                      <SelectContent>
                        {workTypes.map(t => (
                          <SelectItem key={t.id} value={t.name} className="text-base font-medium py-3">{t.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button variant="outline" onClick={() => setIsAddingType(true)} className="h-12 rounded-xl border-dashed">New</Button>
                    <button
                      onClick={saveWorkType}
                      disabled={!workType}
                      className="rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground hover:bg-primary/90 transition-colors active:scale-95 disabled:opacity-50 shrink-0"
                    >
                      {workTypeSaved ? t("saved") : t("save")}
                    </button>
                  </div>
                )}
               </div>
            )}
          </div>
        </div>

        </div>

        {/* Right Column */}
        <div className="space-y-6">

        {/* Change Role */}
        <div className="rounded-2xl bg-card border border-border p-5 shadow-sm">
          <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Role Management</h2>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <RefreshCw size={20} className="text-primary" />
              </div>
              <div>
                <span className="text-base font-semibold text-foreground">App Mode</span>
                <p className="text-[11px] text-muted-foreground">Current: <span className="font-bold text-foreground">{roleLabels[userData?.role || localStorage.getItem("dailywork_role") || ""] || "Not Selected"}</span></p>
              </div>
            </div>
            <button
              onClick={() => setShowRoleChange(true)}
              className="rounded-xl bg-background border border-border hover:bg-muted text-foreground px-5 py-2.5 text-sm font-bold transition-colors active:scale-95"
            >
              Switch Role
            </button>
          </div>
        </div>

        {/* Premium Section */}
        <div className="rounded-2xl bg-card border border-border shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-amber-500/20 via-orange-500/10 to-red-500/20 p-5 pb-6">
            <div className="flex items-center gap-3 mb-2">
              <Sparkles size={24} className="text-amber-500" fill="currentColor" />
              <h2 className="text-lg font-bold text-foreground">DailyWork Premium</h2>
            </div>
            <p className="text-sm text-muted-foreground">Unlock the full potential of your work tracking.</p>
          </div>

          <div className="px-5 pt-0 pb-5 -mt-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-background border border-border rounded-xl p-4 flex flex-col items-center justify-center text-center shadow-sm relative overflow-hidden">
                <div className="absolute top-0 right-0 w-16 h-16 bg-gradient-to-br from-amber-500/20 to-transparent rounded-bl-full opacity-50"></div>
                <p className="text-xs font-bold text-muted-foreground mb-1 uppercase tracking-wider">Personal</p>
                <div className="flex items-baseline gap-1 mb-3">
                  <span className="text-xl font-bold text-foreground">₹99</span>
                  <span className="text-[10px] text-muted-foreground">/mo</span>
                </div>
                <button className="w-full py-2 bg-primary/10 hover:bg-primary/20 text-primary text-xs font-bold rounded-lg transition-colors">
                  Upgrade
                </button>
              </div>
              <div className="bg-background border border-border rounded-xl p-4 flex flex-col items-center justify-center text-center shadow-sm relative overflow-hidden">
                <div className="absolute top-0 right-0 w-16 h-16 bg-gradient-to-br from-orange-500/20 to-transparent rounded-bl-full opacity-50"></div>
                <p className="text-xs font-bold text-muted-foreground mb-1 uppercase tracking-wider">Contractor</p>
                <div className="flex items-baseline gap-1 mb-3">
                  <span className="text-xl font-bold text-foreground">₹199</span>
                  <span className="text-[10px] text-muted-foreground">/mo</span>
                </div>
                <button className="w-full py-2 bg-primary text-primary-foreground hover:bg-primary/90 text-xs font-bold rounded-lg transition-colors shadow-md">
                  Upgrade
                </button>
              </div>
            </div>

            <div className="mt-5 space-y-3">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Premium Features</p>

              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50 border border-border">
                 <div className="flex items-center gap-3">
                   <div className="h-8 w-8 rounded-full bg-background flex items-center justify-center shadow-sm">
                     <FileText size={14} className="text-primary" />
                   </div>
                   <span className="text-sm font-semibold text-foreground">Detailed PDF Exports</span>
                 </div>
                 <Lock size={14} className="text-muted-foreground" />
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50 border border-border">
                 <div className="flex items-center gap-3">
                   <div className="h-8 w-8 rounded-full bg-background flex items-center justify-center shadow-sm">
                     <BarChart2 size={14} className="text-primary" />
                   </div>
                   <span className="text-sm font-semibold text-foreground">Advanced Analytics</span>
                 </div>
                 <Lock size={14} className="text-muted-foreground" />
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50 border border-border">
                 <div className="flex items-center gap-3">
                   <div className="h-8 w-8 rounded-full bg-background flex items-center justify-center shadow-sm">
                     <Shield size={14} className="text-primary" />
                   </div>
                   <span className="text-sm font-semibold text-foreground">Backup & Restore</span>
                 </div>
                 <Lock size={14} className="text-muted-foreground" />
              </div>
            </div>
          </div>
        </div>

        {/* Data & Export */}
        <div className="rounded-2xl bg-card border border-border p-5 shadow-sm space-y-3">
          <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Data & Export</h2>

          <button
            onClick={() => navigate('/history')}
            className="w-full rounded-xl bg-background hover:bg-muted border border-border py-3 px-4 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <FileText size={18} className="text-primary" />
              <span>Export as PDF</span>
            </div>
          </button>

          <button
            onClick={() => navigate('/passbook')}
            className="w-full rounded-xl bg-background hover:bg-muted border border-border py-3 px-4 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <CreditCard size={18} className="text-green-600" />
              <span>Share via WhatsApp</span>
            </div>
          </button>
        </div>

        {/* Data & Privacy & Support */}
        <div className="rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5">
          <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Support & Privacy</h2>

          <button
            onClick={() => setShowHowToUse(true)}
            className="w-full rounded-xl bg-muted/50 hover:bg-muted border border-border py-4 px-4 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <Info size={18} className="text-muted-foreground" />
              <span>How to Use DailyWork</span>
            </div>
          </button>

          <div className="flex items-center gap-4">
            <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
              <Shield size={20} className="text-foreground" />
            </div>
            <div>
              <span className="text-base font-semibold text-foreground">Data & Privacy</span>
              <p className="text-[11px] text-muted-foreground">Your data is stored securely.</p>
            </div>
          </div>
        </div>

        {/* Admin Panel Link */}
        {userData?.role === "admin" && (
          <button
            onClick={() => navigate("/admin")}
            className="w-full rounded-2xl bg-primary py-4 flex items-center justify-center gap-2 text-primary-foreground font-bold text-base active:scale-[0.98] shadow-sm hover:shadow-md transition-all"
          >
            <Shield size={20} />
            Admin Panel
          </button>
        )}

        {/* Logout */}
        <button
          onClick={logout}
          className="w-full rounded-2xl bg-destructive/10 hover:bg-destructive border border-destructive/20 hover:border-destructive py-4 flex items-center justify-center gap-2 text-destructive hover:text-destructive-foreground font-bold text-base active:scale-[0.98] transition-all"
        >
          <LogOut size={20} />
          {t("logout")}
        </button>

        <p className="text-center text-[10px] text-muted-foreground mt-6">DailyWork Version 1.0.0</p>

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
const fs = require('fs');

let code = fs.readFileSync('src/pages/SettingsPage.tsx', 'utf-8');

const replacement = `
        {/* Settings Layout */}
        <div className="flex flex-col md:flex-row gap-8">

          {/* Sidebar Menu (Desktop only) */}
          <div className="hidden md:flex flex-col gap-2 w-64 shrink-0 sticky top-6 self-start">
            <button onClick={() => setActiveTab('account')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'account' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <User size={18} className={activeTab === 'account' ? 'text-primary' : 'text-muted-foreground'} />
              Account
            </button>
            <button onClick={() => setActiveTab('preferences')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'preferences' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <Settings2 size={18} className={activeTab === 'preferences' ? 'text-primary' : 'text-muted-foreground'} />
              App Preferences
            </button>
            <button onClick={() => setActiveTab('role')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'role' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <RefreshCw size={18} className={activeTab === 'role' ? 'text-primary' : 'text-muted-foreground'} />
              Role Management
            </button>
            <button onClick={() => setActiveTab('premium')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'premium' ? 'bg-amber-500/10 text-amber-600 font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <Sparkles size={18} className={activeTab === 'premium' ? 'text-amber-500' : 'text-muted-foreground'} />
              Premium Plan
            </button>
            <button onClick={() => setActiveTab('data')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'data' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <FileText size={18} className={activeTab === 'data' ? 'text-primary' : 'text-muted-foreground'} />
              Data & Export
            </button>
            <button onClick={() => setActiveTab('support')} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all \${activeTab === 'support' ? 'bg-primary/10 text-primary font-bold shadow-sm' : 'hover:bg-muted text-muted-foreground font-semibold'}\`}>
              <Shield size={18} className={activeTab === 'support' ? 'text-primary' : 'text-muted-foreground'} />
              Support & Privacy
            </button>
            {userData?.role === "admin" && (
              <button onClick={() => navigate("/admin")} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all hover:bg-muted text-muted-foreground font-semibold\`}>
                <Shield size={18} className="text-muted-foreground" />
                Admin Panel
              </button>
            )}
            <button onClick={logout} className={\`flex items-center gap-3 p-3.5 rounded-xl text-left transition-all hover:bg-destructive/10 text-destructive font-semibold mt-4\`}>
              <LogOut size={18} />
              {t("logout")}
            </button>
          </div>

          {/* Details Panel */}
          <div className="flex-1 space-y-6 md:space-y-0">
            {/* Account Settings */}
            <div className={\`rounded-2xl bg-card border border-border p-5 shadow-sm \${activeTab === 'account' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'account' ? 'block' : 'hidden'}\`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Account</h2>
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
            <div className={\`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 \${activeTab === 'preferences' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'preferences' ? 'block' : 'hidden'}\`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">App Preferences</h2>

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
            <div className={\`rounded-2xl bg-card border border-border p-5 shadow-sm \${activeTab === 'role' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'role' ? 'block' : 'hidden'}\`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Role Management</h2>
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

            {/* Premium Section */}
            <div className={\`rounded-2xl bg-card border border-border shadow-md overflow-hidden \${activeTab === 'premium' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'premium' ? 'block' : 'hidden'}\`}>
              <div className="bg-gradient-to-r from-amber-500/20 via-orange-500/10 to-red-500/20 p-6 pb-8">
                <div className="flex items-center gap-3 mb-2">
                  <Sparkles size={28} className="text-amber-500" fill="currentColor" />
                  <h2 className="text-xl font-bold text-foreground">DailyWork Premium</h2>
                </div>
                <p className="text-sm text-muted-foreground font-medium">Unlock the full potential of your work tracking.</p>
              </div>

              <div className="px-6 pt-0 pb-6 -mt-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Personal Plan */}
                  <div className={\`bg-background border-2 rounded-2xl p-5 flex flex-col items-center justify-center text-center shadow-sm relative overflow-hidden transition-all \${userData?.role !== 'contractor' ? 'border-primary ring-4 ring-primary/10' : 'border-border opacity-70 scale-[0.98]'}\`}>
                    <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-primary/10 to-transparent rounded-bl-full opacity-50"></div>
                    {userData?.role === 'contractor' && <div className="absolute inset-0 bg-background/50 z-10 flex items-center justify-center backdrop-blur-[1px]"><span className="bg-background text-xs font-bold px-3 py-1 rounded-full border border-border shadow-sm">Switch role to view</span></div>}
                    <p className="text-xs font-bold text-primary mb-1 uppercase tracking-wider">Personal Role</p>
                    <div className="flex items-baseline gap-1 mb-4">
                      <span className="text-3xl font-black text-foreground">₹99</span>
                    </div>
                    <ul className="text-xs text-muted-foreground text-left space-y-2 mb-5 w-full font-medium">
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Basic tracking</li>
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Daily wage logs</li>
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-primary" /> Basic analytics</li>
                    </ul>
                    <button className="w-full py-3 bg-primary/10 hover:bg-primary/20 text-primary text-sm font-bold rounded-xl transition-all active:scale-95 mt-auto">
                      Upgrade Personal
                    </button>
                  </div>

                  {/* Contractor Plan */}
                  <div className={\`bg-background border-2 rounded-2xl p-5 flex flex-col items-center justify-center text-center shadow-md relative overflow-hidden transition-all \${userData?.role === 'contractor' ? 'border-amber-500 ring-4 ring-amber-500/10' : 'border-border opacity-70 scale-[0.98]'}\`}>
                    <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-amber-500/20 to-transparent rounded-bl-full opacity-50"></div>
                    <div className="absolute top-3 left-3 bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm animate-pulse">40% OFF</div>
                    {userData?.role !== 'contractor' && <div className="absolute inset-0 bg-background/50 z-10 flex items-center justify-center backdrop-blur-[1px]"><span className="bg-background text-xs font-bold px-3 py-1 rounded-full border border-border shadow-sm">Switch role to view</span></div>}
                    <p className="text-xs font-bold text-amber-600 mb-1 uppercase tracking-wider mt-2">Contractor Role</p>
                    <div className="flex items-baseline gap-1 mb-4">
                      <span className="text-3xl font-black text-foreground">₹199</span>
                    </div>
                    <ul className="text-xs text-muted-foreground text-left space-y-2 mb-5 w-full font-medium">
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-amber-500" /> Unlimited Workers</li>
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-amber-500" /> PDF Export + Share</li>
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-amber-500" /> Advanced Analytics</li>
                      <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-amber-500" /> Backup & Restore</li>
                    </ul>
                    <button className="w-full py-3 bg-amber-500 text-white hover:bg-amber-600 text-sm font-bold rounded-xl transition-all shadow-md active:scale-95 mt-auto">
                      Upgrade Contractor
                    </button>
                  </div>
                </div>

                {/* Combo Plan */}
                <div className="mt-4 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 border-2 border-indigo-500/30 rounded-2xl p-5 flex flex-col sm:flex-row items-center justify-between gap-4 relative overflow-hidden shadow-sm">
                  <div className="absolute right-0 bottom-0 w-32 h-32 bg-gradient-to-tl from-indigo-500/20 to-transparent rounded-tl-full opacity-50"></div>
                  <div className="z-10 text-center sm:text-left">
                    <div className="flex items-center gap-2 mb-1 justify-center sm:justify-start">
                      <span className="bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm">BEST VALUE</span>
                      <p className="text-sm font-bold text-foreground">Combo Plan</p>
                    </div>
                    <p className="text-xs text-muted-foreground font-medium">Personal + Contractor Premium</p>
                  </div>
                  <div className="z-10 flex items-center gap-4">
                    <div className="text-right">
                      <p className="text-[10px] text-muted-foreground line-through">₹599</p>
                      <p className="text-xl font-black text-indigo-600 dark:text-indigo-400">₹399<span className="text-[10px] text-muted-foreground font-medium">/mo</span></p>
                    </div>
                    <button className="px-5 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-sm font-bold rounded-xl transition-all shadow-md hover:shadow-lg active:scale-95">
                      Get Combo
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Data & Export */}
            <div className={\`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-4 \${activeTab === 'data' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'data' ? 'block' : 'hidden'}\`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Data & Export</h2>

              <button onClick={() => navigate('/history')} className="w-full rounded-xl bg-background hover:bg-muted border border-border py-4 px-5 flex items-center justify-between text-sm font-bold text-foreground transition-all active:scale-[0.98] shadow-sm relative overflow-hidden group">
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
                  <button className="text-xs font-bold text-blue-600 dark:text-blue-400 bg-blue-500/10 hover:bg-blue-500/20 px-3 py-1.5 rounded-lg transition-colors">
                    Enable Auto-Backup
                  </button>
                </div>
              </div>
            </div>

            {/* Support & Privacy */}
            <div className={\`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 \${activeTab === 'support' ? 'block' : 'hidden md:hidden'} md:\${activeTab === 'support' ? 'block' : 'hidden'}\`}>
              <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-2">Support & Privacy</h2>

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
              <div className="pt-4 border-t border-border md:hidden space-y-4">
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

            <div className={\`md:hidden text-center text-[10px] text-muted-foreground mt-6 \${activeTab === 'support' ? 'block' : 'hidden'}\`}>DailyWork Version 1.0.0</div>
          </div>
        </div>
`;

let beginIdx = code.indexOf('<div className="flex flex-col md:flex-row gap-6">');
if (beginIdx === -1) {
    beginIdx = code.indexOf('<div className="grid grid-cols-1 md:grid-cols-2 gap-6">');
}
const endMarker = '</div>\n      {/* How to Use Dialog */}';
const endIdx = code.lastIndexOf(endMarker);

code = code.substring(0, beginIdx) + replacement + code.substring(endIdx);
// Add CheckCircle2, Phone import
code = code.replace('LogOut, Globe, Wallet, User, Shield, Info, Bell, Moon, Sun, Sparkles, CalendarDays, IndianRupee, CreditCard, BarChart2, RefreshCw, Briefcase, Settings2, Lock, FileText } from "lucide-react";',
'LogOut, Globe, Wallet, User, Shield, Info, Bell, Moon, Sun, Sparkles, CalendarDays, IndianRupee, CreditCard, BarChart2, RefreshCw, Briefcase, Settings2, Lock, FileText, CheckCircle2, Phone } from "lucide-react";');

fs.writeFileSync('src/pages/SettingsPage.tsx', code);

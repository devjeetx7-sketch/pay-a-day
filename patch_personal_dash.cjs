const fs = require('fs');

let code = fs.readFileSync('src/pages/PersonalDashboard.tsx', 'utf-8');

const searchBlock = `<div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12 border-2 border-primary/20">
            <AvatarImage src={user?.photoURL || ""} alt={userData?.name || "User"} />
            <AvatarFallback className="bg-primary/10 text-primary text-sm font-bold">
              {initials}
            </AvatarFallback>
          </Avatar>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Hi, {userData?.name?.split(" ")[0] || "User"} 👋</h1>
            <p className="text-sm text-muted-foreground mt-1">Track your work & earnings</p>
          </div>
        </div>
      </div>`;

const replaceBlock = `<div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12 border-2 border-primary/20">
            <AvatarImage src={user?.photoURL || ""} alt={userData?.name || "User"} />
            <AvatarFallback className="bg-primary/10 text-primary text-sm font-bold">
              {initials}
            </AvatarFallback>
          </Avatar>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Hi, {userData?.name?.split(" ")[0] || "User"} 👋</h1>
            <p className="text-sm text-muted-foreground mt-1">Track your work & earnings</p>
          </div>
        </div>
        <button className="relative h-12 w-12 rounded-full bg-card border border-border flex items-center justify-center hover:bg-muted active:scale-95 transition-all shadow-sm">
          <Bell size={22} className="text-foreground" />
          {/* Example notification indicator */}
          <span className="absolute top-3 right-3 h-2.5 w-2.5 rounded-full bg-destructive border-2 border-card animate-pulse"></span>
        </button>
      </div>

      {/* Mini Analytics Preview */}
      <div className="bg-gradient-to-r from-primary/10 to-green-500/10 border border-primary/20 rounded-2xl p-4 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center shrink-0">
             <TrendingUp size={20} className="text-primary" />
          </div>
          <div>
             <h3 className="text-sm font-bold text-foreground">Monthly Goal</h3>
             <p className="text-xs text-muted-foreground font-medium mt-0.5"><span className="text-green-600 font-bold">On track</span> to hit ₹20k</p>
          </div>
        </div>
        <button onClick={() => navigate('/stats')} className="text-xs font-bold text-primary bg-primary/10 hover:bg-primary/20 px-4 py-2 rounded-xl transition-colors">
          View
        </button>
      </div>`;

code = code.replace(searchBlock, replaceBlock);

const importSearch = `import { CalendarDays, Banknote, IndianRupee, HandCoins, ArrowRight, History, FileText, CheckCircle2, AlertCircle } from "lucide-react";`;
const importReplace = `import { CalendarDays, Banknote, IndianRupee, HandCoins, ArrowRight, History, FileText, CheckCircle2, AlertCircle, Bell, TrendingUp } from "lucide-react";`;
code = code.replace(importSearch, importReplace);

fs.writeFileSync('src/pages/PersonalDashboard.tsx', code);

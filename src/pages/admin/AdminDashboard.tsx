import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, getDocs, query, where } from "firebase/firestore";
import {
  Users, TrendingUp, IndianRupee, Calendar, Search, ChevronRight,
  Shield, BarChart3, Clock, LogOut, ArrowLeft
} from "lucide-react";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";

interface UserRecord {
  uid: string;
  name: string;
  email: string;
  role: string;
  daily_wage: number;
  language: string;
  photoURL?: string;
}

interface UserStats {
  totalPresent: number;
  totalAbsent: number;
  totalEarnings: number;
  totalAdvance: number;
  lastActive: string;
}

const AdminDashboard = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [userStats, setUserStats] = useState<Record<string, UserStats>>({});
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [globalStats, setGlobalStats] = useState({
    totalUsers: 0,
    totalPresent: 0,
    totalAbsent: 0,
    totalEarnings: 0,
    totalAdvance: 0,
    activeToday: 0,
  });

  const todayStr = new Date().toISOString().split("T")[0];
  const now = new Date();
  const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
  const monthEnd = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-31`;

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      // Load all users
      const usersSnap = await getDocs(collection(db, "users"));
      const allUsers: UserRecord[] = usersSnap.docs.map((d) => d.data() as UserRecord);
      setUsers(allUsers);

      // Load all attendance
      const attSnap = await getDocs(collection(db, "attendance"));
      const allAtt = attSnap.docs.map((d) => d.data());

      // Compute per-user stats for this month
      const statsMap: Record<string, UserStats> = {};
      let gPresent = 0, gAbsent = 0, gEarnings = 0, gAdvance = 0, gActiveToday = 0;
      const todayActiveUsers = new Set<string>();

      allUsers.forEach((u) => {
        const userAtt = allAtt.filter((a) => a.user_id === u.uid && a.date >= monthStart && a.date <= monthEnd);
        let present = 0, absent = 0, halfDays = 0, advance = 0;
        let lastActive = "";

        userAtt.forEach((a) => {
          if (a.status === "present") {
            present++;
            if (a.type === "half") halfDays++;
          } else if (a.status === "absent") {
            absent++;
          }
          if (a.advance_amount) advance += a.advance_amount;
          if (a.date > lastActive) lastActive = a.date;
          if (a.date === todayStr && a.status !== "advance") todayActiveUsers.add(u.uid);
        });

        const effectiveDays = present - halfDays * 0.5;
        const earnings = effectiveDays * (u.daily_wage || 500);

        statsMap[u.uid] = { totalPresent: present, totalAbsent: absent, totalEarnings: earnings, totalAdvance: advance, lastActive };
        gPresent += present;
        gAbsent += absent;
        gEarnings += earnings;
        gAdvance += advance;
      });

      gActiveToday = todayActiveUsers.size;
      setUserStats(statsMap);
      setGlobalStats({
        totalUsers: allUsers.length,
        totalPresent: gPresent,
        totalAbsent: gAbsent,
        totalEarnings: gEarnings,
        totalAdvance: gAdvance,
        activeToday: gActiveToday,
      });
    } catch (err) {
      console.error("Admin load error:", err);
    }
    setLoading(false);
  };

  const filteredUsers = users.filter((u) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q);
  });

  const getInitials = (name: string) =>
    (name || "U").split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-2xl px-4 pt-6 pb-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <button onClick={() => navigate("/")} className="p-2 rounded-xl active:bg-muted">
              <ArrowLeft size={20} className="text-foreground" />
            </button>
            <div>
              <div className="flex items-center gap-2">
                <Shield size={20} className="text-primary" />
                <h1 className="text-xl font-bold text-foreground">Admin Panel</h1>
              </div>
              <p className="text-xs text-muted-foreground mt-0.5">Manage workers & attendance</p>
            </div>
          </div>
          <button onClick={logout} className="p-2 rounded-xl active:bg-muted text-destructive">
            <LogOut size={20} />
          </button>
        </div>

        {/* Global Stats */}
        <div className="grid grid-cols-3 gap-2 mb-4">
          <div className="rounded-2xl bg-primary/10 border border-primary/20 p-3 text-center">
            <Users size={18} className="text-primary mx-auto mb-1" />
            <p className="text-2xl font-bold text-primary">{globalStats.totalUsers}</p>
            <p className="text-[9px] text-muted-foreground font-medium">Total Workers</p>
          </div>
          <div className="rounded-2xl bg-card border border-border p-3 text-center">
            <Calendar size={18} className="text-foreground mx-auto mb-1" />
            <p className="text-2xl font-bold text-foreground">{globalStats.activeToday}</p>
            <p className="text-[9px] text-muted-foreground font-medium">Active Today</p>
          </div>
          <div className="rounded-2xl bg-card border border-border p-3 text-center">
            <TrendingUp size={18} className="text-foreground mx-auto mb-1" />
            <p className="text-2xl font-bold text-foreground">{globalStats.totalPresent}</p>
            <p className="text-[9px] text-muted-foreground font-medium">Month Present</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2 mb-6">
          <div className="rounded-2xl bg-card border border-border p-4">
            <p className="text-xs text-muted-foreground font-medium">Total Earnings</p>
            <p className="text-xl font-bold text-primary">₹{globalStats.totalEarnings.toLocaleString()}</p>
            <p className="text-[9px] text-muted-foreground">This month (all workers)</p>
          </div>
          <div className="rounded-2xl bg-card border border-border p-4">
            <p className="text-xs text-muted-foreground font-medium">Total Advance</p>
            <p className="text-xl font-bold text-orange-500">₹{globalStats.totalAdvance.toLocaleString()}</p>
            <p className="text-[9px] text-muted-foreground">This month (all workers)</p>
          </div>
        </div>

        {/* Search */}
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search workers by name or email..."
            className="pl-9 h-10"
          />
        </div>

        {/* Users list */}
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-bold text-foreground">All Workers ({filteredUsers.length})</h2>
          <p className="text-[10px] text-muted-foreground">Tap to view details</p>
        </div>

        <div className="flex flex-col gap-2">
          {filteredUsers.map((u) => {
            const stats = userStats[u.uid];
            const netPayable = stats ? Math.max(0, stats.totalEarnings - stats.totalAdvance) : 0;
            return (
              <button
                key={u.uid}
                onClick={() => navigate(`/admin/user/${u.uid}`)}
                className="w-full rounded-2xl bg-card border border-border p-4 text-left transition-all active:scale-[0.98] hover:border-primary/30"
              >
                <div className="flex items-center gap-3">
                  <Avatar className="h-11 w-11">
                    <AvatarImage src={u.photoURL || ""} />
                    <AvatarFallback className="bg-primary/10 text-primary text-sm font-bold">
                      {getInitials(u.name)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-foreground truncate">{u.name || "Unnamed"}</p>
                      {u.role === "admin" && (
                        <Badge variant="outline" className="text-[9px] px-1.5 py-0 border-primary text-primary">Admin</Badge>
                      )}
                    </div>
                    <p className="text-[10px] text-muted-foreground truncate">{u.email}</p>
                    <div className="flex items-center gap-3 mt-1.5">
                      <span className="text-[10px] font-medium text-primary">{stats?.totalPresent || 0}d worked</span>
                      <span className="text-[10px] font-medium text-destructive">{stats?.totalAbsent || 0}d absent</span>
                      <span className="text-[10px] font-medium text-foreground">₹{u.daily_wage || 500}/day</span>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <p className="text-sm font-bold text-primary">₹{netPayable.toLocaleString()}</p>
                    <p className="text-[9px] text-muted-foreground">net payable</p>
                    <ChevronRight size={14} className="text-muted-foreground" />
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {filteredUsers.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <Users size={40} className="text-muted-foreground/30 mb-3" />
            <p className="text-muted-foreground font-medium">No workers found</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;

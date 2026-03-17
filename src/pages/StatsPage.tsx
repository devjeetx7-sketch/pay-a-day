import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell, PieChart, Pie } from "recharts";
import { TrendingUp, ChevronLeft, ChevronRight, CalendarDays } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const StatsPage = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [allRecords, setAllRecords] = useState<any[]>([]);
  const [monthlyData, setMonthlyData] = useState<{ name: string; days: number }[]>([]);
  const [currentMonthStats, setCurrentMonthStats] = useState({
    present: 0, absent: 0, halfDays: 0, overtime: 0, totalEarnings: 0, advanceTotal: 0
  });
  const [allTimeStats, setAllTimeStats] = useState({
    totalDays: 0, totalEarnings: 0,
  });
  const [weeklyData, setWeeklyData] = useState<{ name: string; days: number }[]>([]);

  const dailyWage = userData?.daily_wage || 500;
  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  const monthNamesShort = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

  useEffect(() => {
    if (!user) return;
    loadAllRecords();
  }, [user]);

  useEffect(() => {
    if (allRecords.length > 0 || allRecords.length === 0) {
      computeStats();
    }
  }, [allRecords, selectedMonth]);

  const loadAllRecords = async () => {
    if (!user) return;
    try {
      const q = query(collection(db, "attendance"), where("user_id", "==", user.uid));
      const snap = await getDocs(q);
      setAllRecords(snap.docs.map((d) => d.data()));
    } catch (err) {
      console.error("Error loading stats:", err);
    }
  };

  const computeStats = () => {
    const today = new Date();
    const cm = selectedMonth.getMonth();
    const year = selectedMonth.getFullYear();
    const startDate = `${year}-${String(cm + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(cm + 1).padStart(2, "0")}-31`;

    // Monthly data for last 6 months from selected
    const months: { name: string; days: number }[] = [];
    for (let i = 5; i >= 0; i--) {
      const m = new Date(year, cm - i, 1);
      const mY = m.getFullYear();
      const mM = m.getMonth();
      const s = `${mY}-${String(mM + 1).padStart(2, "0")}-01`;
      const e = `${mY}-${String(mM + 1).padStart(2, "0")}-31`;
      const count = allRecords.filter((r) => r.date >= s && r.date <= e && r.status === "present").length;
      months.push({ name: monthNamesShort[mM], days: count });
    }
    setMonthlyData(months);

    // Weekly data for selected month
    const weeks: { name: string; days: number }[] = [];
    for (let w = 0; w < 4; w++) {
      const wStart = `${year}-${String(cm + 1).padStart(2, "0")}-${String(w * 7 + 1).padStart(2, "0")}`;
      const wEnd = `${year}-${String(cm + 1).padStart(2, "0")}-${String(Math.min((w + 1) * 7, 31)).padStart(2, "0")}`;
      const count = allRecords.filter((r) => r.date >= wStart && r.date <= wEnd && r.status === "present").length;
      weeks.push({ name: `W${w + 1}`, days: count });
    }
    setWeeklyData(weeks);

    // Current month stats
    let present = 0, absent = 0, halfDays = 0, overtime = 0, advanceTotal = 0;
    allRecords
      .filter((r) => r.date >= startDate && r.date <= endDate)
      .forEach((data) => {
        if (data.status === "present") {
          present++;
          if (data.type === "half") halfDays++;
          overtime += data.overtime_hours || 0;
        } else if (data.status === "absent" || data.status === "leave") {
          absent++;
        }
        if (data.advance_amount) {
          advanceTotal += data.advance_amount;
        }
      });

    const effectiveDays = present - halfDays * 0.5;
    setCurrentMonthStats({
      present, absent, halfDays, overtime, advanceTotal,
      totalEarnings: effectiveDays * dailyWage,
    });

    // All-time stats
    let allTimePresent = 0, allTimeHalfDays = 0;
    allRecords.forEach((data) => {
      if (data.status === "present") {
        allTimePresent++;
        if (data.type === "half") allTimeHalfDays++;
      }
    });
    const allTimeEffectiveDays = allTimePresent - allTimeHalfDays * 0.5;
    setAllTimeStats({
      totalDays: allTimeEffectiveDays,
      totalEarnings: allTimeEffectiveDays * dailyWage,
    });

  };

  const prevMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1));

  const pieData = [
    { name: t("present"), value: currentMonthStats.present, color: "hsl(160, 81%, 40%)" },
    { name: t("absent"), value: currentMonthStats.absent, color: "hsl(0, 84%, 60%)" },
  ].filter((d) => d.value > 0);

  const attendanceRate = currentMonthStats.present + currentMonthStats.absent > 0
    ? Math.round((currentMonthStats.present / (currentMonthStats.present + currentMonthStats.absent)) * 100)
    : 0;

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-4">{t("stats")}</h1>

        {/* Month selector */}
        <div className="flex items-center justify-between mb-4 rounded-2xl bg-card border border-border px-4 py-3">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} className="text-foreground" />
          </button>
          <div className="flex items-center gap-2">
            <CalendarDays size={16} className="text-primary" />
            <span className="text-sm font-bold text-foreground">
              {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
            </span>
          </div>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} className="text-foreground" />
          </button>
        </div>

        {/* Streak & Best Streak */}
        <Tabs defaultValue="month" className="w-full">
          <TabsList className="grid w-full grid-cols-2 mb-4 bg-muted/50 p-1">
            <TabsTrigger value="month" className="rounded-xl">{monthNames[selectedMonth.getMonth()]}</TabsTrigger>
            <TabsTrigger value="allTime" className="rounded-xl">All Time</TabsTrigger>
          </TabsList>

          {/* MONTHLY STATS */}
          <TabsContent value="month" className="mt-0">
            {/* Attendance Rate */}
            <div className="rounded-2xl bg-card border border-border p-5 mb-4 flex items-center gap-4">
              <div className="h-20 w-20">
                {pieData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={pieData} innerRadius={24} outerRadius={36} dataKey="value" strokeWidth={0}>
                        {pieData.map((entry, index) => (
                          <Cell key={index} fill={entry.color} />
                        ))}
                      </Pie>
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="h-full w-full rounded-full border-4 border-muted flex items-center justify-center">
                    <span className="text-xs text-muted-foreground">0%</span>
                  </div>
                )}
              </div>
              <div>
                <p className="text-3xl font-bold text-foreground">{attendanceRate}%</p>
                <p className="text-sm text-muted-foreground font-medium">{t("attendanceRate")}</p>
              </div>
            </div>

            {/* Quick stats */}
            <div className="grid grid-cols-2 gap-3 mb-3">
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">{t("earnings")}</p>
                <p className="text-2xl font-bold text-primary">₹{currentMonthStats.totalEarnings.toLocaleString()}</p>
                <p className="text-[10px] text-muted-foreground">{monthNames[selectedMonth.getMonth()]}</p>
              </div>
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">{t("totalOvertime")}</p>
                <p className="text-2xl font-bold text-foreground">{currentMonthStats.overtime} <span className="text-sm">{t("hours")}</span></p>
                <p className="text-[10px] text-muted-foreground">{monthNames[selectedMonth.getMonth()]}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">Advance Deductions</p>
                <p className="text-2xl font-bold text-orange-500">₹{currentMonthStats.advanceTotal.toLocaleString()}</p>
                <p className="text-[10px] text-muted-foreground">{monthNames[selectedMonth.getMonth()]}</p>
              </div>
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">Net Payable</p>
                <p className="text-2xl font-bold text-green-600">
                  ₹{Math.max(0, currentMonthStats.totalEarnings - currentMonthStats.advanceTotal).toLocaleString()}
                </p>
                <p className="text-[10px] text-muted-foreground">After deductions</p>
              </div>
            </div>

            {/* Detail cards */}
            <div className="grid grid-cols-3 gap-2 mb-4">
              <div className="rounded-xl bg-primary/10 p-3 text-center">
                <p className="text-lg font-bold text-primary">{currentMonthStats.present}</p>
                <p className="text-[10px] text-muted-foreground">{t("present")}</p>
              </div>
              <div className="rounded-xl bg-destructive/10 p-3 text-center">
                <p className="text-lg font-bold text-destructive">{currentMonthStats.absent}</p>
                <p className="text-[10px] text-muted-foreground">{t("absent")}</p>
              </div>
              <div className="rounded-xl bg-accent p-3 text-center">
                <p className="text-lg font-bold text-foreground">{currentMonthStats.halfDays}</p>
                <p className="text-[10px] text-muted-foreground">{t("halfDay")}</p>
              </div>
            </div>

            {/* Weekly breakdown */}
            <div className="rounded-2xl bg-card border border-border p-4 mb-4">
              <div className="flex items-center gap-2 mb-3">
                <TrendingUp size={16} className="text-primary" />
                <p className="text-sm font-bold text-foreground">{t("weeklyAvg")} - {monthNames[selectedMonth.getMonth()]}</p>
              </div>
              <div className="h-28">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={weeklyData}>
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                    <YAxis hide />
                    <Bar dataKey="days" radius={[6, 6, 0, 0]}>
                      {weeklyData.map((_, i) => (
                        <Cell key={i} fill={`hsl(160, 81%, ${50 - i * 5}%)`} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </TabsContent>

          {/* ALL TIME STATS */}
          <TabsContent value="allTime" className="mt-0">
            {/* All Time Stats */}
            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">{t("totalEarningsAllTime") || "Total Earnings"}</p>
                <p className="text-2xl font-bold text-primary">₹{allTimeStats.totalEarnings.toLocaleString()}</p>
              </div>
              <div className="rounded-2xl bg-card border border-border p-4">
                <p className="text-xs text-muted-foreground font-medium">{t("totalDaysAllTime") || "Total Days"}</p>
                <p className="text-2xl font-bold text-foreground">{allTimeStats.totalDays}</p>
              </div>
            </div>

            {/* Monthly chart */}
            <div className="rounded-2xl bg-card border border-border p-4 mb-4">
              <p className="text-sm font-bold text-foreground mb-3">{t("totalDays")} - 6 Months</p>
              <div className="h-40">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlyData}>
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                    <YAxis hide />
                    <Bar dataKey="days" radius={[6, 6, 0, 0]}>
                      {monthlyData.map((_, i) => (
                        <Cell key={i} fill={i === monthlyData.length - 1 ? "hsl(160, 81%, 40%)" : "hsl(220, 14%, 90%)"} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>
      <BottomNav />
    </div>
  );
};

export default StatsPage;
import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell } from "recharts";
import { ChevronLeft, ChevronRight, TrendingUp } from "lucide-react";

export const ContractorStats = () => {
  const { user } = useAuth();
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [stats, setStats] = useState({ totalCost: 0, avgWorkerCost: 0, totalWorkDays: 0 });
  const [workerData, setWorkerData] = useState<any[]>([]);

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  useEffect(() => {
    if (user) loadStats();
  }, [user, selectedMonth]);

  const loadStats = async () => {
    if (!user) return;
    const year = selectedMonth.getFullYear();
    const month = String(selectedMonth.getMonth() + 1).padStart(2, "0");
    const yearMonth = `${year}-${month}`;

    try {
      const wQ = query(collection(db, "workers"), where("contractorId", "==", user.uid));
      const wSnap = await getDocs(wQ);

      const workersMap: Record<string, any> = {};
      wSnap.docs.forEach(d => {
        workersMap[`worker_${d.id}`] = d.data();
      });

      let totalCost = 0;
      let totalDays = 0;
      const workerPerfMap: Record<string, { days: number, cost: number }> = {};

      if (wSnap.size > 0) {
        const attQ = query(collection(db, "attendance"), where("contractorId", "==", user.uid));
        const attSnap = await getDocs(attQ);

        attSnap.docs.forEach((doc) => {
          const data = doc.data();
          if (data.date.startsWith(yearMonth) && data.status === "present") {
             const workerData = workersMap[data.user_id];
             if (workerData) {
               const dayVal = data.type === "half" ? 0.5 : 1;
               const costVal = data.type === "half" ? (workerData.wage || 0) / 2 : (workerData.wage || 0);

               if (!workerPerfMap[data.user_id]) workerPerfMap[data.user_id] = { days: 0, cost: 0 };
               workerPerfMap[data.user_id].days += dayVal;
               workerPerfMap[data.user_id].cost += costVal;

               totalDays += dayVal;
               totalCost += costVal;
             }
          }
        });
      }

      const workerPerf = Object.keys(workerPerfMap).map(key => ({
        name: workersMap[key].name,
        days: workerPerfMap[key].days,
        cost: workerPerfMap[key].cost
      }));

      setStats({
          totalCost,
          totalWorkDays: totalDays,
          avgWorkerCost: workerPerf.length > 0 ? Math.round(totalCost / workerPerf.length) : 0
      });

      setWorkerData(workerPerf.sort((a,b) => b.days - a.days));

    } catch (err) {
      console.error("Error loading contractor stats:", err);
    }
  };

  const prevMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1));

  return (
    <div className="space-y-4 animate-in fade-in">
        {/* Month selector */}
        <div className="flex items-center justify-between mb-6 bg-card border border-border rounded-xl p-2">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} />
          </button>
          <span className="text-sm font-bold text-foreground">
            {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-6">
            <div className="bg-card p-4 rounded-2xl border border-border">
                <p className="text-xs text-muted-foreground font-medium mb-1">Total Labour Cost</p>
                <p className="text-2xl font-bold text-primary">₹{stats.totalCost.toLocaleString()}</p>
            </div>
            <div className="bg-card p-4 rounded-2xl border border-border">
                <p className="text-xs text-muted-foreground font-medium mb-1">Total Man Days</p>
                <p className="text-2xl font-bold text-foreground">{stats.totalWorkDays}</p>
            </div>
        </div>

        {/* Worker Performance Chart */}
        <div className="rounded-2xl bg-card border border-border p-4 mb-4">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <TrendingUp size={16} className="text-primary" />
                <p className="text-sm font-bold text-foreground">Top Workers by Days</p>
              </div>
            </div>
            <div className="h-48">
              {workerData.length === 0 ? (
                <div className="h-full flex items-center justify-center text-xs text-muted-foreground">No data for this month</div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={workerData.slice(0, 5)} layout="vertical" margin={{ left: -20, right: 10 }}>
                    <XAxis type="number" hide />
                    <YAxis dataKey="name" type="category" tick={{ fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Bar dataKey="days" radius={[0, 4, 4, 0]} barSize={20}>
                      {workerData.slice(0, 5).map((_, i) => (
                        <Cell key={i} fill={`hsl(160, 81%, ${40 + i * 5}%)`} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
        </div>

        {/* Worker List Details */}
        <div className="space-y-2">
            <h3 className="text-sm font-bold text-muted-foreground mb-2">Cost Breakdown</h3>
            {workerData.length === 0 ? (
                <p className="text-xs text-center py-4 bg-muted rounded-xl">No active workers this month.</p>
            ) : (
                workerData.map((w, i) => (
                    <div key={i} className="flex justify-between items-center p-3 bg-card border border-border rounded-xl">
                        <div className="flex items-center gap-3">
                            <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-primary text-xs font-bold">
                                {w.name.charAt(0)}
                            </div>
                            <div>
                                <p className="font-bold text-sm">{w.name}</p>
                                <p className="text-[10px] text-muted-foreground">{w.days} Days worked</p>
                            </div>
                        </div>
                        <span className="font-bold text-sm text-foreground">₹{w.cost.toLocaleString()}</span>
                    </div>
                ))
            )}
        </div>
    </div>
  );
};

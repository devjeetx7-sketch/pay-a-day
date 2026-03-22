import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp } from "firebase/firestore";
import { Check, X, Clock, Plus, Minus, StickyNote, IndianRupee, FileText, Bell, TrendingUp, Crown } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";

export const PersonalDashboard = () => {
  const navigate = useNavigate();
  const { user, userData } = useAuth();
  const [loading, setLoading] = useState(false);
  const [marked, setMarked] = useState(false);
  const [todayStatus, setTodayStatus] = useState<string | null>(null);
  const [todayNote, setTodayNote] = useState("");

  const [overtimeHours, setOvertimeHours] = useState(0);
  const [note, setNote] = useState("");
  const [absenceReason, setAbsenceReason] = useState("sick");

  const [showAbsentDialog, setShowAbsentDialog] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState("");

  const [stats, setStats] = useState({ totalEarned: 0, todayEarned: 0, monthEarned: 0 });

  const todayStr = new Date().toISOString().split("T")[0];
  const monthStr = todayStr.substring(0, 7);
  const dailyWage = userData?.daily_wage || 500;
  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  useEffect(() => {
    if (user) loadData();
  }, [user]);

  const loadData = async () => {
    if (!user) return;
    try {
      const q = query(collection(db, "attendance"), where("user_id", "==", user.uid));
      const snap = await getDocs(q);

      let foundToday = false;
      let monthEarned = 0;
      let totalEarned = 0;
      let todayEarned = 0;

      snap.docs.forEach(d => {
        const data = d.data();
        if (data.status === "present") {
          let earn = dailyWage;
          if (data.type === "half") earn = earn / 2;

          totalEarned += earn;
          if (data.date.startsWith(monthStr)) monthEarned += earn;
          if (data.date === todayStr) todayEarned = earn;
        }

        if (data.date === todayStr && data.status !== "advance") {
          foundToday = true;
          setMarked(true);
          setTodayStatus(data.status);
          setTodayNote(data.note || "");
        }
      });

      if (!foundToday) {
        setMarked(false);
        setTodayStatus(null);
        setTodayNote("");
      }

      setStats({ totalEarned, monthEarned, todayEarned });
    } catch (err) {
      console.error("Error loading personal data:", err);
    }
  };

  const markAttendance = async (type: "full" | "half" = "full") => {
    if (!user || loading) return;
    setLoading(true);
    try {
      const docId = `${user.uid}_${todayStr}`;
      await setDoc(doc(db, "attendance", docId), {
        user_id: user.uid,
        date: todayStr,
        status: "present",
        type,
        overtime_hours: overtimeHours,
        note,
        daily_wage: dailyWage,
        timestamp: serverTimestamp(),
      });
      setMarked(true);
      setTodayStatus("present");
      setTodayNote(note);
      setOvertimeHours(0);
      setNote("");
      loadData();
    } catch (err) {
      console.error("Error marking attendance:", err);
    }
    setLoading(false);
  };

  const handleMarkAbsent = async () => {
    if (!user || loading) return;
    setLoading(true);
    try {
      const docId = `${user.uid}_${todayStr}`;
      await setDoc(doc(db, "attendance", docId), {
        user_id: user.uid,
        date: todayStr,
        status: "absent",
        reason: absenceReason,
        note,
        timestamp: serverTimestamp(),
      });
      setMarked(true);
      setTodayStatus("absent");
      setTodayNote(note);
      setShowAbsentDialog(false);
      setNote("");
      loadData();
    } catch (err) {
      console.error("Error marking absent:", err);
    }
    setLoading(false);
  };

  const handleSavePayment = async () => {
    if (!user || !paymentAmount) return;
    setLoading(true);
    try {
        const docId = `${user.uid}_${todayStr}_advance`;
        const existingDoc = await getDocs(query(
            collection(db, "attendance"),
            where("user_id", "==", user.uid),
            where("date", "==", todayStr),
            where("status", "==", "advance")
        ));

        let newAmount = Number(paymentAmount);
        if (!existingDoc.empty) {
            newAmount += existingDoc.docs[0].data().advance_amount || 0;
        }

        await setDoc(doc(db, "attendance", docId), {
            user_id: user.uid,
            date: todayStr,
            status: "advance",
            advance_amount: newAmount,
            note: "Advance Payment",
            timestamp: serverTimestamp(),
        });

        setShowPaymentModal(false);
        setPaymentAmount("");
    } catch (err) {
      console.error("Error saving payment:", err);
    }
    setLoading(false);
  };

  const removeAttendance = async () => {
    if (!user || !marked || loading) return;
    setLoading(true);
    try {
      const docId = `${user.uid}_${todayStr}`;
      await deleteDoc(doc(db, "attendance", docId));
      setMarked(false);
      setTodayStatus(null);
      setTodayNote("");
      await loadData();
    } catch (err) {
      console.error("Error removing attendance:", err);
    }
    setLoading(false);
  };

  const initials = (userData?.name || "U")
    .split(" ")
    .map((w: string) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  return (
    <div className="space-y-6 animate-in fade-in w-full">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12 border-2 border-primary/20">
            <AvatarImage src={user?.photoURL || ""} alt={userData?.name || "User"} />
            <AvatarFallback className="bg-primary/10 text-primary text-sm font-bold">
              {initials}
            </AvatarFallback>
          </Avatar>
          <div>
            <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">Hi, {userData?.name?.split(" ")[0] || "User"} <span className="animate-bounce">👋</span></h1>
            <p className="text-sm text-muted-foreground mt-1">Track your work & earnings</p>
          </div>
        </div>
        <button onClick={() => navigate('/premium')} className="relative h-12 w-12 rounded-full bg-gradient-to-br from-amber-100 to-orange-100 dark:from-amber-900/40 dark:to-orange-900/40 border border-amber-200 dark:border-amber-800 flex items-center justify-center hover: active:scale-95 transition-all ">
          <Crown size={22} className="text-amber-600 dark:text-amber-500" />
        </button>
      </div>
            {/* Mini Analytics Preview */}
      <div className="bg-gradient-to-r from-primary/10 to-green-500/10 border border-primary/20 rounded-2xl p-4 flex items-center justify-between ">
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
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <p className="text-xs font-medium text-muted-foreground mb-2">Today's Earnings</p>
          <p className="text-3xl font-bold text-primary">{loading ? <div className="h-8 w-16 bg-muted animate-pulse rounded"></div> : `₹${stats.todayEarned}`}</p>
        </div>
        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <p className="text-xs font-medium text-muted-foreground mb-2">Monthly Earnings</p>
          <p className="text-3xl font-bold text-green-600">{loading ? <div className="h-8 w-20 bg-muted animate-pulse rounded"></div> : `₹${stats.monthEarned}`}</p>
        </div>
      </div>

      <button
        onClick={() => navigate('/passbook')}
        className="w-full bg-primary/10 border border-primary/20 text-primary py-4 rounded-2xl flex items-center justify-center gap-2 font-bold mb-6 active:scale-95 transition-transform"
      >
        <FileText size={20} />
        View My Passbook
      </button>

      {/* Mark Attendance Section */}
      <h2 className="font-bold text-sm text-muted-foreground uppercase tracking-wider mb-2">Daily Log</h2>

      {!marked ? (
        <div className="space-y-3">
          <div className="flex flex-col gap-2 rounded-xl bg-card border border-border px-4 py-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-foreground">Overtime (Hours)</span>
              <div className="flex items-center gap-2">
                <button onClick={() => setOvertimeHours(Math.max(0, overtimeHours - 1))} className="h-10 w-10 rounded-full bg-destructive/10 text-destructive flex items-center justify-center active:scale-90 transition-transform">
                  <Minus size={20} strokeWidth={2.5} />
                </button>
                <div className="relative">
                  <input
                    type="number"
                    min="0"
                    max="24"
                    value={overtimeHours || ""}
                    onChange={(e) => setOvertimeHours(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-16 h-10 text-center text-xl font-bold bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <button onClick={() => setOvertimeHours(overtimeHours + 1)} className="h-10 w-10 rounded-full bg-primary/10 text-primary flex items-center justify-center active:scale-90 transition-transform">
                  <Plus size={20} strokeWidth={2.5} />
                </button>
              </div>
            </div>
            {overtimeHours > 0 && (
              <p className="text-xs text-primary font-medium text-center">
                +{overtimeHours} hours overtime will be added
              </p>
            )}
          </div>

          <div className="rounded-xl bg-card border border-border px-4 py-3">
            <Textarea
              placeholder="Add Note..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="border-0 bg-transparent p-0 text-sm resize-none min-h-[40px] focus-visible:ring-0"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => markAttendance("full")}
              disabled={loading}
              className="rounded-2xl bg-primary text-primary-foreground py-6 flex flex-col items-center justify-center gap-2 transition-all active:scale-95 "
            >
              <div className="h-12 w-12 rounded-full bg-primary-foreground/20 flex items-center justify-center">
                <Check size={28} strokeWidth={3} className="text-primary-foreground" />
              </div>
              <span className="text-sm font-bold">Full Day</span>
            </button>
            <button
              onClick={() => markAttendance("half")}
              disabled={loading}
              className="rounded-2xl bg-accent text-accent-foreground py-6 flex flex-col items-center justify-center gap-2 transition-all active:scale-95  border border-border"
            >
              <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                <Clock size={28} strokeWidth={2} className="text-primary" />
              </div>
              <span className="text-sm font-bold">Half Day</span>
            </button>
          </div>

          <button
            onClick={() => setShowAbsentDialog(true)}
            className="w-full rounded-2xl border-2 border-destructive py-4 flex items-center justify-center gap-2 text-destructive font-bold text-sm active:scale-95"
          >
            <X size={20} />
            Mark Absent
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          <div className={`w-full rounded-2xl py-6 flex flex-col items-center justify-center gap-3 border-2 ${
            todayStatus === "present" ? "bg-primary/10 border-primary" : "bg-destructive/10 border-destructive"
          }`}>
            <div className={`h-14 w-14 rounded-full flex items-center justify-center ${
              todayStatus === "present" ? "bg-primary" : "bg-destructive"
            }`}>
              {todayStatus === "present" ? (
                <Check size={32} strokeWidth={3} className="text-primary-foreground" />
              ) : (
                <X size={32} strokeWidth={3} className="text-destructive-foreground" />
              )}
            </div>
            <span className={`text-lg font-bold ${
              todayStatus === "present" ? "text-primary" : "text-destructive"
            }`}>
              {todayStatus === "present" ? "Marked Present" : "Marked Absent"}
            </span>
            {todayNote && (
              <div className="flex items-center gap-1.5 px-4 text-center text-xs text-muted-foreground">
                <StickyNote size={12} />
                <span>{todayNote}</span>
              </div>
            )}
          </div>

          <button
            onClick={removeAttendance}
            disabled={loading}
            className="w-full rounded-xl border border-destructive py-3 text-destructive text-sm font-bold active:scale-95"
          >
            Remove Attendance
          </button>
        </div>
      )}

      {/* Add Payment Modal */}
      <div className="mt-8">
        <button
          onClick={() => setShowPaymentModal(true)}
          className="w-full rounded-2xl border border-dashed border-primary bg-primary/5 py-4 flex items-center justify-center gap-2 text-primary font-bold text-sm active:scale-95"
        >
          <Plus size={20} />
          Add Advance Payment
        </button>
      </div>

      <Dialog open={showPaymentModal} onOpenChange={setShowPaymentModal}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader><DialogTitle>Add Payment</DialogTitle></DialogHeader>
          <div className="space-y-4 mt-2">
            <div className="relative">
              <IndianRupee className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16}/>
              <input type="number" placeholder="Amount" value={paymentAmount} onChange={e => setPaymentAmount(e.target.value)} className="w-full rounded-xl border border-border bg-background px-9 py-3 text-sm font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary" />
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowPaymentModal(false)}>Cancel</Button>
            <Button onClick={handleSavePayment}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Absent Dialog */}
      <Dialog open={showAbsentDialog} onOpenChange={setShowAbsentDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>Mark Absent</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-2">
              {absenceReasons.map((r) => (
                <button
                  key={r}
                  onClick={() => setAbsenceReason(r)}
                  className={`rounded-xl px-3 py-3 text-sm font-semibold transition-all active:scale-95 ${
                    absenceReason === r
                      ? "bg-destructive text-destructive-foreground"
                      : "bg-muted text-foreground"
                  }`}
                >
                  {r.charAt(0).toUpperCase() + r.slice(1)}
                </button>
              ))}
            </div>
            <Textarea
              placeholder="Add Note (Optional)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAbsentDialog(false)}>Cancel</Button>
            <Button variant="destructive" onClick={handleMarkAbsent}>Confirm</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

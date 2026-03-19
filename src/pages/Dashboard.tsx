import { useState, useEffect } from "react";
import { Check, X, Clock, Minus, Plus, Loader2, StickyNote, Hand, IndianRupee, Users, UserCheck, Wallet } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import {
  collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp,
} from "firebase/firestore";
import BottomNav from "@/components/BottomNav";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";

const Dashboard = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [marked, setMarked] = useState(false);
  const [todayStatus, setTodayStatus] = useState<string | null>(null);
  const [daysWorked, setDaysWorked] = useState(0);
  const [totalOvertime, setTotalOvertime] = useState(0);
  const [leaveDays, setLeaveDays] = useState(0);
  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const [showAbsentDialog, setShowAbsentDialog] = useState(false);
  const [showNoteDialog, setShowNoteDialog] = useState(false);
  const [showAdvanceDialog, setShowAdvanceDialog] = useState(false);
  const [absenceReason, setAbsenceReason] = useState("sick");
  const [overtimeHours, setOvertimeHours] = useState(0);
  const [note, setNote] = useState("");
  const [todayNote, setTodayNote] = useState("");
  const [advanceAmount, setAdvanceAmount] = useState("");
  const [monthlyAdvance, setMonthlyAdvance] = useState(0);
  const [advanceRecords, setAdvanceRecords] = useState<{ date: string; amount: number; note: string }[]>([]);
  const [contractorStats, setContractorStats] = useState({ totalWorkers: 0, todayPresent: 0, pendingPayment: 0 });

  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];
  const [advanceDate, setAdvanceDate] = useState(todayStr);
  const dailyWage = userData?.daily_wage || 500;
  const isContractor = userData?.role === "contractor";

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  useEffect(() => {
    if (!user) return;
    loadMonthData();
  }, [user]);

  const loadMonthData = async () => {
    if (!user) return;
    setPageLoading(true);
    const year = today.getFullYear();
    const month = today.getMonth();
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    const q = query(
      collection(db, "attendance"),
      where("user_id", "==", user.uid)
    );

    try {
      const snap = await getDocs(q);
      let worked = 0;
      let ot = 0;
      let leaves = 0;
      let foundToday = false;
      let advanceTotal = 0;
      const advList: { date: string; amount: number; note: string }[] = [];

      snap.docs.forEach((d) => {
        const data = d.data();
        if (data.date < startDate || data.date > endDate) return;

        if (data.status === "present") {
          worked++;
          if (data.type === "half") worked -= 0.5;
          ot += data.overtime_hours || 0;
        } else if (data.status === "absent" || data.status === "leave") {
          leaves++;
        }
        if (data.advance_amount) {
          advanceTotal += data.advance_amount;
          advList.push({ date: data.date, amount: data.advance_amount, note: data.note || "Advance Payment" });
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

      setDaysWorked(worked);
      setTotalOvertime(ot);
      setLeaveDays(leaves);
      setMonthlyAdvance(advanceTotal);
      setAdvanceRecords(advList.sort((a, b) => b.date.localeCompare(a.date)));

      // Load contractor stats if applicable
      if (isContractor) {
        try {
          const workersSnap = await getDocs(collection(db, "contractors", user.uid, "workers"));
          const workerCount = workersSnap.size;
          
          // Check today's attendance for workers
          let presentToday = 0;
          let totalPending = 0;
          
          for (const wDoc of workersSnap.docs) {
            const wData = wDoc.data();
            const wAttQ = query(
              collection(db, "attendance"),
              where("user_id", "==", `worker_${wDoc.id}`),
            );
            const wAttSnap = await getDocs(wAttQ);
            let wDays = 0;
            let wAdvance = 0;
            
            wAttSnap.docs.forEach((ad) => {
              const aData = ad.data();
              if (aData.date === todayStr && aData.status === "present") presentToday++;
              if (aData.date >= startDate && aData.date <= endDate) {
                if (aData.status === "present") {
                  wDays += aData.type === "half" ? 0.5 : 1;
                }
                if (aData.advance_amount) wAdvance += aData.advance_amount;
              }
            });
            
            totalPending += Math.max(0, (wDays * (wData.wage || 0)) - wAdvance);
          }
          
          setContractorStats({ totalWorkers: workerCount, todayPresent: presentToday, pendingPayment: totalPending });
        } catch (cErr) {
          console.error("Error loading contractor stats:", cErr);
        }
      }
    } catch (err) {
      console.error("Error loading month data:", err);
    }
    setPageLoading(false);
  };

  const markAttendance = async (type: "full" | "half" = "full") => {
    if (!user || marked || loading) return;
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
        timestamp: serverTimestamp(),
      });
      setMarked(true);
      setTodayStatus("present");
      setTodayNote(note);
      setDaysWorked((d) => d + (type === "half" ? 0.5 : 1));
      setTotalOvertime((o) => o + overtimeHours);
      setNote("");
      setOvertimeHours(0);
    } catch (err) {
      console.error("Error marking attendance:", err);
    }
    setLoading(false);
  };

  const markAbsent = async () => {
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
      setLeaveDays((d) => d + 1);
      setShowAbsentDialog(false);
      setNote("");
    } catch (err) {
      console.error("Error marking absent:", err);
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
      await loadMonthData();
    } catch (err) {
      console.error("Error removing attendance:", err);
    }
    setLoading(false);
  };

  const saveNote = async () => {
    if (!user) return;
    try {
      const docId = `${user.uid}_${todayStr}`;
      await setDoc(doc(db, "attendance", docId), { note }, { merge: true });
      setTodayNote(note);
      setShowNoteDialog(false);
    } catch (err) {
      console.error("Error saving note:", err);
    }
  };

  const saveAdvance = async () => {
    if (!user || !advanceAmount) return;
    const amount = parseInt(advanceAmount, 10);
    if (isNaN(amount) || amount <= 0) return;
    setLoading(true);
    try {
      const docId = `${user.uid}_${advanceDate}_advance`;

      // Fetch existing advance to add to it instead of overwriting/duplicating
      const existingDoc = await getDocs(query(
        collection(db, "attendance"),
        where("user_id", "==", user.uid),
        where("date", "==", advanceDate),
        where("status", "==", "advance")
      ));

      let newAmount = amount;
      if (!existingDoc.empty) {
        newAmount += existingDoc.docs[0].data().advance_amount || 0;
      }

      await setDoc(doc(db, "attendance", docId), {
        user_id: user.uid,
        date: advanceDate,
        status: "advance",
        advance_amount: newAmount,
        note: "Advance Payment",
        timestamp: serverTimestamp(),
      });
      setAdvanceAmount("");
      setAdvanceDate(todayStr);
      setShowAdvanceDialog(false);
      await loadMonthData();
    } catch (err) {
      console.error("Error saving advance:", err);
    }
    setLoading(false);
  };

  const firstName = userData?.name?.split(" ")[0] || "";

  if (pageLoading) {
    return (
      <div className="min-h-screen bg-background pb-20">
        <div className="mx-auto max-w-lg px-4 pt-6">
          <div className="mb-6 space-y-2">
            <Skeleton className="h-4 w-24 rounded" />
            <Skeleton className="h-8 w-40 rounded" />
            <Skeleton className="h-4 w-48 rounded" />
          </div>

          <div className="mb-6 space-y-3">
            <Skeleton className="h-16 w-full rounded-xl" />
            <Skeleton className="h-20 w-full rounded-xl" />
            <div className="grid grid-cols-2 gap-3">
              <Skeleton className="h-32 w-full rounded-2xl" />
              <Skeleton className="h-32 w-full rounded-2xl" />
            </div>
            <Skeleton className="h-14 w-full rounded-2xl" />
          </div>

          <Skeleton className="h-14 w-full rounded-2xl mb-6" />

          <div className="grid grid-cols-2 gap-3 mb-3">
            <Skeleton className="h-28 w-full rounded-2xl" />
            <Skeleton className="h-28 w-full rounded-2xl" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Skeleton className="h-24 w-full rounded-2xl" />
            <Skeleton className="h-24 w-full rounded-2xl" />
          </div>
        </div>
        <BottomNav />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        {/* Header */}
        <div className="mb-6">
          <p className="text-muted-foreground font-medium text-sm">{t("welcome")},</p>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-foreground">{firstName}</h1>
            <Hand size={24} className="text-yellow-500 origin-bottom-right rotate-12" />
          </div>
          <p className="text-sm font-medium text-muted-foreground mt-1">
            {t("today")}: {today.getDate()} {monthNames[today.getMonth()]} {today.getFullYear()}
          </p>
        </div>

        {/* Mark Attendance Section */}
        {!marked ? (
          <div className="mb-6 space-y-3">
            {/* Overtime selector */}
            <div className="flex flex-col gap-2 rounded-xl bg-card border border-border px-4 py-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-bold text-foreground">{t("overtime")} (Hours)</span>
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

            {/* Note input */}
            <div className="rounded-xl bg-card border border-border px-4 py-3">
              <Textarea
                placeholder={t("addNote") + "..."}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="border-0 bg-transparent p-0 text-sm resize-none min-h-[40px] focus-visible:ring-0"
              />
            </div>

            {/* Main attendance buttons */}
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => markAttendance("full")}
                disabled={loading}
                className="rounded-2xl bg-primary text-primary-foreground py-6 flex flex-col items-center justify-center gap-2 transition-all active:scale-95 shadow-lg"
              >
                <div className="h-12 w-12 rounded-full bg-primary-foreground/20 flex items-center justify-center">
                  <Check size={28} strokeWidth={3} className="text-primary-foreground" />
                </div>
                <span className="text-sm font-bold">{t("fullDay")}</span>
              </button>
              <button
                onClick={() => markAttendance("half")}
                disabled={loading}
                className="rounded-2xl bg-accent text-accent-foreground py-6 flex flex-col items-center justify-center gap-2 transition-all active:scale-95 shadow-lg border border-border"
              >
                <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                  <Clock size={28} strokeWidth={2} className="text-primary" />
                </div>
                <span className="text-sm font-bold">{t("halfDay")}</span>
              </button>
            </div>

            {/* Mark Absent button */}
            <button
              onClick={() => setShowAbsentDialog(true)}
              className="w-full rounded-2xl border-2 border-destructive py-4 flex items-center justify-center gap-2 text-destructive font-bold text-sm active:scale-95"
            >
              <X size={20} />
              {t("markAbsent")}
            </button>
          </div>
        ) : (
          <div className="mb-6 space-y-3">
            {/* Status card */}
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
                {todayStatus === "present" ? t("marked") : t("absent")}
              </span>
              {todayNote && (
                <div className="flex items-center gap-1.5 px-4 text-center text-xs text-muted-foreground">
                  <StickyNote size={12} />
                  <span>{todayNote}</span>
                </div>
              )}
            </div>

            {/* Action buttons */}
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={removeAttendance}
                disabled={loading}
                className="rounded-xl border border-destructive py-3 text-destructive text-sm font-bold active:scale-95"
              >
                {t("removeAttendance")}
              </button>
              <button
                onClick={() => { setNote(todayNote); setShowNoteDialog(true); }}
                className="rounded-xl border border-border py-3 text-foreground text-sm font-bold active:scale-95"
              >
                {t("addNote")}
              </button>
            </div>
          </div>
        )}

        {/* Add Advance Payment Button */}
        <div className="mb-6">
          <button
            onClick={() => setShowAdvanceDialog(true)}
            className="w-full rounded-2xl border border-dashed border-primary bg-primary/5 py-4 flex items-center justify-center gap-2 text-primary font-bold text-sm active:scale-95"
          >
            <Plus size={20} />
            {t("addAdvancePayment")}
          </button>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-2 gap-3 mb-3">
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("totalDays")}</p>
            <p className="text-3xl font-bold text-foreground">{daysWorked}</p>
            <p className="text-[10px] font-medium text-muted-foreground mt-1">{t("thisMonth")}</p>
          </div>
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("earnings")}</p>
            <p className="text-3xl font-bold text-primary">
              ₹{(daysWorked * dailyWage).toLocaleString()}
            </p>
            <p className="text-[10px] font-medium text-muted-foreground mt-1">{t("thisMonth")}</p>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 mb-3">
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("totalAdvance")}</p>
            <p className="text-2xl font-bold text-orange-500">₹{monthlyAdvance.toLocaleString()}</p>
            <p className="text-[10px] font-medium text-muted-foreground mt-1">{t("thisMonth")}</p>
          </div>
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("netPayable")}</p>
            <p className="text-2xl font-bold text-green-600">
              ₹{Math.max(0, (daysWorked * dailyWage) - monthlyAdvance).toLocaleString()}
            </p>
            <p className="text-[10px] font-medium text-muted-foreground mt-1">{t("afterDeductions")}</p>
          </div>
        </div>

        {/* Recent Advance Payments */}
        {advanceRecords.length > 0 && (
          <div className="mb-3">
            <div className="flex items-center justify-between mb-2">
              <p className="text-xs font-bold text-foreground">Recent Advances</p>
              <span className="text-[10px] text-muted-foreground">{advanceRecords.length} records</span>
            </div>
            <div className="flex flex-col gap-1.5">
              {advanceRecords.slice(0, 5).map((adv, i) => {
                const [y, m, d] = adv.date.split("-");
                return (
                  <div key={i} className="flex items-center justify-between rounded-xl bg-card border border-border px-3 py-2.5">
                    <div className="flex items-center gap-2">
                      <div className="h-8 w-8 rounded-full bg-orange-500/10 flex items-center justify-center">
                        <IndianRupee size={14} className="text-orange-500" />
                      </div>
                      <div>
                        <p className="text-xs font-bold text-foreground">{adv.note}</p>
                        <p className="text-[10px] text-muted-foreground">{d}/{m}/{y}</p>
                      </div>
                    </div>
                    <p className="text-sm font-bold text-orange-500">₹{adv.amount.toLocaleString()}</p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Contractor Summary Cards */}
        {isContractor && (
          <div className="grid grid-cols-3 gap-2 mb-3">
            <div className="rounded-2xl bg-card p-3 border border-border text-center">
              <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-1">
                <Users size={14} className="text-primary" />
              </div>
              <p className="text-lg font-bold text-foreground">{contractorStats.totalWorkers}</p>
              <p className="text-[9px] text-muted-foreground font-medium">{t("totalWorkers")}</p>
            </div>
            <div className="rounded-2xl bg-card p-3 border border-border text-center">
              <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-1">
                <UserCheck size={14} className="text-primary" />
              </div>
              <p className="text-lg font-bold text-foreground">{contractorStats.todayPresent}</p>
              <p className="text-[9px] text-muted-foreground font-medium">{t("todayPresent")}</p>
            </div>
            <div className="rounded-2xl bg-card p-3 border border-border text-center">
              <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-1">
                <Wallet size={14} className="text-primary" />
              </div>
              <p className="text-lg font-bold text-foreground">₹{contractorStats.pendingPayment.toLocaleString()}</p>
              <p className="text-[9px] text-muted-foreground font-medium">{t("pendingPayment")}</p>
            </div>
          </div>
        )}
      </div>

      {/* Advance Dialog */}
      <Dialog open={showAdvanceDialog} onOpenChange={setShowAdvanceDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>{t("addAdvancePayment")}</DialogTitle>
            <DialogDescription>{t("addAdvancePayment")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3 mt-2">
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("selectDate")}</label>
              <input
                type="date"
                value={advanceDate}
                onChange={(e) => setAdvanceDate(e.target.value)}
                className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm font-medium text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
              <input
                type="number"
                value={advanceAmount}
                onChange={(e) => setAdvanceAmount(e.target.value)}
                placeholder="Enter amount"
                className="w-full rounded-xl border border-border bg-background px-8 py-3 text-lg font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowAdvanceDialog(false)}>{t("cancel")}</Button>
            <Button onClick={saveAdvance} disabled={loading || !advanceAmount}>{t("save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Absent Dialog */}
      <Dialog open={showAbsentDialog} onOpenChange={setShowAbsentDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>{t("markAbsent")}</DialogTitle>
            <DialogDescription>{t("reason")}</DialogDescription>
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
                  {t(r)}
                </button>
              ))}
            </div>
            <Textarea
              placeholder={t("addNote") + "..."}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="min-h-[60px]"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAbsentDialog(false)}>{t("cancel")}</Button>
            <Button variant="destructive" onClick={markAbsent} disabled={loading}>{t("confirm")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Note Dialog */}
      <Dialog open={showNoteDialog} onOpenChange={setShowNoteDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>{t("addNote")}</DialogTitle>
            <DialogDescription>{t("notes")}</DialogDescription>
          </DialogHeader>
          <Textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder={t("notes") + "..."}
            className="min-h-[80px]"
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowNoteDialog(false)}>{t("cancel")}</Button>
            <Button onClick={saveNote}>{t("save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <BottomNav />
    </div>
  );
};

export default Dashboard;
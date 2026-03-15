import { useState, useEffect, useRef } from "react";
import { Check, X, Clock, FileText, Minus, Plus } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import {
  collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp,
} from "firebase/firestore";
import BottomNav from "@/components/BottomNav";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

const Dashboard = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [marked, setMarked] = useState(false);
  const [todayStatus, setTodayStatus] = useState<string | null>(null);
  const [daysWorked, setDaysWorked] = useState(0);
  const [totalOvertime, setTotalOvertime] = useState(0);
  const [leaveDays, setLeaveDays] = useState(0);
  const [loading, setLoading] = useState(false);
  const [showAbsentDialog, setShowAbsentDialog] = useState(false);
  const [showNoteDialog, setShowNoteDialog] = useState(false);
  const [absenceReason, setAbsenceReason] = useState("sick");
  const [overtimeHours, setOvertimeHours] = useState(0);
  const [note, setNote] = useState("");
  const [todayNote, setTodayNote] = useState("");

  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];
  const dailyWage = userData?.daily_wage || 500;

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
    const year = today.getFullYear();
    const month = today.getMonth();
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    const q = query(
      collection(db, "attendance"),
      where("user_id", "==", user.uid),
      where("date", ">=", startDate),
      where("date", "<=", endDate)
    );

    const snap = await getDocs(q);
    let worked = 0;
    let ot = 0;
    let leaves = 0;

    snap.docs.forEach((d) => {
      const data = d.data();
      if (data.status === "present") {
        worked++;
        if (data.type === "half") worked -= 0.5;
        ot += data.overtime_hours || 0;
      } else if (data.status === "absent" || data.status === "leave") {
        leaves++;
      }
      if (data.date === todayStr) {
        setMarked(true);
        setTodayStatus(data.status);
        setTodayNote(data.note || "");
      }
    });

    setDaysWorked(worked);
    setTotalOvertime(ot);
    setLeaveDays(leaves);
  };

  const markAttendance = async (type: "full" | "half" = "full") => {
    if (!user || marked || loading) return;
    setLoading(true);
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
    setLoading(false);
    setNote("");
    setOvertimeHours(0);
  };

  const markAbsent = async () => {
    if (!user || loading) return;
    setLoading(true);
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
    setLoading(false);
    setShowAbsentDialog(false);
    setNote("");
  };

  const removeAttendance = async () => {
    if (!user || !marked || loading) return;
    setLoading(true);
    const docId = `${user.uid}_${todayStr}`;
    await deleteDoc(doc(db, "attendance", docId));
    setMarked(false);
    setTodayStatus(null);
    setTodayNote("");
    // Reload data
    await loadMonthData();
    setMarked(false);
    setTodayStatus(null);
    setLoading(false);
  };

  const saveNote = async () => {
    if (!user) return;
    const docId = `${user.uid}_${todayStr}`;
    await setDoc(doc(db, "attendance", docId), { note }, { merge: true });
    setTodayNote(note);
    setShowNoteDialog(false);
  };

  const firstName = userData?.name?.split(" ")[0] || "";

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        {/* Header */}
        <div className="mb-6">
          <p className="text-muted-foreground font-medium text-sm">{t("welcome")},</p>
          <h1 className="text-2xl font-bold text-foreground">{firstName} 👋</h1>
          <p className="text-sm font-medium text-muted-foreground mt-1">
            {t("today")}: {today.getDate()} {monthNames[today.getMonth()]} {today.getFullYear()}
          </p>
        </div>

        {/* Mark Attendance Section */}
        {!marked ? (
          <div className="mb-6 space-y-3">
            {/* Overtime selector */}
            <div className="flex items-center justify-between rounded-xl bg-card border border-border px-4 py-3">
              <span className="text-sm font-bold text-foreground">{t("overtime")}</span>
              <div className="flex items-center gap-3">
                <button onClick={() => setOvertimeHours(Math.max(0, overtimeHours - 1))} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90">
                  <Minus size={16} />
                </button>
                <span className="text-lg font-bold text-foreground w-6 text-center">{overtimeHours}</span>
                <button onClick={() => setOvertimeHours(overtimeHours + 1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90">
                  <Plus size={16} />
                </button>
              </div>
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
                <span className="text-xs text-muted-foreground px-4 text-center">📝 {todayNote}</span>
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
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("totalOvertime")}</p>
            <p className="text-2xl font-bold text-foreground">{totalOvertime} <span className="text-sm">{t("hours")}</span></p>
          </div>
          <div className="rounded-2xl bg-card p-4 border border-border">
            <p className="text-xs font-medium text-muted-foreground mb-1">{t("leaveDays")}</p>
            <p className="text-2xl font-bold text-destructive">{leaveDays}</p>
          </div>
        </div>
      </div>

      {/* Absent Dialog */}
      <Dialog open={showAbsentDialog} onOpenChange={setShowAbsentDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>{t("markAbsent")}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-sm font-medium text-muted-foreground">{t("reason")}</p>
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

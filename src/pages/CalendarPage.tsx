import { useState, useEffect } from "react";
import { ChevronLeft, ChevronRight, Hand } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useAppData } from "@/contexts/AppDataContext";
import { useLanguage } from "@/contexts/LanguageContext";
import BottomNav from "@/components/BottomNav";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

interface DayData {
  id?: string;
  status: string;
  type?: string;
  reason?: string;
  overtime_hours?: number;
  note?: string;
  advance_amount?: number;
  adv_id?: string;
}

const CalendarPage = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const { personalAttendance, personalPayments, addPersonalAttendance, updatePersonalAttendance, deletePersonalAttendance, addPersonalPayment, updatePersonalPayment, deletePersonalPayment } = useAppData() as any;

  const [currentDate, setCurrentDate] = useState(new Date());
  const [dayMap, setDayMap] = useState<Record<number, DayData>>({});
  const [selectedDay, setSelectedDay] = useState<number | null>(null);

  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editStatus, setEditStatus] = useState("Present");
  const [editReason, setEditReason] = useState("sick");
  const [editNote, setEditNote] = useState("");
  const [editOT, setEditOT] = useState(0);
  const [editAdvance, setEditAdvance] = useState(0);

  const currentRole = userData?.role || localStorage.getItem("workday_role");

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];
  const dayLabels = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];
  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  useEffect(() => {
    if (!user || currentRole === "contractor") return;
    loadMonth();
  }, [user, month, year, personalAttendance, personalPayments]);

  const loadMonth = () => {
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    const map: Record<number, DayData> = {};

    personalAttendance.forEach((a: any) => {
      if (a.date < startDate || a.date > endDate) return;
      const day = parseInt(a.date.split("-")[2], 10);
      map[day] = {
        id: a.id,
        status: a.status,
        type: a.status === "Half Day" ? "half" : "full",
        reason: a.reason,
        overtime_hours: a.overtimeHours,
        note: a.note
      };
    });

    personalPayments.forEach((p: any) => {
      if (p.date < startDate || p.date > endDate || p.type !== "Advance") return;
      const day = parseInt(p.date.split("-")[2], 10);
      if (map[day]) {
        map[day].advance_amount = (map[day].advance_amount || 0) + p.amount;
        map[day].adv_id = p.id;
      } else {
        map[day] = { status: "advance", advance_amount: p.amount, adv_id: p.id };
      }
    });

    setDayMap(map);
  };

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const todayDate = new Date();
  const isCurrentMonth = todayDate.getFullYear() === year && todayDate.getMonth() === month;

  const handleDayClick = (day: number) => {
    const data = dayMap[day];
    setSelectedDay(day);
    if (data) {
      setEditStatus(data.status === "advance" ? "Present" : data.status);
      setEditReason(data.reason || "sick");
      setEditNote(data.note || "");
      setEditOT(data.overtime_hours || 0);
      setEditAdvance(data.advance_amount || 0);
    } else {
      setEditStatus("Present");
      setEditReason("sick");
      setEditNote("");
      setEditOT(0);
      setEditAdvance(0);
    }
    setShowEditDialog(true);
  };

  const saveDay = () => {
    if (!user || selectedDay === null) return;
    const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(selectedDay).padStart(2, "0")}`;
    const data = dayMap[selectedDay];

    if (editStatus !== "advance") {
      const attData = {
        date: dateStr,
        status: editStatus,
        dailyWage: userData?.daily_wage || 500,
        overtimeHours: editStatus === "Present" || editStatus === "Half Day" ? editOT : 0,
        reason: editStatus === "Absent" ? editReason : undefined,
        note: editNote
      };

      if (data?.id) {
        updatePersonalAttendance(data.id, attData);
      } else {
        addPersonalAttendance(attData);
      }
    }

    if (editAdvance > 0) {
      const payData = {
        amount: editAdvance,
        type: "Advance",
        date: dateStr,
        note: "Advance Payment (Calendar)"
      };
      if (data?.adv_id) {
        // Find existing to update (we didn't expose updatePayment, so we delete and re-add or ignore for now, assuming addPayment works)
        deletePersonalPayment(data.adv_id);
        addPersonalPayment(payData);
      } else {
        addPersonalPayment(payData);
      }
    } else if (data?.adv_id) {
      deletePersonalPayment(data.adv_id);
    }

    setShowEditDialog(false);
  };

  const deleteDay = () => {
    if (!user || selectedDay === null) return;
    const data = dayMap[selectedDay];
    if (data?.id) deletePersonalAttendance(data.id);
    if (data?.adv_id) deletePersonalPayment(data.adv_id);
    setShowEditDialog(false);
  };

  if (currentRole === "contractor") {
    return (
      <div className="min-h-screen bg-background pb-20 pt-6 px-4 text-center">
        <h1 className="text-xl font-bold mb-4">Calendar</h1>
        <p className="text-muted-foreground bg-card p-6 rounded-2xl border border-border">
          Please use the <b>Contractor Dashboard</b> to view and manage worker attendance and payments per contractor.
        </p>
        <BottomNav />
      </div>
    );
  }

  const presentCount = Object.values(dayMap).filter((d) => d.status === "Present" || d.status === "Half Day").length;
  const absentCount = Object.values(dayMap).filter((d) => d.status === "Absent").length;

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-4">{t("calendar")}</h1>

        {/* Month Selector */}
        <div className="flex items-center justify-between mb-4 bg-card border border-border rounded-xl p-2">
          <button onClick={prevMonth} className="touch-target p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} />
          </button>
          <h2 className="text-sm font-bold text-foreground">
            {monthNames[month]} {year}
          </h2>
          <button onClick={nextMonth} className="touch-target p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} />
          </button>
        </div>

        {/* Month stats */}
        <div className="flex gap-3 mb-3">
          <div className="flex-1 rounded-xl bg-card border border-border py-3 text-center">
            <span className="text-xl font-bold text-primary">{presentCount}</span>
            <p className="text-[10px] mt-1 font-medium text-muted-foreground">{t("present")}</p>
          </div>
          <div className="flex-1 rounded-xl bg-card border border-border py-3 text-center">
            <span className="text-xl font-bold text-destructive">{absentCount}</span>
            <p className="text-[10px] mt-1 font-medium text-muted-foreground">{t("absent")}</p>
          </div>
        </div>

        <p className="text-[10px] text-center text-muted-foreground mb-4">{t("tapToEdit")}</p>

        <div className="bg-card border border-border p-4 rounded-2xl">
          {/* Day Labels */}
          <div className="grid grid-cols-7 gap-1 mb-2">
            {dayLabels.map((d) => (
              <div key={d} className="text-center text-[10px] font-bold text-muted-foreground py-1">
                {d}
              </div>
            ))}
          </div>

          {/* Calendar Grid */}
          <div className="grid grid-cols-7 gap-2">
            {Array.from({ length: firstDayOfWeek }).map((_, i) => (
              <div key={`empty-${i}`} />
            ))}
            {Array.from({ length: daysInMonth }, (_, i) => {
              const day = i + 1;
              const data = dayMap[day];
              const isPresent = data?.status === "Present";
              const isAbsent = data?.status === "Absent";
              const isHalf = data?.status === "Half Day";
              const hasAdvance = data?.advance_amount && data.advance_amount > 0;
              const isToday = isCurrentMonth && day === todayDate.getDate();
              return (
                <button
                  key={day}
                  onClick={() => handleDayClick(day)}
                  className={`aspect-square flex flex-col items-center justify-center rounded-xl text-sm font-bold transition-all relative active:scale-90 ${
                    isPresent
                      ? "bg-green-500/10 text-green-600 border border-green-500/20"
                      : isAbsent
                      ? "bg-destructive/10 text-destructive border border-destructive/20"
                      : isHalf
                      ? "bg-orange-500/10 text-orange-600 border border-orange-500/20"
                      : isToday
                      ? "border-2 border-primary text-foreground"
                      : "text-foreground/80 hover:bg-muted"
                  }`}
                >
                  {day}
                  {hasAdvance && (
                    <div className="absolute bottom-1 w-1.5 h-1.5 rounded-full bg-orange-500" />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Legend */}
        <div className="flex gap-4 mt-6 justify-center bg-card p-3 rounded-xl border border-border">
          <div className="flex items-center gap-1.5">
            <div className="h-2 w-2 rounded-full bg-green-500" />
            <span className="text-[10px] font-bold text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-2 w-2 rounded-full bg-orange-500" />
            <span className="text-[10px] font-bold text-muted-foreground">{t("halfDay")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-2 w-2 rounded-full bg-destructive" />
            <span className="text-[10px] font-bold text-muted-foreground">{t("absent")}</span>
          </div>
        </div>
      </div>

      {/* Edit Dialog */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedDay && `${selectedDay} ${monthNames[month]} ${year}`}
            </DialogTitle>
            <DialogDescription>{t("tapToEdit")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            {/* Status toggle */}
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => setEditStatus("Present")}
                className={`rounded-xl py-3 text-xs font-bold transition-all active:scale-95 ${
                  editStatus === "Present" ? "bg-green-500 text-white" : "bg-muted text-foreground border border-border"
                }`}
              >
                {t("present")}
              </button>
              <button
                onClick={() => setEditStatus("Half Day")}
                className={`rounded-xl py-3 text-xs font-bold transition-all active:scale-95 ${
                  editStatus === "Half Day" ? "bg-orange-500 text-white" : "bg-muted text-foreground border border-border"
                }`}
              >
                {t("halfDay")}
              </button>
              <button
                onClick={() => setEditStatus("Absent")}
                className={`rounded-xl py-3 text-xs font-bold transition-all active:scale-95 ${
                  editStatus === "Absent" ? "bg-destructive text-destructive-foreground" : "bg-muted text-foreground border border-border"
                }`}
              >
                {t("absent")}
              </button>
            </div>

            {(editStatus === "Present" || editStatus === "Half Day") && (
              <div className="flex items-center justify-between bg-muted/50 p-3 rounded-xl border border-border">
                <span className="text-sm font-bold text-foreground">{t("overtime")} (Hrs)</span>
                <div className="flex items-center gap-3">
                  <button onClick={() => setEditOT(Math.max(0, editOT - 1))} className="h-8 w-8 rounded-full bg-background border border-border flex items-center justify-center text-sm font-bold">-</button>
                  <span className="font-bold w-4 text-center">{editOT}</span>
                  <button onClick={() => setEditOT(editOT + 1)} className="h-8 w-8 rounded-full bg-background border border-border flex items-center justify-center text-sm font-bold">+</button>
                </div>
              </div>
            )}

            {editStatus === "Absent" && (
              <div className="grid grid-cols-3 gap-2">
                {absenceReasons.map((r) => (
                  <button
                    key={r}
                    onClick={() => setEditReason(r)}
                    className={`rounded-xl px-2 py-2 text-[10px] font-bold border ${
                      editReason === r ? "bg-destructive text-destructive-foreground border-destructive" : "bg-muted border-border"
                    }`}
                  >
                    {t(r)}
                  </button>
                ))}
              </div>
            )}

            <div className="flex flex-col gap-1.5 pt-2">
              <span className="text-xs font-bold text-muted-foreground block mb-1">Advance Payment (₹)</span>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                <input
                  type="number"
                  value={editAdvance || ""}
                  onChange={(e) => setEditAdvance(Math.max(0, parseInt(e.target.value) || 0))}
                  placeholder="0"
                  className="w-full rounded-xl border border-border bg-background px-9 py-3 text-sm font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>

            <div>
              <span className="text-xs font-bold text-muted-foreground block mb-2">Note</span>
              <Textarea
                value={editNote}
                onChange={(e) => setEditNote(e.target.value)}
                placeholder={t("addNote") + "..."}
                className="min-h-[60px] rounded-xl text-sm"
              />
            </div>
          </div>
          <DialogFooter className="flex gap-2 mt-4">
            {dayMap[selectedDay!] && (
              <Button variant="outline" className="flex-1 rounded-xl text-destructive hover:bg-destructive/10" onClick={deleteDay}>
                Clear
              </Button>
            )}
            <Button onClick={saveDay} className="flex-1 rounded-xl">{t("save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <BottomNav />
    </div>
  );
};

export default CalendarPage;

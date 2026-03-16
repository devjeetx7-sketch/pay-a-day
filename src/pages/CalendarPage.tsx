import { useState, useEffect } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp } from "firebase/firestore";
import BottomNav from "@/components/BottomNav";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

interface DayData {
  status: string;
  type?: string;
  reason?: string;
  overtime_hours?: number;
  note?: string;
}

const CalendarPage = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [dayMap, setDayMap] = useState<Record<number, DayData>>({});
  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editStatus, setEditStatus] = useState("present");
  const [editType, setEditType] = useState("full");
  const [editReason, setEditReason] = useState("sick");
  const [editNote, setEditNote] = useState("");
  const [editOT, setEditOT] = useState(0);

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
    if (!user) return;
    loadMonth();
  }, [user, month, year]);

  const loadMonth = async () => {
    if (!user) return;
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    try {
      // Simple query: only filter by user_id
      const q = query(
        collection(db, "attendance"),
        where("user_id", "==", user.uid)
      );

      const snap = await getDocs(q);
      const map: Record<number, DayData> = {};
      snap.docs.forEach((d) => {
        const data = d.data();
        // Client-side date filtering
        if (data.date < startDate || data.date > endDate) return;
        const day = parseInt(data.date.split("-")[2], 10);
        map[day] = {
          status: data.status,
          type: data.type,
          reason: data.reason,
          overtime_hours: data.overtime_hours,
          note: data.note,
        };
      });
      setDayMap(map);
    } catch (err) {
      console.error("Error loading calendar:", err);
    }
  };

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const todayDate = new Date();
  const isCurrentMonth = todayDate.getFullYear() === year && todayDate.getMonth() === month;

  const handleDayClick = (day: number) => {
    const data = dayMap[day];
    setSelectedDay(day);
    if (data) {
      setEditStatus(data.status);
      setEditType(data.type || "full");
      setEditReason(data.reason || "sick");
      setEditNote(data.note || "");
      setEditOT(data.overtime_hours || 0);
    } else {
      setEditStatus("present");
      setEditType("full");
      setEditReason("sick");
      setEditNote("");
      setEditOT(0);
    }
    setShowEditDialog(true);
  };

  const saveDay = async () => {
    if (!user || selectedDay === null) return;
    try {
      const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(selectedDay).padStart(2, "0")}`;
      const docId = `${user.uid}_${dateStr}`;
      await setDoc(doc(db, "attendance", docId), {
        user_id: user.uid,
        date: dateStr,
        status: editStatus,
        type: editStatus === "present" ? editType : null,
        reason: editStatus === "absent" ? editReason : null,
        overtime_hours: editStatus === "present" ? editOT : 0,
        note: editNote || null,
        timestamp: serverTimestamp(),
      });
      setShowEditDialog(false);
      loadMonth();
    } catch (err) {
      console.error("Error saving day:", err);
    }
  };

  const deleteDay = async () => {
    if (!user || selectedDay === null) return;
    try {
      const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(selectedDay).padStart(2, "0")}`;
      const docId = `${user.uid}_${dateStr}`;
      await deleteDoc(doc(db, "attendance", docId));
      setShowEditDialog(false);
      loadMonth();
    } catch (err) {
      console.error("Error deleting day:", err);
    }
  };

  const presentCount = Object.values(dayMap).filter((d) => d.status === "present").length;
  const absentCount = Object.values(dayMap).filter((d) => d.status === "absent").length;

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-4">{t("calendar")}</h1>

        {/* Month Selector */}
        <div className="flex items-center justify-between mb-4">
          <button onClick={prevMonth} className="touch-target p-2 rounded-lg active:bg-card">
            <ChevronLeft size={24} />
          </button>
          <h2 className="text-lg font-bold text-foreground">
            {monthNames[month]} {year}
          </h2>
          <button onClick={nextMonth} className="touch-target p-2 rounded-lg active:bg-card">
            <ChevronRight size={24} />
          </button>
        </div>

        {/* Month stats */}
        <div className="flex gap-3 mb-3">
          <div className="flex-1 rounded-xl bg-primary/10 py-2 text-center">
            <span className="text-lg font-bold text-primary">{presentCount}</span>
            <span className="text-[10px] ml-1 text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex-1 rounded-xl bg-destructive/10 py-2 text-center">
            <span className="text-lg font-bold text-destructive">{absentCount}</span>
            <span className="text-[10px] ml-1 text-muted-foreground">{t("absent")}</span>
          </div>
        </div>

        <p className="text-[10px] text-center text-muted-foreground mb-2">{t("tapToEdit")}</p>

        {/* Day Labels */}
        <div className="grid grid-cols-7 gap-1 mb-2">
          {dayLabels.map((d) => (
            <div key={d} className="text-center text-xs font-semibold text-muted-foreground py-1">
              {d}
            </div>
          ))}
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: firstDayOfWeek }).map((_, i) => (
            <div key={`empty-${i}`} />
          ))}
          {Array.from({ length: daysInMonth }, (_, i) => {
            const day = i + 1;
            const data = dayMap[day];
            const isPresent = data?.status === "present";
            const isAbsent = data?.status === "absent";
            const isHalf = data?.type === "half";
            const isToday = isCurrentMonth && day === todayDate.getDate();
            return (
              <button
                key={day}
                onClick={() => handleDayClick(day)}
                className={`aspect-square flex flex-col items-center justify-center rounded-full text-sm font-bold transition-colors relative active:scale-90 ${
                  isPresent
                    ? "bg-primary text-primary-foreground"
                    : isAbsent
                    ? "bg-destructive text-destructive-foreground"
                    : isToday
                    ? "border-2 border-primary text-foreground"
                    : "text-foreground/60"
                }`}
              >
                {day}
                {isHalf && (
                  <div className="absolute -bottom-0.5 w-1.5 h-1.5 rounded-full bg-accent-foreground" />
                )}
              </button>
            );
          })}
        </div>

        {/* Legend */}
        <div className="flex gap-4 mt-4 justify-center">
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full bg-primary" />
            <span className="text-[10px] font-medium text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full bg-destructive" />
            <span className="text-[10px] font-medium text-muted-foreground">{t("absent")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full border-2 border-border" />
            <span className="text-[10px] font-medium text-muted-foreground">—</span>
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
          <div className="space-y-3">
            {/* Status toggle */}
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => setEditStatus("present")}
                className={`rounded-xl py-3 text-sm font-bold transition-all active:scale-95 ${
                  editStatus === "present" ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"
                }`}
              >
                {t("present")}
              </button>
              <button
                onClick={() => setEditStatus("absent")}
                className={`rounded-xl py-3 text-sm font-bold transition-all active:scale-95 ${
                  editStatus === "absent" ? "bg-destructive text-destructive-foreground" : "bg-muted text-foreground"
                }`}
              >
                {t("absent")}
              </button>
            </div>

            {editStatus === "present" && (
              <>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => setEditType("full")}
                    className={`rounded-xl py-2 text-xs font-bold ${editType === "full" ? "bg-accent border-2 border-primary" : "bg-muted"}`}
                  >
                    {t("fullDay")}
                  </button>
                  <button
                    onClick={() => setEditType("half")}
                    className={`rounded-xl py-2 text-xs font-bold ${editType === "half" ? "bg-accent border-2 border-primary" : "bg-muted"}`}
                  >
                    {t("halfDay")}
                  </button>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">{t("overtime")}</span>
                  <div className="flex items-center gap-2">
                    <button onClick={() => setEditOT(Math.max(0, editOT - 1))} className="h-7 w-7 rounded-full bg-muted flex items-center justify-center text-sm font-bold">-</button>
                    <span className="font-bold w-4 text-center">{editOT}</span>
                    <button onClick={() => setEditOT(editOT + 1)} className="h-7 w-7 rounded-full bg-muted flex items-center justify-center text-sm font-bold">+</button>
                  </div>
                </div>
              </>
            )}

            {editStatus === "absent" && (
              <div className="grid grid-cols-2 gap-2">
                {absenceReasons.map((r) => (
                  <button
                    key={r}
                    onClick={() => setEditReason(r)}
                    className={`rounded-xl px-2 py-2 text-xs font-semibold ${
                      editReason === r ? "bg-destructive text-destructive-foreground" : "bg-muted"
                    }`}
                  >
                    {t(r)}
                  </button>
                ))}
              </div>
            )}

            <Textarea
              value={editNote}
              onChange={(e) => setEditNote(e.target.value)}
              placeholder={t("addNote") + "..."}
              className="min-h-[50px]"
            />
          </div>
          <DialogFooter className="flex gap-2">
            {dayMap[selectedDay!] && (
              <Button variant="destructive" size="sm" onClick={deleteDay}>
                {t("removeAttendance")}
              </Button>
            )}
            <Button onClick={saveDay}>{t("save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <BottomNav />
    </div>
  );
};

export default CalendarPage;

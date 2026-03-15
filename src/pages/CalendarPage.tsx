import { useState, useEffect } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import BottomNav from "@/components/BottomNav";

const CalendarPage = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [presentDays, setPresentDays] = useState<Set<number>>(new Set());

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];
  const dayLabels = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

  useEffect(() => {
    if (!user) return;
    loadMonth();
  }, [user, month, year]);

  const loadMonth = async () => {
    if (!user) return;
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    const q = query(
      collection(db, "attendance"),
      where("user_id", "==", user.uid),
      where("date", ">=", startDate),
      where("date", "<=", endDate),
      where("status", "==", "present")
    );

    const snap = await getDocs(q);
    const days = new Set<number>();
    snap.docs.forEach((d) => {
      const day = parseInt(d.data().date.split("-")[2], 10);
      days.add(day);
    });
    setPresentDays(days);
  };

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const todayDate = new Date();
  const isCurrentMonth = todayDate.getFullYear() === year && todayDate.getMonth() === month;

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
            const isPresent = presentDays.has(day);
            const isToday = isCurrentMonth && day === todayDate.getDate();
            return (
              <div
                key={day}
                className={`aspect-square flex items-center justify-center rounded-full text-sm font-bold transition-colors ${
                  isPresent
                    ? "bg-primary text-primary-foreground"
                    : isToday
                    ? "border-2 border-primary text-foreground"
                    : "text-foreground/60"
                }`}
              >
                {day}
              </div>
            );
          })}
        </div>

        {/* Legend */}
        <div className="flex gap-4 mt-4 justify-center">
          <div className="flex items-center gap-2">
            <div className="h-3 w-3 rounded-full bg-primary" />
            <span className="text-xs font-medium text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-3 w-3 rounded-full border-2 border-border" />
            <span className="text-xs font-medium text-muted-foreground">{t("absent")}</span>
          </div>
        </div>
      </div>
      <BottomNav />
    </div>
  );
};

export default CalendarPage;

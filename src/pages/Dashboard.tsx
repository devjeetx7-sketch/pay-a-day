import { useState, useEffect } from "react";
import { Check } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import {
  collection,
  query,
  where,
  getDocs,
  doc,
  setDoc,
  serverTimestamp,
} from "firebase/firestore";
import BottomNav from "@/components/BottomNav";

const Dashboard = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [marked, setMarked] = useState(false);
  const [daysWorked, setDaysWorked] = useState(0);
  const [loading, setLoading] = useState(false);

  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];
  const dailyWage = userData?.daily_wage || 500;

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

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
      where("date", "<=", endDate),
      where("status", "==", "present")
    );

    const snap = await getDocs(q);
    setDaysWorked(snap.size);

    const todayExists = snap.docs.some((d) => d.data().date === todayStr);
    setMarked(todayExists);
  };

  const markAttendance = async () => {
    if (!user || marked || loading) return;
    setLoading(true);
    const docId = `${user.uid}_${todayStr}`;
    await setDoc(doc(db, "attendance", docId), {
      user_id: user.uid,
      date: todayStr,
      status: "present",
      timestamp: serverTimestamp(),
    });
    setMarked(true);
    setDaysWorked((d) => d + 1);
    setLoading(false);
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

        {/* Mark Attendance Button */}
        <button
          onClick={markAttendance}
          disabled={marked || loading}
          className={`w-full rounded-2xl py-8 flex flex-col items-center justify-center gap-3 transition-all active:scale-95 shadow-lg mb-6 ${
            marked
              ? "bg-primary/10 border-2 border-primary"
              : "bg-primary text-primary-foreground"
          }`}
        >
          <div
            className={`h-16 w-16 rounded-full flex items-center justify-center ${
              marked ? "bg-primary" : "bg-primary-foreground/20"
            }`}
          >
            <Check
              size={36}
              strokeWidth={3}
              className={marked ? "text-primary-foreground" : "text-primary-foreground"}
            />
          </div>
          <span className={`text-xl font-bold ${marked ? "text-primary" : ""}`}>
            {marked ? t("marked") : t("markAttendance")}
          </span>
        </button>

        {/* Summary Cards */}
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-2xl bg-card p-5 border border-border">
            <p className="text-sm font-medium text-muted-foreground mb-1">
              {t("totalDays")}
            </p>
            <p className="text-3xl font-bold text-foreground">{daysWorked}</p>
            <p className="text-xs font-medium text-muted-foreground mt-1">
              {t("thisMonth")}
            </p>
          </div>
          <div className="rounded-2xl bg-card p-5 border border-border">
            <p className="text-sm font-medium text-muted-foreground mb-1">
              {t("earnings")}
            </p>
            <p className="text-3xl font-bold text-primary">
              ₹{(daysWorked * dailyWage).toLocaleString()}
            </p>
            <p className="text-xs font-medium text-muted-foreground mt-1">
              {t("thisMonth")}
            </p>
          </div>
        </div>
      </div>
      <BottomNav />
    </div>
  );
};

export default Dashboard;

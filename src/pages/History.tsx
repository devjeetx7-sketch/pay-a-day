import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, orderBy, getDocs } from "firebase/firestore";
import BottomNav from "@/components/BottomNav";

interface AttendanceRecord {
  date: string;
  status: string;
}

const History = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [records, setRecords] = useState<AttendanceRecord[]>([]);

  useEffect(() => {
    if (!user) return;
    loadHistory();
  }, [user]);

  const loadHistory = async () => {
    if (!user) return;
    const q = query(
      collection(db, "attendance"),
      where("user_id", "==", user.uid),
      orderBy("date", "desc")
    );
    const snap = await getDocs(q);
    setRecords(snap.docs.map((d) => d.data() as AttendanceRecord));
  };

  const formatDate = (dateStr: string) => {
    const [y, m, d] = dateStr.split("-");
    return `${d}/${m}/${y}`;
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-4">{t("history")}</h1>

        {records.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16">
            <p className="text-muted-foreground font-medium">{t("noRecords")}</p>
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {records.map((r) => (
              <div
                key={r.date}
                className="flex items-center justify-between rounded-xl bg-card border border-border px-4 py-3"
              >
                <span className="text-sm font-bold text-foreground">
                  {formatDate(r.date)}
                </span>
                <span
                  className={`rounded-full px-3 py-1 text-xs font-bold ${
                    r.status === "present"
                      ? "bg-primary/10 text-primary"
                      : "bg-destructive/10 text-destructive"
                  }`}
                >
                  {r.status === "present" ? t("present") : t("absent")}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
      <BottomNav />
    </div>
  );
};

export default History;

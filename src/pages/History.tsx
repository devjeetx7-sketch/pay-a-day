import { useState, useEffect, useRef } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { FileImage, FileText, FileSpreadsheet } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Button } from "@/components/ui/button";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";

interface AttendanceRecord {
  date: string;
  status: string;
  reason?: string;
  type?: string;
  overtime_hours?: number;
  note?: string;
}

const History = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const tableRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user) return;
    loadHistory();
  }, [user]);

  const loadHistory = async () => {
    if (!user) return;
    try {
      const q = query(
        collection(db, "attendance"),
        where("user_id", "==", user.uid)
      );
      const snap = await getDocs(q);
      const data = snap.docs
        .map((d) => d.data() as AttendanceRecord)
        .sort((a, b) => b.date.localeCompare(a.date));
      setRecords(data);
    } catch (err) {
      console.error("Error loading history:", err);
    }
  };

  const formatDate = (dateStr: string) => {
    const [y, m, d] = dateStr.split("-");
    return `${d}/${m}/${y}`;
  };

  const exportCSV = () => {
    const header = "Date,Status,Type,Overtime,Reason,Note\n";
    const rows = records.map((r) =>
      `${r.date},${r.status},${r.type || ""},${r.overtime_hours || 0},${r.reason || ""},${(r.note || "").replace(/,/g, ";")}`
    ).join("\n");
    const blob = new Blob([header + rows], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `attendance_${userData?.name || "report"}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportPDF = async () => {
    if (!tableRef.current) return;
    try {
      const canvas = await html2canvas(tableRef.current, { scale: 2 });
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF("p", "mm", "a4");
      const pdfW = pdf.internal.pageSize.getWidth();
      const pdfH = (canvas.height * pdfW) / canvas.width;
      pdf.setFontSize(16);
      pdf.text(`${userData?.name || ""} - Attendance Report`, 14, 15);
      pdf.addImage(imgData, "PNG", 0, 25, pdfW, pdfH);
      pdf.save(`attendance_${userData?.name || "report"}.pdf`);
    } catch (err) {
      console.error("Error exporting PDF:", err);
    }
  };

  const exportImage = async () => {
    if (!tableRef.current) return;
    try {
      const canvas = await html2canvas(tableRef.current, { scale: 2 });
      const url = canvas.toDataURL("image/png");
      const a = document.createElement("a");
      a.href = url;
      a.download = `attendance_${userData?.name || "report"}.png`;
      a.click();
    } catch (err) {
      console.error("Error exporting image:", err);
    }
  };

  const totalPresent = records.filter((r) => r.status === "present").length;
  const totalAbsent = records.filter((r) => r.status === "absent").length;
  const totalOT = records.reduce((sum, r) => sum + (r.overtime_hours || 0), 0);

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-xl font-bold text-foreground">{t("history")}</h1>
        </div>

        {/* Export buttons */}
        <div className="flex gap-2 mb-4">
          <Button variant="outline" size="sm" onClick={exportPDF} className="flex-1 gap-1">
            <FileText size={14} /> PDF
          </Button>
          <Button variant="outline" size="sm" onClick={exportImage} className="flex-1 gap-1">
            <FileImage size={14} /> JPG
          </Button>
          <Button variant="outline" size="sm" onClick={exportCSV} className="flex-1 gap-1">
            <FileSpreadsheet size={14} /> CSV
          </Button>
        </div>

        {/* Quick stats */}
        <div className="grid grid-cols-3 gap-2 mb-4">
          <div className="rounded-xl bg-primary/10 p-3 text-center">
            <p className="text-xl font-bold text-primary">{totalPresent}</p>
            <p className="text-[10px] font-medium text-muted-foreground">{t("present")}</p>
          </div>
          <div className="rounded-xl bg-destructive/10 p-3 text-center">
            <p className="text-xl font-bold text-destructive">{totalAbsent}</p>
            <p className="text-[10px] font-medium text-muted-foreground">{t("absent")}</p>
          </div>
          <div className="rounded-xl bg-accent p-3 text-center">
            <p className="text-xl font-bold text-foreground">{totalOT}h</p>
            <p className="text-[10px] font-medium text-muted-foreground">{t("overtime")}</p>
          </div>
        </div>

        {/* Records */}
        <div ref={tableRef}>
          {records.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16">
              <p className="text-muted-foreground font-medium">{t("noRecords")}</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {records.map((r) => (
                <div
                  key={r.date}
                  className="rounded-xl bg-card border border-border px-4 py-3"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-bold text-foreground">
                        {formatDate(r.date)}
                      </span>
                      {r.type === "half" && (
                        <span className="ml-2 text-[10px] bg-accent px-2 py-0.5 rounded-full font-medium">{t("halfDay")}</span>
                      )}
                      {(r.overtime_hours || 0) > 0 && (
                        <span className="ml-1 text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full font-medium">
                          +{r.overtime_hours}h OT
                        </span>
                      )}
                    </div>
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
                  {(r.reason || r.note) && (
                    <div className="mt-1.5 flex flex-wrap gap-1">
                      {r.reason && (
                        <span className="text-[10px] bg-destructive/5 text-destructive px-2 py-0.5 rounded-full">
                          {t(r.reason)}
                        </span>
                      )}
                      {r.note && (
                        <span className="text-[10px] text-muted-foreground">📝 {r.note}</span>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
      <BottomNav />
    </div>
  );
};

export default History;

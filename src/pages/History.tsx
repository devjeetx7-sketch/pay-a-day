import { useState, useEffect, useRef } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { FileImage, FileText, FileSpreadsheet, ChevronLeft, ChevronRight, Search } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  const [filteredRecords, setFilteredRecords] = useState<AttendanceRecord[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [exporting, setExporting] = useState(false);
  const tableRef = useRef<HTMLDivElement>(null);

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  useEffect(() => {
    if (!user) return;
    loadHistory();
  }, [user, selectedMonth]);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredRecords(records);
    } else {
      const q = searchQuery.toLowerCase();
      setFilteredRecords(
        records.filter(
          (r) =>
            r.date.includes(q) ||
            r.status.includes(q) ||
            (r.note && r.note.toLowerCase().includes(q)) ||
            (r.reason && t(r.reason).toLowerCase().includes(q))
        )
      );
    }
  }, [searchQuery, records]);

  const loadHistory = async () => {
    if (!user) return;
    const year = selectedMonth.getFullYear();
    const month = selectedMonth.getMonth();
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    try {
      const q = query(
        collection(db, "attendance"),
        where("user_id", "==", user.uid)
      );
      const snap = await getDocs(q);
      const data = snap.docs
        .map((d) => d.data() as AttendanceRecord)
        .filter((r) => r.date >= startDate && r.date <= endDate)
        .sort((a, b) => b.date.localeCompare(a.date));
      setRecords(data);
      setFilteredRecords(data);
    } catch (err) {
      console.error("Error loading history:", err);
    }
  };

  const formatDate = (dateStr: string) => {
    const [y, m, d] = dateStr.split("-");
    return `${d}/${m}/${y}`;
  };

  const getDayName = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString("en", { weekday: "short" });
  };

  const prevMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1));

  const exportCSV = () => {
    const header = "Date,Day,Status,Type,Overtime,Reason,Note\n";
    const rows = records
      .map(
        (r) =>
          `${r.date},${getDayName(r.date)},${r.status},${r.type || ""},${r.overtime_hours || 0},${r.reason || ""},${(r.note || "").replace(/,/g, ";")}`
      )
      .join("\n");
    const blob = new Blob([header + rows], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `attendance_${monthNames[selectedMonth.getMonth()]}_${selectedMonth.getFullYear()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportPDF = async () => {
    if (!tableRef.current || exporting) return;
    setExporting(true);
    try {
      const canvas = await html2canvas(tableRef.current, {
        scale: 2,
        backgroundColor: "#ffffff",
        useCORS: true,
      });
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF("p", "mm", "a4");
      const pdfW = pdf.internal.pageSize.getWidth();
      const pdfH = (canvas.height * pdfW) / canvas.width;

      pdf.setFontSize(18);
      pdf.setTextColor(33, 33, 33);
      pdf.text(`${userData?.name || "User"} - Attendance Report`, 14, 15);
      pdf.setFontSize(11);
      pdf.setTextColor(100, 100, 100);
      pdf.text(`${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`, 14, 22);
      pdf.text(`Present: ${totalPresent} | Absent: ${totalAbsent} | Overtime: ${totalOT}h`, 14, 28);

      pdf.addImage(imgData, "PNG", 5, 35, pdfW - 10, pdfH - 10);
      pdf.save(`attendance_${monthNames[selectedMonth.getMonth()]}_${selectedMonth.getFullYear()}.pdf`);
    } catch (err) {
      console.error("Error exporting PDF:", err);
    }
    setExporting(false);
  };

  const exportImage = async () => {
    if (!tableRef.current || exporting) return;
    setExporting(true);
    try {
      const canvas = await html2canvas(tableRef.current, {
        scale: 2,
        backgroundColor: "#ffffff",
        useCORS: true,
      });
      const url = canvas.toDataURL("image/png");
      const a = document.createElement("a");
      a.href = url;
      a.download = `attendance_${monthNames[selectedMonth.getMonth()]}_${selectedMonth.getFullYear()}.png`;
      a.click();
    } catch (err) {
      console.error("Error exporting image:", err);
    }
    setExporting(false);
  };

  const totalPresent = records.filter((r) => r.status === "present").length;
  const totalAbsent = records.filter((r) => r.status === "absent").length;
  const totalOT = records.reduce((sum, r) => sum + (r.overtime_hours || 0), 0);
  const totalHalf = records.filter((r) => r.type === "half").length;

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-xl font-bold text-foreground">{t("history")}</h1>
        </div>

        {/* Month selector */}
        <div className="flex items-center justify-between mb-3">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-card">
            <ChevronLeft size={20} />
          </button>
          <span className="text-sm font-bold text-foreground">
            {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-card">
            <ChevronRight size={20} />
          </button>
        </div>

        {/* Search */}
        <div className="relative mb-3">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`${t("notes")}...`}
            className="pl-9 h-9 text-sm"
          />
        </div>

        {/* Export buttons */}
        <div className="flex gap-2 mb-4">
          <Button variant="outline" size="sm" onClick={exportPDF} disabled={exporting} className="flex-1 gap-1">
            <FileText size={14} /> PDF
          </Button>
          <Button variant="outline" size="sm" onClick={exportImage} disabled={exporting} className="flex-1 gap-1">
            <FileImage size={14} /> PNG
          </Button>
          <Button variant="outline" size="sm" onClick={exportCSV} className="flex-1 gap-1">
            <FileSpreadsheet size={14} /> CSV
          </Button>
        </div>

        {/* Quick stats */}
        <div className="grid grid-cols-4 gap-2 mb-4">
          <div className="rounded-xl bg-primary/10 p-2.5 text-center">
            <p className="text-lg font-bold text-primary">{totalPresent}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("present")}</p>
          </div>
          <div className="rounded-xl bg-destructive/10 p-2.5 text-center">
            <p className="text-lg font-bold text-destructive">{totalAbsent}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("absent")}</p>
          </div>
          <div className="rounded-xl bg-accent p-2.5 text-center">
            <p className="text-lg font-bold text-foreground">{totalHalf}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("halfDay")}</p>
          </div>
          <div className="rounded-xl bg-accent p-2.5 text-center">
            <p className="text-lg font-bold text-foreground">{totalOT}h</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("overtime")}</p>
          </div>
        </div>

        {/* Records */}
        <div ref={tableRef}>
          {filteredRecords.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16">
              <p className="text-muted-foreground font-medium">{t("noRecords")}</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {filteredRecords.map((r) => (
                <div
                  key={r.date}
                  className="rounded-xl bg-card border border-border px-4 py-3"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-bold text-foreground">
                        {formatDate(r.date)}
                      </span>
                      <span className="ml-1.5 text-[10px] text-muted-foreground font-medium">
                        {getDayName(r.date)}
                      </span>
                      {r.type === "half" && (
                        <span className="ml-2 text-[10px] bg-accent px-2 py-0.5 rounded-full font-medium">
                          {t("halfDay")}
                        </span>
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
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {r.reason && (
                        <span className="text-[10px] bg-destructive/5 text-destructive px-2 py-0.5 rounded-full font-medium">
                          {t(r.reason)}
                        </span>
                      )}
                      {r.note && (
                        <div className="w-full mt-1 rounded-lg bg-muted/50 px-3 py-2">
                          <span className="text-[11px] text-muted-foreground">📝 {r.note}</span>
                        </div>
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

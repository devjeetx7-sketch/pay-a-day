import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { FileText, FileSpreadsheet, ChevronLeft, ChevronRight, Search, Download } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import jsPDF from "jspdf";

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

  const totalPresent = records.filter((r) => r.status === "present").length;
  const totalAbsent = records.filter((r) => r.status === "absent").length;
  const totalOT = records.reduce((sum, r) => sum + (r.overtime_hours || 0), 0);
  const totalHalf = records.filter((r) => r.type === "half").length;
  const dailyWage = userData?.daily_wage || 500;
  const effectiveDays = totalPresent - totalHalf * 0.5;

  const exportPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    const pageW = pdf.internal.pageSize.getWidth();
    const userName = userData?.name || "User";
    const monthLabel = `${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`;

    // Header
    pdf.setFillColor(22, 163, 74);
    pdf.rect(0, 0, pageW, 35, "F");
    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(20);
    pdf.setFont("helvetica", "bold");
    pdf.text("WorkDay - Attendance Report", 14, 15);
    pdf.setFontSize(12);
    pdf.setFont("helvetica", "normal");
    pdf.text(`${userName} | ${monthLabel}`, 14, 24);
    pdf.setFontSize(10);
    pdf.text(`Generated: ${new Date().toLocaleDateString()}`, 14, 31);

    // Summary box
    let y = 45;
    pdf.setTextColor(33, 33, 33);
    pdf.setFillColor(245, 245, 245);
    pdf.roundedRect(10, y, pageW - 20, 22, 3, 3, "F");
    pdf.setFontSize(10);
    pdf.setFont("helvetica", "bold");
    const summaryItems = [
      `Present: ${totalPresent}`,
      `Absent: ${totalAbsent}`,
      `Half Days: ${totalHalf}`,
      `Overtime: ${totalOT}h`,
      `Earnings: ₹${(effectiveDays * dailyWage).toLocaleString()}`,
    ];
    const colW = (pageW - 30) / summaryItems.length;
    summaryItems.forEach((item, i) => {
      pdf.text(item, 15 + i * colW, y + 9);
    });
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(8);
    pdf.setTextColor(120, 120, 120);
    pdf.text(`Daily Wage: ₹${dailyWage} | Effective Days: ${effectiveDays}`, 15, y + 17);

    // Table header
    y = 75;
    pdf.setFillColor(22, 163, 74);
    pdf.rect(10, y, pageW - 20, 10, "F");
    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(9);
    pdf.setFont("helvetica", "bold");
    const cols = [14, 38, 60, 82, 104, 126, 148];
    const headers = ["Date", "Day", "Status", "Type", "OT (hrs)", "Reason", "Note"];
    headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));

    // Table rows
    y += 12;
    pdf.setFont("helvetica", "normal");
    const sortedRecords = [...records].sort((a, b) => a.date.localeCompare(b.date));

    sortedRecords.forEach((r, idx) => {
      if (y > 270) {
        pdf.addPage();
        y = 15;
        // Re-draw header on new page
        pdf.setFillColor(22, 163, 74);
        pdf.rect(10, y, pageW - 20, 10, "F");
        pdf.setTextColor(255, 255, 255);
        pdf.setFontSize(9);
        pdf.setFont("helvetica", "bold");
        headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));
        y += 12;
        pdf.setFont("helvetica", "normal");
      }

      // Alternate row bg
      if (idx % 2 === 0) {
        pdf.setFillColor(250, 250, 250);
        pdf.rect(10, y - 3, pageW - 20, 8, "F");
      }

      pdf.setTextColor(33, 33, 33);
      pdf.setFontSize(8);

      // Status color
      const statusColor = r.status === "present" ? [22, 163, 74] : [220, 38, 38];

      pdf.text(formatDate(r.date), cols[0], y + 2);
      pdf.text(getDayName(r.date), cols[1], y + 2);

      pdf.setTextColor(statusColor[0], statusColor[1], statusColor[2]);
      pdf.text(r.status === "present" ? "Present" : "Absent", cols[2], y + 2);
      pdf.setTextColor(33, 33, 33);

      pdf.text(r.type === "half" ? "Half" : r.type === "full" ? "Full" : "-", cols[3], y + 2);
      pdf.text(String(r.overtime_hours || 0), cols[4], y + 2);
      pdf.text(r.reason ? t(r.reason) : "-", cols[5], y + 2);
      pdf.text((r.note || "-").substring(0, 18), cols[6], y + 2);

      y += 8;
    });

    // Footer
    y += 5;
    pdf.setDrawColor(200, 200, 200);
    pdf.line(10, y, pageW - 10, y);
    y += 6;
    pdf.setTextColor(150, 150, 150);
    pdf.setFontSize(8);
    pdf.text("Generated by WorkDay App", 14, y);
    pdf.text(`Total Records: ${records.length}`, pageW - 50, y);

    pdf.save(`WorkDay_${userName}_${monthLabel.replace(" ", "_")}.pdf`);
  };

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
          <Button variant="outline" size="sm" onClick={exportPDF} className="flex-1 gap-1">
            <FileText size={14} /> PDF
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
        <div>
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
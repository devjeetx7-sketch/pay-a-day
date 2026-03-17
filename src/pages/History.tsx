import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { FileText, FileSpreadsheet, ChevronLeft, ChevronRight, Search, Download, Share2, StickyNote } from "lucide-react";
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
  advance_amount?: number;
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

      // Group records by date to merge advance payments with attendance
      const mergedMap: Record<string, AttendanceRecord> = {};

      snap.docs.forEach((doc) => {
        const data = doc.data() as AttendanceRecord;
        if (data.date >= startDate && data.date <= endDate) {
          if (!mergedMap[data.date]) {
            mergedMap[data.date] = { ...data };
          } else {
            // Merge advance amount
            if (data.status === "advance") {
               mergedMap[data.date].advance_amount = (mergedMap[data.date].advance_amount || 0) + (data.advance_amount || 0);
               // Combine notes safely
               if (data.note && data.note !== "Advance Payment") {
                 mergedMap[data.date].note = mergedMap[data.date].note
                   ? `${mergedMap[data.date].note} | ${data.note}`
                   : data.note;
               }
            } else {
               // Prioritize actual attendance status (present/absent) over 'advance' placeholder
               const prevAdvance = mergedMap[data.date].advance_amount || 0;
               mergedMap[data.date] = { ...data, advance_amount: prevAdvance + (data.advance_amount || 0) };
            }
          }
        }
      });

      const mergedData = Object.values(mergedMap).sort((a, b) => b.date.localeCompare(a.date));

      setRecords(mergedData);
      setFilteredRecords(mergedData);
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
  const totalAdvance = records.reduce((sum, r) => sum + (r.advance_amount || 0), 0);
  const dailyWage = userData?.daily_wage || 500;
  const effectiveDays = totalPresent - totalHalf * 0.5;
  const grossEarnings = effectiveDays * dailyWage;
  const netPayable = Math.max(0, grossEarnings - totalAdvance);

  const exportPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    const pageW = pdf.internal.pageSize.getWidth();
    const userName = userData?.name || "User";
    const monthLabel = `${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`;

    // Header Background
    pdf.setFillColor(15, 23, 42); // slate-900
    pdf.rect(0, 0, pageW, 45, "F");

    // Header Text
    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(24);
    pdf.setFont("helvetica", "bold");
    pdf.text("WorkDay", 14, 20);

    pdf.setFontSize(14);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(148, 163, 184); // slate-400
    pdf.text("Monthly Attendance & Earnings Report", 14, 28);

    pdf.setFontSize(11);
    pdf.setTextColor(248, 250, 252); // slate-50
    pdf.text(`${userName} | ${monthLabel}`, 14, 38);

    pdf.setFontSize(9);
    pdf.setTextColor(148, 163, 184); // slate-400
    pdf.text(`Generated: ${new Date().toLocaleDateString()}`, pageW - 45, 38);

    // Financial Summary Box
    let y = 55;
    pdf.setFillColor(241, 245, 249); // slate-100
    pdf.roundedRect(14, y, pageW - 28, 45, 4, 4, "F");

    // Box Title
    pdf.setTextColor(15, 23, 42); // slate-900
    pdf.setFontSize(12);
    pdf.setFont("helvetica", "bold");
    pdf.text("Financial Summary", 20, y + 10);

    // Line separator
    pdf.setDrawColor(203, 213, 225); // slate-300
    pdf.line(20, y + 14, pageW - 20, y + 14);

    // Financial Stats
    pdf.setFontSize(10);

    // Col 1
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(100, 116, 139); // slate-500
    pdf.text("Daily Wage", 20, y + 22);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text(`Rs. ${dailyWage.toLocaleString()}`, 20, y + 27);

    // Col 2
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(100, 116, 139);
    pdf.text("Gross Earnings", 60, y + 22);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text(`Rs. ${grossEarnings.toLocaleString()}`, 60, y + 27);

    // Col 3
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(100, 116, 139);
    pdf.text("Advance Deducted", 105, y + 22);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(239, 68, 68); // red-500
    pdf.text(`- Rs. ${totalAdvance.toLocaleString()}`, 105, y + 27);

    // Col 4
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(100, 116, 139);
    pdf.text("Net Payable", 150, y + 22);
    pdf.setFontSize(14);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(22, 163, 74); // green-600
    pdf.text(`Rs. ${netPayable.toLocaleString()}`, 150, y + 29);

    // Attendance Summary Box
    y = 108;
    pdf.setFillColor(248, 250, 252); // slate-50
    pdf.roundedRect(14, y, pageW - 28, 25, 4, 4, "F");

    pdf.setFontSize(11);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text("Attendance Overview", 20, y + 8);

    pdf.setFontSize(9);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(71, 85, 105); // slate-600

    const summaryItems = [
      `Effective Days: ${effectiveDays}`,
      `Present: ${totalPresent}`,
      `Absent: ${totalAbsent}`,
      `Half Days: ${totalHalf}`,
      `Overtime: ${totalOT} hrs`
    ];

    const colW = (pageW - 40) / summaryItems.length;
    summaryItems.forEach((item, i) => {
      pdf.text(item, 20 + i * colW, y + 18);
    });

    // Table Setup
    y = 145;
    pdf.setFontSize(14);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text("Detailed Log", 14, y);

    y += 6;
    pdf.setFillColor(241, 245, 249); // slate-100
    pdf.rect(14, y, pageW - 28, 10, "F");
    pdf.setTextColor(71, 85, 105); // slate-600
    pdf.setFontSize(9);
    pdf.setFont("helvetica", "bold");

    const cols = [18, 40, 60, 85, 105, 130, 155];
    const headers = ["Date", "Day", "Status", "Type/OT", "Advance", "Reason", "Note"];
    headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));

    // Table rows
    y += 13;
    pdf.setFont("helvetica", "normal");
    const sortedRecords = [...records].sort((a, b) => a.date.localeCompare(b.date));

    sortedRecords.forEach((r, idx) => {
      if (y > 270) {
        pdf.addPage();
        y = 15;
        // Re-draw header on new page
        pdf.setFillColor(241, 245, 249);
        pdf.rect(14, y, pageW - 28, 10, "F");
        pdf.setTextColor(71, 85, 105);
        pdf.setFontSize(9);
        pdf.setFont("helvetica", "bold");
        headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));
        y += 13;
        pdf.setFont("helvetica", "normal");
      }

      // Alternate row bg
      if (idx % 2 === 0) {
        pdf.setFillColor(248, 250, 252); // slate-50
        pdf.rect(14, y - 4, pageW - 28, 9, "F");
      }

      pdf.setTextColor(51, 65, 85); // slate-700
      pdf.setFontSize(9);

      pdf.text(formatDate(r.date), cols[0], y + 2);
      pdf.text(getDayName(r.date), cols[1], y + 2);

      // Status
      if (r.status === "present") {
        pdf.setTextColor(22, 163, 74);
        pdf.setFont("helvetica", "bold");
        pdf.text("Present", cols[2], y + 2);
      } else if (r.status === "advance") {
        pdf.setTextColor(249, 115, 22);
        pdf.setFont("helvetica", "bold");
        pdf.text("Advance", cols[2], y + 2);
      } else {
        pdf.setTextColor(220, 38, 38);
        pdf.setFont("helvetica", "bold");
        pdf.text("Absent", cols[2], y + 2);
      }

      pdf.setTextColor(51, 65, 85);
      pdf.setFont("helvetica", "normal");

      // Type/OT combined
      let typeOT = "-";
      if (r.type === "half") typeOT = "Half Day";
      if (r.type === "full") typeOT = "Full Day";
      if (r.overtime_hours) typeOT += ` (+${r.overtime_hours}h OT)`;
      if (r.status === "advance") typeOT = "-";
      pdf.text(typeOT, cols[3], y + 2);

      // Advance Amount
      if (r.advance_amount) {
        pdf.setTextColor(249, 115, 22);
        pdf.text(`Rs. ${r.advance_amount}`, cols[4], y + 2);
        pdf.setTextColor(51, 65, 85);
      } else {
        pdf.text("-", cols[4], y + 2);
      }

      pdf.text(r.reason ? t(r.reason) : "-", cols[5], y + 2);
      pdf.text((r.note || "-").substring(0, 20), cols[6], y + 2);

      y += 9;
    });

    // Footer
    y += 10;
    pdf.setDrawColor(226, 232, 240); // slate-200
    pdf.line(14, y, pageW - 14, y);
    y += 8;
    pdf.setTextColor(148, 163, 184);
    pdf.setFontSize(8);
    pdf.text("WorkDay Attendance Tracker - Professionally Generated Report", 14, y);
    pdf.text(`Total Document Entries: ${records.length}`, pageW - 55, y);

    pdf.save(`WorkDay_Report_${userName.replace(" ", "_")}_${monthLabel.replace(" ", "_")}.pdf`);
  };

  const handleShare = async () => {
    const summary = `WorkDay Attendance Report\n${userData?.name || 'User'} | ${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}\n\nPresent: ${totalPresent}\nAbsent: ${totalAbsent}\nOvertime: ${totalOT}h\nTotal Advance: ₹${totalAdvance.toLocaleString()}\n\nNet Payable: ₹${netPayable.toLocaleString()}`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Attendance Report',
          text: summary,
        });
      } catch (err) {
        console.error("Error sharing:", err);
      }
    } else {
      // Fallback if Web Share API is not supported
      alert("Sharing is not supported on this browser.");
    }
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

        {/* Export & Share buttons */}
        <div className="flex gap-2 mb-4">
          <Button variant="outline" size="sm" onClick={exportPDF} className="flex-1 gap-1">
            <FileText size={14} /> PDF
          </Button>
          <Button variant="outline" size="sm" onClick={exportCSV} className="flex-1 gap-1">
            <FileSpreadsheet size={14} /> CSV
          </Button>
          <Button variant="outline" size="sm" onClick={handleShare} className="flex-1 gap-1">
            <Share2 size={14} /> Share
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
                    <div className="flex flex-col items-end gap-1.5">
                      {r.status !== "advance" && (
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-bold ${
                            r.status === "present"
                              ? "bg-primary/10 text-primary"
                              : "bg-destructive/10 text-destructive"
                          }`}
                        >
                          {r.status === "present" ? t("present") : t("absent")}
                        </span>
                      )}
                      {(r.advance_amount || 0) > 0 && (
                        <span className="rounded-full px-3 py-1 text-xs font-bold bg-orange-500/10 text-orange-500">
                          ₹{r.advance_amount} Advance
                        </span>
                      )}
                      {r.status === "advance" && (!r.advance_amount || r.advance_amount === 0) && (
                        <span className="rounded-full px-3 py-1 text-xs font-bold bg-orange-500/10 text-orange-500">
                          Advance
                        </span>
                      )}
                    </div>
                  </div>
                  {(r.reason || r.note) && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {r.reason && (
                        <span className="text-[10px] bg-destructive/5 text-destructive px-2 py-0.5 rounded-full font-medium">
                          {t(r.reason)}
                        </span>
                      )}
                      {r.note && (
                        <div className="w-full mt-1 rounded-lg bg-muted/50 px-3 py-2 flex items-start gap-1.5">
                          <StickyNote size={12} className="text-muted-foreground mt-0.5 shrink-0" />
                          <span className="text-[11px] text-muted-foreground leading-tight">{r.note}</span>
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
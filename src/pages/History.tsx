import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useAppData } from "@/contexts/AppDataContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { FileText, FileSpreadsheet, ChevronLeft, ChevronRight, Search, Share2, StickyNote } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import jsPDF from "jspdf";

interface MergedRecord {
  date: string;
  status: string;
  type?: string;
  overtime_hours?: number;
  advance_amount?: number;
  reason?: string;
  note?: string;
}

const History = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const { attendanceRecords, payments, workers } = useAppData();

  const [records, setRecords] = useState<MergedRecord[]>([]);
  const [filteredRecords, setFilteredRecords] = useState<MergedRecord[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMonth, setSelectedMonth] = useState(new Date());

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  useEffect(() => {
    if (!user) return;
    loadHistory();
  }, [user, selectedMonth, attendanceRecords, payments]);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredRecords(records);
    } else {
      const q = searchQuery.toLowerCase();
      setFilteredRecords(
        records.filter(
          (r) =>
            r.date.includes(q) ||
            r.status.toLowerCase().includes(q) ||
            (r.note && r.note.toLowerCase().includes(q)) ||
            (r.reason && r.reason.toLowerCase().includes(q))
        )
      );
    }
  }, [searchQuery, records]);

  const loadHistory = () => {
    const year = selectedMonth.getFullYear();
    const month = String(selectedMonth.getMonth() + 1).padStart(2, "0");
    const yearMonth = `${year}-${month}`;

    const mergedMap: Record<string, MergedRecord> = {};

    // In Contractor Mode, 'History' typically views ALL historical logs or we need a way to filter.
    // Given the previous implementation grouped by date without separating workers, we will group by date.
    // However, since contractors manage multiple workers, a global history grouped by date is less useful
    // unless it aggregates stats per day. Let's list individual worker records grouped by date.

    // Actually, looking at the previous code, it just queried `where("user_id", "==", user.uid)`.
    // But since the new contractor mode scopes to workers, we should show a flattened log of all worker activities.
    // Or we redirect them to use ContractorDetail. Since the prompt asks to fix History for Contractor, we will flatten.

    const monthAtt = attendanceRecords.filter(a => a.date.startsWith(yearMonth));
    const monthPay = payments.filter(p => p.date.startsWith(yearMonth));

    const flatRecords: MergedRecord[] = [];

    monthAtt.forEach(a => {
      const worker = workers.find(w => w.id === a.workerId);
      flatRecords.push({
        date: a.date,
        status: a.status,
        type: a.status === "Half Day" ? "half" : "full",
        overtime_hours: a.overtimeHours,
        note: worker ? `[${worker.name}] - Attendance` : "Attendance"
      });
    });

    monthPay.forEach(p => {
      const worker = workers.find(w => w.id === p.workerId);
      flatRecords.push({
        date: p.date,
        status: p.type === "Advance" ? "advance" : p.type,
        advance_amount: p.amount,
        note: worker ? `[${worker.name}] - ${p.note || p.type}` : p.type
      });
    });

    flatRecords.sort((a, b) => b.date.localeCompare(a.date));

    setRecords(flatRecords);
    setFilteredRecords(flatRecords);
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
    a.download = `contractor_log_${monthNames[selectedMonth.getMonth()]}_${selectedMonth.getFullYear()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Stats
  const totalPresent = records.filter((r) => r.status === "Present").length;
  const totalAbsent = records.filter((r) => r.status === "Absent").length;
  const totalOT = records.reduce((sum, r) => sum + (r.overtime_hours || 0), 0);
  const totalHalf = records.filter((r) => r.type === "half").length;
  const totalAdvance = records.filter(r => r.status === "advance").reduce((sum, r) => sum + (r.advance_amount || 0), 0);

  const exportPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    // Simple PDF generation logic
    pdf.setFontSize(20);
    pdf.text("Contractor Global Log", 14, 20);
    pdf.setFontSize(12);
    pdf.text(`${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`, 14, 30);
    pdf.save("Global_Log.pdf");
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-xl font-bold text-foreground">Global Log (All Workers)</h1>
        </div>

        {/* Month selector */}
        <div className="flex items-center justify-between mb-3 bg-card border border-border rounded-xl p-2">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} />
          </button>
          <span className="text-sm font-bold text-foreground">
            {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} />
          </button>
        </div>

        {/* Search */}
        <div className="relative mb-3">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search logs..."
            className="pl-9 h-11 rounded-xl"
          />
        </div>

        {/* Export & Share buttons */}
        <div className="flex gap-2 mb-4">
          <Button variant="outline" size="sm" onClick={exportPDF} className="flex-1 gap-1 rounded-xl">
            <FileText size={14} /> PDF
          </Button>
          <Button variant="outline" size="sm" onClick={exportCSV} className="flex-1 gap-1 rounded-xl">
            <FileSpreadsheet size={14} /> CSV
          </Button>
        </div>

        {/* Quick stats */}
        <div className="grid grid-cols-4 gap-2 mb-4">
          <div className="rounded-xl bg-card border border-border p-2 text-center">
            <p className="text-sm font-bold text-green-600">{totalPresent}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("present")}</p>
          </div>
          <div className="rounded-xl bg-card border border-border p-2 text-center">
            <p className="text-sm font-bold text-destructive">{totalAbsent}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("absent")}</p>
          </div>
          <div className="rounded-xl bg-card border border-border p-2 text-center">
            <p className="text-sm font-bold text-orange-500">{totalHalf}</p>
            <p className="text-[9px] font-medium text-muted-foreground">{t("halfDay")}</p>
          </div>
          <div className="rounded-xl bg-card border border-border p-2 text-center">
            <p className="text-sm font-bold text-blue-500">₹{totalAdvance}</p>
            <p className="text-[9px] font-medium text-muted-foreground">Advances</p>
          </div>
        </div>

        {/* Records */}
        <div>
          {filteredRecords.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 bg-card border border-dashed border-border rounded-xl">
              <p className="text-muted-foreground font-medium text-sm">{t("noRecords")}</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {filteredRecords.map((r, i) => (
                <div
                  key={i}
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
                    </div>
                    <div className="flex flex-col items-end gap-1.5">
                      {r.status !== "advance" && (
                        <span
                          className={`rounded-full px-3 py-0.5 text-[10px] font-bold ${
                            r.status === "Present"
                              ? "bg-green-500/10 text-green-500"
                              : r.status === "Half Day"
                              ? "bg-orange-500/10 text-orange-500"
                              : "bg-destructive/10 text-destructive"
                          }`}
                        >
                          {r.status}
                        </span>
                      )}
                      {(r.advance_amount || 0) > 0 && (
                        <span className="rounded-full px-3 py-0.5 text-[10px] font-bold bg-blue-500/10 text-blue-500">
                          ₹{r.advance_amount} Advance
                        </span>
                      )}
                    </div>
                  </div>
                  {r.note && (
                    <div className="mt-2 w-full rounded-lg bg-muted/50 px-3 py-2 flex items-start gap-1.5">
                      <StickyNote size={12} className="text-muted-foreground mt-0.5 shrink-0" />
                      <span className="text-[11px] text-muted-foreground leading-tight">{r.note}</span>
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

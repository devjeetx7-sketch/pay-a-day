import { useState } from "react";
import { useAppData } from "@/contexts/AppDataContext";
import { useAuth } from "@/contexts/AuthContext";
import { ChevronLeft, ChevronRight, Download, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import BottomNav from "@/components/BottomNav";
import { generatePersonalPDF } from "@/lib/pdfExport";

const PersonalHistory = () => {
  const { userData } = useAuth();
  const { personalAttendance, personalPayments } = useAppData();

  const [selectedMonth, setSelectedMonth] = useState(new Date());

  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

  const year = selectedMonth.getFullYear();
  const monthStr = String(selectedMonth.getMonth() + 1).padStart(2, '0');
  const yearMonth = `${year}-${monthStr}`;

  const monthAttendance = personalAttendance.filter(a => a.date.startsWith(yearMonth)).sort((a,b) => b.date.localeCompare(a.date));
  const monthPayments = personalPayments.filter(p => p.date.startsWith(yearMonth)).sort((a,b) => b.date.localeCompare(a.date));

  const presentDays = monthAttendance.filter(a => a.status === 'Present').length;
  const halfDays = monthAttendance.filter(a => a.status === 'Half Day').length;
  const totalWorkDays = presentDays + (halfDays * 0.5);

  const dailyWage = userData?.daily_wage || 500;
  const monthEarnings = totalWorkDays * dailyWage;

  const monthReceived = monthPayments.filter(p => p.type === 'Received' || p.type === 'Advance').reduce((s, p) => s + p.amount, 0);
  const monthDeductions = monthPayments.filter(p => p.type === 'Deduction').reduce((s, p) => s + p.amount, 0);
  const balance = monthEarnings - monthReceived - monthDeductions;

  const handleExportPDF = () => {
    generatePersonalPDF(
      userData?.name || "User",
      `${monthNames[selectedMonth.getMonth()]} ${year}`,
      monthAttendance,
      monthPayments,
      monthEarnings,
      monthReceived + monthDeductions, // Assuming deductions count as paid for balance
      balance,
      totalWorkDays,
      dailyWage
    );
  };

  const prevMonth = () => setSelectedMonth(new Date(year, selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(year, selectedMonth.getMonth() + 1, 1));

  return (
    <div className="min-h-screen bg-background pb-20 pt-6 px-4 max-w-lg mx-auto">
      <h1 className="text-xl font-bold text-foreground mb-4">My History</h1>

      {/* Month selector */}
      <div className="flex items-center justify-between mb-6 bg-card border border-border rounded-xl p-2">
        <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted"><ChevronLeft size={20} /></button>
        <span className="text-sm font-bold">{monthNames[selectedMonth.getMonth()]} {year}</span>
        <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted"><ChevronRight size={20} /></button>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-6">
        <div className="bg-card p-4 rounded-2xl border border-border text-center">
          <p className="text-xs text-muted-foreground mb-1">Total Earned</p>
          <p className="text-xl font-bold text-primary">₹{monthEarnings}</p>
        </div>
        <div className="bg-card p-4 rounded-2xl border border-border text-center">
          <p className="text-xs text-muted-foreground mb-1">Balance</p>
          <p className={`text-xl font-bold ${balance >= 0 ? 'text-green-600' : 'text-red-600'}`}>₹{balance}</p>
        </div>
      </div>

      <Button onClick={handleExportPDF} className="w-full mb-6 rounded-xl flex items-center justify-center gap-2" variant="outline">
        <FileText size={18} /> Export PDF Report
      </Button>

      {/* Lists */}
      <div className="space-y-6">
        <div>
          <h2 className="text-sm font-bold text-muted-foreground mb-3">Attendance Records</h2>
          {monthAttendance.length === 0 ? <p className="text-xs text-muted-foreground text-center py-4 bg-muted rounded-xl">No attendance records this month</p> : (
            <div className="space-y-2">
              {monthAttendance.map(a => (
                <div key={a.id} className="flex justify-between items-center p-3 bg-card border border-border rounded-xl">
                  <div>
                    <p className="font-bold text-sm">{a.date}</p>
                    <p className="text-xs text-muted-foreground">₹{a.dailyWage}/day</p>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                    a.status === 'Present' ? 'bg-green-500/10 text-green-500' :
                    a.status === 'Half Day' ? 'bg-orange-500/10 text-orange-500' : 'bg-red-500/10 text-red-500'
                  }`}>
                    {a.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div>
          <h2 className="text-sm font-bold text-muted-foreground mb-3">Payment Records</h2>
          {monthPayments.length === 0 ? <p className="text-xs text-muted-foreground text-center py-4 bg-muted rounded-xl">No payment records this month</p> : (
            <div className="space-y-2">
              {monthPayments.map(p => (
                <div key={p.id} className="flex justify-between items-center p-3 bg-card border border-border rounded-xl">
                  <div>
                    <p className="font-bold text-sm">{p.date}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        p.type === 'Received' ? 'bg-green-500/10 text-green-500' :
                        p.type === 'Advance' ? 'bg-orange-500/10 text-orange-500' :
                        p.type === 'Deduction' ? 'bg-red-500/10 text-red-500' : 'bg-blue-500/10 text-blue-500'
                      }`}>{p.type}</span>
                      {p.note && <span className="text-[10px] text-muted-foreground">{p.note}</span>}
                    </div>
                  </div>
                  <span className={`font-bold text-sm ${p.type === 'Deduction' ? 'text-red-500' : 'text-foreground'}`}>
                    {p.type === 'Deduction' ? '-' : ''}₹{p.amount}
                  </span>
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

export default PersonalHistory;

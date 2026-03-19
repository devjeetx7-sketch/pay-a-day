import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAppData } from "@/contexts/AppDataContext";
import { ArrowLeft, User, Phone, IndianRupee, Calendar, Briefcase, Calculator } from "lucide-react";
import { generatePersonalPDF } from "@/lib/pdfExport";
import { Button } from "@/components/ui/button";

export const WorkerDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { workers, attendanceRecords, payments } = useAppData();

  const worker = workers.find(w => w.id === id);
  const workerAttendance = attendanceRecords.filter(a => a.workerId === id);
  const workerPayments = payments.filter(p => p.workerId === id);

  const [selectedMonth, setSelectedMonth] = useState(new Date().toISOString().substring(0, 7)); // YYYY-MM

  if (!worker) return <div className="p-6 text-center text-red-500 font-bold">Worker not found</div>;

  const monthAttendance = workerAttendance.filter(a => a.date.startsWith(selectedMonth));
  const monthPayments = workerPayments.filter(p => p.date.startsWith(selectedMonth));

  const presentDays = monthAttendance.filter(a => a.status === 'Present').length;
  const absentDays = monthAttendance.filter(a => a.status === 'Absent').length;
  const halfDays = monthAttendance.filter(a => a.status === 'Half Day').length;
  const totalWorkDays = presentDays + (halfDays * 0.5);

  const grossEarned = totalWorkDays * worker.dailyWage;

  const totalAdvance = workerPayments.filter(p => p.type === 'Advance').reduce((sum, p) => sum + p.amount, 0);
  const totalPaid = workerPayments.filter(p => p.type === 'Paid').reduce((sum, p) => sum + p.amount, 0);
  const totalDeductions = workerPayments.filter(p => p.type === 'Deduction').reduce((sum, p) => sum + p.amount, 0);

  const finalBalance = grossEarned - totalAdvance - totalPaid - totalDeductions;

  return (
    <div className="pb-20 pt-6 px-4 max-w-lg mx-auto min-h-screen bg-background">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="p-2 bg-muted rounded-full active:scale-90"><ArrowLeft size={20} /></button>
        <div>
          <h1 className="text-xl font-bold text-foreground">{worker.name}</h1>
          <p className="text-xs text-muted-foreground">{worker.role}</p>
        </div>
      </div>

      {/* Info Card */}
      <div className="bg-card p-4 rounded-2xl border border-border mb-6 flex items-center justify-between">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm">
            <Briefcase size={16} className="text-muted-foreground" />
            <span className="font-medium">{worker.role}</span>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <IndianRupee size={16} className="text-muted-foreground" />
            <span className="font-medium text-primary">₹{worker.dailyWage} / day</span>
          </div>
        </div>
        <div className="space-y-2">
          {worker.phone && (
            <div className="flex items-center gap-2 text-sm">
              <Phone size={16} className="text-muted-foreground" />
              <span className="font-medium">{worker.phone}</span>
            </div>
          )}
          <div className="flex items-center gap-2 text-sm">
            <Calendar size={16} className="text-muted-foreground" />
            <span className="font-medium text-xs">Joined {new Date(worker.joinDate).toLocaleDateString()}</span>
          </div>
        </div>
      </div>

      {/* Stats Selector */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold">Month Overview</h2>
        <input
          type="month"
          value={selectedMonth}
          onChange={(e) => setSelectedMonth(e.target.value)}
          className="bg-muted px-3 py-1.5 rounded-lg text-sm font-bold border-0"
        />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-2 mb-6 text-center">
        <div className="bg-green-500/10 border border-green-500/20 p-2 rounded-xl">
          <p className="text-lg font-bold text-green-600">{presentDays}</p>
          <p className="text-[10px] font-medium text-green-600/80">Present</p>
        </div>
        <div className="bg-red-500/10 border border-red-500/20 p-2 rounded-xl">
          <p className="text-lg font-bold text-red-600">{absentDays}</p>
          <p className="text-[10px] font-medium text-red-600/80">Absent</p>
        </div>
        <div className="bg-orange-500/10 border border-orange-500/20 p-2 rounded-xl">
          <p className="text-lg font-bold text-orange-600">{halfDays}</p>
          <p className="text-[10px] font-medium text-orange-600/80">Half</p>
        </div>
        <div className="bg-primary/10 border border-primary/20 p-2 rounded-xl">
          <p className="text-lg font-bold text-primary">{totalWorkDays}</p>
          <p className="text-[10px] font-medium text-primary/80">Work Days</p>
        </div>
      </div>

      {/* Earnings Logic */}
      <div className="bg-card p-4 rounded-2xl border border-border mb-6">
        <div className="flex items-center gap-2 mb-4 border-b border-border pb-3">
          <Calculator size={18} className="text-primary" />
          <h3 className="font-bold text-foreground">Earnings & Balance</h3>
        </div>

        <div className="space-y-3">
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">Gross Earned</span>
            <span className="font-bold text-foreground">₹{grossEarned}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">Total Advance</span>
            <span className="font-bold text-orange-500">- ₹{totalAdvance}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">Total Paid</span>
            <span className="font-bold text-green-500">- ₹{totalPaid}</span>
          </div>
          {totalDeductions > 0 && (
            <div className="flex justify-between items-center text-sm">
              <span className="text-muted-foreground">Deductions</span>
              <span className="font-bold text-red-500">- ₹{totalDeductions}</span>
            </div>
          )}
          <div className="flex justify-between items-center text-lg pt-3 border-t border-border mt-2">
            <span className="font-bold text-foreground">Final Balance</span>
            <span className={`font-bold ${finalBalance >= 0 ? 'text-primary' : 'text-red-500'}`}>
              ₹{finalBalance}
            </span>
          </div>
        </div>
      </div>

      <Button onClick={() => {
        generatePersonalPDF(
          worker.name,
          selectedMonth,
          monthAttendance,
          monthPayments,
          grossEarned,
          totalAdvance + totalPaid,
          finalBalance,
          totalWorkDays,
          worker.dailyWage
        );
      }} className="w-full rounded-xl" variant="outline">Export Individual PDF</Button>
    </div>
  );
};

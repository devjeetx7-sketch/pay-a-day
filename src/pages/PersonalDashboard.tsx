import { useState } from "react";
import { useAppData } from "@/contexts/AppDataContext";
import { Check, X, Clock, Plus, IndianRupee, Hand, Minus, StickyNote } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useNavigate } from "react-router-dom";

export const PersonalDashboard = () => {
  const { userData } = useAuth();
  const navigate = useNavigate();
  const { personalAttendance, addPersonalAttendance, updatePersonalAttendance, personalPayments, addPersonalPayment } = useAppData();

  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];
  const currentMonth = todayStr.substring(0, 7);

  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showAbsentDialog, setShowAbsentDialog] = useState(false);
  const [paymentForm, setPaymentForm] = useState({ amount: "", type: "Received" as any, date: todayStr, note: "" });

  const [overtimeHours, setOvertimeHours] = useState(0);
  const [note, setNote] = useState("");
  const [absenceReason, setAbsenceReason] = useState("sick");

  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  const dailyWage = userData?.daily_wage || 500;

  const todayRecord = personalAttendance.find(a => a.date === todayStr);

  const monthAttendance = personalAttendance.filter(a => a.date.startsWith(currentMonth));
  const monthPayments = personalPayments.filter(p => p.date.startsWith(currentMonth));

  const presentDays = monthAttendance.filter(a => a.status === 'Present').length;
  const halfDays = monthAttendance.filter(a => a.status === 'Half Day').length;
  const totalWorkDays = presentDays + (halfDays * 0.5);

  const monthEarnings = totalWorkDays * dailyWage;
  const monthReceived = monthPayments.filter(p => p.type === 'Received' || p.type === 'Advance').reduce((s, p) => s + p.amount, 0);
  const monthDeductions = monthPayments.filter(p => p.type === 'Deduction').reduce((s, p) => s + p.amount, 0);
  const balance = monthEarnings - monthReceived - monthDeductions;

  const markAttendance = (status: 'Present' | 'Absent' | 'Half Day', extraData: any = {}) => {
    if (todayRecord) {
      updatePersonalAttendance(todayRecord.id, { status, dailyWage, ...extraData });
    } else {
      addPersonalAttendance({ date: todayStr, status, dailyWage, ...extraData });
    }
  };

  const handleMarkAbsent = () => {
    markAttendance('Absent', { reason: absenceReason, note });
    setShowAbsentDialog(false);
    setNote("");
  };

  const handleSavePayment = () => {
    if (!paymentForm.amount) return;
    addPersonalPayment({
      amount: Number(paymentForm.amount),
      type: paymentForm.type,
      date: paymentForm.date,
      note: paymentForm.note
    });
    setShowPaymentModal(false);
    setPaymentForm({ amount: "", type: "Received", date: todayStr, note: "" });
  };

  return (
    <div className="pb-20">
      <div className="flex justify-between items-start mb-6">
        <div>
          <p className="text-muted-foreground font-medium text-sm flex items-center gap-1">
            Welcome back <Hand size={14} className="text-yellow-500 origin-bottom-right rotate-12" />
          </p>
          <h1 className="text-2xl font-bold text-foreground">My Work Dashboard</h1>
        </div>
        <div className="bg-primary/10 text-primary px-3 py-1.5 rounded-full text-xs font-bold border border-primary/20">
          Personal Mode
        </div>
      </div>

      {/* Today Status */}
      <div className="bg-card rounded-2xl p-4 border border-border mb-6">
        <h2 className="font-bold mb-3">Today's Status</h2>

        {(!todayRecord || todayRecord.status !== 'Absent') && (
          <div className="flex flex-col gap-2 rounded-xl bg-muted/50 border border-border px-4 py-3 mb-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-foreground">Overtime (Hours)</span>
              <div className="flex items-center gap-2">
                <button onClick={() => setOvertimeHours(Math.max(0, overtimeHours - 1))} className="h-8 w-8 rounded-full bg-destructive/10 text-destructive flex items-center justify-center active:scale-90 transition-transform">
                  <Minus size={16} strokeWidth={2.5} />
                </button>
                <input
                  type="number"
                  min="0"
                  max="24"
                  value={todayRecord?.overtimeHours !== undefined ? todayRecord.overtimeHours : overtimeHours}
                  onChange={(e) => {
                    const val = Math.max(0, parseInt(e.target.value) || 0);
                    setOvertimeHours(val);
                    if (todayRecord) updatePersonalAttendance(todayRecord.id, { overtimeHours: val });
                  }}
                  className="w-12 h-8 text-center text-sm font-bold bg-background border border-border rounded-md focus:outline-none"
                />
                <button onClick={() => setOvertimeHours(overtimeHours + 1)} className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center active:scale-90 transition-transform">
                  <Plus size={16} strokeWidth={2.5} />
                </button>
              </div>
            </div>
            {todayRecord && todayRecord.overtimeHours && todayRecord.overtimeHours > 0 && (
                <p className="text-xs text-primary font-medium text-center">
                  +{todayRecord.overtimeHours} hours overtime recorded
                </p>
            )}
          </div>
        )}

        <div className="grid grid-cols-3 gap-3">
          <button
            onClick={() => markAttendance('Present', { overtimeHours })}
            className={`flex flex-col items-center justify-center py-4 rounded-xl transition-all active:scale-95 ${todayRecord?.status === 'Present' ? 'bg-green-500 text-white shadow-lg' : 'bg-muted border border-border'}`}
          >
            <Check size={24} className="mb-1" />
            <span className="text-xs font-bold">Present</span>
          </button>
          <button
            onClick={() => setShowAbsentDialog(true)}
            className={`flex flex-col items-center justify-center py-4 rounded-xl transition-all active:scale-95 ${todayRecord?.status === 'Absent' ? 'bg-red-500 text-white shadow-lg' : 'bg-muted border border-border'}`}
          >
            <X size={24} className="mb-1" />
            <span className="text-xs font-bold">Absent</span>
          </button>
          <button
            onClick={() => markAttendance('Half Day', { overtimeHours })}
            className={`flex flex-col items-center justify-center py-4 rounded-xl transition-all active:scale-95 ${todayRecord?.status === 'Half Day' ? 'bg-orange-500 text-white shadow-lg' : 'bg-muted border border-border'}`}
          >
            <Clock size={24} className="mb-1" />
            <span className="text-xs font-bold">Half Day</span>
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      <h2 className="font-bold mb-3">This Month ({new Date().toLocaleString('default', { month: 'long' })})</h2>
      <div className="grid grid-cols-2 gap-3 mb-6">
        <div className="bg-card p-4 rounded-2xl border border-border">
          <p className="text-xs font-medium text-muted-foreground mb-1">Total Days Worked</p>
          <p className="text-3xl font-bold">{totalWorkDays}</p>
        </div>
        <div className="bg-card p-4 rounded-2xl border border-border">
          <p className="text-xs font-medium text-muted-foreground mb-1">Total Earnings</p>
          <p className="text-3xl font-bold text-primary">₹{monthEarnings}</p>
        </div>
        <div className="bg-card p-4 rounded-2xl border border-border">
          <p className="text-xs font-medium text-muted-foreground mb-1">Total Received</p>
          <p className="text-2xl font-bold text-green-600">₹{monthReceived}</p>
        </div>
        <div className="bg-card p-4 rounded-2xl border border-border">
          <p className="text-xs font-medium text-muted-foreground mb-1">Balance</p>
          <p className={`text-2xl font-bold ${balance >= 0 ? 'text-primary' : 'text-red-500'}`}>₹{balance}</p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="grid grid-cols-2 gap-3">
        <button onClick={() => navigate('/history')} className="bg-muted text-foreground py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2">
          View History
        </button>
        <button onClick={() => setShowPaymentModal(true)} className="bg-primary text-primary-foreground py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2">
          <Plus size={16} /> Add Payment
        </button>
      </div>

      {/* Add Payment Modal */}
      <Dialog open={showPaymentModal} onOpenChange={setShowPaymentModal}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader><DialogTitle>Add Payment</DialogTitle></DialogHeader>
          <div className="space-y-4 mt-2">
            <div className="relative">
              <IndianRupee className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16}/>
              <Input type="number" placeholder="Amount" value={paymentForm.amount} onChange={e => setPaymentForm({...paymentForm, amount: e.target.value})} className="pl-9" />
            </div>
            <Select value={paymentForm.type} onValueChange={(v: any) => setPaymentForm({...paymentForm, type: v})}>
              <SelectTrigger><SelectValue placeholder="Type" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="Received">Received</SelectItem>
                <SelectItem value="Advance">Advance</SelectItem>
                <SelectItem value="Bonus">Bonus</SelectItem>
                <SelectItem value="Deduction">Deduction</SelectItem>
              </SelectContent>
            </Select>
            <Input type="date" value={paymentForm.date} onChange={e => setPaymentForm({...paymentForm, date: e.target.value})} />
            <Input placeholder="Note (Optional)" value={paymentForm.note} onChange={e => setPaymentForm({...paymentForm, note: e.target.value})} />
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowPaymentModal(false)}>Cancel</Button>
            <Button onClick={handleSavePayment}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      {/* Absent Dialog */}
      <Dialog open={showAbsentDialog} onOpenChange={setShowAbsentDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>Mark Absent</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-2">
              {absenceReasons.map((r) => (
                <button
                  key={r}
                  onClick={() => setAbsenceReason(r)}
                  className={`rounded-xl px-3 py-3 text-sm font-semibold transition-all active:scale-95 ${
                    absenceReason === r
                      ? "bg-destructive text-destructive-foreground"
                      : "bg-muted text-foreground"
                  }`}
                >
                  {r.charAt(0).toUpperCase() + r.slice(1)}
                </button>
              ))}
            </div>
            <Input
              placeholder="Add Note (Optional)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAbsentDialog(false)}>Cancel</Button>
            <Button variant="destructive" onClick={handleMarkAbsent}>Confirm</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
};

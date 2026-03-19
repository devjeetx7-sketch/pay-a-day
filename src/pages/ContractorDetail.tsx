import { useState } from "react";
import { useAppData } from "@/contexts/AppDataContext";
import { useParams, useNavigate } from "react-router-dom";
import { WorkerRole } from "@/types";
import { Plus, Users, Calendar, IndianRupee, FileText, ArrowLeft, Pencil, Trash2, Check, X, Clock, Loader2 } from "lucide-react";
import { generateContractorPDF } from "@/lib/pdfExport";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export const ContractorDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {
    contractors, workers, attendanceRecords, payments,
    addWorker, updateWorker, deleteWorker,
    addAttendance, updateAttendance,
    addPayment, deletePayment
  } = useAppData();

  const [activeTab, setActiveTab] = useState<'workers' | 'attendance' | 'payments' | 'summary'>('workers');

  const contractor = contractors.find(c => c.id === id);
  const contractorWorkers = workers.filter(w => w.contractorId === id);
  const contractorAttendance = attendanceRecords.filter(a => a.contractorId === id);
  const contractorPayments = payments.filter(p => p.contractorId === id);

  const todayStr = new Date().toISOString().split("T")[0];

  // UI States
  const [showAddWorker, setShowAddWorker] = useState(false);
  const [editingWorkerId, setEditingWorkerId] = useState<string | null>(null);
  const [showAddPayment, setShowAddPayment] = useState(false);
  const [selectedDate, setSelectedDate] = useState(todayStr);

  // Forms
  const [workerForm, setWorkerForm] = useState({ name: "", role: "Labour" as WorkerRole, dailyWage: "", phone: "" });
  const [paymentForm, setPaymentForm] = useState({ workerId: "", amount: "", type: "Advance" as any, date: todayStr, note: "" });

  if (!contractor) return <div className="p-6 text-center text-red-500 font-bold">Contractor not found</div>;

  // Render Tabs
  const renderWorkers = () => (
    <div className="space-y-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-bold">Workers ({contractorWorkers.length})</h2>
        <Button onClick={() => setShowAddWorker(true)} size="sm" className="rounded-xl"><Plus size={16} className="mr-1"/> Add</Button>
      </div>
      {contractorWorkers.map(w => (
        <div key={w.id} onClick={() => navigate(`/worker/${w.id}`)} className="bg-card p-4 rounded-2xl border border-border flex items-center justify-between cursor-pointer active:scale-[0.98]">
          <div>
            <p className="font-bold text-base">{w.name}</p>
            <div className="flex gap-2 items-center mt-1">
              <span className="text-[10px] bg-secondary px-2 py-0.5 rounded-full font-bold">{w.role}</span>
              <span className="text-[11px] font-medium text-muted-foreground">₹{w.dailyWage}/day</span>
            </div>
            {w.phone && <p className="text-[10px] text-muted-foreground mt-1">{w.phone}</p>}
          </div>
          <div className="flex gap-2">
            <button onClick={(e) => { e.stopPropagation(); setWorkerForm({...w, dailyWage: String(w.dailyWage)}); setEditingWorkerId(w.id); setShowAddWorker(true); }} className="p-2 bg-muted rounded-full">
              <Pencil size={14} />
            </button>
            <button onClick={(e) => { e.stopPropagation(); deleteWorker(w.id); }} className="p-2 bg-destructive/10 text-destructive rounded-full">
              <Trash2 size={14} />
            </button>
          </div>
        </div>
      ))}
    </div>
  );

  const renderAttendance = () => {
    return (
      <div className="space-y-4">
        <div className="flex items-center gap-3 bg-card p-3 rounded-2xl border border-border">
          <Calendar size={20} className="text-primary" />
          <Input type="date" value={selectedDate} onChange={e => setSelectedDate(e.target.value)} className="border-0 focus-visible:ring-0 p-0 h-auto bg-transparent font-bold text-lg" />
        </div>

        {contractorWorkers.length === 0 ? <p className="text-center text-muted-foreground py-8">No workers added yet.</p> : null}

        {contractorWorkers.map(w => {
          const record = contractorAttendance.find(a => a.workerId === w.id && a.date === selectedDate);
          const status = record?.status;

          const mark = (s: 'Present' | 'Absent' | 'Half Day' | 'Holiday') => {
            if (record) updateAttendance(record.id, { status: s });
            else addAttendance({ workerId: w.id, contractorId: contractor.id, date: selectedDate, status: s, overtimeHours: 0 });
          };

          return (
            <div key={w.id} className="bg-card p-4 rounded-2xl border border-border">
              <div className="flex justify-between items-center mb-3">
                <p className="font-bold">{w.name}</p>
                <span className="text-[10px] font-bold text-muted-foreground">{w.role}</span>
              </div>
              <div className="flex gap-2">
                <button onClick={() => mark('Present')} className={`flex-1 py-2 rounded-xl text-xs font-bold border ${status === 'Present' ? 'bg-green-500 text-white border-green-500' : 'border-border text-foreground bg-background'}`}>P</button>
                <button onClick={() => mark('Absent')} className={`flex-1 py-2 rounded-xl text-xs font-bold border ${status === 'Absent' ? 'bg-red-500 text-white border-red-500' : 'border-border text-foreground bg-background'}`}>A</button>
                <button onClick={() => mark('Half Day')} className={`flex-1 py-2 rounded-xl text-xs font-bold border ${status === 'Half Day' ? 'bg-orange-500 text-white border-orange-500' : 'border-border text-foreground bg-background'}`}>H</button>
                <button onClick={() => mark('Holiday')} className={`flex-1 py-2 rounded-xl text-xs font-bold border ${status === 'Holiday' ? 'bg-blue-500 text-white border-blue-500' : 'border-border text-foreground bg-background'}`}>HO</button>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  const renderPayments = () => (
    <div className="space-y-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-bold">Payments</h2>
        <Button onClick={() => setShowAddPayment(true)} size="sm" className="rounded-xl"><Plus size={16} className="mr-1"/> Add</Button>
      </div>
      {contractorPayments.length === 0 ? <p className="text-center text-muted-foreground py-8">No payments recorded.</p> : null}
      {contractorPayments.map(p => {
        const w = contractorWorkers.find(worker => worker.id === p.workerId);
        return (
          <div key={p.id} className="bg-card p-4 rounded-2xl border border-border flex justify-between items-center">
            <div>
              <p className="font-bold">{w?.name || 'Unknown Worker'}</p>
              <div className="flex gap-2 items-center mt-1">
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                  p.type === 'Advance' ? 'bg-orange-500/10 text-orange-500' :
                  p.type === 'Paid' ? 'bg-green-500/10 text-green-500' :
                  p.type === 'Deduction' ? 'bg-red-500/10 text-red-500' : 'bg-blue-500/10 text-blue-500'
                }`}>{p.type}</span>
                <span className="text-[10px] text-muted-foreground">{p.date}</span>
              </div>
              {p.note && <p className="text-[10px] text-muted-foreground mt-1">{p.note}</p>}
            </div>
            <div className="flex items-center gap-3">
              <p className={`font-bold ${p.type === 'Deduction' ? 'text-red-500' : 'text-foreground'}`}>
                {p.type === 'Deduction' ? '-' : ''}₹{p.amount}
              </p>
              <button onClick={() => deletePayment(p.id)} className="p-1.5 bg-destructive/10 text-destructive rounded-full">
                <Trash2 size={12} />
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );

  const renderSummary = () => {
    // Basic summary for current month
    const yearMonth = selectedDate.substring(0, 7);

    return (
      <div className="space-y-4">
        <div className="flex items-center gap-3 bg-card p-3 rounded-2xl border border-border mb-4">
          <Calendar size={20} className="text-primary" />
          <Input type="month" value={yearMonth} onChange={e => setSelectedDate(e.target.value + "-01")} className="border-0 focus-visible:ring-0 p-0 h-auto bg-transparent font-bold text-lg" />
        </div>

        <div className="overflow-x-auto border border-border rounded-xl">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-muted">
              <tr>
                <th className="p-3 font-bold text-xs text-muted-foreground">Worker</th>
                <th className="p-3 font-bold text-xs text-muted-foreground text-center">P</th>
                <th className="p-3 font-bold text-xs text-muted-foreground text-center">H</th>
                <th className="p-3 font-bold text-xs text-muted-foreground text-right">Earned</th>
                <th className="p-3 font-bold text-xs text-muted-foreground text-right">Adv/Paid</th>
                <th className="p-3 font-bold text-xs text-muted-foreground text-right">Bal</th>
              </tr>
            </thead>
            <tbody className="bg-card divide-y divide-border">
              {contractorWorkers.map(w => {
                const monthAtt = contractorAttendance.filter(a => a.workerId === w.id && a.date.startsWith(yearMonth));
                const monthPay = contractorPayments.filter(p => p.workerId === w.id && p.date.startsWith(yearMonth));

                const pDays = monthAtt.filter(a => a.status === 'Present').length;
                const hDays = monthAtt.filter(a => a.status === 'Half Day').length;
                const earned = (pDays * w.dailyWage) + (hDays * w.dailyWage * 0.5);

                const paidTotal = monthPay.filter(p => p.type === 'Paid' || p.type === 'Advance').reduce((sum, p) => sum + p.amount, 0);
                const dedTotal = monthPay.filter(p => p.type === 'Deduction').reduce((sum, p) => sum + p.amount, 0);

                const balance = earned - paidTotal - dedTotal;

                return (
                  <tr key={w.id}>
                    <td className="p-3 font-bold text-xs">{w.name}</td>
                    <td className="p-3 text-center text-xs text-green-600 font-bold">{pDays}</td>
                    <td className="p-3 text-center text-xs text-orange-500 font-bold">{hDays}</td>
                    <td className="p-3 text-right text-xs font-bold">₹{earned}</td>
                    <td className="p-3 text-right text-xs font-bold text-blue-600">₹{paidTotal}</td>
                    <td className="p-3 text-right text-xs font-bold text-primary">₹{balance}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <Button onClick={() => {
          const exportData = contractorWorkers.map(w => {
            const monthAtt = contractorAttendance.filter(a => a.workerId === w.id && a.date.startsWith(yearMonth));
            const monthPay = contractorPayments.filter(p => p.workerId === w.id && p.date.startsWith(yearMonth));

            const pDays = monthAtt.filter(a => a.status === 'Present').length;
            const hDays = monthAtt.filter(a => a.status === 'Half Day').length;
            const aDays = monthAtt.filter(a => a.status === 'Absent').length;
            const earned = (pDays * w.dailyWage) + (hDays * w.dailyWage * 0.5);

            const paidTotal = monthPay.filter(p => p.type === 'Paid' || p.type === 'Advance').reduce((sum, p) => sum + p.amount, 0);
            const dedTotal = monthPay.filter(p => p.type === 'Deduction').reduce((sum, p) => sum + p.amount, 0);

            return {
              name: w.name,
              role: w.role,
              wage: w.dailyWage,
              present: pDays,
              absent: aDays,
              half: hDays,
              worked: pDays + (hDays * 0.5),
              earned: earned,
              paid: paidTotal,
              balance: earned - paidTotal - dedTotal
            };
          });
          generateContractorPDF(contractor.name, yearMonth, exportData);
        }} className="w-full rounded-xl mt-4" variant="outline"><FileText size={16} className="mr-2"/> Export PDF Report</Button>
      </div>
    );
  };

  return (
    <div className="pb-20 pt-6 px-4 max-w-lg mx-auto min-h-screen bg-background">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate("/")} className="p-2 bg-muted rounded-full active:scale-90"><ArrowLeft size={20} /></button>
        <div>
          <h1 className="text-xl font-bold text-foreground">{contractor.name}</h1>
          <p className="text-xs text-muted-foreground">{contractor.company || contractor.phone}</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex bg-muted p-1 rounded-2xl mb-6">
        {[
          { id: 'workers', icon: Users, label: 'Workers' },
          { id: 'attendance', icon: Calendar, label: 'Attd.' },
          { id: 'payments', icon: IndianRupee, label: 'Pay' },
          { id: 'summary', icon: FileText, label: 'Summ' },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id as any)}
            className={`flex-1 flex flex-col items-center justify-center py-2.5 rounded-xl transition-all ${activeTab === t.id ? 'bg-background shadow-sm text-primary font-bold' : 'text-muted-foreground font-medium hover:text-foreground'}`}
          >
            <t.icon size={18} className="mb-1" />
            <span className="text-[10px]">{t.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
        {activeTab === 'workers' && renderWorkers()}
        {activeTab === 'attendance' && renderAttendance()}
        {activeTab === 'payments' && renderPayments()}
        {activeTab === 'summary' && renderSummary()}
      </div>

      {/* Add Worker Dialog */}
      <Dialog open={showAddWorker} onOpenChange={setShowAddWorker}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader><DialogTitle>{editingWorkerId ? "Edit Worker" : "Add Worker"}</DialogTitle></DialogHeader>
          <div className="space-y-4 mt-2">
            <Input placeholder="Name" value={workerForm.name} onChange={e => setWorkerForm({...workerForm, name: e.target.value})} />
            <Select value={workerForm.role} onValueChange={(v: any) => setWorkerForm({...workerForm, role: v})}>
              <SelectTrigger><SelectValue placeholder="Role" /></SelectTrigger>
              <SelectContent>
                {['Labour', 'Helper', 'Mistry', 'Supervisor', 'Electrician', 'Plumber', 'Carpenter', 'Other'].map(r => (
                  <SelectItem key={r} value={r}>{r}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Input type="number" placeholder="Daily Wage (₹)" value={workerForm.dailyWage} onChange={e => setWorkerForm({...workerForm, dailyWage: e.target.value})} />
            <Input placeholder="Phone (Optional)" value={workerForm.phone} onChange={e => setWorkerForm({...workerForm, phone: e.target.value})} />
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowAddWorker(false)}>Cancel</Button>
            <Button onClick={() => {
              const data = { ...workerForm, dailyWage: Number(workerForm.dailyWage) || 0, contractorId: contractor.id, joinDate: new Date().toISOString() };
              if (editingWorkerId) updateWorker(editingWorkerId, data);
              else addWorker(data);
              setShowAddWorker(false);
              setEditingWorkerId(null);
              setWorkerForm({ name: "", role: "Labour", dailyWage: "", phone: "" });
            }}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add Payment Dialog */}
      <Dialog open={showAddPayment} onOpenChange={setShowAddPayment}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader><DialogTitle>Add Payment</DialogTitle></DialogHeader>
          <div className="space-y-4 mt-2">
            <Select value={paymentForm.workerId} onValueChange={v => setPaymentForm({...paymentForm, workerId: v})}>
              <SelectTrigger><SelectValue placeholder="Select Worker" /></SelectTrigger>
              <SelectContent>
                {contractorWorkers.map(w => <SelectItem key={w.id} value={w.id}>{w.name}</SelectItem>)}
              </SelectContent>
            </Select>
            <Input type="number" placeholder="Amount (₹)" value={paymentForm.amount} onChange={e => setPaymentForm({...paymentForm, amount: e.target.value})} />
            <Select value={paymentForm.type} onValueChange={(v: any) => setPaymentForm({...paymentForm, type: v})}>
              <SelectTrigger><SelectValue placeholder="Type" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="Advance">Advance</SelectItem>
                <SelectItem value="Paid">Paid</SelectItem>
                <SelectItem value="Bonus">Bonus</SelectItem>
                <SelectItem value="Deduction">Deduction</SelectItem>
              </SelectContent>
            </Select>
            <Input type="date" value={paymentForm.date} onChange={e => setPaymentForm({...paymentForm, date: e.target.value})} />
            <Textarea placeholder="Note (Optional)" value={paymentForm.note} onChange={e => setPaymentForm({...paymentForm, note: e.target.value})} />
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowAddPayment(false)}>Cancel</Button>
            <Button onClick={() => {
              if(!paymentForm.workerId || !paymentForm.amount) return;
              addPayment({
                workerId: paymentForm.workerId, contractorId: contractor.id,
                amount: Number(paymentForm.amount), type: paymentForm.type,
                date: paymentForm.date, note: paymentForm.note
              });
              setShowAddPayment(false);
              setPaymentForm({ workerId: "", amount: "", type: "Advance", date: todayStr, note: "" });
            }}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

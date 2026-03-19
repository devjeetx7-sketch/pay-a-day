import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { doc, getDoc, collection, query, where, getDocs } from "firebase/firestore";
import { ArrowLeft, Phone, IndianRupee, Calendar, Briefcase, Calculator, FileText, Share2, Plus, Minus, FileSpreadsheet } from "lucide-react";
import { Button } from "@/components/ui/button";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

export const WorkerDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, userData } = useAuth();

  const [worker, setWorker] = useState<any>(null);
  const [records, setRecords] = useState<any[]>([]);
  const [selectedMonth, setSelectedMonth] = useState(new Date().toISOString().substring(0, 7)); // YYYY-MM
  const [loading, setLoading] = useState(true);

  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

  useEffect(() => {
    if (user && id) loadWorkerData();
  }, [user, id, selectedMonth]);

  const loadWorkerData = async () => {
    if (!user || !id) return;
    setLoading(true);
    try {
      const wRef = doc(db, "workers", id);
      const wSnap = await getDoc(wRef);
      if (wSnap.exists()) {
        setWorker({ id: wSnap.id, ...wSnap.data() });
      }

      const attQ = query(
        collection(db, "attendance"),
        where("user_id", "==", `worker_${id}`),
        where("contractorId", "==", user.uid)
      );
      const attSnap = await getDocs(attQ);

      const rList: any[] = [];
      attSnap.docs.forEach(doc => {
        const data = doc.data();
        if (data.date.startsWith(selectedMonth)) {
            rList.push({ id: doc.id, ...data });
        }
      });

      rList.sort((a, b) => b.date.localeCompare(a.date));
      setRecords(rList);
    } catch (err) {
      console.error("Error loading worker details:", err);
    }
    setLoading(false);
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-muted-foreground text-sm font-bold">Loading Worker Passbook...</div>;
  }

  if (!worker) {
    return <div className="p-6 text-center text-red-500 font-bold">Worker not found</div>;
  }

  // Calculate Stats
  let presentDays = 0;
  let halfDays = 0;
  let absentDays = 0;
  let totalAdvance = 0;
  let overtimeHours = 0;

  records.forEach(r => {
    if (r.status === 'present') {
      if (r.type === 'half') halfDays++;
      else presentDays++;
      overtimeHours += r.overtime_hours || 0;
    } else if (r.status === 'absent') {
      absentDays++;
    } else if (r.status === 'advance' && r.advance_amount) {
      totalAdvance += r.advance_amount;
    }
  });

  const totalDailyWorks = presentDays + (halfDays * 0.5);
  const grossEarned = totalDailyWorks * (worker.wage || 500);
  const finalBalance = grossEarned - totalAdvance;

  const [yearStr, monthStr] = selectedMonth.split("-");
  const monthNameStr = monthNames[parseInt(monthStr, 10) - 1];

  const exportPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    const pageW = pdf.internal.pageSize.getWidth();

    // Header
    pdf.setFillColor(15, 23, 42); // slate-900
    pdf.rect(0, 0, pageW, 45, "F");

    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(24);
    pdf.setFont("helvetica", "bold");
    pdf.text("DailyWork Pro", 14, 20);

    pdf.setFontSize(14);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(148, 163, 184); // slate-400
    pdf.text("Worker Monthly Passbook", 14, 28);

    pdf.setFontSize(11);
    pdf.setTextColor(248, 250, 252);
    pdf.text(`${worker.name} (${worker.workType || 'Labour'}) | ${monthNameStr} ${yearStr}`, 14, 38);

    pdf.setFontSize(9);
    pdf.setTextColor(148, 163, 184);
    pdf.text(`Contractor: ${userData?.name || "Admin"}`, pageW - 65, 38);

    // Summary Stats
    pdf.setTextColor(0, 0, 0);
    pdf.setFontSize(14);
    pdf.setFont("helvetica", "bold");
    pdf.text("Financial Summary", 14, 55);

    autoTable(pdf, {
      startY: 60,
      head: [['Daily Wage', 'Work Days', 'Total Earned', 'Total Advance', 'Net Payable']],
      body: [[
        `Rs. ${worker.wage || 500}`,
        totalDailyWorks.toString(),
        `Rs. ${grossEarned}`,
        `Rs. ${totalAdvance}`,
        `Rs. ${finalBalance}`
      ]],
      theme: 'grid',
      headStyles: { fillColor: [30, 64, 175], textColor: [255, 255, 255] },
      styles: { fontSize: 10, halign: 'center' },
    });

    // Attendance & Advance Table
    pdf.setFontSize(14);
    pdf.setFont("helvetica", "bold");
    // @ts-ignore
    pdf.text("Detailed Logs", 14, pdf.lastAutoTable.finalY + 15);

    const logBody = records.map(r => {
      let status = "-";
      let amount = "-";
      if (r.status === 'present') status = r.type === 'half' ? 'Half Day' : 'Present';
      if (r.status === 'absent') status = 'Absent';
      if (r.status === 'advance') amount = `Rs. ${r.advance_amount}`;

      return [
        r.date.split("-").reverse().join("/"),
        status,
        amount,
        (r.note || "-").substring(0, 25)
      ];
    });

    autoTable(pdf, {
      // @ts-ignore
      startY: pdf.lastAutoTable.finalY + 20,
      head: [['Date', 'Status/Type', 'Advance Amount', 'Note']],
      body: logBody,
      theme: 'striped',
      headStyles: { fillColor: [100, 116, 139], textColor: [255, 255, 255] },
      styles: { fontSize: 9 },
    });

    // Footer
    const pageCount = pdf.internal.pages.length - 1;
    for(let i = 1; i <= pageCount; i++) {
      pdf.setPage(i);
      pdf.setFontSize(8);
      pdf.setTextColor(150);
      pdf.text(`Generated by DailyWork Pro - Professionally Managed Workforce`, 14, 290);
    }

    pdf.save(`${worker.name.replace(" ", "_")}_Passbook_${monthNameStr}_${yearStr}.pdf`);
  };

  const handleWhatsAppShare = () => {
    const text = `*Worker Passbook: ${worker.name}*\n*Month:* ${monthNameStr} ${yearStr}\n*Contractor:* ${userData?.name || "Admin"}\n\n*Summary:*\n• Daily Wage: ₹${worker.wage || 500}\n• Total Days Worked: ${totalDailyWorks}\n• Gross Earned: ₹${grossEarned}\n• Total Advance: ₹${totalAdvance}\n\n*Net Payable Balance: ₹${finalBalance}*\n\nGenerated via DailyWork Pro App`;
    const encodedText = encodeURIComponent(text);
    window.open(`https://wa.me/?text=${encodedText}`, "_blank");
  };

  return (
    <div className="pb-20 pt-6 px-4 max-w-lg mx-auto min-h-screen bg-background animate-in fade-in slide-in-from-bottom-4">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="p-2 bg-card border border-border rounded-full active:scale-90 shadow-sm"><ArrowLeft size={20} className="text-foreground" /></button>
        <div>
          <h1 className="text-2xl font-bold text-foreground">{worker.name}</h1>
          <p className="text-sm font-medium text-muted-foreground">{worker.workType || "Labour"}</p>
        </div>
      </div>

      {/* Info Card */}
      <div className="bg-card p-5 rounded-2xl border border-border mb-6 shadow-sm flex items-center justify-between">
        <div className="space-y-3">
          <div className="flex items-center gap-2.5 text-sm">
            <Briefcase size={16} className="text-primary" />
            <span className="font-bold text-foreground">{worker.workType || "Labour"}</span>
          </div>
          <div className="flex items-center gap-2.5 text-sm">
            <IndianRupee size={16} className="text-green-600" />
            <span className="font-bold text-green-600">₹{worker.wage || 500} / day</span>
          </div>
        </div>
        <div className="space-y-3">
          {worker.phone && (
            <div className="flex items-center gap-2.5 text-sm">
              <Phone size={16} className="text-blue-500" />
              <span className="font-bold text-foreground">{worker.phone}</span>
            </div>
          )}
          <div className="flex items-center gap-2.5 text-sm">
            <Calendar size={16} className="text-orange-500" />
            <span className="font-bold text-foreground text-xs">Joined {new Date(worker.created_at?.toDate() || Date.now()).toLocaleDateString()}</span>
          </div>
        </div>
      </div>

      {/* Stats Selector */}
      <div className="flex items-center justify-between mb-4 bg-muted/50 p-1.5 rounded-xl border border-border">
        <h2 className="text-sm font-bold text-foreground ml-2">Passbook Month</h2>
        <input
          type="month"
          value={selectedMonth}
          onChange={(e) => setSelectedMonth(e.target.value)}
          className="bg-card px-3 py-1.5 rounded-lg text-sm font-bold border border-border text-primary shadow-sm"
        />
      </div>

      {/* Action Buttons */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <Button onClick={exportPDF} variant="outline" className="h-12 rounded-xl flex items-center justify-center gap-2 border-primary text-primary hover:bg-primary/5 shadow-sm">
            <FileText size={18} /> Export PDF
        </Button>
        <Button onClick={handleWhatsAppShare} className="h-12 rounded-xl flex items-center justify-center gap-2 bg-[#25D366] text-white hover:bg-[#25D366]/90 shadow-sm">
            <Share2 size={18} /> WhatsApp
        </Button>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-4 gap-2 mb-6 text-center">
        <div className="bg-card border border-border p-2.5 rounded-xl shadow-sm">
          <p className="text-xl font-bold text-green-600">{presentDays}</p>
          <p className="text-[9px] font-bold text-muted-foreground mt-0.5 uppercase tracking-wider">Present</p>
        </div>
        <div className="bg-card border border-border p-2.5 rounded-xl shadow-sm">
          <p className="text-xl font-bold text-red-600">{absentDays}</p>
          <p className="text-[9px] font-bold text-muted-foreground mt-0.5 uppercase tracking-wider">Absent</p>
        </div>
        <div className="bg-card border border-border p-2.5 rounded-xl shadow-sm">
          <p className="text-xl font-bold text-orange-500">{halfDays}</p>
          <p className="text-[9px] font-bold text-muted-foreground mt-0.5 uppercase tracking-wider">Half</p>
        </div>
        <div className="bg-card border border-border p-2.5 rounded-xl shadow-sm">
          <p className="text-xl font-bold text-primary">{totalDailyWorks}</p>
          <p className="text-[9px] font-bold text-muted-foreground mt-0.5 uppercase tracking-wider">Man Days</p>
        </div>
      </div>

      {/* Earnings Logic */}
      <div className="bg-card p-5 rounded-2xl border border-border mb-6 shadow-sm">
        <div className="flex items-center gap-2.5 mb-4 border-b border-border pb-3">
          <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
            <Calculator size={16} className="text-primary" />
          </div>
          <h3 className="font-bold text-foreground text-lg">Financial Summary</h3>
        </div>

        <div className="space-y-3.5">
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground font-medium">Gross Earned</span>
            <span className="font-bold text-foreground">₹{grossEarned.toLocaleString()}</span>
          </div>
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground font-medium">Total Advance</span>
            <span className="font-bold text-orange-500">- ₹{totalAdvance.toLocaleString()}</span>
          </div>
          <div className="flex justify-between items-center text-xl pt-3 border-t border-border mt-2">
            <span className="font-bold text-foreground">Net Payable</span>
            <span className={`font-black ${finalBalance >= 0 ? 'text-primary' : 'text-red-500'}`}>
              ₹{finalBalance.toLocaleString()}
            </span>
          </div>
        </div>
      </div>

      {/* Logs List */}
      <div>
        <h3 className="text-sm font-bold text-muted-foreground mb-3 flex items-center gap-2">
            <FileSpreadsheet size={16} /> Detailed Ledger
        </h3>
        {records.length === 0 ? (
            <div className="text-center py-10 bg-card rounded-2xl border border-dashed border-border text-sm font-medium text-muted-foreground">
                No records found for this month.
            </div>
        ) : (
            <div className="space-y-2.5">
                {records.map((r, idx) => (
                    <div key={idx} className="bg-card p-3.5 rounded-xl border border-border flex justify-between items-center shadow-sm">
                        <div>
                            <p className="font-bold text-sm text-foreground">{r.date.split("-").reverse().join("/")}</p>
                            {r.note && <p className="text-[10px] text-muted-foreground mt-0.5 max-w-[200px] truncate">{r.note}</p>}
                        </div>
                        <div className="flex flex-col items-end gap-1.5">
                            {r.status !== 'advance' && (
                                <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full ${r.status === 'present' ? 'bg-green-500/10 text-green-600' : 'bg-red-500/10 text-red-600'}`}>
                                    {r.status === 'present' ? (r.type === 'half' ? 'Half Day' : 'Present') : 'Absent'}
                                </span>
                            )}
                            {r.advance_amount && (
                                <span className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-orange-500/10 text-orange-600 flex items-center gap-1">
                                    <Minus size={10} /> ₹{r.advance_amount} Advance
                                </span>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        )}
      </div>
    </div>
  );
};

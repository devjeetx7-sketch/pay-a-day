import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, getDoc, updateDoc, deleteDoc } from "firebase/firestore";
import {
  ArrowLeft, Calendar, IndianRupee, Clock, TrendingUp, ChevronLeft, ChevronRight,
  Edit3, Trash2, Save, User, Mail, Wallet, Shield
} from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell } from "recharts";
import jsPDF from "jspdf";

interface UserData {
  uid: string;
  name: string;
  email: string;
  role: string;
  daily_wage: number;
  language: string;
  photoURL?: string;
}

interface AttendanceRecord {
  date: string;
  status: string;
  type?: string;
  reason?: string;
  overtime_hours?: number;
  note?: string;
  advance_amount?: number;
}

const AdminUserDetail = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const [userData, setUserData] = useState<UserData | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [editName, setEditName] = useState("");
  const [editWage, setEditWage] = useState("");
  const [editRole, setEditRole] = useState("");
  const [saving, setSaving] = useState(false);

  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

  useEffect(() => {
    if (userId) loadUser();
  }, [userId]);

  useEffect(() => {
    if (userId) loadAttendance();
  }, [userId, selectedMonth]);

  const loadUser = async () => {
    if (!userId) return;
    try {
      const snap = await getDoc(doc(db, "users", userId));
      if (snap.exists()) {
        const data = snap.data() as UserData;
        setUserData(data);
        setEditName(data.name);
        setEditWage(String(data.daily_wage || 500));
        setEditRole(data.role || "user");
      }
    } catch (err) {
      console.error("Error loading user:", err);
    }
  };

  const loadAttendance = async () => {
    if (!userId) return;
    setLoading(true);
    const year = selectedMonth.getFullYear();
    const month = selectedMonth.getMonth();
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    try {
      const q = query(collection(db, "attendance"), where("user_id", "==", userId));
      const snap = await getDocs(q);

      const mergedMap: Record<string, AttendanceRecord> = {};
      snap.docs.forEach((d) => {
        const data = d.data() as AttendanceRecord;
        if (data.date >= startDate && data.date <= endDate) {
          if (!mergedMap[data.date]) {
            mergedMap[data.date] = { ...data };
          } else {
            if (data.status === "advance") {
              mergedMap[data.date].advance_amount = (mergedMap[data.date].advance_amount || 0) + (data.advance_amount || 0);
            } else {
              const prevAdv = mergedMap[data.date].advance_amount || 0;
              mergedMap[data.date] = { ...data, advance_amount: prevAdv + (data.advance_amount || 0) };
            }
          }
        }
      });

      setRecords(Object.values(mergedMap).sort((a, b) => b.date.localeCompare(a.date)));
    } catch (err) {
      console.error("Error loading attendance:", err);
    }
    setLoading(false);
  };

  const saveUserEdit = async () => {
    if (!userId) return;
    setSaving(true);
    try {
      await updateDoc(doc(db, "users", userId), {
        name: editName.trim(),
        daily_wage: parseInt(editWage, 10) || 500,
        role: editRole,
      });
      await loadUser();
      setShowEditDialog(false);
    } catch (err) {
      console.error("Error updating user:", err);
    }
    setSaving(false);
  };

  const deleteAttendanceRecord = async (dateStr: string) => {
    if (!userId) return;
    try {
      const docId = `${userId}_${dateStr}`;
      await deleteDoc(doc(db, "attendance", docId)).catch(() => {});
      const advDocId = `${userId}_${dateStr}_advance`;
      await deleteDoc(doc(db, "attendance", advDocId)).catch(() => {});
      await loadAttendance();
    } catch (err) {
      console.error("Error deleting record:", err);
    }
  };

  const prevMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1));

  const totalPresent = records.filter((r) => r.status === "present").length;
  const totalAbsent = records.filter((r) => r.status === "absent").length;
  const totalHalf = records.filter((r) => r.type === "half").length;
  const totalOT = records.reduce((s, r) => s + (r.overtime_hours || 0), 0);
  const totalAdvance = records.reduce((s, r) => s + (r.advance_amount || 0), 0);
  const dailyWage = userData?.daily_wage || 500;
  const effectiveDays = totalPresent - totalHalf * 0.5;
  const grossEarnings = effectiveDays * dailyWage;
  const netPayable = Math.max(0, grossEarnings - totalAdvance);

  const weeklyData = Array.from({ length: 4 }, (_, w) => {
    const year = selectedMonth.getFullYear();
    const m = selectedMonth.getMonth();
    const wStart = `${year}-${String(m + 1).padStart(2, "0")}-${String(w * 7 + 1).padStart(2, "0")}`;
    const wEnd = `${year}-${String(m + 1).padStart(2, "0")}-${String(Math.min((w + 1) * 7, 31)).padStart(2, "0")}`;
    const count = records.filter((r) => r.date >= wStart && r.date <= wEnd && r.status === "present").length;
    return { name: `W${w + 1}`, days: count };
  });

  const formatDate = (d: string) => { const [y, m, dd] = d.split("-"); return `${dd}/${m}/${y}`; };
  const getDayName = (d: string) => new Date(d).toLocaleDateString("en", { weekday: "short" });
  const getInitials = (name: string) => (name || "U").split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);

  const exportUserPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    const pageW = pdf.internal.pageSize.getWidth();
    const userName = userData?.name || "User";
    const monthLabel = `${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`;

    pdf.setFillColor(15, 23, 42);
    pdf.rect(0, 0, pageW, 45, "F");
    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(24);
    pdf.setFont("helvetica", "bold");
    pdf.text("WorkDay Admin Report", 14, 20);
    pdf.setFontSize(12);
    pdf.setTextColor(148, 163, 184);
    pdf.text(`Worker: ${userName} | ${monthLabel}`, 14, 30);
    pdf.setFontSize(9);
    pdf.text(`Generated: ${new Date().toLocaleDateString()}`, 14, 38);

    let y = 55;
    pdf.setFillColor(241, 245, 249);
    pdf.roundedRect(14, y, pageW - 28, 35, 4, 4, "F");
    pdf.setTextColor(15, 23, 42);
    pdf.setFontSize(11);
    pdf.setFont("helvetica", "bold");
    pdf.text("Summary", 20, y + 10);
    pdf.setFontSize(9);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(71, 85, 105);
    pdf.text(`Present: ${totalPresent}  |  Absent: ${totalAbsent}  |  Half: ${totalHalf}  |  OT: ${totalOT}hrs`, 20, y + 18);
    pdf.text(`Gross: Rs.${grossEarnings.toLocaleString()}  |  Advance: Rs.${totalAdvance.toLocaleString()}  |  Net: Rs.${netPayable.toLocaleString()}`, 20, y + 26);

    y = 100;
    pdf.setFontSize(12);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text("Attendance Log", 14, y);
    y += 8;

    const cols = [18, 42, 65, 90, 115, 140, 165];
    const headers = ["Date", "Day", "Status", "Type", "OT", "Advance", "Note"];
    pdf.setFillColor(241, 245, 249);
    pdf.rect(14, y, pageW - 28, 8, "F");
    pdf.setFontSize(8);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(71, 85, 105);
    headers.forEach((h, i) => pdf.text(h, cols[i], y + 6));
    y += 10;
    pdf.setFont("helvetica", "normal");

    const sorted = [...records].sort((a, b) => a.date.localeCompare(b.date));
    sorted.forEach((r, idx) => {
      if (y > 275) { pdf.addPage(); y = 15; }
      if (idx % 2 === 0) { pdf.setFillColor(248, 250, 252); pdf.rect(14, y - 3, pageW - 28, 8, "F"); }
      pdf.setTextColor(51, 65, 85);
      pdf.setFontSize(8);
      pdf.text(formatDate(r.date), cols[0], y + 2);
      pdf.text(getDayName(r.date), cols[1], y + 2);
      if (r.status === "present") { pdf.setTextColor(22, 163, 74); pdf.setFont("helvetica", "bold"); }
      else { pdf.setTextColor(220, 38, 38); pdf.setFont("helvetica", "bold"); }
      pdf.text(r.status === "present" ? "Present" : r.status === "advance" ? "Advance" : "Absent", cols[2], y + 2);
      pdf.setTextColor(51, 65, 85); pdf.setFont("helvetica", "normal");
      pdf.text(r.type || "-", cols[3], y + 2);
      pdf.text(r.overtime_hours ? `${r.overtime_hours}h` : "-", cols[4], y + 2);
      if (r.advance_amount) { pdf.setTextColor(249, 115, 22); pdf.text(`Rs.${r.advance_amount}`, cols[5], y + 2); pdf.setTextColor(51, 65, 85); }
      else pdf.text("-", cols[5], y + 2);
      pdf.text((r.note || "-").substring(0, 18), cols[6], y + 2);
      y += 8;
    });

    pdf.save(`Admin_Report_${userName.replace(/\s/g, "_")}_${monthLabel.replace(/\s/g, "_")}.pdf`);
  };

  if (!userData) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-2xl px-4 pt-6 pb-8">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => navigate("/admin")} className="p-2 rounded-xl active:bg-muted">
            <ArrowLeft size={20} className="text-foreground" />
          </button>
          <h1 className="text-lg font-bold text-foreground">Worker Details</h1>
        </div>

        {/* Profile Card */}
        <div className="rounded-2xl bg-card border border-border p-5 mb-4">
          <div className="flex items-center gap-4">
            <Avatar className="h-16 w-16">
              <AvatarImage src={userData.photoURL || ""} />
              <AvatarFallback className="bg-primary/10 text-primary text-lg font-bold">
                {getInitials(userData.name)}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <p className="text-lg font-bold text-foreground truncate">{userData.name}</p>
                {userData.role === "admin" && (
                  <Badge variant="outline" className="text-[9px] px-1.5 py-0 border-primary text-primary">Admin</Badge>
                )}
              </div>
              <p className="text-xs text-muted-foreground truncate">{userData.email}</p>
              <p className="text-xs text-primary font-medium mt-1">₹{userData.daily_wage}/day</p>
            </div>
            <button
              onClick={() => setShowEditDialog(true)}
              className="p-2.5 rounded-xl bg-primary/10 text-primary active:scale-90"
            >
              <Edit3 size={18} />
            </button>
          </div>
        </div>

        {/* Month Selector */}
        <div className="flex items-center justify-between mb-4 rounded-2xl bg-card border border-border px-4 py-3">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} className="text-foreground" />
          </button>
          <span className="text-sm font-bold text-foreground">
            {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} className="text-foreground" />
          </button>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-2 mb-3">
          <div className="rounded-xl bg-primary/10 p-3 text-center">
            <p className="text-lg font-bold text-primary">{totalPresent}</p>
            <p className="text-[9px] text-muted-foreground font-medium">Present</p>
          </div>
          <div className="rounded-xl bg-destructive/10 p-3 text-center">
            <p className="text-lg font-bold text-destructive">{totalAbsent}</p>
            <p className="text-[9px] text-muted-foreground font-medium">Absent</p>
          </div>
          <div className="rounded-xl bg-accent p-3 text-center">
            <p className="text-lg font-bold text-foreground">{totalOT}h</p>
            <p className="text-[9px] text-muted-foreground font-medium">Overtime</p>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-2 mb-4">
          <div className="rounded-2xl bg-card border border-border p-3 text-center">
            <p className="text-xs text-muted-foreground">Gross</p>
            <p className="text-base font-bold text-primary">₹{grossEarnings.toLocaleString()}</p>
          </div>
          <div className="rounded-2xl bg-card border border-border p-3 text-center">
            <p className="text-xs text-muted-foreground">Advance</p>
            <p className="text-base font-bold text-orange-500">₹{totalAdvance.toLocaleString()}</p>
          </div>
          <div className="rounded-2xl bg-card border border-border p-3 text-center">
            <p className="text-xs text-muted-foreground">Net</p>
            <p className="text-base font-bold text-green-600">₹{netPayable.toLocaleString()}</p>
          </div>
        </div>

        {/* Weekly Chart */}
        <div className="rounded-2xl bg-card border border-border p-4 mb-4">
          <p className="text-sm font-bold text-foreground mb-3">Weekly Breakdown</p>
          <div className="h-24">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyData}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis hide />
                <Bar dataKey="days" radius={[6, 6, 0, 0]}>
                  {weeklyData.map((_, i) => (
                    <Cell key={i} fill={`hsl(160, 81%, ${50 - i * 5}%)`} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Export */}
        <Button onClick={exportUserPDF} className="w-full mb-4 gap-2">
          <IndianRupee size={16} /> Export PDF Report
        </Button>

        {/* Attendance Records */}
        <h2 className="text-sm font-bold text-foreground mb-3">Attendance Log ({records.length})</h2>
        <div className="flex flex-col gap-2">
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : records.length === 0 ? (
            <div className="text-center py-12">
              <Calendar size={36} className="text-muted-foreground/30 mx-auto mb-2" />
              <p className="text-muted-foreground text-sm">No records this month</p>
            </div>
          ) : (
            records.map((r) => (
              <div key={r.date} className="rounded-xl bg-card border border-border px-4 py-3">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-sm font-bold text-foreground">{formatDate(r.date)}</span>
                    <span className="ml-1.5 text-[10px] text-muted-foreground">{getDayName(r.date)}</span>
                    {r.type === "half" && (
                      <span className="ml-2 text-[10px] bg-accent px-2 py-0.5 rounded-full font-medium">Half</span>
                    )}
                    {(r.overtime_hours || 0) > 0 && (
                      <span className="ml-1 text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full font-medium">
                        +{r.overtime_hours}h OT
                      </span>
                    )}
                    {(r.advance_amount || 0) > 0 && (
                      <span className="ml-1 text-[10px] bg-orange-500/10 text-orange-600 px-2 py-0.5 rounded-full font-bold">
                        ₹{r.advance_amount} adv
                      </span>
                    )}
                    {r.note && r.note !== "Advance Payment" && (
                      <p className="text-[10px] text-muted-foreground mt-1 truncate max-w-[200px]">📝 {r.note}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`rounded-full px-3 py-1 text-xs font-bold ${
                      r.status === "present" ? "bg-primary/10 text-primary"
                        : r.status === "advance" ? "bg-orange-500/10 text-orange-600"
                        : "bg-destructive/10 text-destructive"
                    }`}>
                      {r.status === "present" ? "Present" : r.status === "advance" ? "Advance" : "Absent"}
                    </span>
                    <button
                      onClick={() => deleteAttendanceRecord(r.date)}
                      className="p-1.5 rounded-lg text-destructive/60 hover:text-destructive active:scale-90"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Edit User Dialog */}
      <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>Edit Worker</DialogTitle>
            <DialogDescription>Update worker details</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">Name</label>
              <div className="flex items-center gap-2">
                <User size={16} className="text-muted-foreground" />
                <Input value={editName} onChange={(e) => setEditName(e.target.value)} />
              </div>
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">Daily Wage (₹)</label>
              <div className="flex items-center gap-2">
                <Wallet size={16} className="text-muted-foreground" />
                <Input type="number" value={editWage} onChange={(e) => setEditWage(e.target.value)} />
              </div>
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">Role</label>
              <div className="flex items-center gap-2">
                <Shield size={16} className="text-muted-foreground" />
                <select
                  value={editRole}
                  onChange={(e) => setEditRole(e.target.value)}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm font-medium text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="user">User</option>
                  <option value="admin">Admin</option>
                </select>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={saveUserEdit} disabled={saving} className="gap-2">
              <Save size={16} /> {saving ? "Saving..." : "Save Changes"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default AdminUserDetail;

import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useAppData } from "@/contexts/AppDataContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { Plus, Pencil, Trash2, Phone, Search, Users } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { useNavigate } from "react-router-dom";

const emptyWorker = { name: "", phone: "", role: "Labour", dailyWage: "500", contractorId: "" };

const WorkersPage = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const { workers, contractors, addWorker, updateWorker, deleteWorker } = useAppData();

  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [showDelete, setShowDelete] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyWorker);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const currentRole = userData?.role || localStorage.getItem("workday_role");

  if (currentRole !== "contractor") {
    return (
      <div className="min-h-screen bg-background pb-20 pt-6 px-4 text-center">
        <h1 className="text-xl font-bold mb-4">Workers</h1>
        <p className="text-muted-foreground bg-card p-6 rounded-2xl border border-border">
          This feature is only available in Contractor mode.
        </p>
        <BottomNav />
      </div>
    );
  }

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = "Name is required";
    if (form.phone.trim() && !/^\d{10}$/.test(form.phone.trim())) e.phone = "Phone must be 10 digits";
    const wage = parseInt(form.dailyWage, 10);
    if (!form.dailyWage || isNaN(wage) || wage <= 0) e.wage = "Valid wage required";
    if (!form.contractorId) e.contractorId = "Contractor assignment required";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = () => {
    if (!validate()) return;
    const data = {
      name: form.name.trim(),
      phone: form.phone.trim(),
      role: form.role as any,
      dailyWage: parseInt(form.dailyWage, 10),
      contractorId: form.contractorId,
      joinDate: new Date().toISOString()
    };

    if (editingId) {
      updateWorker(editingId, data);
    } else {
      addWorker(data);
    }

    setShowForm(false);
    setEditingId(null);
    setForm(emptyWorker);
    setErrors({});
  };

  const handleDelete = () => {
    if (showDelete) {
      deleteWorker(showDelete);
      setShowDelete(null);
    }
  };

  const openEdit = (w: any) => {
    setForm({
      name: w.name,
      phone: w.phone || "",
      role: w.role,
      dailyWage: String(w.dailyWage),
      contractorId: w.contractorId,
    });
    setEditingId(w.id);
    setErrors({});
    setShowForm(true);
  };

  const openAdd = () => {
    const defaultContractor = contractors.length > 0 ? contractors[0].id : "";
    setForm({ ...emptyWorker, contractorId: defaultContractor });
    setEditingId(null);
    setErrors({});
    setShowForm(true);
  };

  const filtered = workers.filter((w) =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    (w.phone && w.phone.includes(search))
  );

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-xl font-bold text-foreground">Global Directory</h1>
            <p className="text-xs text-muted-foreground">{workers.length} Total Workers</p>
          </div>
          <button
            onClick={openAdd}
            className="h-10 w-10 rounded-full bg-primary flex items-center justify-center active:scale-90 transition-transform"
          >
            <Plus size={20} className="text-primary-foreground" />
          </button>
        </div>

        {/* Search */}
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search all workers..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 h-11 rounded-xl"
          />
        </div>

        {/* Worker List */}
        {filtered.length === 0 ? (
          <div className="text-center py-12 bg-card border border-dashed border-border rounded-xl mt-4">
            <Users size={40} className="mx-auto text-muted-foreground mb-3 opacity-50" />
            <p className="text-sm font-bold text-foreground mb-1">
              {search ? "No matches found" : "No workers added"}
            </p>
            <p className="text-xs text-muted-foreground">
              {search ? "Try a different search term" : "Add your first worker to begin"}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filtered.map((w) => {
              const contractor = contractors.find(c => c.id === w.contractorId);
              return (
                <div
                  key={w.id}
                  onClick={() => navigate(`/worker/${w.id}`)}
                  className="rounded-2xl bg-card border border-border p-4 flex items-center gap-3 cursor-pointer active:scale-[0.98] transition-all hover:shadow-sm"
                >
                  <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-lg shrink-0">
                    {w.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-base font-bold text-foreground truncate">{w.name}</p>
                    <div className="flex items-center gap-2 text-[10px] text-muted-foreground font-medium mt-0.5">
                      <span className="bg-secondary px-2 py-0.5 rounded-full">{w.role}</span>
                      <span>•</span>
                      <span className="text-primary font-bold">₹{w.dailyWage}/day</span>
                    </div>
                    {contractor && (
                      <p className="text-[10px] text-muted-foreground mt-1.5 truncate">Contractor: {contractor.name}</p>
                    )}
                  </div>
                  <div className="flex flex-col gap-2 shrink-0">
                    <button
                      onClick={(e) => { e.stopPropagation(); openEdit(w); }}
                      className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90"
                    >
                      <Pencil size={14} className="text-foreground" />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); setShowDelete(w.id); }}
                      className="h-8 w-8 rounded-full bg-destructive/10 flex items-center justify-center active:scale-90"
                    >
                      <Trash2 size={14} className="text-destructive" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Add/Edit Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="max-w-sm mx-auto max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingId ? "Edit Worker" : "Add Worker"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Contractor Assigment *</label>
              <Select value={form.contractorId} onValueChange={(v) => setForm({ ...form, contractorId: v })}>
                <SelectTrigger className="h-11 rounded-xl"><SelectValue placeholder="Select Contractor" /></SelectTrigger>
                <SelectContent>
                  {contractors.map(c => (
                    <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.contractorId && <p className="text-[10px] text-destructive mt-1 font-bold">{errors.contractorId}</p>}
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Name *</label>
              <Input className="h-11 rounded-xl" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              {errors.name && <p className="text-[10px] text-destructive mt-1 font-bold">{errors.name}</p>}
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Role</label>
              <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v })}>
                <SelectTrigger className="h-11 rounded-xl"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {['Labour', 'Helper', 'Mistry', 'Supervisor', 'Electrician', 'Plumber', 'Carpenter', 'Other'].map(r => (
                    <SelectItem key={r} value={r}>{r}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Daily Wage (₹) *</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                <Input type="number" value={form.dailyWage} onChange={(e) => setForm({ ...form, dailyWage: e.target.value })} className="pl-8 h-11 rounded-xl" />
              </div>
              {errors.wage && <p className="text-[10px] text-destructive mt-1 font-bold">{errors.wage}</p>}
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Phone (Optional)</label>
              <Input type="tel" maxLength={10} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value.replace(/\D/g, "") })} className="h-11 rounded-xl" />
              {errors.phone && <p className="text-[10px] text-destructive mt-1 font-bold">{errors.phone}</p>}
            </div>
          </div>
          <DialogFooter className="mt-6 flex gap-2">
            <Button variant="outline" className="flex-1 rounded-xl" onClick={() => setShowForm(false)}>Cancel</Button>
            <Button className="flex-1 rounded-xl" onClick={handleSave}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={!!showDelete} onOpenChange={() => setShowDelete(null)}>
        <DialogContent className="max-w-xs mx-auto">
          <DialogHeader>
            <DialogTitle>Remove Worker?</DialogTitle>
            <DialogDescription>This worker and their attendance records will be permanently deleted.</DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex gap-2 mt-4">
            <Button variant="outline" className="flex-1 rounded-xl" onClick={() => setShowDelete(null)}>Cancel</Button>
            <Button variant="destructive" className="flex-1 rounded-xl" onClick={handleDelete}>Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <BottomNav />
    </div>
  );
};

export default WorkersPage;

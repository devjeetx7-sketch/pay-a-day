import { useState } from "react";
import { Plus, Search, Users, Pencil, Trash2, ArrowRight } from "lucide-react";
import { useAppData } from "@/contexts/AppDataContext";
import { Contractor } from "@/types";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from "@/components/ui/dialog";
import { useNavigate } from "react-router-dom";

export const ContractorHome = () => {
  const { contractors, addContractor, updateContractor, deleteContractor, workers, attendanceRecords } = useAppData();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ name: "", phone: "", company: "" });
  const [showDelete, setShowDelete] = useState<string | null>(null);

  const filtered = contractors.filter(c =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    (c.company && c.company.toLowerCase().includes(search.toLowerCase()))
  );

  const handleSave = () => {
    if (!form.name.trim()) return;
    if (editingId) {
      updateContractor(editingId, form);
    } else {
      addContractor(form);
    }
    setShowForm(false);
    setEditingId(null);
    setForm({ name: "", phone: "", company: "" });
  };

  const openAdd = () => {
    setForm({ name: "", phone: "", company: "" });
    setEditingId(null);
    setShowForm(true);
  };

  const openEdit = (c: Contractor, e: React.MouseEvent) => {
    e.stopPropagation();
    setForm({ name: c.name, phone: c.phone, company: c.company || "" });
    setEditingId(c.id);
    setShowForm(true);
  };

  const handleDelete = () => {
    if (showDelete) {
      deleteContractor(showDelete);
      setShowDelete(null);
    }
  };

  const todayStr = new Date().toISOString().split("T")[0];

  return (
    <div className="pb-20">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Contractors</h1>
          <p className="text-sm text-muted-foreground">{contractors.length} total</p>
        </div>
        <button
          onClick={openAdd}
          className="h-12 w-12 rounded-full bg-primary flex items-center justify-center active:scale-95 transition-transform shadow-lg"
        >
          <Plus size={24} className="text-primary-foreground" />
        </button>
      </div>

      <div className="relative mb-6">
        <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search contractors..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10 h-12 rounded-xl bg-card border-border"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-12 rounded-2xl border border-dashed border-border bg-card/50">
          <Users size={40} className="mx-auto text-muted-foreground mb-3 opacity-50" />
          <p className="text-sm font-bold text-foreground mb-1">No contractors found</p>
          <p className="text-xs text-muted-foreground mb-4">Add your first contractor to get started</p>
          <Button onClick={openAdd} variant="outline" className="rounded-xl">Add Contractor</Button>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(c => {
            const contractorWorkers = workers.filter(w => w.contractorId === c.id);
            const presentToday = attendanceRecords.filter(a => a.contractorId === c.id && a.date === todayStr && a.status === 'Present').length;
            const absentToday = attendanceRecords.filter(a => a.contractorId === c.id && a.date === todayStr && a.status === 'Absent').length;
            const halfToday = attendanceRecords.filter(a => a.contractorId === c.id && a.date === todayStr && a.status === 'Half Day').length;

            return (
              <div
                key={c.id}
                onClick={() => navigate(`/contractor/${c.id}`)}
                className="rounded-2xl bg-card border border-border p-4 transition-all active:scale-[0.98] cursor-pointer shadow-sm hover:shadow-md"
              >
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-bold text-lg text-foreground">{c.name}</h3>
                    {c.company && <p className="text-xs font-medium text-primary mt-0.5">{c.company}</p>}
                    <p className="text-xs text-muted-foreground mt-1">{c.phone || "No phone added"}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={(e) => openEdit(c, e)}
                      className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90"
                    >
                      <Pencil size={14} className="text-foreground" />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); setShowDelete(c.id); }}
                      className="h-8 w-8 rounded-full bg-destructive/10 flex items-center justify-center active:scale-90"
                    >
                      <Trash2 size={14} className="text-destructive" />
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-border">
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center justify-center px-2 py-1 rounded-md bg-secondary text-secondary-foreground text-[10px] font-bold">
                      {contractorWorkers.length} Workers
                    </span>
                    <span className="text-[10px] text-muted-foreground font-medium">
                      {presentToday} P • {absentToday} A • {halfToday} H
                    </span>
                  </div>
                  <ArrowRight size={16} className="text-muted-foreground" />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add/Edit Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>{editingId ? "Edit Contractor" : "Add Contractor"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1.5 block">Name *</label>
              <Input value={form.name} onChange={e => setForm({...form, name: e.target.value})} className="h-11 rounded-xl" placeholder="Contractor name" />
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1.5 block">Phone</label>
              <Input value={form.phone} onChange={e => setForm({...form, phone: e.target.value})} className="h-11 rounded-xl" placeholder="Phone number" />
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1.5 block">Company (Optional)</label>
              <Input value={form.company} onChange={e => setForm({...form, company: e.target.value})} className="h-11 rounded-xl" placeholder="Company name" />
            </div>
          </div>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setShowForm(false)} className="rounded-xl">Cancel</Button>
            <Button onClick={handleSave} disabled={!form.name.trim()} className="rounded-xl">Save Contractor</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={!!showDelete} onOpenChange={() => setShowDelete(null)}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>Delete Contractor?</DialogTitle>
            <p className="text-sm text-muted-foreground mt-2">This will permanently delete this contractor and ALL their workers, attendance, and payment records. This cannot be undone.</p>
          </DialogHeader>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowDelete(null)} className="rounded-xl">Cancel</Button>
            <Button variant="destructive" onClick={handleDelete} className="rounded-xl">Delete Everything</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

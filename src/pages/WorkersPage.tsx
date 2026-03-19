import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import {
  collection, getDocs, addDoc, updateDoc, deleteDoc, doc, query, where, serverTimestamp,
} from "firebase/firestore";
import { Plus, Pencil, Trash2, Phone, CreditCard, Search, Users, X } from "lucide-react";
import BottomNav from "@/components/BottomNav";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useWorkTypes } from "@/hooks/useWorkTypes";

interface Worker {
  id: string;
  name: string;
  phone: string;
  aadhar: string;
  age: number;
  workType: string;
  wage: number;
}

const emptyWorker = { name: "", phone: "", aadhar: "", age: "", workType: "Labour", wage: "500" };

const WorkersPage = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const { workTypes, addWorkType } = useWorkTypes();
  const [workers, setWorkers] = useState<Worker[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [showDelete, setShowDelete] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyWorker);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [customType, setCustomType] = useState("");
  const [isAddingType, setIsAddingType] = useState(false);

  useEffect(() => {
    if (user) loadWorkers();
  }, [user]);

  const loadWorkers = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const q = query(collection(db, "workers"), where("contractorId", "==", user.uid));
      const snap = await getDocs(q);
      setWorkers(snap.docs.map((d) => ({ id: d.id, ...d.data() } as Worker)));
    } catch (err) {
      console.error("Error loading workers:", err);
    }
    setLoading(false);
  };

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = "Name is required";
    if (!form.phone.trim()) e.phone = "Phone is required";
    else if (!/^\d{10}$/.test(form.phone.trim())) e.phone = "Phone must be 10 digits";
    else {
      const duplicate = workers.find((w) => w.phone === form.phone.trim() && w.id !== editingId);
      if (duplicate) e.phone = "Phone number already exists";
    }
    if (!form.aadhar.trim()) e.aadhar = "Aadhar is required";
    else if (!/^\d{12}$/.test(form.aadhar.trim())) e.aadhar = "Aadhar must be 12 digits";
    const age = parseInt(form.age, 10);
    if (!form.age || isNaN(age) || age < 14 || age > 100) e.age = "Valid age required (14-100)";
    const wage = parseInt(form.wage, 10);
    if (!form.wage || isNaN(wage) || wage <= 0) e.wage = "Valid wage required";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!user || !validate()) return;
    setSaving(true);
    const data = {
      name: form.name.trim(),
      phone: form.phone.trim(),
      aadhar: form.aadhar.trim(),
      age: parseInt(form.age, 10),
      workType: form.workType,
      wage: parseInt(form.wage, 10),
    };
    try {
      if (editingId) {
        await updateDoc(doc(db, "workers", editingId), data);
      } else {
        await addDoc(collection(db, "workers"), {
          ...data,
          contractorId: user.uid,
          created_at: serverTimestamp(),
        });
      }
      setShowForm(false);
      setEditingId(null);
      setForm(emptyWorker);
      setErrors({});
      await loadWorkers();
    } catch (err) {
      console.error("Error saving worker:", err);
    }
    setSaving(false);
  };

  const handleDelete = async () => {
    if (!user || !showDelete) return;
    try {
      await deleteDoc(doc(db, "workers", showDelete));
      setShowDelete(null);
      await loadWorkers();
    } catch (err) {
      console.error("Error deleting worker:", err);
    }
  };

  const openEdit = (w: Worker) => {
    setForm({
      name: w.name,
      phone: w.phone,
      aadhar: w.aadhar,
      age: String(w.age),
      workType: w.workType,
      wage: String(w.wage),
    });
    setEditingId(w.id);
    setErrors({});
    setShowForm(true);
  };

  const openAdd = () => {
    setForm(emptyWorker);
    setEditingId(null);
    setErrors({});
    setShowForm(true);
  };

  const filtered = workers.filter((w) =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    w.phone.includes(search)
  );

  const handleAddCustomType = async () => {
    if (!customType.trim()) return;
    const added = await addWorkType(customType);
    if (added) {
      setForm({ ...form, workType: added.name });
      setIsAddingType(false);
      setCustomType("");
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      <div className="mx-auto max-w-lg px-4 pt-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-xl font-bold text-foreground">{t("workers")}</h1>
            <p className="text-xs text-muted-foreground">{workers.length} {t("totalWorkers")}</p>
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
            placeholder={t("searchWorkers")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>

        {/* Worker List */}
        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => <Skeleton key={i} className="h-20 w-full rounded-2xl" />)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12">
            <Users size={40} className="mx-auto text-muted-foreground mb-3" />
            <p className="text-sm font-bold text-foreground mb-1">
              {search ? t("noResults") : t("noWorkers")}
            </p>
            <p className="text-xs text-muted-foreground">
              {search ? t("tryDifferentSearch") : t("addFirstWorker")}
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map((w) => (
              <div
                key={w.id}
                className="rounded-2xl bg-card border border-border p-4 flex items-center gap-3"
              >
                <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                  {w.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-bold text-foreground truncate">{w.name}</p>
                  <div className="flex items-center gap-2 text-[10px] text-muted-foreground font-medium mt-0.5">
                    <span className="bg-secondary px-2 py-0.5 rounded-full">{w.workType}</span>
                    <span>•</span>
                    <span className="text-primary font-bold">₹{w.wage}/day</span>
                  </div>
                </div>
                <div className="flex gap-1.5">
                  <button
                    onClick={() => openEdit(w)}
                    className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90"
                  >
                    <Pencil size={14} className="text-foreground" />
                  </button>
                  <button
                    onClick={() => setShowDelete(w.id)}
                    className="h-8 w-8 rounded-full bg-destructive/10 flex items-center justify-center active:scale-90"
                  >
                    <Trash2 size={14} className="text-destructive" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add/Edit Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="max-w-sm mx-auto max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingId ? t("editWorker") : t("addWorker")}</DialogTitle>
            <DialogDescription>{editingId ? t("editWorkerDesc") : t("addWorkerDesc")}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3 mt-2">
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("name")}</label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              {errors.name && <p className="text-xs text-destructive mt-1">{errors.name}</p>}
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("phone")}</label>
              <Input type="tel" maxLength={10} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value.replace(/\D/g, "") })} />
              {errors.phone && <p className="text-xs text-destructive mt-1">{errors.phone}</p>}
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("aadhar")}</label>
              <Input type="tel" maxLength={12} value={form.aadhar} onChange={(e) => setForm({ ...form, aadhar: e.target.value.replace(/\D/g, "") })} />
              {errors.aadhar && <p className="text-xs text-destructive mt-1">{errors.aadhar}</p>}
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("age")}</label>
              <Input type="number" value={form.age} onChange={(e) => setForm({ ...form, age: e.target.value })} />
              {errors.age && <p className="text-xs text-destructive mt-1">{errors.age}</p>}
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">Work Type</label>
              {isAddingType ? (
                <div className="flex gap-2">
                  <Input
                    placeholder="E.g., Welder"
                    value={customType}
                    onChange={(e) => setCustomType(e.target.value)}
                  />
                  <Button onClick={handleAddCustomType} size="sm">Add</Button>
                  <Button variant="outline" onClick={() => setIsAddingType(false)} size="sm">Cancel</Button>
                </div>
              ) : (
                <div className="flex gap-2">
                  <Select value={form.workType} onValueChange={(v) => setForm({ ...form, workType: v })}>
                    <SelectTrigger className="flex-1"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {workTypes.map(t => (
                        <SelectItem key={t.id} value={t.name}>{t.name}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button variant="outline" onClick={() => setIsAddingType(true)} size="sm">New</Button>
                </div>
              )}
            </div>
            <div>
              <label className="text-xs text-muted-foreground font-medium mb-1 block">{t("dailyWage")}</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                <Input type="number" value={form.wage} onChange={(e) => setForm({ ...form, wage: e.target.value })} className="pl-8" />
              </div>
              {errors.wage && <p className="text-xs text-destructive mt-1">{errors.wage}</p>}
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setShowForm(false)}>{t("cancel")}</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? t("saving") : t("save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={!!showDelete} onOpenChange={() => setShowDelete(null)}>
        <DialogContent className="max-w-xs mx-auto">
          <DialogHeader>
            <DialogTitle>{t("deleteWorker")}</DialogTitle>
            <DialogDescription>{t("deleteWorkerConfirm")}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDelete(null)}>{t("cancel")}</Button>
            <Button variant="destructive" onClick={handleDelete}>{t("confirm")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <BottomNav />
    </div>
  );
};

export default WorkersPage;

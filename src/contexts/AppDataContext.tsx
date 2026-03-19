import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from "react";
import { v4 as uuidv4 } from "uuid";
import { useAuth } from "@/contexts/AuthContext";
import {
  Contractor, Worker, AttendanceRecord, Payment,
  PersonalAttendance, PersonalPayment
} from "@/types";

interface AppDataContextType {
  // Contractor Mode State
  contractors: Contractor[];
  workers: Worker[];
  attendanceRecords: AttendanceRecord[];
  payments: Payment[];

  // Personal Mode State
  personalAttendance: PersonalAttendance[];
  personalPayments: PersonalPayment[];

  // Contractor Actions
  addContractor: (contractor: Omit<Contractor, 'id' | 'createdAt'>) => void;
  updateContractor: (id: string, contractor: Partial<Contractor>) => void;
  deleteContractor: (id: string) => void;

  addWorker: (worker: Omit<Worker, 'id'>) => void;
  updateWorker: (id: string, worker: Partial<Worker>) => void;
  deleteWorker: (id: string) => void;

  addAttendance: (record: Omit<AttendanceRecord, 'id'>) => void;
  updateAttendance: (id: string, record: Partial<AttendanceRecord>) => void;
  deleteAttendance: (id: string) => void;

  addPayment: (payment: Omit<Payment, 'id'>) => void;
  deletePayment: (id: string) => void;

  // Personal Actions
  addPersonalAttendance: (record: Omit<PersonalAttendance, 'id'>) => void;
  updatePersonalAttendance: (id: string, record: Partial<PersonalAttendance>) => void;
  deletePersonalAttendance: (id: string) => void;

  addPersonalPayment: (payment: Omit<PersonalPayment, 'id'>) => void;
  deletePersonalPayment: (id: string) => void;

  // Migration Setters
  setContractorsDirectly: (data: any[]) => void;
  setWorkersDirectly: (data: any[]) => void;
  setPersonalAttendanceDirectly: (data: any[]) => void;
  setPersonalPaymentsDirectly: (data: any[]) => void;
  setAttendanceRecordsDirectly: (data: any[]) => void;
  setPaymentsDirectly: (data: any[]) => void;
}

const AppDataContext = createContext<AppDataContextType | null>(null);

export const useAppData = () => {
  const ctx = useContext(AppDataContext);
  if (!ctx) throw new Error("useAppData must be used within AppDataProvider");
  return ctx;
};

// Helper to load state from local storage
function loadFromStorage<T>(key: string, defaultValue: T): T {
  try {
    const item = localStorage.getItem(key);
    if (item) {
      return JSON.parse(item);
    }
  } catch (error) {
    console.warn(`Error loading ${key} from storage:`, error);
  }
  return defaultValue;
}

export const AppDataProvider = ({ children }: { children: ReactNode }) => {
  const { user } = useAuth();
  const uid = user?.uid || 'guest';

  // Contractor State
  const [contractors, setContractors] = useState<Contractor[]>([]);
  const [workers, setWorkers] = useState<Worker[]>([]);
  const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);

  // Personal State
  const [personalAttendance, setPersonalAttendance] = useState<PersonalAttendance[]>([]);
  const [personalPayments, setPersonalPayments] = useState<PersonalPayment[]>([]);

  // Initial Load from Storage when user changes
  useEffect(() => {
    setContractors(loadFromStorage(`workday_contractors_${uid}`, []));
    setWorkers(loadFromStorage(`workday_workers_${uid}`, []));
    setAttendanceRecords(loadFromStorage(`workday_attendance_${uid}`, []));
    setPayments(loadFromStorage(`workday_payments_${uid}`, []));
    setPersonalAttendance(loadFromStorage(`workday_personal_attendance_${uid}`, []));
    setPersonalPayments(loadFromStorage(`workday_personal_payments_${uid}`, []));
  }, [uid]);

  // Sync to local storage when state changes
  useEffect(() => { localStorage.setItem(`workday_contractors_${uid}`, JSON.stringify(contractors)); }, [contractors, uid]);
  useEffect(() => { localStorage.setItem(`workday_workers_${uid}`, JSON.stringify(workers)); }, [workers, uid]);
  useEffect(() => { localStorage.setItem(`workday_attendance_${uid}`, JSON.stringify(attendanceRecords)); }, [attendanceRecords, uid]);
  useEffect(() => { localStorage.setItem(`workday_payments_${uid}`, JSON.stringify(payments)); }, [payments, uid]);
  useEffect(() => { localStorage.setItem(`workday_personal_attendance_${uid}`, JSON.stringify(personalAttendance)); }, [personalAttendance, uid]);
  useEffect(() => { localStorage.setItem(`workday_personal_payments_${uid}`, JSON.stringify(personalPayments)); }, [personalPayments, uid]);

  // --- Contractor Actions ---

  const addContractor = useCallback((data: Omit<Contractor, 'id' | 'createdAt'>) => {
    const newContractor: Contractor = {
      ...data,
      id: uuidv4(),
      createdAt: new Date().toISOString()
    };
    setContractors(prev => [...prev, newContractor]);
  }, []);

  const updateContractor = useCallback((id: string, data: Partial<Contractor>) => {
    setContractors(prev => prev.map(c => c.id === id ? { ...c, ...data } : c));
  }, []);

  const deleteContractor = useCallback((id: string) => {
    setContractors(prev => prev.filter(c => c.id !== id));
    setWorkers(prev => prev.filter(w => w.contractorId !== id));
    setAttendanceRecords(prev => prev.filter(a => a.contractorId !== id));
    setPayments(prev => prev.filter(p => p.contractorId !== id));
  }, []);

  const addWorker = useCallback((data: Omit<Worker, 'id'>) => {
    const newWorker: Worker = { ...data, id: uuidv4() };
    setWorkers(prev => [...prev, newWorker]);
  }, []);

  const updateWorker = useCallback((id: string, data: Partial<Worker>) => {
    setWorkers(prev => prev.map(w => w.id === id ? { ...w, ...data } : w));
  }, []);

  const deleteWorker = useCallback((id: string) => {
    setWorkers(prev => prev.filter(w => w.id !== id));
    setAttendanceRecords(prev => prev.filter(a => a.workerId !== id));
    setPayments(prev => prev.filter(p => p.workerId !== id));
  }, []);

  const addAttendance = useCallback((data: Omit<AttendanceRecord, 'id'>) => {
    const newRecord: AttendanceRecord = { ...data, id: uuidv4() };
    setAttendanceRecords(prev => {
      // Rule: only ONE status per worker per date
      const filtered = prev.filter(r => !(r.workerId === data.workerId && r.date === data.date));
      return [...filtered, newRecord];
    });
  }, []);

  const updateAttendance = useCallback((id: string, data: Partial<AttendanceRecord>) => {
    setAttendanceRecords(prev => prev.map(a => a.id === id ? { ...a, ...data } : a));
  }, []);

  const deleteAttendance = useCallback((id: string) => {
    setAttendanceRecords(prev => prev.filter(a => a.id !== id));
  }, []);

  const addPayment = useCallback((data: Omit<Payment, 'id'>) => {
    const newPayment: Payment = { ...data, id: uuidv4() };
    setPayments(prev => [newPayment, ...prev]); // Newest first
  }, []);

  const deletePayment = useCallback((id: string) => {
    setPayments(prev => prev.filter(p => p.id !== id));
  }, []);

  // --- Personal Actions ---

  const addPersonalAttendance = useCallback((data: Omit<PersonalAttendance, 'id'>) => {
    const newRecord: PersonalAttendance = { ...data, id: uuidv4() };
    setPersonalAttendance(prev => {
      // One entry per day
      const filtered = prev.filter(r => r.date !== data.date);
      return [...filtered, newRecord];
    });
  }, []);

  const updatePersonalAttendance = useCallback((id: string, data: Partial<PersonalAttendance>) => {
    setPersonalAttendance(prev => prev.map(a => a.id === id ? { ...a, ...data } : a));
  }, []);

  const deletePersonalAttendance = useCallback((id: string) => {
    setPersonalAttendance(prev => prev.filter(a => a.id !== id));
  }, []);

  const addPersonalPayment = useCallback((data: Omit<PersonalPayment, 'id'>) => {
    const newPayment: PersonalPayment = { ...data, id: uuidv4() };
    setPersonalPayments(prev => [newPayment, ...prev]);
  }, []);

  const deletePersonalPayment = useCallback((id: string) => {
    setPersonalPayments(prev => prev.filter(p => p.id !== id));
  }, []);

  const value: AppDataContextType = {
    setContractorsDirectly: setContractors,
    setWorkersDirectly: setWorkers,
    setPersonalAttendanceDirectly: setPersonalAttendance,
    setPersonalPaymentsDirectly: setPersonalPayments,
    setAttendanceRecordsDirectly: setAttendanceRecords,
    setPaymentsDirectly: setPayments,
    contractors, workers, attendanceRecords, payments,
    personalAttendance, personalPayments,
    addContractor, updateContractor, deleteContractor,
    addWorker, updateWorker, deleteWorker,
    addAttendance, updateAttendance, deleteAttendance,
    addPayment, deletePayment,
    addPersonalAttendance, updatePersonalAttendance, deletePersonalAttendance,
    addPersonalPayment, deletePersonalPayment
  };

  return (
    <AppDataContext.Provider value={value}>
      {children}
    </AppDataContext.Provider>
  );
};

export interface Contractor {
  id: string;
  name: string;
  phone: string;
  company?: string;
  createdAt: string; // ISO string for easy serialization
}

export type WorkerRole = 'Helper' | 'Mistry' | 'Labour' | 'Supervisor' | 'Electrician' | 'Plumber' | 'Carpenter' | 'Other';

export interface Worker {
  id: string;
  contractorId: string;
  name: string;
  role: WorkerRole;
  dailyWage: number;
  phone?: string;
  joinDate: string; // ISO string
}

export interface AttendanceRecord {
  id: string;
  workerId: string;
  contractorId: string;
  date: string; // YYYY-MM-DD
  status: 'Present' | 'Absent' | 'Half Day' | 'Holiday';
  overtimeHours: number;
}

export interface Payment {
  id: string;
  workerId: string;
  contractorId: string;
  amount: number;
  type: 'Advance' | 'Paid' | 'Bonus' | 'Deduction';
  date: string; // YYYY-MM-DD
  note?: string;
}

// Personal Mode

export interface PersonalAttendance {
  id: string;
  date: string; // YYYY-MM-DD
  status: 'Present' | 'Absent' | 'Half Day';
  dailyWage: number;
  overtimeHours?: number;
  reason?: string;
  note?: string;
}

export interface PersonalPayment {
  id: string;
  amount: number;
  type: 'Received' | 'Advance' | 'Bonus' | 'Deduction';
  date: string; // YYYY-MM-DD
  note?: string;
}

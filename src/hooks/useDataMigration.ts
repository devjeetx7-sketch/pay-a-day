import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useAppData } from "@/contexts/AppDataContext";
import { db } from "@/lib/firebase";
import { collection, getDocs, query, where } from "firebase/firestore";

export const useDataMigration = () => {
  const { user } = useAuth();
  const {
    setContractorsDirectly,
    setWorkersDirectly,
    setPersonalAttendanceDirectly,
    setPersonalPaymentsDirectly,
    setAttendanceRecordsDirectly,
    setPaymentsDirectly
  } = useAppData() as any;

  const [isMigrating, setIsMigrating] = useState(false);

  useEffect(() => {
    const migrateData = async () => {
      if (!user) return;

      const hasMigrated = localStorage.getItem(`workday_migrated_${user.uid}`);
      if (hasMigrated === "true") return;

      setIsMigrating(true);
      try {
        console.log("Starting Firebase to LocalStorage migration...");

        // Migrate Personal Mode Attendance & Payments
        const attendanceQ = query(collection(db, "attendance"), where("user_id", "==", user.uid));
        const attSnap = await getDocs(attendanceQ);

        const newPersonalAttendance: any[] = [];
        const newPersonalPayments: any[] = [];

        attSnap.docs.forEach(doc => {
          const data = doc.data();
          if (data.status === "present" || data.status === "absent" || data.status === "half") {
            newPersonalAttendance.push({
              id: doc.id,
              date: data.date,
              status: data.status === "present" && data.type === "half" ? "Half Day" : data.status === "present" ? "Present" : "Absent",
              dailyWage: data.daily_wage || 500,
              overtimeHours: data.overtime_hours || 0,
              note: data.note || "",
              reason: data.reason || ""
            });
          }
          if (data.advance_amount && data.advance_amount > 0) {
            newPersonalPayments.push({
              id: `${doc.id}_adv`,
              amount: data.advance_amount,
              type: "Advance",
              date: data.date,
              note: data.note || "Advance Payment"
            });
          }
        });

        if (newPersonalAttendance.length > 0) setPersonalAttendanceDirectly(newPersonalAttendance);
        if (newPersonalPayments.length > 0) setPersonalPaymentsDirectly(newPersonalPayments);

        // Migrate Contractor Mode Workers
        const workersRef = collection(db, "contractors", user.uid, "workers");
        const workersSnap = await getDocs(workersRef);

        if (!workersSnap.empty) {
            const defaultContractorId = `migrated_contractor_${user.uid}`;
            const newContractors = [{
                id: defaultContractorId,
                name: "My Migrated Contractors",
                phone: "",
                createdAt: new Date().toISOString()
            }];
            setContractorsDirectly(newContractors);

            const newWorkers: any[] = [];
            const contractorAttendance: any[] = [];
            const contractorPayments: any[] = [];

            for (const docSnap of workersSnap.docs) {
                const data = docSnap.data();
                const workerId = docSnap.id;

                newWorkers.push({
                    id: workerId,
                    contractorId: defaultContractorId,
                    name: data.name || "Unknown",
                    role: data.workType === "mistry" ? "Mistry" : data.workType === "helper" ? "Helper" : "Labour",
                    dailyWage: data.wage || 500,
                    phone: data.phone || "",
                    joinDate: new Date().toISOString()
                });

                // Fetch attendance for each worker
                const wAttQ = query(collection(db, "attendance"), where("user_id", "==", `worker_${workerId}`));
                const wAttSnap = await getDocs(wAttQ);

                wAttSnap.docs.forEach(attDoc => {
                    const attData = attDoc.data();
                    if (attData.status === "present" || attData.status === "absent" || attData.status === "half") {
                        contractorAttendance.push({
                            id: attDoc.id,
                            workerId,
                            contractorId: defaultContractorId,
                            date: attData.date,
                            status: attData.status === "present" && attData.type === "half" ? "Half Day" : attData.status === "present" ? "Present" : "Absent",
                            overtimeHours: attData.overtime_hours || 0,
                            note: attData.note || "",
                            reason: attData.reason || ""
                        });
                    }
                    if (attData.advance_amount && attData.advance_amount > 0) {
                        contractorPayments.push({
                            id: `${attDoc.id}_adv`,
                            workerId,
                            contractorId: defaultContractorId,
                            amount: attData.advance_amount,
                            type: "Advance",
                            date: attData.date,
                            note: attData.note || "Advance Payment"
                        });
                    }
                });
            }

            setWorkersDirectly(newWorkers);
            if (contractorAttendance.length > 0) setAttendanceRecordsDirectly(contractorAttendance);
            if (contractorPayments.length > 0) setPaymentsDirectly(contractorPayments);
        }

        localStorage.setItem(`workday_migrated_${user.uid}`, "true");
        console.log("Migration complete.");
      } catch (error) {
        console.error("Migration failed:", error);
      } finally {
        setIsMigrating(false);
      }
    };

    migrateData();
  }, [user]);

  return { isMigrating };
};

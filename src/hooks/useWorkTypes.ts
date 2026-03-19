import { useState, useEffect } from "react";
import { db } from "@/lib/firebase";
import { collection, getDocs, addDoc, query, where } from "firebase/firestore";
import { useAuth } from "@/contexts/AuthContext";

export interface WorkType {
  id: string;
  name: string;
  createdBy: string;
}

export const useWorkTypes = () => {
  const { user } = useAuth();
  const [workTypes, setWorkTypes] = useState<WorkType[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) loadWorkTypes();
  }, [user]);

  const loadWorkTypes = async () => {
    if (!user) return;
    setLoading(true);
    try {
      // System defaults + User custom types
      const qSystem = query(collection(db, "workTypes"), where("createdBy", "==", "system"));
      const qUser = query(collection(db, "workTypes"), where("createdBy", "==", user.uid));

      const [snapSystem, snapUser] = await Promise.all([getDocs(qSystem), getDocs(qUser)]);

      let allTypes = [
        ...snapSystem.docs.map(d => ({ id: d.id, ...d.data() } as WorkType)),
        ...snapUser.docs.map(d => ({ id: d.id, ...d.data() } as WorkType))
      ];

      if (allTypes.length === 0) {
        // Fallback defaults if collection empty
        allTypes = [
          { id: '1', name: "Labour", createdBy: "system" },
          { id: '2', name: "Helper", createdBy: "system" },
          { id: '3', name: "Mistri", createdBy: "system" },
          { id: '4', name: "Painter", createdBy: "system" },
          { id: '5', name: "Plumber", createdBy: "system" },
          { id: '6', name: "Electrician", createdBy: "system" },
        ];
      }

      setWorkTypes(allTypes);
    } catch (err) {
      console.error("Error loading work types:", err);
    }
    setLoading(false);
  };

  const addWorkType = async (name: string) => {
    if (!user || !name.trim()) return null;
    const formattedName = name.trim().charAt(0).toUpperCase() + name.trim().slice(1).toLowerCase();

    // Prevent duplicate
    if (workTypes.some(t => t.name.toLowerCase() === formattedName.toLowerCase())) {
        return null;
    }

    try {
      const docRef = await addDoc(collection(db, "workTypes"), {
        name: formattedName,
        createdBy: user.uid
      });
      const newType = { id: docRef.id, name: formattedName, createdBy: user.uid };
      setWorkTypes(prev => [...prev, newType]);
      return newType;
    } catch (err) {
      console.error("Error adding work type:", err);
      return null;
    }
  };

  return { workTypes, addWorkType, loading };
};

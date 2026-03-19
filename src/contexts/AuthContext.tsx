import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { User, onAuthStateChanged, signInWithPopup, signOut } from "firebase/auth";
import { doc, getDoc, setDoc, serverTimestamp } from "firebase/firestore";
import { auth, db, googleProvider } from "@/lib/firebase";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  loginWithGoogle: () => Promise<void>;
  logout: () => Promise<void>;
  userData: UserData | null;
  refreshUserData: () => Promise<void>;
}

export interface UserData {
  uid: string;
  name: string;
  email: string;
  role: string;
  daily_wage: number;
  advance_payment?: number;
  language: string;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [userData, setUserData] = useState<UserData | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchUserData = async (u: User) => {
    const ref = doc(db, "users", u.uid);
    const snap = await getDoc(ref);
    if (snap.exists()) {
      const data = snap.data() as UserData;
      setUserData(data);
      if (data.role) localStorage.setItem("dailywork_role", data.role);
    } else {
      const newUser: UserData = {
        uid: u.uid,
        name: u.displayName || "",
        email: u.email || "",
        role: "",
        daily_wage: 500,
        language: "en",
      };
      await setDoc(ref, { ...newUser, created_at: serverTimestamp() });
      setUserData(newUser);
    }
  };

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (u) => {
      setUser(u);
      if (u) {
        await fetchUserData(u);
      } else {
        setUserData(null);
        localStorage.removeItem("dailywork_role");
      }
      setLoading(false);
    });
    return unsub;
  }, []);

  const refreshUserData = async () => {
    if (user) await fetchUserData(user);
  };

  const loginWithGoogle = async () => {
    await signInWithPopup(auth, googleProvider);
  };

  const logout = async () => {
    await signOut(auth);
    localStorage.removeItem("dailywork_role");
  };

  return (
    <AuthContext.Provider value={{ user, loading, loginWithGoogle, logout, userData, refreshUserData }}>
      {children}
    </AuthContext.Provider>
  );
};

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
}

export interface UserData {
  uid: string;
  name: string;
  email: string;
  role: string;
  daily_wage: number;
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

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (u) => {
      setUser(u);
      if (u) {
        const ref = doc(db, "users", u.uid);
        const snap = await getDoc(ref);
        if (snap.exists()) {
          setUserData(snap.data() as UserData);
        } else {
          const newUser: UserData = {
            uid: u.uid,
            name: u.displayName || "",
            email: u.email || "",
            role: "user",
            daily_wage: 500,
            language: "en",
          };
          await setDoc(ref, { ...newUser, created_at: serverTimestamp() });
          setUserData(newUser);
        }
      } else {
        setUserData(null);
      }
      setLoading(false);
    });
    return unsub;
  }, []);

  const loginWithGoogle = async () => {
    await signInWithPopup(auth, googleProvider);
  };

  const logout = async () => {
    await signOut(auth);
  };

  return (
    <AuthContext.Provider value={{ user, loading, loginWithGoogle, logout, userData }}>
      {children}
    </AuthContext.Provider>
  );
};

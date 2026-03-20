// mock auth for tests
import { useAuth } from "@/contexts/AuthContext";
import { useEffect } from "react";

export function MockAuth() {
  const { setUser } = useAuth();
  useEffect(() => {
    // Quick and dirty mock for tests
    if (window.location.search.includes('mock=true')) {
       // bypass logic
    }
  }, []);
  return null;
}

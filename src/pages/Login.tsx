import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { Navigate } from "react-router-dom";
import { Briefcase } from "lucide-react";

const Login = () => {
  const { user, loginWithGoogle, loginWithEmail, registerWithEmail, loading } = useAuth();
  const { t } = useLanguage();
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (user) return <Navigate to="/" replace />;

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-6">
      <div className="mb-8 flex flex-col items-center gap-3">
        <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-primary">
          <Briefcase size={40} className="text-primary-foreground" />
        </div>
        <h1 className="text-3xl font-bold text-foreground">{t("appName")}</h1>
        <p className="text-base font-medium text-muted-foreground text-center">
          {t("loginSubtitle")}
        </p>
      </div>

      <button
        onClick={loginWithGoogle}
        className="flex w-full max-w-xs items-center justify-center gap-3 rounded-lg border-2 border-border bg-background px-6 py-4 text-base font-semibold text-foreground shadow-sm transition-all active:scale-95 touch-target"
      >
        <svg width="24" height="24" viewBox="0 0 24 24">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
        </svg>
        {t("loginWith")}
      </button>

      <div className="mt-6 w-full max-w-xs relative flex items-center justify-center">
        <span className="absolute bg-background px-2 text-xs font-medium text-muted-foreground">OR</span>
        <div className="h-px w-full bg-border" />
      </div>

      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setError("");
          setAuthLoading(true);
          try {
            if (isRegistering) {
              await registerWithEmail(email, password, name || email.split("@")[0]);
            } else {
              await loginWithEmail(email, password);
            }
          } catch (err: any) {
            setError(err.message || "An error occurred");
          }
          setAuthLoading(false);
        }}
        className="mt-6 flex w-full max-w-xs flex-col gap-3"
      >
        {isRegistering && (
          <input
            type="text"
            placeholder="Name (optional)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-lg border-2 border-border bg-background px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none"
          />
        )}
        <input
          type="email"
          required
          placeholder="Email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full rounded-lg border-2 border-border bg-background px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none"
        />
        <input
          type="password"
          required
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full rounded-lg border-2 border-border bg-background px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none"
        />
        {error && <p className="text-xs text-destructive text-center">{error}</p>}
        <button
          type="submit"
          disabled={authLoading}
          className="mt-2 w-full rounded-lg bg-primary px-4 py-3 text-base font-semibold text-primary-foreground shadow-sm transition-all active:scale-95 disabled:opacity-70 disabled:active:scale-100"
        >
          {authLoading ? "Loading..." : isRegistering ? "Sign Up" : "Log In"}
        </button>
      </form>

      <div className="mt-4 text-sm text-muted-foreground">
        {isRegistering ? "Already have an account?" : "Don't have an account?"}{" "}
        <button
          onClick={() => {
            setIsRegistering(!isRegistering);
            setError("");
          }}
          className="font-bold text-primary hover:underline"
        >
          {isRegistering ? "Log In" : "Sign Up"}
        </button>
      </div>
    </div>
  );
};

export default Login;

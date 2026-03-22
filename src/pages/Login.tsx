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
    <div className="flex min-h-screen bg-background relative overflow-hidden flex-col lg:flex-row">
      {/* Left side: Minimal Branding */}
      <div className="hidden lg:flex flex-col justify-center items-center w-1/2 p-12 bg-muted/20 relative z-10">
        <div className="max-w-md w-full text-center space-y-6">
          <div className="flex justify-center mb-8">
            <div className="flex h-24 w-24 items-center justify-center rounded-3xl bg-primary text-primary-foreground ">
              <Briefcase size={48} />
            </div>
          </div>
          <h2 className="text-3xl font-black text-foreground">{t("appName")}</h2>
          <p className="text-lg font-medium text-muted-foreground">{t("loginSubtitle")}</p>
        </div>
      </div>

      {/* Right side: Login form */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 sm:px-12 relative z-10 w-full lg:w-1/2">
        <div className="w-full max-w-sm space-y-8 mt-12 lg:mt-0">
          <div className="lg:hidden flex flex-col items-center gap-4 mb-2">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary text-primary-foreground ">
              <Briefcase size={32} />
            </div>
            <div className="text-center">
                <h1 className="text-3xl font-black text-foreground mb-1">{t("appName")}</h1>
                <p className="text-sm font-medium text-muted-foreground">
                {t("loginSubtitle")}
                </p>
            </div>
          </div>

          <div className="hidden lg:block mb-8 text-center lg:text-left">
            <h2 className="text-3xl font-black text-foreground mb-2">{t("welcomeBack")}</h2>
            <p className="text-sm text-muted-foreground font-medium">{t("loginDesc")}</p>
          </div>

          <div className="bg-transparent p-0 space-y-6">
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
                className="flex w-full flex-col gap-4"
            >
                {isRegistering && (
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">{t("fullName")}</label>
                    <input
                        type="text"
                        placeholder="John Doe"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="w-full rounded-2xl border-b-2 border-border bg-transparent px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors "
                    />
                </div>
                )}
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">{t("emailAddress")}</label>
                    <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full rounded-2xl border-b-2 border-border bg-transparent px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors "
                    />
                </div>
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">{t("password")}</label>
                    <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full rounded-2xl border-b-2 border-border bg-transparent px-4 py-3 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors "
                    />
                </div>
                {error && <p className="text-xs text-destructive text-center font-medium bg-destructive/10 p-2 rounded-lg">{error}</p>}
                <button
                type="submit"
                disabled={authLoading}
                className="mt-2 w-full rounded-full bg-primary px-4 py-4 text-sm font-bold text-primary-foreground transition-all active:scale-95 disabled:opacity-70 disabled:pointer-events-none"
                >
                {authLoading ? t("loading") : isRegistering ? t("createAccount") : t("login")}
                </button>
            </form>

            <div className="relative flex items-center justify-center">
                <span className="absolute bg-background px-3 text-[10px] font-bold tracking-wider text-muted-foreground uppercase z-10">{t("or")}</span>
                <div className="h-px w-full bg-border" />
            </div>

            <button
                onClick={loginWithGoogle}
                className="flex w-full items-center justify-center gap-3 rounded-full border border-border bg-transparent hover:bg-muted px-6 py-4 text-sm font-bold text-foreground transition-all active:scale-95 touch-target hover:border-border"
            >
                <svg width="24" height="24" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                </svg>
                {t("signInWithGoogle")}
            </button>
          </div>

          <div className="text-center text-sm font-medium text-muted-foreground">
            {isRegistering ? t("alreadyHaveAccount") : t("dontHaveAccount")}{" "}
            <button
                onClick={() => {
                setIsRegistering(!isRegistering);
                setError("");
                }}
                className="font-black text-primary hover:underline transition-all"
            >
                {isRegistering ? t("login") : t("signUp")}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
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
    <div className="flex min-h-screen bg-background relative overflow-hidden">
      {/* Decorative background blur blobs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/20 rounded-full blur-3xl opacity-50 pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-primary/10 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

      {/* Left side: Illustration for large screens */}
      <div className="hidden lg:flex flex-col justify-center items-center w-1/2 p-12 bg-primary/5 border-r border-border relative z-10">
        <div className="max-w-md w-full text-center space-y-6">
          <div className="flex justify-center mb-8">
            <div className="w-64 h-64 bg-card rounded-full flex items-center justify-center shadow-xl border-4 border-background overflow-hidden relative group">
                <div className="absolute inset-0 bg-primary/10 group-hover:bg-primary/20 transition-colors"></div>
                {/* SVG Illustration - Undraw style representation */}
                <svg width="200" height="200" viewBox="0 0 500 500" className="opacity-90 scale-110">
                    <path fill="currentColor" className="text-primary/20" d="M351.5 249.2c-5.7-18.1-14.7-34.9-26.6-49.8-11.9-14.9-26.4-27.4-42.9-37.1-16.5-9.7-34.7-16.5-53.7-20.2-19-3.7-38.6-4.1-57.9-1.2-19.3 2.9-38.1 8.9-55.5 17.7-17.4 8.8-33.3 20.3-46.8 34-13.5 13.7-24.5 29.5-32.3 46.8-7.8 17.3-12.4 35.8-13.5 54.7-1.1 18.9 1.2 38 6.9 56.1 5.7 18.1 14.7 34.9 26.6 49.8 11.9 14.9 26.4 27.4 42.9 37.1 16.5 9.7 34.7 16.5 53.7 20.2 19 3.7 38.6 4.1 57.9 1.2 19.3-2.9 38.1-8.9 55.5-17.7 17.4-8.8 33.3-20.3 46.8-34 13.5-13.7 24.5-29.5 32.3-46.8 7.8-17.3 12.4-35.8 13.5-54.7 1.1-18.9-1.2-38-6.9-56.1z"></path>
                    <g transform="translate(150, 150)">
                        <rect x="20" y="50" width="160" height="100" rx="10" fill="currentColor" className="text-primary"></rect>
                        <path d="M40 70 L160 70" stroke="white" strokeWidth="4" strokeLinecap="round"></path>
                        <path d="M40 90 L120 90" stroke="white" strokeWidth="4" strokeLinecap="round"></path>
                        <path d="M40 110 L140 110" stroke="white" strokeWidth="4" strokeLinecap="round"></path>
                        <circle cx="150" cy="120" r="15" fill="white" className="opacity-80"></circle>
                    </g>
                    <path fill="currentColor" className="text-primary/40" d="M120 300 Q150 250 200 280 T300 200 T400 250 L400 400 L120 400 Z"></path>
                </svg>
            </div>
          </div>
          <h2 className="text-3xl font-black text-foreground">{t("appName")}</h2>
          <p className="text-lg font-medium text-muted-foreground">{t("loginSubtitle")}</p>
          <div className="flex gap-2 justify-center pt-8">
            <div className="w-2 h-2 rounded-full bg-primary"></div>
            <div className="w-2 h-2 rounded-full bg-primary/30"></div>
            <div className="w-2 h-2 rounded-full bg-primary/30"></div>
          </div>
        </div>
      </div>

      {/* Right side: Login form */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 sm:px-12 relative z-10 w-full lg:w-1/2 bg-background/50 backdrop-blur-sm lg:bg-transparent lg:backdrop-blur-none">
        <div className="w-full max-w-sm space-y-8">
          <div className="lg:hidden flex flex-col items-center gap-4 mb-2">
            <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-primary shadow-xl shadow-primary/20">
              <Briefcase size={36} className="text-primary-foreground" />
            </div>
            <div className="text-center">
                <h1 className="text-3xl font-black text-foreground mb-1">{t("appName")}</h1>
                <p className="text-sm font-medium text-muted-foreground">
                {t("loginSubtitle")}
                </p>
            </div>
          </div>

          <div className="hidden lg:block mb-8 text-center lg:text-left">
            <h2 className="text-3xl font-black text-foreground mb-2">Welcome Back</h2>
            <p className="text-sm text-muted-foreground font-medium">Log in to manage your workers and tasks</p>
          </div>

          <div className="bg-card/80 backdrop-blur-md border border-border shadow-lg rounded-3xl p-6 sm:p-8 space-y-6">
            <button
                onClick={loginWithGoogle}
                className="flex w-full items-center justify-center gap-3 rounded-2xl border-2 border-border/50 bg-background hover:bg-muted px-6 py-4 text-sm font-bold text-foreground shadow-sm transition-all active:scale-95 touch-target hover:border-border"
            >
                <svg width="24" height="24" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                </svg>
                {t("loginWith")}
            </button>

            <div className="relative flex items-center justify-center">
                <span className="absolute bg-card px-3 text-[10px] font-bold tracking-wider text-muted-foreground uppercase z-10">OR</span>
                <div className="h-px w-full bg-border/80" />
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
                className="flex w-full flex-col gap-4"
            >
                {isRegistering && (
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">Full Name</label>
                    <input
                        type="text"
                        placeholder="John Doe"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="w-full rounded-2xl border-2 border-border/50 bg-background px-4 py-3.5 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors"
                    />
                </div>
                )}
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">Email Address</label>
                    <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full rounded-2xl border-2 border-border/50 bg-background px-4 py-3.5 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors"
                    />
                </div>
                <div>
                    <label className="text-xs font-bold text-muted-foreground block mb-1">Password</label>
                    <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full rounded-2xl border-2 border-border/50 bg-background px-4 py-3.5 text-sm font-semibold text-foreground focus:border-primary focus:outline-none transition-colors"
                    />
                </div>
                {error && <p className="text-xs text-destructive text-center font-medium bg-destructive/10 p-2 rounded-lg">{error}</p>}

                <button
                type="submit"
                disabled={authLoading}
                className="mt-2 w-full rounded-2xl bg-primary px-4 py-4 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/30 transition-all active:scale-95 hover:shadow-primary/40 hover:-translate-y-0.5 disabled:opacity-70 disabled:pointer-events-none"
                >
                {authLoading ? "Loading..." : isRegistering ? "Create Account" : "Log In"}
                </button>
            </form>
          </div>

          <div className="text-center text-sm font-medium text-muted-foreground">
            {isRegistering ? "Already have an account?" : "Don't have an account?"}{" "}
            <button
                onClick={() => {
                setIsRegistering(!isRegistering);
                setError("");
                }}
                className="font-black text-primary hover:underline transition-all"
            >
                {isRegistering ? "Log In" : "Sign Up"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
const fs = require('fs');

let code = fs.readFileSync('src/pages/Login.tsx', 'utf-8');

// The error happened because the file actually has a duplicate login component or a missing closing brace before export default. Let's fix this.
// I'll re-read the original file state to know what we have replaced.
// Since the file has 175 lines now and we can see what it is, I can see that `if (loading) { return (` has no closing `}` before the new `return (` block.

let newCode = code.replace(
`  if (loading) {
    return (
    <div className="flex min-h-screen`,
`  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (user) return <Navigate to="/" replace />;

  return (
    <div className="flex min-h-screen`
);

fs.writeFileSync('src/pages/Login.tsx', newCode);

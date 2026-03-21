const fs = require('fs');

let code = fs.readFileSync('src/pages/WorkersPage.tsx', 'utf-8');

// We want to replace the list items.
// Search for `<div className="space-y-2">` and replace its children.
const search = `        ) : (
          <div className="space-y-2">
            {filtered.map((w) => (
              <div
                key={w.id}
                onClick={() => navigate(\`/worker/\${w.id}\`)}
                className="rounded-2xl bg-card border border-border p-4 flex items-center gap-3 cursor-pointer active:scale-[0.98] transition-all hover:shadow-sm"
              >
                <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                  {w.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-bold text-foreground truncate">{w.name}</p>
                  <div className="flex items-center gap-2 text-[10px] text-muted-foreground font-medium mt-0.5">
                    <span className="bg-secondary px-2 py-0.5 rounded-full">{w.workType}</span>
                    <span>•</span>
                    <span className="text-primary font-bold">₹{w.wage}/day</span>
                  </div>
                </div>
                <div className="flex flex-col gap-2 shrink-0">
                  <button
                    onClick={(e) => { e.stopPropagation(); openEdit(w); }}
                    className="h-8 w-8 rounded-full bg-muted flex items-center justify-center active:scale-90"
                  >
                    <Pencil size={14} className="text-foreground" />
                  </button>
                  <button
                    onClick={(e) => { e.stopPropagation(); setShowDelete(w.id); }}
                    className="h-8 w-8 rounded-full bg-destructive/10 flex items-center justify-center active:scale-90"
                  >
                    <Trash2 size={14} className="text-destructive" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}`;

const replace = `        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {filtered.map((w) => (
              <div
                key={w.id}
                onClick={() => navigate(\`/worker/\${w.id}\`)}
                className="rounded-3xl bg-card border-2 border-transparent hover:border-primary/20 p-5 flex items-center gap-4 cursor-pointer active:scale-[0.97] transition-all shadow-sm hover:shadow-md relative overflow-hidden group"
              >
                <div className="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                <div className="h-14 w-14 rounded-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center text-primary font-black text-xl shadow-inner z-10 shrink-0">
                  {w.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0 z-10">
                  <p className="text-base font-bold text-foreground truncate">{w.name}</p>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground font-medium mt-1">
                    <span className="bg-secondary/80 px-2 py-0.5 rounded-md text-foreground">{w.workType}</span>
                    <span className="opacity-50">•</span>
                    <span className="text-primary font-bold bg-primary/10 px-2 py-0.5 rounded-md">₹{w.wage}/day</span>
                  </div>
                </div>
                <div className="flex flex-col gap-2 shrink-0 z-10">
                  <button
                    onClick={(e) => { e.stopPropagation(); openEdit(w); }}
                    className="h-9 w-9 rounded-full bg-background border border-border flex items-center justify-center hover:bg-muted active:scale-90 transition-all shadow-sm"
                  >
                    <Pencil size={16} className="text-foreground" />
                  </button>
                  <button
                    onClick={(e) => { e.stopPropagation(); setShowDelete(w.id); }}
                    className="h-9 w-9 rounded-full bg-destructive/10 border border-destructive/20 flex items-center justify-center hover:bg-destructive hover:text-destructive-foreground active:scale-90 transition-all shadow-sm"
                  >
                    <Trash2 size={16} className="text-destructive" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}`;

code = code.replace(search, replace);
fs.writeFileSync('src/pages/WorkersPage.tsx', code);

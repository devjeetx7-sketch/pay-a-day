const fs = require('fs');
let code = fs.readFileSync('src/pages/WorkersPage.tsx', 'utf-8');

// The prompt also says "Increase padding (16–24dp)". The parent has `max-w-lg`. Let's increase it to `md:max-w-3xl lg:max-w-5xl` to accommodate grid layout.
code = code.replace('<div className="mx-auto max-w-lg px-4 pt-6">', '<div className="mx-auto max-w-lg md:max-w-3xl lg:max-w-5xl px-4 md:px-6 pt-8 pb-12">');
// Header changes.
code = code.replace('<h1 className="text-xl font-bold text-foreground">{t("workers")}</h1>', '<h1 className="text-3xl font-black text-foreground">{t("workers")}</h1>');
code = code.replace('<p className="text-xs text-muted-foreground">{workers.length} {t("totalWorkers")}</p>', '<p className="text-sm font-medium text-muted-foreground mt-1">{workers.length} {t("totalWorkers")}</p>');

fs.writeFileSync('src/pages/WorkersPage.tsx', code);

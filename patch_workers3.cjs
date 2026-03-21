const fs = require('fs');
let code = fs.readFileSync('src/pages/WorkersPage.tsx', 'utf-8');

// For the Worker Home button, "Add subtle press animation (scale effect)" and "Use Material You cards"
const btnSearch = `<button
            onClick={openAdd}
            className="h-10 w-10 rounded-full bg-primary flex items-center justify-center active:scale-90 transition-transform"
          >`;

const btnReplace = `<button
            onClick={openAdd}
            className="h-12 w-12 md:h-14 md:w-14 rounded-2xl bg-primary flex items-center justify-center active:scale-[0.92] transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5"
          >`;

code = code.replace(btnSearch, btnReplace);

// Let's also adjust search input styling.
const searchSearch = `<div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("searchWorkers")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>`;
const searchReplace = `<div className="relative mb-8 shadow-sm">
          <Search size={20} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("searchWorkers")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-12 h-14 rounded-2xl text-base border-2 border-border/50 focus-visible:ring-primary/20 bg-card"
          />
        </div>`;

code = code.replace(searchSearch, searchReplace);

fs.writeFileSync('src/pages/WorkersPage.tsx', code);

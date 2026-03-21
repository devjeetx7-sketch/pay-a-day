const fs = require('fs');

let code = fs.readFileSync('src/pages/SettingsPage.tsx', 'utf-8');

// I'll manually replace the strings to make sure it matches.
code = code.replace("className={`rounded-2xl bg-card border border-border p-5 shadow-sm ${activeTab === 'account' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'account' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border p-5 shadow-sm block md:${activeTab === 'account' ? 'block' : 'hidden'}`}");

code = code.replace("className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 ${activeTab === 'preferences' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'preferences' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 block md:${activeTab === 'preferences' ? 'block' : 'hidden'}`}");

code = code.replace("className={`rounded-2xl bg-card border border-border p-5 shadow-sm ${activeTab === 'role' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'role' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border p-5 shadow-sm block md:${activeTab === 'role' ? 'block' : 'hidden'}`}");

code = code.replace("className={`rounded-2xl bg-card border border-border shadow-md overflow-hidden ${activeTab === 'premium' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'premium' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border shadow-md overflow-hidden block md:${activeTab === 'premium' ? 'block' : 'hidden'}`}");

code = code.replace("className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-4 ${activeTab === 'data' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'data' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-4 block md:${activeTab === 'data' ? 'block' : 'hidden'}`}");

code = code.replace("className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 ${activeTab === 'support' ? 'block' : 'hidden md:hidden'} md:${activeTab === 'support' ? 'block' : 'hidden'}`}",
  "className={`rounded-2xl bg-card border border-border p-5 shadow-sm space-y-5 block md:${activeTab === 'support' ? 'block' : 'hidden'}`}");

fs.writeFileSync('src/pages/SettingsPage.tsx', code);

cat src/pages/SettingsPage.tsx | sed 's/<div className="grid grid-cols-1 md:grid-cols-2 gap-6">/<div className="flex flex-col md:flex-row gap-6">/g' > temp.tsx
mv temp.tsx src/pages/SettingsPage.tsx

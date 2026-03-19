# DailyWork

**DailyWork** is an advanced, dual-mode web application designed to help both **Contractors** and **Individuals** manage their daily work, attendance, earnings, and payments.

Crafted by **Dharmjeet**.

## 🚀 Features

### 🏗️ Contractor Mode
A full-fledged worker management system.
- **Worker Management:** Add, edit, and assign workers to dynamic "Custom Work Types" (e.g., Electrician, Mistri, Painter).
- **Batch Attendance:** Quickly log Present, Absent, Half Days for all active workers via the Contractor Calendar.
- **Financial Passbook:** Track gross earnings and advance payments per worker. Easily view "Net Payable" balances.
- **Global Logs & Stats:** Access comprehensive statistics showing total man days, aggregated labor cost, and detailed historical logs across the entire workforce.
- **PDF & WhatsApp Integration:** Instantly generate professional "Worker Passbooks" as PDF receipts or share them directly via a formatted WhatsApp message.

### 👤 Personal Mode
A dedicated dashboard for tracking your own self-employed daily wage.
- **Daily Check-ins:** Mark your own attendance directly from the dashboard, tracking overtime hours, specific absence reasons, and daily notes.
- **Payment Log:** Document your incoming payments and track remaining balance based on a customizable default daily wage.
- **Analytics:** View total earned, pending balance, and monthly summaries via the Personal Dashboard and Stats page.
- **Self PDF Export:** Export your personal monthly work log to PDF to maintain an accurate ledger.

## 🛠️ Tech Stack

- **Frontend:** React, TypeScript, Vite, Tailwind CSS, Shadcn UI
- **Backend & Auth:** Firebase Firestore, Firebase Authentication (Google Sign-In)
- **Data Visualizations:** Recharts
- **PDF Generation:** jsPDF & jspdf-autotable

## ⚙️ Getting Started

1. Clone the repository
2. Install dependencies: `npm install` or `bun install`
3. Setup your `.env` file referencing `.env.example` to provide your Firebase configuration.
4. Run the development server: `npm run dev`

---
*Built with ❤️ by Dharmjeet*

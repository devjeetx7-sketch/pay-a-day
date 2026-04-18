import re

strings = ['1. Marking Attendance', '2. Net Payable & Earnings', '3. Advance Payments', '4. Calendar & History', 'A quick guide to tracking your work effectively', 'ABSENT', 'ADVANCE DEDUCTIONS', 'Aadhaar Number (Optional)', 'Aadhaar must be exactly 12 digits', 'Absent', 'Access to your data is restricted to your authenticated account.', 'Add Advance Payment', 'Add Note (Optional)', 'Add Note...', 'Add Worker', 'Advance', 'Advance Payment (₹)', 'Age', 'All Time Stats', 'Amount', 'App Preferences', 'Apply', 'Are you sure you want to log out of your account?', 'Are you sure you want to mark yourself absent for today?', 'Are you sure you want to permanently delete this worker? This action cannot be undone.', 'Attendance', 'Attendance & Payment logs', 'Attendance Breakdown', 'Attendance Rate', 'Calendar', 'Call', 'Cancel', 'Cancel anytime. No hidden fees.', 'Change Role', 'Changing role will refresh the app and switch your dashboard.', 'Close App', 'Combo Premium', 'Confirm', 'Continue', 'Continue Offline', 'Copy Error', 'Cost Breakdown', 'Custom Work Type *', 'DAILY LOG', 'Daily Costs', 'Daily Earnings', 'Daily Wage', 'Daily Wage *', 'Daily attendance for all workers', 'Daily attendance reminder', 'DailyWork', 'DailyWork Premium', 'Data & Export', 'Data & Privacy Policy', 'Data & Support', 'Data Collection', 'Delete', 'Delete Worker', 'Detailed Ledger', 'Developer Contact', 'EARNINGS', 'Edit', 'Email', 'Enter valid 10-digit mobile number', 'Export PDF', 'Export as PDF', 'FEATURES', 'FREE', 'Filter:', 'Financial Summary', 'Full Day', 'Full Name', 'Full Name *', 'Get Combo Plan', 'Global Statistics', 'Gross Earned', 'HALF', 'Half Day', 'How to Use DailyWork', 'How will you use this app?', 'If you receive money ahead of time, click Add Advance on the Dashboard or add it directly on a specific date inside the Calendar. This is automatically deducted.', 'Language', 'Log Out', 'MAN DAYS', 'Manage Workers', 'Manage your daily labor & earnings', 'Mark Absent', 'Mark Attendance', 'Mark Today', 'Monthly Earnings', 'Must be 14+', 'Net Payable', 'No Internet Connection', 'No data.', 'No records found', 'No records found for this month.', 'No workers found. Add workers first.', 'OR', 'OT Amount (₹)', "On the Dashboard, click Full Day or Half Day to mark today's attendance. Add overtime using the + / - buttons before saving. If you didn't work, click Mark Absent.", 'Only Contractor Premium', 'Overtime (Hours)', 'PREMIUM', 'PRESENT', 'Passbook Month', 'Phone', 'Phone Number', 'Present', 'Profile Settings', 'QUICK ACTIONS', 'Quick guide & tutorials', 'Reach out for feedback', 'Reach out to us for any feedback or support', 'Reminders', 'Remove', 'Remove Attendance', 'Restart', 'Role Management', 'Save', 'Scan Aadhaar QR Code', 'Search by name, phone or role...', 'Search by worker name...', 'Security Practices', 'Select Role', 'Select your billing cycle', 'Select your preferred language', 'Send digital passbook', 'Settings', 'Share via WhatsApp', 'Statistics', 'Stats', 'Storage Method', 'Stored securely in cloud', 'Stored securely in the cloud', 'Switch between Contractor and Personal mode', 'System Overview', 'TOTAL DAYS', 'TOTAL EARNINGS', 'TOTAL OVERTIME', 'Tap any day to edit attendance or add note/advance.', 'Tap to edit attendance or add note/advance.', 'Thank you for your purchase ❤️', 'Theme', "Today's Earnings", 'Total Advance', 'Total Man Days', 'Total Workforce', 'Try Again', 'UNLOCK EVERYTHING', 'Unlock all features', 'Unlocks both Contractor & Personal Modes permanently.', 'Upgrade to Premium', 'Use the Calendar to edit past records (e.g. if you forgot to mark attendance yesterday). Use Passbook to export your monthly logs as a PDF or CSV.', 'User Control', 'View My Passbook', 'We collect your name, phone number, and attendance records to provide the core functionality of the app.', 'WhatsApp', 'Worker History', 'Worker performance & costs', 'You can delete your data by deleting your account, or export your records from the app.', "You're currently offline. You can still use the app in offline mode. Your data will sync automatically when connection is restored.", 'Your Earnings are automatically calculated by multiplying your working days with your Daily Wage. Net Payable shows your final take-home amount: (Total Earnings - Advance).', 'Your data is securely stored on Firebase, protected by industry-standard security measures.', 'support@dailywork.com', 'www.dailywork.com', '₹']

def generate_key(s):
    # simple conversion to snake case
    s = re.sub(r'[^a-zA-Z0-9\s]', '', s)
    s = s.strip()
    s = re.sub(r'\s+', '_', s).lower()
    if len(s) == 0:
        return 'rupee_symbol' if '₹' in s else 'blank'
    if len(s) > 40:
        return s[:40] + "_msg"
    return s

import sys
d = {}
for s in strings:
    if len(s.strip()) == 0 or s == '₹': continue
    k = generate_key(s)
    # handle duplicates
    orig_k = k
    idx = 1
    while k in d:
        k = f"{orig_k}_{idx}"
        idx += 1
    d[k] = s

# print output for strings.xml format
print("<resources>")
for k, v in d.items():
    # escape apostrophe and ampersand
    v = v.replace("'", "\\'").replace("&", "&amp;")
    print(f'    <string name="{k}">{v}</string>')
print("</resources>")

with open("android-app/app/src/main/java/com/dailywork/attedance/DailyWorkApplication.kt", "r") as f:
    content = f.read()

content = content.replace("    private fun setupCrashHandler()\n        setupWorkManager() {", "    private fun setupCrashHandler() {\n        setupWorkManager()")

with open("android-app/app/src/main/java/com/dailywork/attedance/DailyWorkApplication.kt", "w") as f:
    f.write(content)

with open("android-app/app/src/main/java/com/dailywork/attedance/DailyWorkApplication.kt", "r") as f:
    content = f.read()

import_lines = """import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.dailywork.attedance.worker.SyncWorker
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
"""
if "import androidx.work.PeriodicWorkRequestBuilder" not in content:
    content = content.replace("import android.content.Context", "import android.content.Context\n" + import_lines)

workmanager_method = """
    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
"""

if "setupWorkManager()" not in content:
    content = content.replace("        setupCrashHandler()", "        setupCrashHandler()\n        setupWorkManager()")

if "private fun setupWorkManager()" not in content:
    # Safely insert the method before the last brace
    last_brace_idx = content.rfind("}")
    content = content[:last_brace_idx] + workmanager_method + "\n" + content[last_brace_idx:]

with open("android-app/app/src/main/java/com/dailywork/attedance/DailyWorkApplication.kt", "w") as f:
    f.write(content)

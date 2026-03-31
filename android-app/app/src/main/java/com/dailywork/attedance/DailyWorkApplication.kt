package com.dailywork.attedance

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import com.dailywork.attedance.ui.CrashActivity

class DailyWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("DailyWorkApp", "Uncaught exception", exception)

            val stackTrace = Log.getStackTraceString(exception)

            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_ERROR_DETAILS, stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            startActivity(intent)

            Process.killProcess(Process.myPid())
            System.exit(10)
        }
    }
}

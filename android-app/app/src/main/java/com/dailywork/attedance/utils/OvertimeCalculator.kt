package com.dailywork.attedance.utils

object OvertimeCalculator {
    /**
     * Calculates overtime amount based on daily wage and overtime hours.
     * Logic: (Daily Wage / 8 hours) * Overtime Hours
     */
    fun calculateOvertimeAmount(dailyWage: Double, overtimeHours: Int): Double {
        if (overtimeHours <= 0) return 0.0
        val hourlyRate = dailyWage / 8.0
        return hourlyRate * overtimeHours
    }
}

package com.dailywork.attedance.utils

object OvertimeWageParser {
    private val REGEX = Regex("\\[OT_WAGE_(\\d+\\.?\\d*)\\]")

    fun extractWage(note: String?): Double? {
        if (note == null) return null
        return REGEX.find(note)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun cleanNote(note: String?): String? {
        if (note == null) return null
        val cleaned = note.replace(REGEX, "").trim()
        return if (cleaned.isEmpty()) null else cleaned
    }

    fun appendWage(note: String?, wage: Double?): String? {
        val cleaned = cleanNote(note) ?: ""
        if (wage == null) return if (cleaned.isEmpty()) null else cleaned
        return if (cleaned.isEmpty()) "[OT_WAGE_$wage]" else "$cleaned [OT_WAGE_$wage]"
    }
}

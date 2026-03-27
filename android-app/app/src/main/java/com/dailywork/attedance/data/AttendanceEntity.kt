package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey
    val id: String, // format: {userId}_{date} or {userId}_{date}_advance
    val userId: String, // Either the main user's uid or worker_id
    val contractorId: String?, // Only if it's a worker's attendance under a contractor
    val date: String, // YYYY-MM-DD
    val monthId: String, // YYYY-MM (for partitioned queries)
    val status: String, // present, absent, advance
    val type: String?, // full, half
    val reason: String?,
    val overtimeHours: Int?,
    val note: String?,
    val advanceAmount: Double?,
    val timestamp: Long,
    val isSyncing: Boolean = false // Flag for optimistic UI
)

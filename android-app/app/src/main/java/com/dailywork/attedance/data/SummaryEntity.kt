package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary")
data class SummaryEntity(
    @PrimaryKey
    val userId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val totalExpenses: Double,
    val lastUpdated: Long
)

package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val monthId: String, // format YYYY_MM
    val title: String,
    val status: String,
    val timestamp: Long,
    val categoryId: String,
    val isSyncing: Boolean = false // Flag for optimistic UI
)

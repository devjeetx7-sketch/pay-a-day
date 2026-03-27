package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val monthId: String, // format YYYY_MM
    val amount: Double,
    val categoryId: String,
    val timestamp: Long,
    val isSyncing: Boolean = false // Flag for optimistic UI
)

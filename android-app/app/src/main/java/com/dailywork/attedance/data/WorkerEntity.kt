package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val id: String, // The worker's ID in firestore
    val contractorId: String,
    val name: String,
    val phone: String,
    val aadhar: String,
    val age: String,
    val workType: String,
    val wage: Double,
    val timestamp: Long,
    val isSyncing: Boolean = false // Flag for optimistic UI
)

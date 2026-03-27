package com.dailywork.attedance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String?,
    val role: String,
    val dailyWage: Double,
    val workType: String?,
    val profileImageUrl: String?,
    val isPremium: Boolean,
    val createdAt: Long,
    val isSyncing: Boolean = false
)

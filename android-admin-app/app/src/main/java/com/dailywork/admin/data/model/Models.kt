package com.dailywork.admin.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // contractor, personal
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val lastActive: Long = 0L,
    val createdAt: Long = 0L
)

data class AppConfig(
    val languageEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val roleUiEnabled: Boolean = true
)

data class AdminStats(
    val totalUsers: Int = 0,
    val contractorCount: Int = 0,
    val personalCount: Int = 0,
    val blockedCount: Int = 0
)

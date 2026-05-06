package com.dailywork.admin.data.repository

import com.dailywork.admin.data.model.AppConfig
import com.dailywork.admin.data.model.User
import com.dailywork.admin.data.model.AdminStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminFirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getUsers(role: String): Flow<List<User>> = callbackFlow {
        val subscription = db.collection("users")
            .whereEqualTo("role", role)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun setUserBlockedStatus(userId: String, isBlocked: Boolean) {
        db.collection("users").document(userId)
            .update("isBlocked", isBlocked)
            .await()
    }

    fun getAdminStats(): Flow<AdminStats> = callbackFlow {
        val subscription = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
                val stats = AdminStats(
                    totalUsers = users.size,
                    contractorCount = users.count { it.role == "contractor" },
                    personalCount = users.count { it.role == "personal" },
                    blockedCount = users.count { it.isBlocked }
                )
                trySend(stats)
            }
        awaitClose { subscription.remove() }
    }

    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val subscription = db.collection("config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val config = snapshot?.toObject(AppConfig::class.java) ?: AppConfig()
                trySend(config)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateAppConfig(config: AppConfig) {
        db.collection("config").document("settings")
            .set(config)
            .await()
    }
}

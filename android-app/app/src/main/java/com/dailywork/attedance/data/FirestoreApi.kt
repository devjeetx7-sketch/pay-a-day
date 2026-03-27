package com.dailywork.attedance.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreApi {
    private val db = FirebaseFirestore.getInstance()

    // --- User ---
    suspend fun fetchUser(userId: String): UserEntity? {
        val doc = db.collection("users").document(userId).get().await()
        if (!doc.exists()) return null
        return UserEntity(
            id = doc.id,
            name = doc.getString("name") ?: "",
            phone = doc.getString("phone"),
            role = doc.getString("role") ?: "",
            dailyWage = doc.getDouble("daily_wage") ?: 500.0,
            workType = doc.getString("workType"),
            profileImageUrl = doc.getString("profileImageUrl"),
            isPremium = doc.getBoolean("isPremium") ?: false,
            createdAt = doc.getTimestamp("created_at")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    // --- Tasks ---
    suspend fun fetchTasks(userId: String, monthId: String): List<TaskEntity> {
        val snapshot = db.collection("users").document(userId)
            .collection("tasks_$monthId")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        return snapshot.documents.mapNotNull { doc ->
            TaskEntity(
                id = doc.id,
                userId = userId,
                monthId = monthId,
                title = doc.getString("title") ?: "",
                status = doc.getString("status") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                categoryId = doc.getString("categoryId") ?: ""
            )
        }
    }

    suspend fun saveTask(task: TaskEntity) {
        val data = hashMapOf(
            "title" to task.title,
            "status" to task.status,
            "timestamp" to task.timestamp,
            "categoryId" to task.categoryId
        )
        db.collection("users").document(task.userId)
            .collection("tasks_${task.monthId}").document(task.id)
            .set(data, SetOptions.merge()).await()
    }

    // --- Summary ---
    suspend fun fetchSummary(userId: String): SummaryEntity? {
        val doc = db.collection("users").document(userId)
            .collection("summary").document("dashboard")
            .get().await()

        if (!doc.exists()) return null

        return SummaryEntity(
            userId = userId,
            totalTasks = doc.getLong("totalTasks")?.toInt() ?: 0,
            completedTasks = doc.getLong("completedTasks")?.toInt() ?: 0,
            pendingTasks = doc.getLong("pendingTasks")?.toInt() ?: 0,
            totalExpenses = doc.getDouble("totalExpenses") ?: 0.0,
            lastUpdated = doc.getLong("lastUpdated") ?: 0L
        )
    }

    // --- Attendance ---
    suspend fun fetchAttendance(userId: String, monthId: String, isContractor: Boolean = false): List<AttendanceEntity> {
        val snapshot = db.collection("users").document(userId)
            .collection("attendance_$monthId")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()

        return snapshot.documents.mapNotNull { doc ->
            AttendanceEntity(
                id = doc.id,
                userId = doc.getString("user_id") ?: "",
                contractorId = doc.getString("contractorId"),
                date = doc.getString("date") ?: "",
                monthId = monthId,
                status = doc.getString("status") ?: "",
                type = doc.getString("type"),
                reason = doc.getString("reason"),
                overtimeHours = doc.getLong("overtime_hours")?.toInt(),
                note = doc.getString("note"),
                advanceAmount = doc.getDouble("advance_amount"),
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }
    }

    suspend fun saveAttendance(attendance: AttendanceEntity) {
        val data = hashMapOf<String, Any>(
            "user_id" to attendance.userId,
            "date" to attendance.date,
            "status" to attendance.status,
            "timestamp" to attendance.timestamp
        )

        attendance.contractorId?.let { data["contractorId"] = it }
        attendance.type?.let { data["type"] = it }
        attendance.reason?.let { data["reason"] = it }
        attendance.overtimeHours?.let { data["overtime_hours"] = it }
        attendance.note?.let { data["note"] = it }
        attendance.advanceAmount?.let { data["advance_amount"] = it }

        db.collection("users").document(attendance.contractorId ?: attendance.userId)
            .collection("attendance_${attendance.monthId}").document(attendance.id)
            .set(data, SetOptions.merge()).await()
    }

    suspend fun deleteAttendance(userId: String, monthId: String, docId: String) {
        db.collection("users").document(userId)
            .collection("attendance_$monthId").document(docId)
            .delete().await()
    }

    // --- Workers ---
    suspend fun fetchWorkers(contractorId: String): List<WorkerEntity> {
        val snapshot = db.collection("users").document(contractorId)
            .collection("workers")
            .get().await()

        return snapshot.documents.mapNotNull { doc ->
            WorkerEntity(
                id = doc.id,
                contractorId = contractorId,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: "",
                aadhar = doc.getString("aadhar") ?: "",
                age = doc.getString("age") ?: "",
                workType = doc.getString("workType") ?: "",
                wage = doc.getDouble("wage") ?: 0.0,
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }
    }

    suspend fun saveWorker(worker: WorkerEntity) {
        val data = hashMapOf(
            "name" to worker.name,
            "phone" to worker.phone,
            "aadhar" to worker.aadhar,
            "age" to worker.age,
            "workType" to worker.workType,
            "wage" to worker.wage,
            "timestamp" to worker.timestamp
        )

        db.collection("users").document(worker.contractorId)
            .collection("workers").document(worker.id)
            .set(data, SetOptions.merge()).await()
    }

    suspend fun deleteWorker(workerId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("workers").document(workerId)
            .delete().await()
    }
}

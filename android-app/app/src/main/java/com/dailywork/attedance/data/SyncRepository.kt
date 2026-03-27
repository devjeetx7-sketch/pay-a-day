package com.dailywork.attedance.data

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SyncRepository(
    private val database: AppDatabase,
    private val firestoreApi: FirestoreApi
) {
    // --- User ---
    fun getUserFlow(userId: String): Flow<UserEntity?> {
        return database.userDao().getUserFlow(userId)
    }

    suspend fun syncUser(userId: String) {
        try {
            val user = firestoreApi.fetchUser(userId)
            if (user != null) {
                database.userDao().insert(user)
            }
        } catch (e: Exception) {}
    }

    // --- Summary ---
    fun getSummaryFlow(userId: String): Flow<SummaryEntity?> {
        return database.summaryDao().getSummaryFlow(userId)
    }

    suspend fun syncSummary(userId: String) {
        try {
            val summary = firestoreApi.fetchSummary(userId)
            if (summary != null) {
                database.summaryDao().insert(summary)
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    // --- Tasks ---
    fun getTasksFlow(userId: String, monthId: String): Flow<List<TaskEntity>> {
        return database.taskDao().getTasksFlow(userId, monthId)
    }

    suspend fun syncTasks(userId: String, monthId: String) {
        try {
            val networkTasks = firestoreApi.fetchTasks(userId, monthId)
            database.taskDao().insertAll(networkTasks)
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    suspend fun createTaskOptimistically(userId: String, monthId: String, title: String, categoryId: String) {
        val taskId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val task = TaskEntity(
            id = taskId,
            userId = userId,
            monthId = monthId,
            title = title,
            status = "PENDING",
            timestamp = timestamp,
            categoryId = categoryId,
            isSyncing = true
        )

        // Optimistic UI update via Room
        database.taskDao().insert(task)

        try {
            firestoreApi.saveTask(task.copy(isSyncing = false))
            database.taskDao().insert(task.copy(isSyncing = false)) // confirm sync
        } catch (e: Exception) {
            // Revert or show error indicator
        }
    }

    // --- Attendance ---
    fun getPersonalAttendanceFlow(userId: String, monthId: String): Flow<List<AttendanceEntity>> {
        return database.attendanceDao().getPersonalAttendanceFlow(userId, monthId)
    }

    fun getPersonalAttendancePagingSource(userId: String, monthId: String): PagingSource<Int, AttendanceEntity> {
        return database.attendanceDao().getPersonalAttendancePagingSource(userId, monthId)
    }

    fun getContractorAttendanceFlow(contractorId: String, monthId: String): Flow<List<AttendanceEntity>> {
        return database.attendanceDao().getContractorAttendanceFlow(contractorId, monthId)
    }

    fun getWorkerAttendancePagingSource(contractorId: String, workerUserId: String, monthId: String): PagingSource<Int, AttendanceEntity> {
        return database.attendanceDao().getWorkerAttendancePagingSource(contractorId, workerUserId, monthId)
    }

    // Stats Aggregations
    fun getPersonalStatusCountFlow(userId: String, monthId: String, status: String, type: String? = null): Flow<Int> {
        return database.attendanceDao().getPersonalStatusCountFlow(userId, monthId, status, type)
    }

    fun getPersonalAdvanceSumFlow(userId: String, monthId: String): Flow<Double?> {
        return database.attendanceDao().getPersonalAdvanceSumFlow(userId, monthId)
    }

    fun getPersonalOvertimeSumFlow(userId: String, monthId: String): Flow<Int?> {
        return database.attendanceDao().getPersonalOvertimeSumFlow(userId, monthId)
    }

    fun getWorkerStatusCountFlow(contractorId: String, workerUserId: String, monthId: String, status: String, type: String? = null): Flow<Int> {
        return database.attendanceDao().getWorkerStatusCountFlow(contractorId, workerUserId, monthId, status, type)
    }

    fun getWorkerAdvanceSumFlow(contractorId: String, workerUserId: String, monthId: String): Flow<Double?> {
        return database.attendanceDao().getWorkerAdvanceSumFlow(contractorId, workerUserId, monthId)
    }

    fun getPersonalAllTimePresentFlow(userId: String): Flow<Int> {
        return database.attendanceDao().getPersonalAllTimePresentFlow(userId)
    }

    fun getPersonalAllTimeHalfFlow(userId: String): Flow<Int> {
        return database.attendanceDao().getPersonalAllTimeHalfFlow(userId)
    }

    suspend fun syncAttendance(userId: String, monthId: String, isContractor: Boolean = false) {
        try {
            val networkAttendance = firestoreApi.fetchAttendance(userId, monthId, isContractor)
            database.attendanceDao().insertAll(networkAttendance)
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun markAttendanceOptimistically(attendance: AttendanceEntity) {
        // Optimistic UI update via Room
        database.attendanceDao().insert(attendance.copy(isSyncing = true))

        try {
            firestoreApi.saveAttendance(attendance)
            database.attendanceDao().insert(attendance.copy(isSyncing = false)) // confirm sync
        } catch (e: Exception) {
            // Revert
            database.attendanceDao().deleteById(attendance.id)
        }
    }

    suspend fun deleteAttendanceOptimistically(userId: String, monthId: String, docId: String) {
        try {
            database.attendanceDao().deleteById(docId)
            firestoreApi.deleteAttendance(userId, monthId, docId)
        } catch (e: Exception) {
            // Error handling
        }
    }

    // --- Workers ---
    fun getWorkersFlow(contractorId: String): Flow<List<WorkerEntity>> {
        return database.workerDao().getWorkersFlow(contractorId)
    }

    suspend fun syncWorkers(contractorId: String) {
        try {
            val workers = firestoreApi.fetchWorkers(contractorId)
            database.workerDao().insertAll(workers)
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun saveWorkerOptimistically(worker: WorkerEntity) {
        database.workerDao().insert(worker.copy(isSyncing = true))
        try {
            firestoreApi.saveWorker(worker)
            database.workerDao().insert(worker.copy(isSyncing = false))
        } catch (e: Exception) {
            // Error handling
        }
    }

    suspend fun deleteWorkerOptimistically(workerId: String) {
        try {
            database.workerDao().deleteById(workerId)
            firestoreApi.deleteWorker(workerId)
        } catch (e: Exception) {}
    }
}
